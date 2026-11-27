package Zeze.Netty;

import java.io.IOException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.AdaptiveByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 缓冲整个响应体的模板输出 Writer：按 ContentLength 发送，fail 时改发 500。
 */
public class HttpExchangeContentLengthWriter extends HttpExchangeUtf8Writer {
	private final HttpExchange x;
	private final ByteBuf html = AdaptiveByteBufAllocator.DEFAULT.buffer(64 * 1024);
	private boolean failed;

	public HttpExchangeContentLengthWriter(HttpExchange x) {
		this.x = x;
	}

	/** 标记渲染失败：close 时释放缓冲并改发 500，不发出截断内容。 */
	public void fail() {
		failed = true;
	}

	public int getContentLength() {
		return html.readableBytes();
	}

	@Override
	protected void writeBytes(byte[] bytes, int off, int len) {
		html.writeBytes(bytes, off, len);
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		if (failed) {
			html.release();
			x.send500("internal server error: template render failed");
			return;
		}
		finishEncoding();
		x.send(HttpResponseStatus.OK, "text/html; charset=utf-8", html);
	}
}
