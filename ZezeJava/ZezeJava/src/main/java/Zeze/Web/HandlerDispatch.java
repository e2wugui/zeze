package Zeze.Web;

import java.io.IOException;
import Zeze.Builtin.Web.Request;
import Zeze.Builtin.Web.RequestInputStream;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Util.OutLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerDispatch implements HttpHandler {
	public final HttpService Service;

	public HandlerDispatch(HttpService httpService) {
		Service = httpService;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		LinkdHttpExchange xout = null;
		try {
			var x = xout = new LinkdHttpExchange(Service, exchange);
			// dispatch request to server
			var req = new Request();
			x.fillRequest(req.Argument);
			choiceProviderAndDispatch(x, req, (p) -> processRequestResult(x, req));
		} catch (Throwable ex) {
			if (null != xout)
				xout.sendErrorResponse(ex);
		}
	}

	protected <A extends Bean, R extends Bean> void choiceProviderAndDispatch(
			LinkdHttpExchange x, Rpc<A, R> req, ProtocolHandle<Rpc<A, R>> resultHandle) throws IOException {

		var linkApp = Service.LinkdApp;
		var linkProvider = linkApp.LinkdProvider;
		var serviceName = linkProvider.MakeServiceName(Web.ModuleId);
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

	protected long processRequestResult(LinkdHttpExchange x, Request req) throws IOException {
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
			x.closeResponseBody();
		}

		if (!x.isRequestBodyClosed()) {
			var input = new RequestInputStream();
			x.fillInput(input.Argument);
			choiceProviderAndDispatch(x, input, (p) -> processRequestInputResult(x, input));
		}
		return 0;
	}

	protected long processRequestInputResult(LinkdHttpExchange x, RequestInputStream req) throws IOException {
		if (req.isTimeout()) {
			x.close();
			return 0;
		}

		if (req.getResultCode() != 0) {
			x.close();
			return 0;
		}

		if (!x.isRequestBodyClosed()) {
			var input2 = new RequestInputStream();
			x.fillInput(input2.Argument);
			choiceProviderAndDispatch(x, input2, (p) -> processRequestInputResult(x, input2));
		}
		return 0;
	}
}
