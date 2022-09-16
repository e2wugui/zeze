package Zeze.Web;

import java.io.IOException;
import Zeze.Builtin.Web.Request;
import Zeze.Builtin.Web.RequestInputStream;
import Zeze.IModule;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Util.OutLong;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerDispatch implements HttpHandler {
	protected final HttpService service;

	public HandlerDispatch(HttpService httpService) {
		service = httpService;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		LinkdHttpExchange xout = null;
		try {
			var x = xout = new LinkdHttpExchange(service, exchange);
			// dispatch request to server
			var req = new Request();
			x.fillRequest(req.Argument);
			choiceProviderAndDispatch(x, req, (p) -> processRequestResult(x, req));
		} catch (Throwable ex) {
			if (null != xout) {
				xout.sendErrorResponse(ex);
				xout.close(); // 一般发生在 dispatch 之前，直接关闭。
			}
		}
	}

	protected <A extends Bean, R extends Bean> void choiceProviderAndDispatch(
			LinkdHttpExchange x, Rpc<A, R> req, ProtocolHandle<Rpc<A, R>> resultHandle) throws IOException {
		var linkApp = service.linkdApp;
		var linkProvider = linkApp.linkdProvider;
		var serviceName = linkProvider.makeServiceName(Web.ModuleId);
		var services = linkApp.zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (services == null)
			throw new UnsupportedOperationException("not found service name: " + serviceName);
		var hash = x.exchange.getRemoteAddress().getAddress().hashCode();
		var provider = new OutLong();
		if (!linkProvider.getDistribute().choiceHash(services, hash, provider)) {
			x.sendErrorResponse("Provider Not Found.");
			x.close(); // 请求还没有转给server，直接关闭。
			return;
		}
		x.provider = provider.value; // 保存选中的server，重新派发或者报错时再次使用。
		if (!req.Send(linkApp.linkdProviderService.GetSocket(provider.value), resultHandle)) {
			x.sendErrorResponse("Distribute error.");
			x.close(); // 请求还没有转给server，直接关闭。
		}
	}

	private static int internalErrorToHttpCode(long error) {
		int code = IModule.getErrorCode(error);
		switch (code) {
		case 0:
			return 200;
		case Web.UnknownPath404:
			return 404;
		case Web.ServletException:
			return 500;
		default:
			return 510 + code;
		}
	}

	protected long processRequestResult(LinkdHttpExchange x, Request req) throws IOException {
		// process http response
		if (req.isTimeout()) {
			x.sendErrorResponse("timeout.");
			x.close(true); // timeout，尝试通知server关闭。
			return 0;
		}
		if (req.getResultCode() != 0) {
			x.sendErrorResponse(internalErrorToHttpCode(req.getResultCode()),
					"ResultCode=" + req.getResultCode()
							+ "\nMessage=" + req.Result.getMessage()
							+ "\n" + req.Result.getStacktrace());
			x.close(); // server 返回错误，直接关闭。
			return 0;
		}
		x.sendResponse(req.Result);
		if (!x.isRequestBodyClosed()) {
			var input = new RequestInputStream();
			x.fillInput(input.Argument);
			x.redispatch(input, (p) -> processRequestInputResult(x, input));
		}
		return 0;
	}

	@SuppressWarnings("MethodMayBeStatic")
	protected long processRequestInputResult(LinkdHttpExchange x, RequestInputStream req) throws IOException {
		if (req.isTimeout()) {
			x.close(true); // timeout 尝试通知server关闭
			return 0;
		}

		if (req.getResultCode() != 0) {
			x.close(); // server返回错误，直接关闭。
			return 0;
		}

		if (!x.isRequestBodyClosed()) {
			var input2 = new RequestInputStream();
			x.fillInput(input2.Argument);
			x.redispatch(input2, (p) -> processRequestInputResult(x, input2));
		}
		return 0;
	}
}
