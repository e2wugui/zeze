package Zeze.Net;

import java.io.IOException;
import java.net.SocketAddress;
import Zeze.Netty.HttpExchange;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FastLock;
import Zeze.Util.TimeThrottle;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 服务端 websocket 连接（基于 HttpExchange 的 AsyncSocket 适配）。
 */
public class Websocket extends AsyncSocket {
	@Override
	public Service getService() {
		var service = super.getService();
		assert service != null;
		return service;
	}

	private static final Logger logger = LogManager.getLogger(Websocket.class);

	private final HttpExchange x;
	private final ByteBuffer input = ByteBuffer.Allocate();
	private final SocketAddress remote;
	private final @Nullable TimeThrottle timeThrottle;

	private final FastLock lock = new FastLock();

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
		this.remote = x.channel().remoteAddress();
		this.timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		resetActiveSendRecvTime();
	}

	@Override
	public Type getType() {
		return Type.eServer;
	}

	@Override
	public @Nullable TimeThrottle getTimeThrottle() {
		return timeThrottle;
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
		fireOnSocketDisposed();
	}

	void processInput(ByteBuf buf) {
		setActiveRecvTime();
		int n = buf.readableBytes();
		super.recvCount++;
		super.recvSize += n;
		try {
			input.EnsureWrite(n);
			buf.readBytes(input.Bytes, input.WriteIndex, n);
			input.WriteIndex += n;
			getService().OnSocketProcessInputBuffer(this, input);
		} catch (Exception e) {
			close(e);
			return;
		}
		input.Compact();
	}

	@Override
	public boolean Send(byte [] bytes, int offset, int length) {
		ByteBuffer.VerifyArrayIndex(bytes, offset, length);
		if (isClosed())
			return false;
		// 以 channel 级待写字节数检查发送缓冲堆积上限，避免慢客户端导致内存无界增长。
		var outBuf = x.channel().unsafe().outboundBuffer();
		var pending = outBuf != null ? outBuf.totalPendingWriteBytes() : 0;
		try {
			if (!getService().checkOverflow(this, pending + length, bytes, offset, length))
				return false;
		} catch (Exception e) {
			close(e);
			return false;
		}
		if (isClosed())
			return false;
		// 检查写回执：同步失败直接 close 返回 false，异步失败在 listener 中 close。
		var cf = x.sendWebSocket(bytes, offset, length);
		if (cf.isDone()) {
			var cause = cf.cause();
			if (cause != null) {
				close(cause);
				return false;
			}
		} else {
			cf.addListener(f -> {
				var cause = f.cause();
				if (cause != null)
					close(cause);
			});
		}
		setActiveSendTime();
		lock.lock();
		try {
			// 这里是多线程访问的。
			super.sendCount++;
			super.sendSize += length;
			super.sendRawSize += length;
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		return remote;
	}
}
