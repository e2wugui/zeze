package Zeze.Web;

import java.io.IOException;
import java.util.Map;
import Zeze.Builtin.Web.Request;
import com.sun.net.httpserver.HttpExchange;

public abstract class HttpAuth extends HandlerDispatch {
	public abstract String auth(Map<String, String> params) throws Throwable;

	public HttpAuth(HttpService service) {
		super(service);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		LinkdHttpExchange xout = null;
		try {
			var x = xout = new LinkdHttpExchange(service, exchange);
			var query = parseQuery(x);
			if (null != query) {
				var account = auth(query);
				if (null != account) {
					var req = new Request();
					x.fillRequest(req.Argument);
					req.Argument.setAuthedAccount(account);
					choiceProviderAndDispatch(x, req, (p) -> processRequestResult(x, req));
					return;
				}
				x.sendErrorResponse("auth fail.");
			}
		} catch (Throwable ex) {
			if (null != xout) {
				xout.sendErrorResponse(ex);
				xout.close(); // 一般发生在 dispatch 前，直接关闭。
			}
		}
	}

	private static Map<String, String> parseQuery(LinkdHttpExchange x) throws IOException {
		switch (x.exchange.getRequestMethod()) {
		case "GET":
			return HttpService.parseQuery(x.exchange.getRequestURI().getQuery());

		case "POST":
			return HttpService.parseQuery(HttpService.readRequestBody(x.exchange));

		default:
			x.sendErrorResponse("Unknown Method.");
			return null;
		}
	}
}
