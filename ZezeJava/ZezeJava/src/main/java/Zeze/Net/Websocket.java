package Zeze.Net;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.SocketAddress;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FastLock;
import Zeze.Util.TimeThrottle;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Websocket extends AsyncSocket {
	private static final @NotNull VarHandle closedHandle;

	private final HttpExchange x;
	private byte closed;
	private final ByteBuffer input = ByteBuffer.Allocate();
	private final SocketAddress remote;
	private final TimeThrottle timeThrottle;

	private final FastLock lock = new FastLock();

	static {
		try {
			var lookup = MethodHandles.lookup();
			closedHandle = lookup.findVarHandle(Websocket.class, "closed", byte.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
		this.remote = x.channel().remoteAddress();
		this.timeThrottle = TimeThrottle.create(getService().getSocketOptions());
	}

	@Override
	public Type getType() {
		return Type.eServer;
	}

	@Override
	public TimeThrottle getTimeThrottle() {
		return timeThrottle;
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

		try {
			getService().OnSocketClose(this, ex);
		} catch (Exception e) {
			logger.error("Service.OnSocketClose exception:", e);
		}
		if (null == ex)
			x.closeConnectionOnFlush(null); // 总是gracefully
		else
			x.closeConnectionNow();

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
		return closed != 0;
	}
}
