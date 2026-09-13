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
	private boolean closed; // send会转移html所有权(netty发送完释放),Closeable契约要求close幂等,二次close不得重复发送已释放的ByteBuf
	private boolean failed; // 渲染中途异常由调用方置位：close不得把半截页面按200+完整ContentLength发出（FND5-17）

	public HttpExchangeContentLengthWriter(@NotNull HttpExchange x) {
		this.x = x;
	}

	/** 模板process抛异常时调用方置位，close改走失败收尾（不发半截内容，缓冲自释放）。 */
	public void fail() {
		failed = true;
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
		if (closed)
			return;
		closed = true;
		if (failed) {
			// 半截页面不是成功响应：丢弃缓冲（未send则所有权未转移，自释放防泄漏）。
			// 此刻exchange未写出任何字节（头也未发出），单发一个带ContentLength的500干净
			// 合法；随后生命周期close(null)先从exchanges摘除本exchange，上层异常传播路径
			// 的exceptionCaught/send500不会产生第二个响应。曾只release不发——客户端得到
			// 零字节空回复或挂起到idle超时（FND5-17复审）。
			html.release();
			x.send500("internal server error: template render failed");
			return;
		}
		x.send(HttpResponseStatus.OK, "text/html; charset=utf-8", html);
	}
}
