package Zeze.Web;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import Zeze.Builtin.Web.BHeader;
import Zeze.Builtin.Web.BRequest;
import Zeze.Builtin.Web.BResponse;
import Zeze.Builtin.Web.BStream;
import Zeze.Builtin.Web.CloseExchange;
import Zeze.Net.Binary;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import com.sun.net.httpserver.HttpExchange;

public class LinkdHttpExchange {
	// private static final Logger logger = LogManager.getLogger(LinkdHttpExchange.class);

	private final long exchangeId;
	long provider;

	private long activeTime = System.currentTimeMillis();

	private final HttpService service;
	final HttpExchange exchange;

	private boolean responseBodyClosed = false;
	private boolean requestBodyClosed = false;

	// HttpServer提供InputStream，为了判断是否读取完成需要多读一次。
	// 为了避免这个额外的读取（导致转给Server的Rpc多一次）。
	// 使用下面的两个变量，当提供了Content-Length并且requestTransferLength等于它时，马上设置Finish。
	private int postContentLength = -1;
	private int requestTransferLength; // 已经传输的长度。

	public boolean isRequestBodyClosed() {
		return requestBodyClosed;
	}

	public boolean isResponseBodyClosed() {
		return responseBodyClosed;
	}

	protected <A extends Bean, R extends Bean> void redispatch(
			Rpc<A, R> req, ProtocolHandle<Rpc<A, R>> resultHandle) {

		if (!req.Send(service.linkdApp.LinkdProviderService.GetSocket(provider), resultHandle)) {
			close(true); // 重新派发错误时，尝试通知server。
		}
	}

	public void closeResponseBody() throws IOException {
		if (responseBodyClosed)
			return;
		exchange.getResponseBody().close();
		responseBodyClosed = true;
		tryClose();
	}

	public void tryCloseIfTimeout(long now) {
		if (now - activeTime > 10 * 1000)
			close(true);
	}

	public void closeRequestBody() throws IOException {
		if (requestBodyClosed)
			return;
		exchange.getRequestBody().close();
		requestBodyClosed = true;
		tryClose();
	}

	private void tryClose() {
		//logger.info("tryClose " + exchange.getRequestURI().getPath() + " " + requestBodyClosed + " " + responseBodyClosed, new RuntimeException());
		if (requestBodyClosed && responseBodyClosed)
			close(); // 正常关闭。
	}

	public LinkdHttpExchange(HttpService s, HttpExchange x) {
		exchangeId = s.exchangeIdPal.next();
		if (null != s.exchanges.putIfAbsent(exchangeId, this))
			throw new IllegalStateException("Impossible! duplicate exchangeId.");
		exchange = x;
		service = s;
	}

	public void close() {
		close(false);
	}

	public void close(boolean closeServer) {
		if (null != service.exchanges.remove(exchangeId)) {
			exchange.close();
			if (closeServer) {
				var ce = new CloseExchange();
				ce.Argument.setExchangeId(exchangeId);
				ce.Send(service.linkdApp.LinkdProviderService.GetSocket(provider)); // no wait; no check error.
			}
		}
	}

	public void fillInput(BStream s) throws IOException {
		s.setExchangeId(exchangeId);
		tryReadInputBody((finish, body) -> {
			s.setFinish(finish);
			s.setBody(body);
		});
	}

	/**
	 * 注意填充req时不会重置其中的变量，所以要求req必须是新创建的Rpc的Argument。
	 * 不能重用Rpc和BRequest。
	 */
	public void fillRequest(BRequest req) throws IOException {
		req.setExchangeId(exchangeId);
		req.setMethod(exchange.getRequestMethod());
		req.setPath(exchange.getRequestURI().getPath());
		var query = exchange.getRequestURI().getQuery();
		if (null != query)
			req.setQuery(query);
		for (var e : exchange.getRequestHeaders().entrySet()) {
			var header = new BHeader();
			header.getValues().addAll(e.getValue());
			req.getHeaders().put(e.getKey(), header);
		}

		var method = exchange.getRequestMethod();
		switch (method) {
		case "GET":
		case "HEAD":
			req.setFinish(true);
			closeRequestBody();
			return; // done

		case "POST":
			var cl = exchange.getRequestHeaders().getFirst("Content-Length");
			if (null != cl)
				postContentLength = Integer.parseInt(cl);
			break;
		}

		tryReadInputBody((finish, body) -> {
			req.setFinish(finish);
			req.setBody(body);
		});
	}

	private void tryReadInputBody(BiConsumer<Boolean, Binary> c) throws IOException {
		activeTime = System.currentTimeMillis();
		// 本质上linkd是异步的，需要HttpServer也是异步的。
		// 由于Jdk自带HttpServer是同步的，下面的写法尝试最小化阻塞时间。
		// 但不能避免阻塞，考虑使用【zp的支持异步的HttpServer】。
		// 目前使用设置Executor到HttpServe中，用多线程版本。
		var inputStream = exchange.getRequestBody();
		var available = inputStream.available();
		if (available > 8192)
			available = 8192; // 跟Server之间是包转发，最多一次读取这么多。
		else if (available < 1024)
			available = 1024; // 数据没有准备好，最少读取这个多，这个数字参考了ip包大小（链路层数据包）。

		var body = new byte[available];
		var size = inputStream.read(body);
		if (size >= 0) {
			requestTransferLength += size;
			var finish = postContentLength != -1 && requestTransferLength >= postContentLength;
			if (finish)
				closeRequestBody();
			c.accept(finish, new Binary(body, 0, size));
		} else {
			closeRequestBody();
			c.accept(true, Binary.Empty);
		}
	}

	public void sendResponse(BStream stream) throws IOException {
		activeTime = System.currentTimeMillis();
		exchange.getResponseBody().write(stream.getBody().bytesUnsafe());
		if (stream.isFinish())
			closeResponseBody();
	}

	public void sendResponse(BResponse response) throws IOException {
		activeTime = System.currentTimeMillis();
		var headers = exchange.getResponseHeaders();
		for (var e : response.getHeaders()) {
			var list = new ArrayList<String>(e.getValue().getValues().size());
			list.addAll(e.getValue().getValues());
			headers.put(e.getKey(), list);
		}

		var bytes = response.getBody().bytesUnsafe();
		exchange.sendResponseHeaders(response.getCode(), 0);
		var body = exchange.getResponseBody();
		body.write(bytes);

		if (response.isFinish()) {
			closeResponseBody();
		}
	}

	public void sendErrorResponse(String message) throws IOException {
		sendErrorResponse(200, message);
	}

	public void sendErrorResponse(int code, String message) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=utf-8"));

		var bytes = message.getBytes(StandardCharsets.UTF_8);
		var len = bytes.length;
		exchange.sendResponseHeaders(code, len > 0 ? len : -1);
		if (len > 0) {
			try (var body = exchange.getResponseBody()) {
				body.write(bytes);
			} finally {
				closeResponseBody();
			}
		}
	}

	public void sendErrorResponse(Throwable ex) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=UTF-8"));
		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			ex.printStackTrace(new PrintStream(body, false, StandardCharsets.UTF_8));
		} finally {
			closeResponseBody();
		}
	}
}
