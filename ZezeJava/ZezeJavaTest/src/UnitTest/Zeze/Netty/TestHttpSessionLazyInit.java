package UnitTest.Zeze.Netty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import demo.App;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Transaction;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-7回归:HttpSession.getCookieSession原由channelRead在EventLoop线程同步执行DB事务
 * (表缓存miss时读盘/乐观锁redo循环都发生在IO线程,DB抖动期间该EventLoop上所有连接停摆)。
 * 修复后session初始化延迟到用户handler首次调用HttpExchange.getCookieSession()时执行:
 * Normal派发的handler运行在池线程的事务里,session表访问同事务完成,不再触碰EventLoop。
 * 本测试依赖demo.App的数据库环境,耗时较长,不标@Fast(integrationTest)。
 */
public class TestHttpSessionLazyInit {
	private static final String Path = "/testLazyCookieSession";

	private static volatile boolean inEventLoopWhenCalled;
	private static volatile boolean inTransactionWhenCalled;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		var app = App.getInstance();
		app.Start();
		// Normal派发(默认):handler在oneByOne队列的池线程事务里执行
		app.HttpServer.addHandler(Path, 8192, TransactionLevel.Serializable, DispatchMode.Normal,
				TestHttpSessionLazyInit::onRequest);
	}

	private static void onRequest(HttpExchange x) throws Exception {
		inEventLoopWhenCalled = x.channel().eventLoop().inEventLoop();
		inTransactionWhenCalled = Transaction.getCurrent() != null;
		var cs = x.getCookieSession(); // lazy初始化:应发生在当前(池)线程的当前事务内
		Assertions.assertFalse(x.channel().eventLoop().inEventLoop(),
				"session init must not run on the channel EventLoop");
		Assertions.assertNotNull(Transaction.getCurrent(), "session init should join the caller transaction");
		var old = cs.getProperty("count");
		var count = old == null ? 0 : Integer.parseInt(old);
		cs.setProperty("count", String.valueOf(count + 1));
		x.sendPlainText(HttpResponseStatus.OK, String.valueOf(count));
	}

	@Test
	public void testLazyCookieSession() throws Exception {
		var uri = URI.create("http://127.0.0.1:10000" + Path);

		// 第一次请求:无Cookie,创建会话并Set-Cookie,计数返回0
		var res1 = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res1.statusCode());
		Assertions.assertEquals("0", res1.body());
		var setCookie = res1.headers().firstValue("set-cookie").orElse(null);
		Assertions.assertNotNull(setCookie, "首次请求必须Set-Cookie");
		Assertions.assertTrue(setCookie.startsWith("ZEZESESSIONID="), "set-cookie=" + setCookie);
		var sessionId = setCookie.substring("ZEZESESSIONID=".length()).split(";", 2)[0];

		// 第二次请求:带回Cookie,同会话计数累积(属性跨请求持久),未过期不再Set-Cookie
		var res2 = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri)
				.header("Cookie", "ZEZESESSIONID=" + sessionId).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res2.statusCode());
		Assertions.assertEquals("1", res2.body());
		Assertions.assertTrue(res2.headers().firstValue("set-cookie").isEmpty(), "未过期不应重复Set-Cookie");

		// 两次请求的handler均派发到池线程的事务内执行、session初始化未在EventLoop上
		// （onRequest 内已逐项断言，这里在请求完成后汇总兜底——静态标志需请求后才有值）
		Assertions.assertFalse(inEventLoopWhenCalled, "handler must be dispatched off the EventLoop (Normal mode)");
		Assertions.assertTrue(inTransactionWhenCalled, "handler must run inside a transaction (Serializable)");
	}
}
