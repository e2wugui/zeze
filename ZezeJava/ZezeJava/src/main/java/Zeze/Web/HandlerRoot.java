package Zeze.Web;

import java.io.IOException;
import java.util.Map;
import Zeze.Builtin.Web.AuthOk;
import Zeze.Builtin.Web.Request;
import Zeze.Builtin.Web.RequestInputStream;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Util.OutLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerRoot implements HttpHandler {

	private static String getPath0(String path) {
		int i = path.indexOf('/');
		if (i > 0)
			return path.substring(0, i);
		if (i < 0)
			return path;
		i = path.indexOf('/', 1);
		return i > 0 ? path.substring(1, i) : path.substring(1);
	}

	public static String LinkdAuthName = "/linkd.auth";
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		var x = new LinkdHttpExchange(exchange);
		try {
			var path = exchange.getRequestURI().getPath();
			if (path.endsWith(LinkdAuthName)) {
				var query = parseQuery(x);
				if (null != query) {
					var path0 = getPath0(path);
					var localAuth = HttpService.linkdApp.WebAuth.get(path0);
					if (null != localAuth) {
						var account = localAuth.auth(path0, query);
						if (null != account)
							handleAuthOk(x, path, account);
						else
							x.sendErrorResponse("auth fail.");
						return; // done
					}
				}
			}
			// dispatch request to server
			var req = new Request();
			x.fillRequest(req.Argument);
			choiceProviderAndDispatch(x, req, (p) -> processRequestResult(x, req));
		} catch (Throwable ex) {
			x.sendErrorResponse(ex);
		}
	}

	private void handleAuthOk(LinkdHttpExchange x, String servletName, String account) throws IOException {
		var authOk = new AuthOk();
		x.fillRequest(authOk.Argument.getRequest());
		authOk.Argument.setAccount(account);

		choiceProviderAndDispatch(x, authOk, (p) -> {
			return 0;
		});
	}

	private <A extends Bean, R extends Bean> void choiceProviderAndDispatch(
			LinkdHttpExchange x, Rpc<A, R> req, ProtocolHandle<Rpc<A, R>> resultHandle) throws IOException {

		var linkApp = HttpService.linkdApp;
		var linkProvider = linkApp.LinkdProvider;
		var serviceName = linkProvider.MakeServiceName(HttpService.WebModuleId);
		var services = linkApp.Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		var hash = x.exchange.getRemoteAddress().getAddress().hashCode();
		var provider = new OutLong();
		if (!linkProvider.Distribute.ChoiceHash(services, hash, provider)) {
			x.sendErrorResponse("Provider Not Found.");
			x.close();
			return;
		}
		if (!req.Send(linkApp.LinkdProviderService.GetSocket(provider.Value), resultHandle)) {
			x.sendErrorResponse("Distribute error.");
			x.close();
			return;
		}
	}

	private long processRequestResult(LinkdHttpExchange x, Request req) throws IOException {
		// process http response
		if (req.isTimeout()) {
			x.sendErrorResponse("timeout.");
			x.close();
			return 0;
		}
		if (req.getResultCode() != 0) {
			x.sendErrorResponse("ResultCode=" + req.getResultCode()
					+ "\nMessage=" + req.Result.getMessage()
					+ "\n" + req.Result.getStacktrace());
			x.close();
			return 0;
		}
		x.sendResponse(req.Result);
		if (req.Result.isFinish()) {
			x.closeOutputStream();
		}

		if (!x.isInputStreamClosed()) {
			var input = new RequestInputStream();
			x.fillInput(input.Argument);
			choiceProviderAndDispatch(x, input, (p) -> processRequestInputResult(x, input));
		}
		return 0;
	}

	private long processRequestInputResult(LinkdHttpExchange x, RequestInputStream req) throws IOException {
		if (req.isTimeout()) {
			x.close();
			return 0;
		}

		if (req.getResultCode() != 0) {
			x.close();
			return 0;
		}

		if (!x.isInputStreamClosed()) {
			var input2 = new RequestInputStream();
			x.fillInput(input2.Argument);
			choiceProviderAndDispatch(x, input2, (p) -> processRequestInputResult(x, input2));
		}
		return 0;
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
