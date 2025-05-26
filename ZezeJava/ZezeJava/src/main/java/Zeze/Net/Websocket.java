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
	private final SocketAddress remote;

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
		this.remote = x.channel().remoteAddress();
	}

	@Override
	protected boolean close(@Nullable Throwable ex, boolean gracefully) {
		x.closeConnectionOnFlush(null); // 总是gracefully
		this.closed = true;
		return true;
	}

	void processInput(ByteBuf buf) throws Exception {
		int n = buf.readableBytes();
		input.EnsureWrite(n);
		buf.readBytes(input.Bytes, input.WriteIndex, n);
		input.WriteIndex += n;
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
		return remote;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
}
