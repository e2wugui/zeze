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
	private boolean failed; // 渲染中途异常由调用方置位：close不得把截断页面按正常终结符收尾（FND5-17复审）

	public HttpExchangeStreamWriter(@NotNull HttpExchange x) {
		this.x = x;
		x.beginStream(HttpResponseStatus.OK, HttpServer.setDate(new DefaultHttpHeaders())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE));
	}

	public int getContentLength() {
		return contentLength;
	}

	/** 模板process抛异常时调用方置位，close改走失败收尾（断连使客户端可检测截断）。 */
	public void fail() {
		failed = true;
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
		if (failed) {
			// 200头与半截内容已在线，状态码无法改写：不发LastHttpContent终结符（那会把截断
			// 页面伪装成完整200+keep-alive），直接断连——curl报18(transfer closed)、浏览器
			// 报网络错误，客户端可检测到截断。
			x.closeConnectionOnFlush(null);
			return;
		}
		x.endStream();
	}
}
