package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.jspecify.annotations.Nullable;

/**
 * 基于 HttpExchange 流式 API（beginStream/sendStream/endStream）的 OutputStream 适配层，
 * 支持固定长度、分块编码（含背压等待）与无响应体三种模式。
 */
public final class HttpResponseWithBodyStream {
	private static final NoBodyStream noBodyStream = new NoBodyStream();

	private HttpResponseWithBodyStream() {
	}

	public static OutputStream sendHeadersAndGetBody(HttpExchange x,
															  HttpResponseStatus status,
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
		private final HttpExchange x;
		private final ByteBuf buffer;
		private boolean closed;

		public FixedLengthBodyStream(HttpExchange x, int contentLength) {
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
		public void write(byte [] b, int off, int len) {
			checkOpen();
			Objects.checkFromIndexSize(off, len, b.length);
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
				buffer.release();
				// 已承诺 Content-Length 但写入不足，关闭连接避免客户端悬挂等待。
				x.context().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IOException("Incomplete content: Expected " + expected + " bytes, actual " + actual);
			}
			x.sendStream(buffer);
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
				closed = true;
				buffer.release();
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
		private static final long SlowPeerTimeoutMillis = 5_000; // 慢客户端背压等待上限，避免长期占用派发线程

		private final HttpExchange x;
		private boolean closed;

		public ChunkedBodyStream(HttpExchange x) {
			this.x = x;
		}

		@Override
		public void write(int b) throws IOException {
			checkOpen();
			awaitWritable();
			x.sendStream(new byte[]{(byte)b});
		}

		@Override
		public void write(byte [] b, int off, int len) throws IOException {
			checkOpen();
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0)
				return;
			awaitWritable();
			// OutputStream 允许 write 返回后复用入参数组，异步发送需拷贝。
			x.sendStream(Arrays.copyOfRange(b, off, off + len));
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			x.endStream();
		}

		// outbound 越水位时发空载体并等待刷新完成（h2 子通道只接受 HttpObject）；EventLoop 线程跳过防死锁。
		private void awaitWritable() throws IOException {
			var ch = x.channel();
			if (ch.isWritable() || ch.eventLoop().inEventLoop())
				return;
			var carrier = x.isH2() ? new DefaultHttpContent(Unpooled.EMPTY_BUFFER) : Unpooled.EMPTY_BUFFER;
			var f = ch.writeAndFlush(carrier);
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
		public void write(byte [] b, int off, int len) {
			throw new IllegalStateException("No body allowed");
		}

		@Override
		public void close() {
			// 无操作
		}
	}
}
