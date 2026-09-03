package Zeze.Netty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HttpResponseWithBodyStream {
	private static final NoBodyStream noBodyStream = new NoBodyStream();

	private HttpResponseWithBodyStream() {
	}

	public static @NotNull OutputStream sendHeadersAndGetBody(@NotNull ChannelHandlerContext ctx,
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
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
		ctx.writeAndFlush(response); // 立即发送header并结束
		return noBodyStream;
	}

	// ========================= 三种Body处理模式 =========================

	/**
	 * 固定长度模式（contentLength > 0）
	 */
	private static class FixedLengthBodyStream extends OutputStream {
		private final @NotNull ChannelHandlerContext ctx;
		private final @NotNull ByteBuf buffer;
		private boolean closed;

		public FixedLengthBodyStream(@NotNull ChannelHandlerContext ctx, int contentLength) {
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
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IOException("Incomplete content: Expected " + expected + " bytes, actual " + actual);
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
				int remaining = buffer.writableBytes();
				closed = true; // 溢出后流作废，后续write/close不再触碰已释放的buffer
				buffer.release();
				// 溢出同样意味着承诺的Content-Length无法兑现，关闭连接避免客户端悬挂（同close异常路径）。
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				throw new IllegalStateException("Overflow: Attempt to write " + len +
						" bytes, remaining capacity " + remaining);
			}
		}
	}

	/**
	 * 分块编码模式（contentLength == 0）
	 */
	private static class ChunkedBodyStream extends OutputStream {
		private final @NotNull ChannelHandlerContext ctx;
		private boolean closed;

		public ChunkedBodyStream(@NotNull ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void write(int b) {
			checkOpen();
			ByteBuf chunk = Unpooled.wrappedBuffer(new byte[]{(byte)b});
			ctx.write(new DefaultHttpContent(chunk));
		}

		@Override
		public void write(byte @NotNull [] b, int off, int len) {
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
		public void write(byte @NotNull [] b, int off, int len) {
			throw new IllegalStateException("No body allowed");
		}

		@Override
		public void close() {
			// 无操作
		}
	}
}
