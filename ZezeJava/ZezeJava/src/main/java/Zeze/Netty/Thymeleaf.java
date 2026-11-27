package Zeze.Netty;

import Zeze.Util.ConcurrentHashSet;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

/**
 * Thymeleaf 模板渲染服务：渲染页面并通过 HttpExchange 发送（按大小在流式/缓冲两种 writer 间切换）。
 */
public class Thymeleaf {
	private final TemplateEngine templateEngine = new TemplateEngine();
	private final FileTemplateResolver templateResolver = new FileTemplateResolver();
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public Thymeleaf(String templateDir) {
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setPrefix(templateDir);
		templateResolver.setSuffix(".html");
		templateEngine.setTemplateResolver(templateResolver);
	}

	public TemplateEngine getTemplateEngine() {
		return templateEngine;
	}

	public FileTemplateResolver getTemplateResolver() {
		return templateResolver;
	}

	/** 检查模板名是否包含路径穿越字符（..、:、\），防越出模板目录。 */
	public static boolean isTraversalTemplateName(String url) {
		return url.contains("..") || url.indexOf(':') >= 0 || url.indexOf('\\') >= 0;
	}

	public void sendResponse(HttpExchange x, Context context) throws Exception {
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
						out.fail();
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
