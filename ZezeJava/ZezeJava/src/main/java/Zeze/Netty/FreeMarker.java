package Zeze.Netty;

import java.io.File;
import java.util.TimeZone;
import Zeze.Util.ConcurrentHashSet;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * FreeMarker 模板渲染服务：渲染页面并通过 HttpExchange 发送（按大小在流式/缓冲两种 writer 间切换）。
 */
public class FreeMarker {
	private static final Logger logger = LogManager.getLogger(FreeMarker.class);

	private final Configuration freeMarker = new Configuration(Configuration.VERSION_2_3_32);
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public FreeMarker(File templateDir) throws Exception {
		freeMarker.setDirectoryForTemplateLoading(templateDir);

		freeMarker.setDefaultEncoding(HttpServer.defaultCharset.name());
		freeMarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freeMarker.setLogTemplateExceptions(false);
		freeMarker.setWrapUncheckedExceptions(true);
		freeMarker.setFallbackOnNullLoopVariable(false);
		freeMarker.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

		freeMarker.setTemplateUpdateDelayMilliseconds(5000);
	}

	public Configuration getConfiguration() {
		return freeMarker;
	}

	/**
	 * 服务端格式化页面＆发送结果。
	 *
	 * @param x         http context.
	 * @param modelBean Map Or JavaBean.
	 */
	public void sendResponse(HttpExchange x, @Nullable Object modelBean) throws Exception {
		var url = x.path() + ".ftlh";
		Template tmpl;
		try {
			tmpl = freeMarker.getTemplate(url);
		} catch (Throwable t) {
			// 此时尚未写出任何响应，直接发 500 避免事务路径吞异常后客户端挂起到超时。
			logger.error("FreeMarker getTemplate failed: {}", url, t);
			x.send500("internal server error: template not found: " + url);
			throw t;
		}
		if (withContentLength.contains(url)) {
			try (var out = new HttpExchangeContentLengthWriter(x)) {
				try {
					tmpl.process(modelBean, out);
				} catch (Throwable t) {
					out.fail();
					throw t;
				}
				if (out.getContentLength() > 64 * 1024)
					withContentLength.remove(url);
			}
		} else {
			try (var out = new HttpExchangeStreamWriter(x)) {
				try {
					tmpl.process(modelBean, out);
				} catch (Throwable t) {
					// 流式渲染失败改为断连，并切到缓冲分支使后续重试可返回 500。
					out.fail();
					withContentLength.add(url);
					throw t;
				}
				if (out.getContentLength() < 16 * 1024)
					withContentLength.add(url);
			}
		}
	}
}
