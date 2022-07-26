package Zeze.Web;

import java.io.IOException;
import java.util.HashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerQuery implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			HashMap<String, String> httpRequest = null;
			switch (exchange.getRequestMethod()) {
			case "GET":
				httpRequest = HttpService.parseQuery(exchange.getRequestURI().getQuery());
				break;

			case "POST":
				httpRequest = HttpService.parseQuery(HttpService.readRequestBody(exchange));
				break;
			}

			HttpService.sendErrorResponse(exchange, httpRequest.toString());

		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}
}
