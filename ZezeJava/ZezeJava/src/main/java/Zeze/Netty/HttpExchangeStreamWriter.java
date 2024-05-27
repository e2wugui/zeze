package Zeze.Netty;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public class HttpExchangeStreamWriter extends Writer {
	private final @NotNull HttpExchange x;
	private int contentLength;

	public HttpExchangeStreamWriter(@NotNull HttpExchange x) {
		this.x = x;
		//noinspection VulnerableCodeUsages
		x.beginStream(HttpResponseStatus.OK, HttpServer.setDate(new DefaultHttpHeaders())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE));
	}

	public int getContentLength() {
		return contentLength;
	}

	@Override
	public void write(char @NotNull [] cbuf, int off, int len) {
		var byteBuffer = HttpServer.defaultCharset.encode(CharBuffer.wrap(cbuf, off, len));
		contentLength += byteBuffer.remaining();
		x.sendStream(byteBuffer);
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		x.endStream();
	}
}
