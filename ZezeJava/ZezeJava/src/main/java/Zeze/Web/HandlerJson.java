package Zeze.Web;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerJson implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			String json = null;
			switch (exchange.getRequestMethod()) {
			case "GET":
				var query = exchange.getRequestURI().getQuery();
				// 只允许一个参数，并且是json=
				if (!query.startsWith("json=") || query.contains("&")) {
					HttpService.sendErrorResponse(exchange, "JSON PARAMETER, ERROR FORMAT!");
					return;
				}
				json = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
				break;

			case "POST":
				json = HttpService.readRequestBody(exchange);
				// RequestBody 应该是不需要close的吧？
				break;
			}

			HttpService.sendErrorResponse(exchange, json);

		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}
}
