package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import Zeze.Netty.HttpHandler;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-2回归:getHandler前缀匹配不能只回退一次floorEntry。
 * 注册嵌套前缀"/a"和"/abc"后请求"/abd":字典序"/a" &lt; "/abc" &lt; "/abd",
 * floorEntry("/abd")是更长的非前缀键"/abc",修复前直接放弃返回null(404),
 * 丢失了合法前缀"/a"的匹配。修复后必须继续向下回退直到命中前缀或无候选,且保持最长前缀优先。
 */
@Fast
public class TestHttpServerPrefixHandler {
	private static Netty netty;
	private static HttpServer server;
	private static int port;
	private static HttpHandler hExact;
	private static HttpHandler hPrefixA;
	private static HttpHandler hPrefixABC;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure, Direct派发即inline执行
		server.addHandler("/exact", hExact = new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, "exact")));
		server.addPrefixHandler("/a", hPrefixA = new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, "prefixA")));
		server.addPrefixHandler("/abc", hPrefixABC = new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, "prefixABC")));
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	// 纯逻辑断言:不依赖网络,直接覆盖getHandler的各种匹配分支
	@Test
	public void testGetHandlerLogic() {
		// 嵌套前缀漏配场景(核心缺陷):floorEntry("/abd")="/abc"非前缀,必须回退命中"/a"
		Assertions.assertSame(hPrefixA, server.getHandler("/abd"));

		// 最长前缀优先:/abcd先命中"/abc"
		Assertions.assertSame(hPrefixABC, server.getHandler("/abcd"));

		// 前缀等于路径本身:floorEntry直接命中
		Assertions.assertSame(hPrefixABC, server.getHandler("/abc"));

		// floorEntry命中第一个候选
		Assertions.assertSame(hPrefixA, server.getHandler("/ab"));

		// 精确匹配优先于前缀
		Assertions.assertSame(hExact, server.getHandler("/exact"));

		// 多级嵌套+干扰键:字典序更大的非前缀键不能挡住更短的真实前缀
		var server2 = new HttpServer();
		var hA = newHandler();
		var hABC = newHandler();
		var hABCD = newHandler();
		var hZZZ = newHandler();
		server2.addPrefixHandler("/a", hA);
		server2.addPrefixHandler("/abc", hABC);
		server2.addPrefixHandler("/abcd", hABCD);
		server2.addPrefixHandler("/zzz", hZZZ); // 干扰键:字典序大于目标路径的场景下首查不中
		Assertions.assertSame(hA, server2.getHandler("/abd"));
		Assertions.assertSame(hABCD, server2.getHandler("/abcde"));
		Assertions.assertSame(hABCD, server2.getHandler("/abcdx")); // floorEntry("/abcdx")="/abcd"命中
		Assertions.assertSame(hABC, server2.getHandler("/abcx")); // floorEntry("/abcx")="/abc"命中
		Assertions.assertSame(hA, server2.getHandler("/ab")); // floorEntry("/ab")="/a"命中
		Assertions.assertSame(hZZZ, server2.getHandler("/zzzzz/1"));
		Assertions.assertNull(server2.getHandler("/b"));
		Assertions.assertNull(server2.getHandler("/none"));

		// 空前缀表
		Assertions.assertNull(new HttpServer().getHandler("/x"));
	}

	private static @NotNull HttpHandler newHandler() {
		return new HttpHandler(0, TransactionLevel.None, DispatchMode.Direct, x -> {
		});
	}

	// 端到端:真socket请求验证404变命中
	@Test
	public void testGetHandlerHttp() throws Exception {
		var client = HttpClient.newHttpClient();
		// 嵌套前缀场景:修复前404,修复后由"/a"前缀处理器返回prefixA
		var res = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/abd")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		Assertions.assertEquals("prefixA", res.body());

		// 最长前缀优先
		var res2 = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/abcdef")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res2.statusCode());
		Assertions.assertEquals("prefixABC", res2.body());

		// 无任何匹配仍404
		var res3 = client.send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/zzz")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(404, res3.statusCode());
	}
}
