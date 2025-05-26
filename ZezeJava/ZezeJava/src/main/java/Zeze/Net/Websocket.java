package Zeze.Net;

import java.net.SocketAddress;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FastLock;
import Zeze.Util.TimeThrottle;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Websocket extends AsyncSocket {
	private final HttpExchange x;
	private boolean closed = false;
	private final ByteBuffer input = ByteBuffer.Allocate();
	private final SocketAddress remote;
	private final Type type;
	private final TimeThrottle timeThrottle;

	private final FastLock lock = new FastLock();

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
		this.remote = x.channel().remoteAddress();
		this.type = Type.eServer;
		this.timeThrottle = TimeThrottle.create(getService().getSocketOptions());
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public TimeThrottle getTimeThrottle() {
		return timeThrottle;
	}

	@Override
	protected boolean close(@Nullable Throwable ex, boolean gracefully) {
		x.closeConnectionOnFlush(null); // 总是gracefully
		this.closed = true;
		if (timeThrottle != null)
			timeThrottle.close();
		return true;
	}

	void processInput(ByteBuf buf) throws Exception {
		int n = buf.readableBytes();
		super.recvCount ++;
		super.recvSize += n;
		input.EnsureWrite(n);
		buf.readBytes(input.Bytes, input.WriteIndex, n);
		input.WriteIndex += n;
		getService().OnSocketProcessInputBuffer(this, input);
		input.Compact();
	}

	@Override
	public boolean Send(byte @NotNull [] bytes, int offset, int length) {
		lock.lock();
		try {
			// 这里是多线程访问的。
			super.sendCount++;
			super.sendSize += length;
			super.sendRawSize += length;
		} finally {
			lock.unlock();
		}
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
