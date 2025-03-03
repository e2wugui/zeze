package Zeze.Util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HttpResponseWithBodyStream {

	public static OutputStream sendHeadersAndGetBody(ChannelHandlerContext ctx,
													 HttpResponseStatus status,
													 Map<String, Object> headers,
													 int contentLength) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		for (Map.Entry<String, Object> e : headers.entrySet()) {
			response.headers().set(e.getKey(), e.getValue());
		}

		if (contentLength > 0) {
			// 固定长度模式
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
			ctx.write(response);  // 先发送header（不要立即flush）
			return new FixedLengthBodyStream(ctx, contentLength);

		}
		if (contentLength == 0) {
			// 分块编码模式
			response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			ctx.write(response);  // 发送header
			return new ChunkedBodyStream(ctx);

		}
		// contentLength <= -1
		// 无响应体模式
		ctx.writeAndFlush(response); // 立即发送header并结束
		return new NoBodyStream();
	}

	// ========================= 三种Body处理模式 =========================

	/**
	 * 固定长度模式（contentLength > 0）
	 */
	private static class FixedLengthBodyStream extends OutputStream {
		private final ChannelHandlerContext ctx;
		private final ByteBuf buffer;
		private boolean closed;

		public FixedLengthBodyStream(ChannelHandlerContext ctx, int contentLength) {
			this.ctx = ctx;
			this.buffer = ctx.alloc().buffer(contentLength);
		}

		@Override
		public void write(int b) {
			checkOpen();
			ensureCapacity(1);
			buffer.writeByte(b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
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
				throw new IOException("Incomplete content: Expected " +
						buffer.capacity() + " bytes, actual " + buffer.readableBytes());
			}
			ctx.writeAndFlush(new DefaultLastHttpContent(buffer));
		}

		private void checkOpen() {
			if (closed) {
				throw new IllegalStateException("Stream closed");
			}
		}

		private void ensureCapacity(int len) {
			if (buffer.writableBytes() < len) {
				throw new IllegalStateException("Overflow: Attempt to write " + len +
						" bytes, remaining capacity " + buffer.writableBytes());
			}
		}
	}

	/**
	 * 分块编码模式（contentLength == 0）
	 */
	private static class ChunkedBodyStream extends OutputStream {
		private final ChannelHandlerContext ctx;
		private boolean closed;

		public ChunkedBodyStream(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void write(int b) {
			write(new byte[]{(byte)b}, 0, 1);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			checkOpen();
			ByteBuf chunk = Unpooled.copiedBuffer(b, off, len);
			ctx.write(new DefaultHttpContent(chunk));
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
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
		public void write(byte[] b, int off, int len) {
			throw new IllegalStateException("No body allowed");
		}

		@Override
		public void close() {
			// 无操作
		}
	}
}
