package Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebsocketClient extends AsyncSocket {
	private static final @NotNull Logger logger = LogManager.getLogger(WebsocketClient.class);

	private volatile @Nullable WebSocket webSocket;
	private final @NotNull HttpClient httpClient;
	private final @Nullable TimeThrottle timeThrottle;
	private final @NotNull SocketAddress remote;
	private final @Nullable Connector connector;

	// sendBinary串行化（JDK WebSocketImpl单在途约束）：专用锁对象，不用公共monitor
	//（见onOpen注释的锁序顾虑；这里只在追加链节点时短暂持有，不跨用户回调）。
	private final @NotNull Object sendLock = new Object();
	private @NotNull CompletableFuture<Void> sendChain = CompletableFuture.completedFuture(null); // sendLock守护

	public WebsocketClient(@NotNull Service service, @NotNull String wsUrl, @Nullable Object userState,
						   @Nullable Connector connector) {
		super(service);
		super.userState = userState;
		this.connector = connector;
		var uri = URI.create(wsUrl);
		// createUnresolved：构造非阻塞（Connector锁内调用前提），解析由buildAsync异步进行；remote仅日志用
		remote = InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort());
		timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		httpClient = HttpClient.newHttpClient();
		httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
			final @NotNull Zeze.Serialize.ByteBuffer input = Zeze.Serialize.ByteBuffer.Allocate();

			@Override
			public void onOpen(WebSocket webSocket) {
				// 残余竞态：close()恰在本检查与tryAccept之间完整执行完时，迟到的登记由
				// Service.addSocket在置死互斥下拒绝（不入表、返回false，不再回调
				// OnHandshakeDone）。本检查仍保留：省去对死连接的request与限流检查动作。
				// 不在此补调OnSocketClose：破坏"恰好一次"契约（调用方清理按一次编写）。
				if (isClosed()) { // 关闭先于握手完成（如Connector.stop）时废弃迟到的连接
					webSocket.abort();
					return;
				}
				webSocket.request(1);
				WebsocketClient.this.webSocket = webSocket;
				// FND7-63：HTTP升级即握手完成，连接已建立，纳入KeepAlive管理（对齐TcpSocket连接
				// 成功时机）；addSocket前reset，checkKeepAlive不会观察到未管理的条目。
				resetActiveSendRecvTime();
				// FND4-35补连接成功钩子：url型Connector依赖Connector.OnSocketConnected置
				// isConnected=true并回落重连退避，缺调则isConnected恒false、退避封顶后永不回落。
				// 注意不能改调Service.OnSocketConnected：HandshakeClient/HandshakeBoth家族覆写
				// 该方法为"仅addSocket、推迟OnHandshakeDone"（等应用层握手协议完成），而websocket
				// 的HTTP升级本身就是握手完成，必须直调OnHandshakeDone——经OnSocketConnected会令
				// 握手永不完成，Connector.WaitReady挂死（4d563735e引入、回归修正）。
				if (connector != null)
					connector.OnSocketConnected(WebsocketClient.this);
				// FND8-55：限流+注册+握手完成统一走tryAccept；超限显式关闭（不走JDK回调异常链），
				// 撞号（addSocket false，连接已被关闭）不再回调OnHandshakeDone。
				try {
					service.tryAccept(WebsocketClient.this);
				} catch (IllegalStateException e) { // too many connections
					WebsocketClient.this.close(e);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
				setActiveRecvTime(); // FND7-63：维护活跃时间，checkKeepAlive才能回收静默死链
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
	protected void doClose(@Nullable Throwable ex, boolean gracefully) {
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
		fireOnSocketDisposed(); // 对齐TcpSocket/Websocket家族：本次调用完成了关闭
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
		setActiveSendTime(); // FND7-63：维护活跃时间（发送已被接受，直接发或按序入队）
		var bb = ByteBuffer.wrap(bytes, offset, length);
		synchronized (sendLock) {
			if (sendChain.isDone()) {
				// 空闲：直接发送，保留FND3-24同步失败契约（sendBinary异常完成时close并返回false）。
				// 链空闲==无在途sendBinary（所有发送都经本链），不会触发JDK单在途约束。
				var cf = ws.sendBinary(bb, true);
				sendChain = cf.handle((__, ex) -> {
					if (ex != null)
						close(unwrap(ex)); // 异步完成的失败同样close
					return null; // 链恢复正常完成，后续追加不连带失败
				});
				if (cf.isDone() && cf.isCompletedExceptionally()) {
					close(unwrap(cf.exceptionNow())); // 同步失败：close因CAS恰好一次，handle里的不会重复生效
					return false;
				}
				return true;
			}
			// 有在途sendBinary：直接调用会同步抛IllegalStateException("Send pending")并当连接级
			// 错误误杀整条ws（第七轮60轮压测11/11的根因：登录期业务线程发rpc与派发线程应答并发）。
			// 按序入队（保持协议顺序）；失败经链上handle close，之后Send见webSocket==null返回false，
			// 调用方经连接关闭路径感知（不再静默丢帧）。
			sendChain = sendChain
					.thenCompose(ignored -> {
						var w = webSocket;
						return w != null ? w.sendBinary(bb, true) : CompletableFuture.completedFuture(null);
					})
					.handle((__, ex) -> {
						if (ex != null)
							close(unwrap(ex));
						return null;
					});
			return true;
		}
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return timeThrottle;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		return remote;
	}
}
