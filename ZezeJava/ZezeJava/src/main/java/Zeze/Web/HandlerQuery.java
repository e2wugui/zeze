package Zeze.Web;

import java.io.IOException;
import java.util.HashMap;
import Zeze.Builtin.Web.RequestQuery;
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

			var agent = new RequestQuery();
			agent.Argument.setCookie(""); // todo
			agent.Argument.getQuery().putAll(req);

			// TODO choice server
			if (!agent.Send(HttpService.LinkdApp.LinkdProviderService.GetSocket(), (p) -> {
				if (agent.isTimeout())
					HttpService.sendErrorResponse(exchange, "timeout");
				else {
					HttpService.sendResponse(exchange, agent.Result);
				}
				return 0;
			})) {
				HttpService.sendErrorResponse(exchange, "dispatch error");
			}

		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}
}
