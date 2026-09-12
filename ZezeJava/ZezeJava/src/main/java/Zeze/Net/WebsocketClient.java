package Zeze.Net;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebsocketClient extends AsyncSocket {
	private static final @NotNull Logger logger = LogManager.getLogger(WebsocketClient.class);
	private static final @NotNull VarHandle closedHandle;

	private volatile @Nullable WebSocket webSocket;
	private final @NotNull HttpClient httpClient;
	private final @Nullable TimeThrottle timeThrottle;
	private final @NotNull SocketAddress remote;
	private final @Nullable Connector connector;
	@SuppressWarnings("unused")
	private byte closed;

	static {
		try {
			var lookup = MethodHandles.lookup();
			closedHandle = lookup.findVarHandle(WebsocketClient.class, "closed", byte.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public WebsocketClient(@NotNull Service service, @NotNull String wsUrl, @Nullable Object userState,
						   @Nullable Connector connector) {
		super(service);
		super.userState = userState;
		this.connector = connector;
		var uri = URI.create(wsUrl);
		remote = new InetSocketAddress(uri.getHost(), uri.getPort());
		timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		httpClient = HttpClient.newHttpClient();
		httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
			final @NotNull Zeze.Serialize.ByteBuffer input = Zeze.Serialize.ByteBuffer.Allocate();

			@Override
			public void onOpen(WebSocket webSocket) {
				// 已知接受的残余竞态：close()恰在本检查与addSocket之间完整执行完时，
				// 其socketMap.remove因条目尚未注册而空转，随后addSocket留下closed=1的
				// 僵尸条目（有界：泄漏至Service对象废弃；该socket的OnSocketClose已随
				// close发出过一次）。触发需stop与握手完成微秒级精确交错。不修的原因：
				// synchronized(this)是公共对象monitor且持锁跨用户回调，锁序风险不可审计；
				// 事后补调OnSocketClose则破坏"恰好一次"契约（调用方清理按一次编写）。
				if (isClosed()) { // 关闭先于握手完成（如Connector.stop）时废弃迟到的连接
					webSocket.abort();
					return;
				}
				webSocket.request(1);
				WebsocketClient.this.webSocket = webSocket;
				// FND4-35补连接成功钩子：url型Connector依赖Connector.OnSocketConnected置
				// isConnected=true并回落重连退避，缺调则isConnected恒false、退避封顶后永不回落。
				// 注意不能改调Service.OnSocketConnected：HandshakeClient/HandshakeBoth家族覆写
				// 该方法为"仅addSocket、推迟OnHandshakeDone"（等应用层握手协议完成），而websocket
				// 的HTTP升级本身就是握手完成，必须直调OnHandshakeDone——经OnSocketConnected会令
				// 握手永不完成，Connector.WaitReady挂死（4d563735e引入、回归修正）。
				if (connector != null)
					connector.OnSocketConnected(WebsocketClient.this);
				service.addSocket(WebsocketClient.this);
				try {
					service.OnHandshakeDone(WebsocketClient.this);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
				webSocket.request(1);
				var n = data.remaining();
				input.EnsureWrite(n);
				data.get(input.Bytes, input.WriteIndex, n);
				input.WriteIndex += n;
				try {
					service.OnSocketProcessInputBuffer(WebsocketClient.this, input);
					input.Compact();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}

			@Override
			public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
				var ex = new Exception("peer closed. status=" + statusCode + " reason=" + reason);
				WebsocketClient.this.close(ex);
				return null;
			}

			@Override
			public void onError(WebSocket webSocket, Throwable error) {
				WebsocketClient.this.close(error);
			}
		}).whenComplete((webSocket, ex) -> {
			// 握手失败时future以异常完成，必须close走OnSocketClose，否则Connector永远收不到通知
			if (ex != null)
				close(unwrap(ex));
		});
	}

	@Override
	public Type getType() {
		return Type.eClient;
	}

	@Override
	public @Nullable Connector getConnector() {
		return connector;
	}

	@Override
	public boolean close(@Nullable Throwable ex, boolean gracefully) {
		if (!closedHandle.compareAndSet(this, (byte)0, (byte)1)) // 阻止递归关闭
			return false;

		if (ex != null) {
			if (ex instanceof IOException)
				logger.info("close: {} {}", this, ex);
			else
				logger.warn("close: {} exception:", this, ex);
		} else
			logger.info("close: {}{}", this, gracefully ? " gracefully" : "");

		if (connector != null) { // 对齐TcpSocket：先通知Connector安排重连，再通知Service
			try {
				connector.OnSocketClose(this, ex);
			} catch (Exception e) {
				logger.error("Connector.OnSocketClose exception:", e);
			}
		}
		try {
			getService().OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("OnSocketClose", e);
		}

		if (timeThrottle != null)
			timeThrottle.close();
		try {
			httpClient.shutdownNow(); // 释放HttpClient的selector线程与executor
		} catch (Exception e) {
			logger.warn("httpClient.shutdownNow exception:", e);
		}
		var ws = webSocket;
		webSocket = null; // 关闭后Send必须返回false：已abort连接上的帧不得被报告"发送成功"
		if (ws != null) {
			ws.abort();
		}
		return true; // 对齐TcpSocket/Websocket家族：本次调用完成了关闭
	}

	// whenComplete/exceptionNow 交付的异常可能被 CompletionException 包装，关闭日志取根因
	private static @NotNull Throwable unwrap(@NotNull Throwable ex) {
		return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
	}

	@Override
	public boolean Send(byte @NotNull [] bytes, int offset, int length) {
		var ws = webSocket;
		if (ws == null) // 握手未完成或已关闭
			return false;
		// 检查写回执，不能恒返回true：写失败的帧（连接已关等）不会到达对端，恒true会让
		// Protocol/Rpc.Send误判成功、请求静默丢失只能等超时兜底。对齐服务端Websocket.Send
		// （FND3-24）的形态：回执同步完成时失败立即close并返回false；异步完成挂whenComplete，
		// 失败同样close。close的closedHandle CAS保证OnSocketClose等清理恰好一次。
		var cf = ws.sendBinary(ByteBuffer.wrap(bytes, offset, length), true);
		if (cf.isDone()) {
			if (!cf.isCompletedExceptionally())
				return true;
			close(unwrap(cf.exceptionNow()));
			return false;
		}
		cf.whenComplete((__, ex) -> {
			if (ex != null)
				close(unwrap(ex));
		});
		return true;
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return timeThrottle;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		return remote;
	}

	@Override
	public boolean isClosed() {
		return closed != 0;
	}
}
