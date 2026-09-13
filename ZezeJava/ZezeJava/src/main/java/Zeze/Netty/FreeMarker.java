package Zeze.Netty;

import java.io.File;
import java.util.TimeZone;
import Zeze.Util.ConcurrentHashSet;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FreeMarker {
	private static final @NotNull Logger logger = LogManager.getLogger(FreeMarker.class);

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
	 * 服务端格式化页面＆发送结果。
	 *
	 * @param x         http context.
	 * @param modelBean Map Or JavaBean.
	 */
	public void sendResponse(@NotNull HttpExchange x, @Nullable Object modelBean) throws Exception {
		var url = x.path() + ".ftlh";
		Template tmpl;
		try {
			tmpl = freeMarker.getTemplate(url);
		} catch (Throwable t) {
			// 模板缺失/读取异常发生在Writer创建之前，不经过fail()/close路径——默认事务路径下
			// 异常被triggerActions吞掉，生命周期close(null)是CLOSE_FINISH（不发响应不断连），
			// 客户端挂到idle超时后零字节关闭。此刻exchange未写出任何字节，直接单发500干净
			// 合法（与HttpExchangeContentLengthWriter失败收尾同形态）；随后生命周期close(null)
			// 先摘除exchange，上层异常传播路径不会产生第二个响应。
			logger.error("FreeMarker getTemplate failed: {}", url, t);
			x.send500("internal server error: template not found: " + url);
			throw t;
		}
		if (withContentLength.contains(url)) {
			try (var out = new HttpExchangeContentLengthWriter(x)) {
				try {
					tmpl.process(modelBean, out);
				} catch (Throwable t) {
					out.fail(); // 渲染异常：close不发200截断页（FND5-17）
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
					// 渲染异常（FND5-17复审）：close改断连（200头已在线无法改写状态码，
					// 不发终结符防截断页伪装完整200），并晋升到缓冲分支——重试走可发500的
					// 可修复路径（首渲染即失败的模板此前会永远停留在流式分支）。
					out.fail();
					withContentLength.add(url);
					throw t;
				}
				if (out.getContentLength() < 16 * 1024)
					withContentLength.add(url);
			}
		}
		// todo Netty 主要是close问题。我印象中只要使用x.send即可，不需要关心close。
	}
}
