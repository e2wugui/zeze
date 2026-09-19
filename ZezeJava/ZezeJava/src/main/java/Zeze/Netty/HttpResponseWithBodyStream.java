package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HttpResponseWithBodyStream {
	private static final NoBodyStream noBodyStream = new NoBodyStream();

	private HttpResponseWithBodyStream() {
	}

	// 响应经HttpExchange的序化器按pipelining到达序写出（FND8-46孪生：曾直写共享channel的ctx，
	// 先到慢请求的响应会被/metrics响应插队）。异常中止路径（Content-Length无法兑现）仍直写并
	// 关闭连接——连接随即失效，无错序可观察。
	public static @NotNull OutputStream sendHeadersAndGetBody(@NotNull HttpExchange x,
															  @NotNull HttpResponseStatus status,
															  @Nullable Map<String, Object> headers,
															  int contentLength) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		if (headers != null) {
			for (Map.Entry<String, Object> e : headers.entrySet()) {
				response.headers().set(e.getKey(), e.getValue());
			}
		}

		if (contentLength > 0) {
			// 固定长度模式
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
			x.writeResponse(response, false, null); // 先发送header（不要立即flush）
			return new FixedLengthBodyStream(x, contentLength);

		}
		if (contentLength == 0) {
			// 分块编码模式
			response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			x.writeResponse(response, false, null); // 发送header
			return new ChunkedBodyStream(x);

		}
		// contentLength <= -1
		// 无响应体模式
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
		x.writeResponse(response, true, null); // 立即发送header并结束
		return noBodyStream;
	}

	// ========================= 三种Body处理模式 =========================

	/**
	 * 固定长度模式（contentLength > 0）
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
				// Content-Length已承诺但写入不足：不发LastHttpContent也要关闭连接，
				// 否则客户端按Content-Length等剩余字节，悬挂到服务端空闲超时（默认60秒级）才被掐断。
				x.context().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IOException("Incomplete content: Expected " + expected + " bytes, actual " + actual);
			}
			x.writeResponse(new DefaultLastHttpContent(buffer), true, null);
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
		// 慢客户端等待上限：对齐服务端默认写空闲超时（writeIdleTimeout），超过按对端过慢失败
		private static final long SlowPeerTimeoutMillis = 60_000;

		private final @NotNull HttpExchange x;
		private boolean closed;

		public ChunkedBodyStream(@NotNull HttpExchange x) {
			this.x = x;
		}

		@Override
		public void write(int b) throws IOException {
			checkOpen();
			awaitWritable();
			ByteBuf chunk = Unpooled.wrappedBuffer(new byte[]{(byte)b});
			x.writeResponse(new DefaultHttpContent(chunk), false, null);
		}

		@Override
		public void write(byte @NotNull [] b, int off, int len) throws IOException {
			checkOpen();
			awaitWritable();
			ByteBuf chunk = Unpooled.copiedBuffer(b, off, len);
			x.writeResponse(new DefaultHttpContent(chunk), false, null);
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
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
	 * 无响应体模式（contentLength <= -1）
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
