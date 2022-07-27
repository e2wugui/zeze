package Zeze.Web;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import Zeze.Builtin.Web.AuthOk;
import Zeze.Builtin.Web.BHttpResponse;
import Zeze.Builtin.Web.RequestJson;
import Zeze.Builtin.Web.RequestQuery;
import Zeze.Net.Rpc;
import Zeze.Util.OutLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerRoot implements HttpHandler {

	private String getPath0(String path) {
		return path.split("/")[0];
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			var path = exchange.getRequestURI().getPath();
			if (path.endsWith("/jsonauth")) {
				var agent = handleJson(exchange);
				if (null != agent) {
					var path0 = getPath0(agent.Argument.getServletName());
					var localAuth = HttpService.linkdApp.WebAuth.get(path0);
					if (null != localAuth) {
						var account = localAuth.auth(path0, agent);
						if (null != account)
							handleAuthOk(exchange, agent.Argument.getServletName(), account, agent.Argument.getCookie());
						else
							HttpService.sendErrorResponse(exchange, "auth fail.");
						return; // done
					}
					choiceProviderAndDispatch(exchange, agent);
				}
				return; // done
			}

			if (path.endsWith("/json")) {
				var agent = handleJson(exchange);
				if (null != agent)
					choiceProviderAndDispatch(exchange, agent);
				return; // done
			}

			if (path.endsWith("/queryauth")) {
				var agent = handleQuery(exchange);
				if (null != agent) {
					var path0 = getPath0(agent.Argument.getServletName());
					var localAuth = HttpService.linkdApp.WebAuth.get(path0);
					if (null != localAuth) {
						var account = localAuth.auth(path0, agent);
						if (null != account)
							handleAuthOk(exchange, agent.Argument.getServletName(), account, agent.Argument.getCookie());
						else
							HttpService.sendErrorResponse(exchange, "auth fail.");
						return; // done
					}
					choiceProviderAndDispatch(exchange, agent);
				}
				return; // done
			}

			if (path.endsWith("/query")) {
				var agent = handleQuery(exchange);
				if (null != agent)
					choiceProviderAndDispatch(exchange, agent);
				return; // done
			}

			HttpService.sendErrorResponse(exchange, "Unknown Proxy Method.");
		} catch (Throwable ex) {
			HttpService.sendErrorResponse(exchange, ex);
		}
	}

	private void handleAuthOk(HttpExchange exchange, String servletName, String account, List<String> cookie) throws IOException {
		var authOk = new AuthOk();
		authOk.Argument.setServletName(servletName);
		authOk.Argument.setAccount(account);
		authOk.Argument.getCookie().addAll(cookie);

		choiceProviderAndDispatch(exchange, authOk);
	}

	private void choiceProviderAndDispatch(HttpExchange exchange, Rpc<?, BHttpResponse> agent) throws IOException {
		var linkApp = HttpService.linkdApp;
		var linkProvider = linkApp.LinkdProvider;
		var serviceName = linkProvider.MakeServiceName(HttpService.WebModuleId);
		var services = linkApp.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		var hash = exchange.getRemoteAddress().getAddress().hashCode();
		var provider = new OutLong();
		if (linkProvider.Distribute.ChoiceHash(services, hash, provider)) {
			if (!agent.Send(linkApp.LinkdProviderService.GetSocket(provider.Value), (p) -> {
				// process http response
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
	}

	private RequestJson handleJson(HttpExchange exchange) throws IOException {
		String json = null;
		switch (exchange.getRequestMethod()) {
		case "GET":
			var query = exchange.getRequestURI().getQuery();
			// 只允许一个参数，并且是json=
			if (!query.startsWith("json=") || query.contains("&")) {
				HttpService.sendErrorResponse(exchange, "JSON PARAMETER, ERROR FORMAT!");
				return null;
			}
			json = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
			break;

		case "POST":
			json = HttpService.readRequestBody(exchange);
			// RequestBody 应该是不需要close的吧？
			break;

		default:
			HttpService.sendErrorResponse(exchange, "Unknown Method.");
			return null;
		}

		var agent = new RequestJson();
		agent.Argument.setServletName(HttpService.parseServletName(exchange));
		var cookie = exchange.getRequestHeaders().get("Cookie");
		if (null != cookie)
			agent.Argument.getCookie().addAll(cookie);
		agent.Argument.setJson(json);

		return agent;
	}

	private RequestQuery handleQuery(HttpExchange exchange) throws IOException {
		var agent = new RequestQuery();
		switch (exchange.getRequestMethod()) {
		case "GET":
			HttpService.parseQuery(exchange.getRequestURI().getQuery(), agent.Argument.getQuery());
			break;

		case "POST":
			HttpService.parseQuery(HttpService.readRequestBody(exchange), agent.Argument.getQuery());
			break;

		default:
			HttpService.sendErrorResponse(exchange, "Unknown Method.");
			return null;
		}

		agent.Argument.setServletName(HttpService.parseServletName(exchange));
		var cookie = exchange.getRequestHeaders().get("Cookie");
		if (null != cookie)
			agent.Argument.getCookie().addAll(cookie);
		return agent;
	}
}
