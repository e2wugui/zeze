package Zeze.Web;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import Zeze.Arch.LinkdApp;
import Zeze.Builtin.Web.BHttpResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpService {
	public static Zeze.Arch.LinkdApp linkdApp;
	public static final int WebModuleId = AbstractWeb.ModuleId;
	public static final int RequestBodyMaxSize = 1024 * 1024;

	public static void sendResponse(HttpExchange exchange, BHttpResponse response) throws IOException {
		var headers = exchange.getResponseHeaders();
		headers.put("Content-Type", List.of(response.getContentType()));
		headers.put("Set-Cookie2", response.getCookie());

		var bytes = response.getBody().InternalGetBytesUnsafe();
		var len = bytes.length;
		exchange.sendResponseHeaders(200, len > 0 ? len : -1);
		if (len > 0) {
			try (var body = exchange.getResponseBody()) {
				body.write(bytes);
			}
		}
	}

	public static String parseServletName(HttpExchange exchange) {
		var path = exchange.getRequestURI().getPath();
		var last = path.lastIndexOf('/');
		return path.substring(0, last);
	}

	public static void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=utf-8"));

		var bytes = message.getBytes(StandardCharsets.UTF_8);
		var len = bytes.length;
		exchange.sendResponseHeaders(200, len > 0 ? len : -1);
		if (len > 0) {
			try (var body = exchange.getResponseBody()) {
				body.write(bytes);
			}
		}
	}

	public static void sendErrorResponse(HttpExchange exchange, Throwable ex) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=UTF-8"));
		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			ex.printStackTrace(new PrintStream(body, false, StandardCharsets.UTF_8));
		}
	}

	public static void parseQuery(String query, Map<String, String> result) {
		if (null == query)
			return;

		var items = query.split("&");
		for (var item : items) {
			if (item.isEmpty())
				continue;
			var i = item.indexOf('=');
			if (i < 0)
				i = item.length();
			var key = URLDecoder.decode(item.substring(0, i), StandardCharsets.UTF_8);
			var val = URLDecoder.decode(item.substring(i), StandardCharsets.UTF_8);
			result.put(key,val);
		}
	}

	public static String readRequestBody(HttpExchange exchange) throws IOException {
		try (var body = exchange.getRequestBody()) {
			return new String(body.readNBytes(RequestBodyMaxSize), StandardCharsets.UTF_8);
		}
	}

	private final HttpServer httpServer;

	public HttpService(LinkdApp app, int port) throws IOException {
		linkdApp = app;
		var addr = new InetSocketAddress(port);
		httpServer = HttpServer.create(addr, 100);
		// 对Linkd来说，所有的请求处理都是异步的，设置Executor不是很必要。
		// 但对于大负载，单个后台线程会不会忙不过来？
		httpServer.setExecutor(null);
		httpServer.createContext("/", new HandlerRoot());
		httpServer.start();
	}

	public void stop() {
		httpServer.stop(10);
	}
}
