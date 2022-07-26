package Zeze.Web;

import java.io.IOException;
import java.util.HashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerQuery implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			HashMap<String, String> req = null;
			switch (exchange.getRequestMethod()) {
			case "GET":
				req = HttpService.parseQuery(exchange.getRequestURI().getQuery());
				break;

			case "POST":
				req = HttpService.parseQuery(HttpService.readRequestBody(exchange));
				break;
			}

			HttpService.sendErrorResponse(exchange, req.toString());

		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}
}
