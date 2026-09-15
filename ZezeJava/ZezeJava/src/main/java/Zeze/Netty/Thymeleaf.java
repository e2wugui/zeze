package Zeze.Netty;

import Zeze.Util.ConcurrentHashSet;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class Thymeleaf {
	private final TemplateEngine templateEngine = new TemplateEngine();
	private final FileTemplateResolver templateResolver = new FileTemplateResolver();
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public Thymeleaf(@NotNull String templateDir) {
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setPrefix(templateDir);
		templateResolver.setSuffix(".html");
		templateEngine.setTemplateResolver(templateResolver);
	}

	public @NotNull TemplateEngine getTemplateEngine() {
		return templateEngine;
	}

	public @NotNull FileTemplateResolver getTemplateResolver() {
		return templateResolver;
	}

	/**
	 * FND6-16：模板名直接取解码后的请求路径（可含../），FileTemplateResolver无canonical检查，
	 * 可越出模板目录读任意.html。比照HttpServer.addFileHandler判例：合法模板名为不含..、:、
	 * 反斜杠的相对根路径，越根即FORBIDDEN。
	 */
	public static boolean isTraversalTemplateName(@NotNull String url) {
		return url.contains("..") || url.indexOf(':') >= 0 || url.indexOf('\\') >= 0;
	}

	public void sendResponse(@NotNull HttpExchange x, @NotNull Context context) throws Exception {
		var url = x.path();
		if (isTraversalTemplateName(url)) {
			x.close(x.sendPlainText(HttpResponseStatus.FORBIDDEN, ""));
			return;
		}
		if (withContentLength.contains(url)) {
			try (var out = new HttpExchangeContentLengthWriter(x)) {
				try {
					templateEngine.process(url, context, out);
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
					templateEngine.process(url, context, out);
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
	}
}
