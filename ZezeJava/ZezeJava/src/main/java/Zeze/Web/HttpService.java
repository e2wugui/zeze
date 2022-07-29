package Zeze.Web;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Zeze.Arch.LinkdApp;
import Zeze.Builtin.Web.BHeader;
import Zeze.Builtin.Web.BRequest;
import Zeze.Builtin.Web.BResponse;
import Zeze.Net.Binary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpService {
	public static Zeze.Arch.LinkdApp linkdApp;
	public static final int WebModuleId = AbstractWeb.ModuleId;
	public static final int RequestBodyMaxSize = 1024 * 1024;


	public static Map<String, String> parseQuery(String query) {
		var result = new HashMap<String, String>();
		if (null == query)
			return result;

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
		return result;
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
