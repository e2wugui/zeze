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
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 客户端 websocket 连接（基于 JDK HttpClient WebSocket 的 AsyncSocket 适配）。
 */
public class WebsocketClient extends AsyncSocket {
	@Override
	public Service getService() {
		var service = super.getService();
		assert service != null;
		return service;
	}

	private static final Logger logger = LogManager.getLogger(WebsocketClient.class);

	private volatile @Nullable WebSocket webSocket;
	private final HttpClient httpClient;
	private final @Nullable TimeThrottle timeThrottle;
	private final SocketAddress remote;
	private final @Nullable Connector connector;

	// 串行化 JDK WebSocket.sendBinary（JDK 不允许并发在途发送）；仅在追加链节点时短暂持锁。
	private final Object sendLock = new Object();
	private CompletableFuture<@Nullable Void> sendChain = CompletableFuture.completedFuture(null);

	private final AtomicLong sendPendingBytes = new AtomicLong(); // sendChain 在途字节数

	public WebsocketClient(Service service, String wsUrl, @Nullable Object userState,
						   @Nullable Connector connector) {
		super(service);
		super.userState = userState;
		this.connector = connector;
		var uri = URI.create(wsUrl);
		var port = uri.getPort();
		if (port < 0)
			port = "wss".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
		// createUnresolved 保证构造非阻塞（由 buildAsync 异步解析）。
		remote = InetSocketAddress.createUnresolved(uri.getHost(), port);
		timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		httpClient = HttpClient.newHttpClient();
		httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
			final Zeze.Serialize.ByteBuffer input = Zeze.Serialize.ByteBuffer.Allocate();

			@Override
			public void onOpen(WebSocket webSocket) {
				// 与连接置死互斥：若已关闭则直接释放迟到的传输句柄。
				if (runIfOpen(() -> WebsocketClient.this.webSocket = webSocket) == null) {
					webSocket.abort();
					return;
				}
				webSocket.request(1);
				resetActiveSendRecvTime();
				// 直接通知 Connector 并走 tryAccept（HTTP 升级即握手完成，不经 Handshake 延迟路径）。
				if (connector != null)
					connector.OnSocketConnected(WebsocketClient.this);
				try {
					service.tryAccept(WebsocketClient.this);
				} catch (IllegalStateException e) { // too many connections
					WebsocketClient.this.close(e);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public @Nullable CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
				setActiveRecvTime();
				webSocket.request(1);
				var n = data.remaining();
				WebsocketClient.this.recvCount++;
				WebsocketClient.this.recvSize += n;
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
			public @Nullable CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
				var ex = new Exception("peer closed. status=" + statusCode + " reason=" + reason);
				WebsocketClient.this.close(ex);
				return null;
			}

			@Override
			public void onError(WebSocket webSocket, Throwable error) {
				WebsocketClient.this.close(error);
			}
		}).whenComplete((webSocket, ex) -> {
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

		if (connector != null) { // 先通知 Connector 安排重连，再通知 Service
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
			httpClient.shutdownNow();
		} catch (Exception e) {
			logger.warn("httpClient.shutdownNow exception:", e);
		}
		var ws = webSocket;
		webSocket = null;
		if (ws != null) {
			ws.abort();
		}
		fireOnSocketDisposed();
	}

	private static Throwable unwrap(Throwable ex) {
		return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
	}

	@Override
	public boolean Send(byte[] bytes, int offset, int length) {
		var ws = webSocket;
		if (ws == null || isClosed())
			return false;
		setActiveSendTime();
		var bb = ByteBuffer.wrap(bytes, offset, length);
		synchronized (sendLock) {
			sendCount++;
			sendSize += length;
			sendRawSize += length;
			var newSize = sendPendingBytes.addAndGet(length);
			try {
				if (!getService().checkOverflow(this, newSize, bytes, offset, length)) {
					sendPendingBytes.addAndGet(-length);
					return false;
				}
			} catch (Exception e) {
				sendPendingBytes.addAndGet(-length);
				close(e);
				return false;
			}
			if (sendChain.isDone()) {
				// 链空闲时直接发送，保持同步失败可立即返回 false。
				var cf = ws.sendBinary(bb, true);
				sendChain = cf.handle((__, ex) -> {
					sendPendingBytes.addAndGet(-length);
					if (ex != null)
						close(unwrap(ex));
					return null;
				});
				if (cf.isDone() && cf.isCompletedExceptionally()) {
					close(unwrap(cf.exceptionNow()));
					return false;
				}
				return true;
			}
			// 有在途发送时按序挂入链尾，满足 JDK 单在途约束并保持帧序。
			sendChain = sendChain
				.thenCompose(ignored -> {
					var w = webSocket;
					return w != null ? w.sendBinary(bb, true) : CompletableFuture.completedFuture(null);
				})
				.handle((__, ex) -> {
					sendPendingBytes.addAndGet(-length);
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
