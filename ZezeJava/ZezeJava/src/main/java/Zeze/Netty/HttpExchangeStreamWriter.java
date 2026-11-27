package Zeze.Netty;

import java.io.IOException;
import java.util.Arrays;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 流式（chunked）模板输出 Writer：逐块发送，fail 时改断连使客户端可检测截断。
 */
public class HttpExchangeStreamWriter extends HttpExchangeUtf8Writer {
	private final HttpExchange x;
	private int contentLength;
	private boolean failed;

	public HttpExchangeStreamWriter(HttpExchange x) {
		this.x = x;
		var headers = HttpServer.setDate(new DefaultHttpHeaders())
				.set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
		if (!x.isH2())
			headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		x.appendResponseHeaders(headers);
		x.beginStream(HttpResponseStatus.OK, headers);
	}

	public int getContentLength() {
		return contentLength;
	}

	/** 标记渲染失败：close 时不发终结符而直接断连，使客户端感知响应截断。 */
	public void fail() {
		failed = true;
	}

	@Override
	protected void writeBytes(byte[] bytes, int off, int len) {
		contentLength += len;
		// 编码器返回后会复用内部字节数组，异步发送需拷贝。
		x.sendStream(Arrays.copyOfRange(bytes, off, off + len));
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		if (failed) {
			// 200 头已发出无法改写状态码，不发终结符直接断连，避免截断页被当成完整响应。
			x.closeConnectionOnFlush(null);
			return;
		}
		finishEncoding();
		x.endStream();
	}
}
