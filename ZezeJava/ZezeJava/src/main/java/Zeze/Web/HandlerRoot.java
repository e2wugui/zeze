package Zeze.Web;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import Zeze.Builtin.Web.RequestJson;
import Zeze.Builtin.Web.RequestQuery;
import Zeze.Util.OutLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerRoot implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		var path = exchange.getRequestURI().getPath();
		if (path.endsWith("/json"))
			handleJson(exchange);
		else if (path.endsWith("/query"))
			handleQuery(exchange);
		else
			HttpService.sendErrorResponse(exchange, "Unknown Proxy Method.");
	}

	private void handleJson(HttpExchange exchange) throws IOException {
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

			var agent = new RequestJson();
			agent.Argument.setServletName(HttpService.parseServletName(exchange));
			var cookie = exchange.getRequestHeaders().get("Cookie");
			if (null != cookie)
				agent.Argument.getCookie().addAll(cookie);
			agent.Argument.setJson(json);

			// HttpService.sendErrorResponse(exchange, json + "@" + HttpService.parseServletName(exchange));
			var linkApp = HttpService.linkdApp;
			var linkProvider = linkApp.LinkdProvider;
			var serviceName = linkProvider.MakeServiceName(HttpService.WebModuleId);
			var services = linkApp.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);;
			var hash = exchange.getRemoteAddress().getAddress().hashCode();
			var provider = new OutLong();
			if (linkProvider.Distribute.ChoiceHash(services, hash, provider)) {
				if (!agent.Send(HttpService.linkdApp.LinkdProviderService.GetSocket(provider.Value), (p) -> {
					if (agent.isTimeout()) {
						HttpService.sendErrorResponse(exchange, "timeout.");
					} else if (agent.getResultCode() != 0) {
						HttpService.sendErrorResponse(exchange, "ResultCode=" + agent.getResultCode());
					}
					else {
						HttpService.sendResponse(exchange, agent.Result);
					}
					return 0;
				})) {
					HttpService.sendErrorResponse(exchange, "Distribute error.");
				}
			} else {
				HttpService.sendErrorResponse(exchange, "Provider Not Found.");
			}
		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}

	private void handleQuery(HttpExchange exchange) throws IOException {
		try {
			var agent = new RequestQuery();
			switch (exchange.getRequestMethod()) {
			case "GET":
				HttpService.parseQuery(exchange.getRequestURI().getQuery(), agent.Argument.getQuery());
				break;

			case "POST":
				HttpService.parseQuery(HttpService.readRequestBody(exchange), agent.Argument.getQuery());
				break;
			}

			agent.Argument.setServletName(HttpService.parseServletName(exchange));
			var cookie = exchange.getRequestHeaders().get("Cookie");
			if (null != cookie)
				agent.Argument.getCookie().addAll(cookie);

			// HttpService.sendErrorResponse(exchange, agent.Argument.getQuery() + "@" + HttpService.parseServletName(exchange));

			var linkApp = HttpService.linkdApp;
			var linkProvider = linkApp.LinkdProvider;
			var serviceName = linkProvider.MakeServiceName(HttpService.WebModuleId);
			var services = linkApp.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);;
			var hash = exchange.getRemoteAddress().getAddress().hashCode();
			var provider = new OutLong();
			if (linkProvider.Distribute.ChoiceHash(services, hash, provider)) {
				if (!agent.Send(linkApp.LinkdProviderService.GetSocket(provider.Value), (p) -> {
					if (agent.isTimeout()) {
						HttpService.sendErrorResponse(exchange, "timeout.");
					} else if (agent.getResultCode() != 0) {
						HttpService.sendErrorResponse(exchange, "ResultCode=" + agent.getResultCode());
					} else {
						HttpService.sendResponse(exchange, agent.Result);
					}
					return 0;
				})) {
					HttpService.sendErrorResponse(exchange, "Distribute error.");
				}
			} else {
				HttpService.sendErrorResponse(exchange, "Provider Not Found.");
			}
		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}
}
