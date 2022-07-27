package Zeze.Web;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Zeze.Builtin.Web.BHttpResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpService {
	public static Zeze.Arch.LinkdApp LinkdApp;

	public final static int WebModuleId = AbstractWeb.ModuleId;

	public static void sendResponse(HttpExchange exchange, BHttpResponse response) throws IOException {
		var headers = exchange.getResponseHeaders();
		headers.put("Content-Type", List.of(response.getContentType()));
		headers.put("Set-Cookie2", response.getCookie());

		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			body.write(response.getBody().InternalGetBytesUnsafe());
		}
	}

	public static String parseServletName(HttpExchange exchange) {
		return exchange.getRequestURI().getPath();
		/*
		var path = exchange.getRequestURI().getPath();
		var last = path.lastIndexOf('/');
		return path.substring(0, last);
		*/
	}

	public static void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=utf-8"));
		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			body.write(message.getBytes(StandardCharsets.UTF_8));
		}
	}

	public static void sendErrorResponse(HttpExchange exchange, Throwable ex) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=UTF-8"));
		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			ex.printStackTrace(new PrintStream(body, false, "utf-8"));
		}
	}

	public static void parseQuery(String query, Map<String, String> result) {
		var items = query.split("&");
		for (var item : items) {
			var pair = item.split("=");
			var key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
			var val = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
			result.put(key,val);
		}
	}

	public static String readRequestBody(HttpExchange exchange) throws IOException {
		try (var body = exchange.getRequestBody()) {
			return new String(body.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public final static ExecutorService Executor = Executors.newFixedThreadPool(100);
	public static void main(String args[]) throws IOException {
		var addr = new InetSocketAddress(80);
		var server = HttpServer.create(addr, 100);
		// 对Linkd来说，所有的请求处理都是异步的，设置Executor不是很必要。
		// 但对于大负载，单个后台线程会不会忙不过来？
		server.setExecutor(null);
		server.createContext("/", new HandlerRoot());
		server.start();
	}
}
