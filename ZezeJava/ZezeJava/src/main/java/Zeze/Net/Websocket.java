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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Websocket extends AsyncSocket {
	private static final @NotNull Logger logger = LogManager.getLogger(Websocket.class);

	private final HttpExchange x;
	private final ByteBuffer input = ByteBuffer.Allocate();
	private final SocketAddress remote;
	private final TimeThrottle timeThrottle;

	private final FastLock lock = new FastLock();

	public Websocket(HttpExchange x, Service service) {
		super(service);
		this.x = x;
		this.remote = x.channel().remoteAddress();
		this.timeThrottle = TimeThrottle.create(getService().getSocketOptions());
		resetActiveSendRecvTime(); // 升级完成连接已建立，纳入KeepAlive管理（对齐TcpSocket构造时机）
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

	void processInput(ByteBuf buf) throws Exception {
		setActiveRecvTime(); // FND7-63：维护活跃时间，checkKeepAlive才能回收静默死链
		int n = buf.readableBytes();
		super.recvCount++;
		super.recvSize += n;
		try {
			input.EnsureWrite(n);
			buf.readBytes(input.Bytes, input.WriteIndex, n);
			input.WriteIndex += n;
			getService().OnSocketProcessInputBuffer(this, input);
		} catch (Exception e) {
			// N3-F1：解码异常对齐TcpSocket（异常上抛→doException→close）断连语义。
			// 不关闭则连接存活、后续帧继续注入，且抛出路径跳过Compact——input无界增长，
			// 远程可触发无界内存增长。
			close(e);
			return;
		}
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
		// N3-F2：发送堆积上限——OutputBufferMaxSize此前对websocket服务端连接失效（checkOverflow
		// 唯一调用方是TcpSocket.Send），慢速客户端下outbound缓冲无界积压可OOM。以channel级在途
		// 字节（totalPendingWriteBytes，HttpServer背压日志同口径）+本帧为newSize做同款上限检查，
		// 超限返回false（与TcpSocket.Send语义对齐，调用方按发送失败感知）。
		var outBuf = x.channel().unsafe().outboundBuffer();
		var pending = outBuf != null ? outBuf.totalPendingWriteBytes() : 0;
		try {
			if (!getService().checkOverflow(this, pending + length, bytes, offset, length))
				return false;
		} catch (Exception e) {
			close(e);
			return false;
		}
		// 检查写回执,不能恒返回true:写失败的帧(管线状态不符/连接已关等)不会到达对端。
		// 在EventLoop上调用时future同步完成,失败立即close并返回false,调用方(Protocol/Rpc.Send)
		// 能感知发送失败;非EventLoop线程调用时future异步完成,挂listener失败同样close,
		// 不再静默丢帧。close的markClosed置死保证OnSocketClose等清理恰好一次。
		setActiveSendTime(); // FND7-63：维护活跃时间（发送已被接受进入发送管线）
		var cf = x.sendWebSocket(bytes, offset, length);
		if (cf.isDone()) {
			var cause = cf.cause();
			if (cause == null)
				return true;
			close(cause);
			return false;
		}
		cf.addListener(f -> {
			var cause = f.cause();
			if (cause != null)
				close(cause);
		});
		return true;
	}

	@Override
	public @Nullable SocketAddress getRemoteAddress() {
		return remote;
	}
}
