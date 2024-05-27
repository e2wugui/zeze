package Zeze.Netty;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public class HttpExchangeContentLengthWriter extends Writer {
	private final @NotNull HttpExchange x;
	private final @NotNull ByteBuf html = PooledByteBufAllocator.DEFAULT.buffer(64 * 1024);

	public HttpExchangeContentLengthWriter(@NotNull HttpExchange x) {
		this.x = x;
	}

	public int getContentLength() {
		return html.readableBytes();
	}

	@Override
	public void write(char @NotNull [] cbuf, int off, int len) {
		html.writeCharSequence(CharBuffer.wrap(cbuf, off, len), HttpServer.defaultCharset);
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		x.send(HttpResponseStatus.OK, "text/html; charset=utf-8", html);
	}
}
