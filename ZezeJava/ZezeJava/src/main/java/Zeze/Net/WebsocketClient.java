package Zeze.Net;

import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import Zeze.Util.TimeThrottle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebsocketClient extends AsyncSocket {
	private static final @NotNull Logger logger = LogManager.getLogger();
	private WebSocket webSocket;
	private volatile boolean closed = false;
	private final TimeThrottle timeThrottle;
	private SocketAddress remote;
	private final Connector connector;

	public WebsocketClient(Service service, String wsUrl, Object userState, Connector connector) {
		super(service);
		super.userState = userState;
		this.connector = connector;
		this.timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create(wsUrl), new WebSocket.Listener() {
					@Override
					public void onOpen(WebSocket webSocket) {
						webSocket.request(1);
						WebsocketClient.this.webSocket = webSocket;
						service.addSocket(WebsocketClient.this);
						try {
							service.OnHandshakeDone(WebsocketClient.this);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}

					final Zeze.Serialize.ByteBuffer input = Zeze.Serialize.ByteBuffer.Allocate();
					@Override
					public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
						webSocket.request(1);
						var n = data.remaining();
						input.EnsureWrite(n);
						data.get(input.Bytes, input.WriteIndex, n);
						input.WriteIndex += n;
						try {
							service.OnSocketProcessInputBuffer(WebsocketClient.this, input);
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
				});
	}

	@Override
	public Type getType() {
		return Type.eClient;
	}

	@Override
	public Connector getConnector() {
		return connector;
	}

	@Override
	protected boolean close(@Nullable Throwable ex, boolean gracefully) {
		try {
			getService().OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("OnSocketClose", e);
		}
		closed = true;
		if (timeThrottle != null)
			timeThrottle.close();
		if (null != webSocket)
			webSocket.abort();
		return false;
	}

	@Override
	public boolean Send(byte @NotNull [] bytes, int offset, int length) {
		webSocket.sendBinary(ByteBuffer.wrap(bytes, offset, length), true);
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
		return closed;
	}
}
