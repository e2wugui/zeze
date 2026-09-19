package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 完全构建于HttpExchange公开流式API（beginStream/sendStream/endStream）之上的OutputStream适配层，
// 供Prometheus exporter-common等需要OutputStream语义的库使用。无同包特权通道：响应经序化器按
// pipelining到达序写出（FND8-46），close即endStream（exchange随之终结，幂等，框架auto-close为no-op）。
// 异常中止路径（Content-Length无法兑现）仍直写ctx并关连接——连接随即失效，无错序可观察。
public final class HttpResponseWithBodyStream {
	private static final NoBodyStream noBodyStream = new NoBodyStream();

	private HttpResponseWithBodyStream() {
	}

	public static @NotNull OutputStream sendHeadersAndGetBody(@NotNull HttpExchange x,
															  @NotNull HttpResponseStatus status,
															  @Nullable Map<String, Object> headers,
															  int contentLength) {
		var httpHeaders = new DefaultHttpHeaders();
		if (headers != null) {
			for (Map.Entry<String, Object> e : headers.entrySet())
				httpHeaders.set(e.getKey(), e.getValue());
		}

		if (contentLength > 0) {
			// 固定长度模式：设了CONTENT_LENGTH则beginStream不会再设CHUNKED
			httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
			x.beginStream(status, httpHeaders);
			return new FixedLengthBodyStream(x, contentLength);

		}
		if (contentLength == 0) {
			// 分块编码模式：无CONTENT_LENGTH，beginStream自动设CHUNKED
			x.beginStream(status, httpHeaders);
			return new ChunkedBodyStream(x);

		}
		// contentLength <= -1
		// 无响应体模式：直接开始并终结（endStream幂等关闭exchange）
		httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, 0);
		x.beginStream(status, httpHeaders);
		x.endStream();
		return noBodyStream;
	}

	// ========================= 三种Body处理模式 =========================

	/**
	 * 固定长度模式（contentLength > 0）：全部缓冲，close时一次性送出（上限即承诺的contentLength）
	 */
	private static class FixedLengthBodyStream extends OutputStream {
		private final @NotNull HttpExchange x;
		private final @NotNull ByteBuf buffer;
		private boolean closed;

		public FixedLengthBodyStream(@NotNull HttpExchange x, int contentLength) {
			this.x = x;
			this.buffer = x.context().alloc().buffer(contentLength);
		}

		@Override
		public void write(int b) {
			checkOpen();
			ensureCapacity(1);
			buffer.writeByte(b);
		}

		@Override
		public void write(byte @NotNull [] b, int off, int len) {
			checkOpen();
			ensureCapacity(len);
			buffer.writeBytes(b, off, len);
		}

		@Override
		public void close() throws IOException {
			if (closed)
				return;
			closed = true;
			if (buffer.writableBytes() > 0) {
				int expected = buffer.capacity();
				int actual = buffer.readableBytes();
				buffer.release(); // 异常路径也要释放pooled ByteBuf
				// Content-Length已承诺但写入不足：不发终结符也要关闭连接，
				// 否则客户端按Content-Length等剩余字节，悬挂到服务端空闲超时（默认60秒级）才被掐断。
				x.context().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IOException("Incomplete content: Expected " + expected + " bytes, actual " + actual);
			}
			x.sendStream(buffer); // 所有权转移；CL模式下endStream的空终结符不产生额外字节
			x.endStream();
		}

		private void checkOpen() {
			if (closed) {
				throw new IllegalStateException("Stream closed");
			}
		}

		private void ensureCapacity(int len) {
			if (buffer.writableBytes() < len) {
				int remaining = buffer.writableBytes();
				closed = true; // 溢出后流作废，后续write/close不再触碰已释放的buffer
				buffer.release();
				// 溢出同样意味着承诺的Content-Length无法兑现，关闭连接避免客户端悬挂（同close异常路径）。
				x.context().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IllegalStateException("Overflow: Attempt to write " + len +
						" bytes, remaining capacity " + remaining);
			}
		}
	}

	/**
	 * 分块编码模式（contentLength == 0）
	 */
	private static class ChunkedBodyStream extends OutputStream {
		// 慢客户端等待上限。这个等待发生在handler的派发线程上（虚拟线程配置下无耗尽问题；
		// 平台线程池为CPU×30固定大小），过长会放大慢客户端攻击面：N个不读的连接各占住一个
		// worker直到超时。5s对合法慢客户端足够排空水位级积压（32KB@7KB/s），死连接由服务端
		// 写空闲超时（writeIdleTimeout，60s级）兜底，此处只需约束线程占用时长。
		private static final long SlowPeerTimeoutMillis = 5_000;

		private final @NotNull HttpExchange x;
		private boolean closed;

		public ChunkedBodyStream(@NotNull HttpExchange x) {
			this.x = x;
		}

		@Override
		public void write(int b) throws IOException {
			checkOpen();
			awaitWritable();
			x.sendStream(new byte[]{(byte)b});
		}

		@Override
		public void write(byte @NotNull [] b, int off, int len) throws IOException {
			checkOpen();
			awaitWritable();
			// OutputStream契约：write返回后调用方即可复用缓冲（GZIPOutputStream的deflate循环
			// 就复用内部buf逐块写出），sendStream是零拷贝包装——这里必须拷贝。
			x.sendStream(Arrays.copyOfRange(b, off, off + len));
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			x.endStream(); // chunked终结符（0\r\n\r\n）+exchange终结（幂等）
		}

		// 背压：outbound缓冲越过水位（writePendingLimit，慢客户端）时等待当前积压写出再继续。
		// 等待手段：提交一个空buffer写并await其完成——空写不产生字节、不参与响应序（仅触发flush），
		// 完成即积压已推向socket。EventLoop线程不得等待（会死锁）；超时视为对端过慢。
		private void awaitWritable() throws IOException {
			var ch = x.channel();
			if (ch.isWritable() || ch.eventLoop().inEventLoop())
				return;
			var f = ch.writeAndFlush(Unpooled.EMPTY_BUFFER);
			if (!f.awaitUninterruptibly(SlowPeerTimeoutMillis))
				throw new IOException("peer too slow (write buffer saturated): " + ch.remoteAddress());
			if (!f.isSuccess())
				throw new IOException("write backlog failed: " + ch.remoteAddress(), f.cause());
		}

		private void checkOpen() {
			if (closed) {
				throw new IllegalStateException("Stream closed");
			}
		}
	}

	/**
	 * 无响应体模式（contentLength <= -1）：响应已在sendHeadersAndGetBody内完成
	 */
	private static class NoBodyStream extends OutputStream {
		@Override
		public void write(int b) {
			throw new IllegalStateException("No body allowed");
		}

		@Override
		public void write(byte @NotNull [] b, int off, int len) {
			throw new IllegalStateException("No body allowed");
		}

		@Override
		public void close() {
			// 无操作
		}
	}
}
