package Zeze.Netty;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import Zeze.Util.ConcurrentHashSet;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FreeMarker {
	private final Configuration freeMarker = new Configuration(Configuration.VERSION_2_3_32);
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public FreeMarker(@NotNull File templateDir) throws Exception {
		freeMarker.setDirectoryForTemplateLoading(templateDir);

		// Recommended settings for new projects:
		freeMarker.setDefaultEncoding(HttpServer.defaultCharset.name());
		freeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freeMarker.setLogTemplateExceptions(false);
		freeMarker.setWrapUncheckedExceptions(true);
		freeMarker.setFallbackOnNullLoopVariable(false);
		freeMarker.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

		freeMarker.setTemplateUpdateDelayMilliseconds(5000); // 5 seconds
	}

	public @NotNull Configuration getConfiguration() {
		return freeMarker;
	}

	/**
	 * 服务端格式化页面 & 发送结果。
	 *
	 * @param x         http context.
	 * @param modelBean Map Or JavaBean.
	 */
	public void sendResponse(@NotNull HttpExchange x, @Nullable Object modelBean) throws Exception {
		var url = x.path() + ".ftlh";
		var tmpl = freeMarker.getTemplate(url);
		if (withContentLength.contains(url)) {
			try (var out = new HttpExchangeContentLengthWriter(x)) {
				tmpl.process(modelBean, out);
				if (out.getContentLength() > 64 * 1024)
					withContentLength.remove(url);
			}
		} else {
			try (var out = new HttpExchangeStreamWriter(x)) {
				tmpl.process(modelBean, out);
				if (out.getContentLength() < 16 * 1024)
					withContentLength.add(url);
			}
		}
		// todo Netty 主要是close问题。我印象中只要使用x.send即可，不需要关心close。
	}
}
