package Zeze.Netty;

import Zeze.Util.ConcurrentHashSet;
import org.jetbrains.annotations.NotNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class Thymeleaf {
	private final TemplateEngine templateEngine;
	private final FileTemplateResolver templateResolver;
	private final ConcurrentHashSet<String> withContentLength = new ConcurrentHashSet<>();

	public Thymeleaf(@NotNull String templateDir) {
		templateEngine = new TemplateEngine();
		templateResolver = new FileTemplateResolver();
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
