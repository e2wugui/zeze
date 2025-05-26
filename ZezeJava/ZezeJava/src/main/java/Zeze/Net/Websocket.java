package Zeze.Net;

import java.net.SocketAddress;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.ByteBuffer;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Websocket extends AsyncSocket {
	private final HttpExchange x;
	private boolean closed = false;
	private final ByteBuffer input = ByteBuffer.Allocate();

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
	}

	@Override
	protected boolean close(@Nullable Throwable ex, boolean gracefully) {
		x.closeConnectionOnFlush(null); // 总是gracefully
		this.closed = true;
		return true;
	}

	void processInput(ByteBuf buf) throws Exception {
		input.Append(buf.array(), buf.arrayOffset(), buf.readableBytes());
		getService().OnSocketProcessInputBuffer(this, input);
		input.Compact();
	}

	@Override
	public boolean Send(byte @NotNull [] bytes, int offset, int length) {
		x.sendWebSocket(bytes, offset, length);
		return true;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		// todo 怎么拿到http连接的remoteAddress。
		return null;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
}
