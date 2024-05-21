package Zeze.Netty;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.TimeZone;
import Zeze.Util.ConcurrentHashSet;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public class FreeMarker {
	public static final String eDefaultEncoding = "UTF-8";
	private final Configuration freeMarker;
	private final Charset defaultCharset;
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public FreeMarker(File templateDir) throws Exception {
		freeMarker = new Configuration(Configuration.VERSION_2_3_32);

		freeMarker.setDirectoryForTemplateLoading(templateDir);

		// Recommended settings for new projects:
		freeMarker.setDefaultEncoding(eDefaultEncoding);
		freeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freeMarker.setLogTemplateExceptions(false);
		freeMarker.setWrapUncheckedExceptions(true);
		freeMarker.setFallbackOnNullLoopVariable(false);
		freeMarker.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

		freeMarker.setTemplateUpdateDelayMilliseconds(5000); // 5 seconds

		defaultCharset = Charset.forName(eDefaultEncoding);
	}

	/**
	 * 服务端格式化页面 & 发送结果。
	 *
	 * @param x http context.
	 * @param modelBean Map Or JavaBean.
	 * @throws Exception exception
	 */
	public void sendResponse(HttpExchange x, Object modelBean) throws Exception {
		var url = x.path();
		var tmpl = freeMarker.getTemplate(url);
		if (withContentLength.contains(url)) {
			// todo 需确定FreeMarker输出是什么，只是html内容，不包括头？这里假定只有html内容。
			try (var out = new HttpExchangeContentLengthWriter(x)) {
				tmpl.process(modelBean, out);
				if (out.getContentLength() > 64 * 1024)
					withContentLength.remove(url);
			}
		} else {
			// todo 流式写的时候，头怎么给？还需确定FreeMarker输出是什么，只是html内容，不包括头？
			//   HttpExchange.sendStream 也需要确认，自己写的，但怎么用忘了。
			try (var out = new HttpExchangeStreamWriter(x)) {
				tmpl.process(modelBean, out);
				if (out.getContentLength() < 16 * 1024)
					withContentLength.add(url);
			}
		}
		// todo Netty 主要是close问题。我印象中只要使用x.send即可，不需要关心close。
	}

	public class HttpExchangeContentLengthWriter extends Writer {
		private final HttpExchange x;
		private final ByteBuf html = Unpooled.buffer(64 * 1024);

		public HttpExchangeContentLengthWriter(HttpExchange x) {
			this.x = x;
		}

		public int getContentLength() {
			return html.readableBytes();
		}

		@Override
		public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
			var charBuffer = CharBuffer.wrap(cbuf, off, len);
			html.writeCharSequence(charBuffer, defaultCharset);
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

	public class HttpExchangeStreamWriter extends Writer {
		private final HttpExchange x;
		private int contentLength;

		public HttpExchangeStreamWriter(HttpExchange x) {
			this.x = x;
		}

		public int getContentLength() {
			return contentLength;
		}

		@Override
		public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
			var charBuffer = CharBuffer.wrap(cbuf, off, len);
			var byteBuffer = defaultCharset.encode(charBuffer);
			contentLength += byteBuffer.limit() - byteBuffer.position();
			x.sendStream(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
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
}
