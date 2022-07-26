package Zeze.Web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpService {
	public final static ExecutorService Executor = Executors.newFixedThreadPool(100);
	public static void main(String args[]) throws IOException {
		var addr = new InetSocketAddress(80);
		var server = HttpServer.create(addr, 100);
		// 对Linkd来说，所有的请求处理都是异步的，设置Executor不是很必要。
		// 但对于大负载，单个后台线程会不会忙不过来？
		server.setExecutor(null);
		server.createContext("/hello", new HelloHandler());
		server.createContext("/handler/json", new HandlerJson());
		server.createContext("/handler/protocol", new HandlerProtocol());
		server.createContext("/handler/query", new HandlerQuery());
		server.start();
	}

	public static class HelloHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			var query = exchange.getRequestURI().getQuery();
			// 异步实验。
			Executor.execute(() -> {
				OutputStream response = null;
				try {
					exchange.sendResponseHeaders(200, 0);
					response = exchange.getResponseBody();
					response.write(("hello: " + query).getBytes(StandardCharsets.UTF_8));
				} catch (Throwable ex) {
					ex.printStackTrace(new PrintStream(response));
				} finally {
					try {
						if (null != response)
							response.close();
					} catch (IOException e) {
						// skip
					}
				}
			});
		}
	}
}
