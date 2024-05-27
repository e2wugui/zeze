package Zeze.Netty;

import Zeze.Util.ConcurrentHashSet;
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

	public void sendResponse(@NotNull HttpExchange x, @NotNull Context context) throws Exception {
		var url = x.path();
		if (withContentLength.contains(url)) {
			if (withContentLength.contains(url)) {
				try (var out = new HttpExchangeContentLengthWriter(x)) {
					templateEngine.process(url, context, out);
					if (out.getContentLength() > 64 * 1024)
						withContentLength.remove(url);
				}
			} else {
				try (var out = new HttpExchangeStreamWriter(x)) {
					templateEngine.process(url, context, out);
					if (out.getContentLength() < 16 * 1024)
						withContentLength.add(url);
				}
			}
		}
	}
}
