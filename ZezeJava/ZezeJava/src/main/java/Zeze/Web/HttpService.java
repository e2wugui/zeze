package Zeze.Web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import Zeze.Arch.LinkdApp;
import Zeze.Builtin.Web.CloseExchange;
import Zeze.Builtin.Web.ResponseOutputStream;
import Zeze.IModule;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpService {
	public static final int RequestBodyMaxSize = 1024 * 1024;

	public final Zeze.Arch.LinkdApp LinkdApp;

	final ConcurrentHashMap<Long, LinkdHttpExchange> Exchanges = new ConcurrentHashMap<>();

	final PersistentAtomicLong ExchangeIdPal;

	public long InternalCloseExchange(CloseExchange r) {
		Exchanges.remove(r.Argument.getExchangeId());
		r.SendResult();
		return 0;
	}

	public long InternalResponseOutputStream(ResponseOutputStream r) {
		var x = Exchanges.get(r.Argument.getExchangeId());
		if (null == x) {
			r.SendResultCode(IModule.ErrorCode(Web.ModuleId, Web.ExchangeIdNotFound));
			return 0;
		}
		try {
			x.sendResponse(r.Argument);
			r.SendResult();
		} catch (Throwable ex) {
			r.SendResultCode(IModule.ErrorCode(Web.ModuleId, Web.OnDownloadException));
		}
		return 0;
	}

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

	public HttpService(LinkdApp app, int port, Executor executor) throws IOException {
		LinkdApp = app;
		var addr = new InetSocketAddress(port);
		httpServer = HttpServer.create(addr, 100);
		httpServer.setExecutor(executor);
		httpServer.createContext("/", new HandlerDispatch(this));
		ExchangeIdPal = PersistentAtomicLong.getOrAdd(app.GetName() + ".http");
	}

	public void interceptAuthContext(String path, HttpAuth auth) {
		httpServer.createContext(path, auth);
	}

	public void start() {
		httpServer.start();
		Task.schedule(2000, 2000, this::timer);
	}

	public void stop() {
		httpServer.stop(10);
	}

	private void timer() {
		var now = System.currentTimeMillis();
		for (var x : Exchanges.values())
			x.tryCloseIfTimeout(now);
	}
}
