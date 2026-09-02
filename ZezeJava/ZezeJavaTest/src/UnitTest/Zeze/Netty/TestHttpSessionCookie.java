package UnitTest.Zeze.Netty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import demo.App;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// N-17回归：HttpSession.getCookieSession 由 channelRead 在 eventLoop 线程调用，
// 无当前事务直接访问 _tSession，启用会话后每个请求在 handler 执行前必炸（500+断连）；
// 且 enableHttpSession 从不注册会话表。通过 demo.App 启用后，
// 不带/带 Cookie 各请求一次，验证会话创建、Set-Cookie 与属性跨请求持久。
public class TestHttpSessionCookie {
	private static final String Path = "/testCookieSession";

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		var app = App.getInstance();
		app.Start();
		app.HttpServer.addHandler(Path, 8192, TransactionLevel.Serializable, DispatchMode.Direct,
				TestHttpSessionCookie::onRequest);
	}

	private static void onRequest(HttpExchange x) {
		var cs = x.getCookieSession();
		var old = cs.getProperty("count");
		var count = old == null ? 0 : Integer.parseInt(old);
		cs.setProperty("count", String.valueOf(count + 1));
		x.sendPlainText(HttpResponseStatus.OK, String.valueOf(count));
	}

	@Test
	public void testCookieSession() throws Exception {
		var uri = URI.create("http://127.0.0.1:10000" + Path);

		// 第一次请求：无Cookie，应创建会话并Set-Cookie，计数返回0。
		var res1 = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res1.statusCode());
		Assertions.assertEquals("0", res1.body());
		var setCookie = res1.headers().firstValue("set-cookie").orElse(null);
		Assertions.assertNotNull(setCookie, "第一次请求必须Set-Cookie");
		Assertions.assertTrue(setCookie.startsWith("ZEZESESSIONID="), "set-cookie=" + setCookie);
		var sessionId = setCookie.substring("ZEZESESSIONID=".length()).split(";", 2)[0];
		Assertions.assertFalse(sessionId.isEmpty());

		// 第二次请求：带回Cookie，同一会话，计数累积到1（属性跨请求持久），未过期不再Set-Cookie。
		var res2 = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri)
				.header("Cookie", "ZEZESESSIONID=" + sessionId).GET().build(), HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res2.statusCode());
		Assertions.assertEquals("1", res2.body());
		Assertions.assertTrue(res2.headers().firstValue("set-cookie").isEmpty(), "未过期不应重复Set-Cookie");
	}
}
