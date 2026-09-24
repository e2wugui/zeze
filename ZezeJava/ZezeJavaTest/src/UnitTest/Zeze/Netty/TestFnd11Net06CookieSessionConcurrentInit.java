package UnitTest.Zeze.Netty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.CountDownLatch;
import demo.App;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpSession;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND11 net-06回归：getCookieSession的cookieSession字段为无同步check-then-act——
 * handler内detach后（文档明示"任意线程、任意时机"合法）多线程并发首调，双双观察到null
 * 各自建会话：DB多出一行冗余会话、响应携带两条互异Set-Cookie、字段后写覆盖先写
 * （线程A写的properties与客户端最终持有的cookie不一致）。
 * 修复后volatile+锁双检：会话只建一次、Set-Cookie只发一条、并发调用拿到同一实例。
 * 依赖demo.App的数据库环境（integrationTest，不标@Fast）。
 */
public class TestFnd11Net06CookieSessionConcurrentInit {
	private static final String Path = "/testFnd11Net06ConcurrentCookieSession";

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		var app = App.getInstance();
		app.Start();
		app.HttpServer.addHandler(Path, 8192, TransactionLevel.Serializable, DispatchMode.Normal,
				TestFnd11Net06CookieSessionConcurrentInit::onRequest);
	}

	private static void onRequest(HttpExchange x) throws Exception {
		x.detach(); // 移交任意线程：制造并发首调窗口
		var barrier = new CyclicBarrier(2);
		var done = new CountDownLatch(2);
		var results = new AtomicReferenceArray<HttpSession.CookieSession>(2);
		var failure = new AtomicReferenceArray<Throwable>(2);
		for (int i = 0; i < 2; i++) {
			var idx = i;
			var t = new Thread(() -> {
				try {
					//noinspection ResultOfMethodCallIgnored
					barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
					results.set(idx, x.getCookieSession());
				} catch (Throwable e) {
					failure.set(idx, e);
				} finally {
					done.countDown();
				}
			}, "fnd11-net06-" + idx);
			t.start();
		}
		Assertions.assertTrue(done.await(15, java.util.concurrent.TimeUnit.SECONDS), "并发首调必须完成");
		for (int i = 0; i < 2; i++)
			if (failure.get(i) != null)
				throw new AssertionError("并发首调线程" + i + "异常", failure.get(i));
		Assertions.assertSame(results.get(0), results.get(1), "并发首调必须拿到同一CookieSession实例");
		Assertions.assertNotNull(results.get(0));
		x.sendPlainText(HttpResponseStatus.OK, "done");
	}

	@Test
	public void testConcurrentFirstInitSingleSession() throws Exception {
		var uri = URI.create("http://127.0.0.1:10000" + Path);
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode(), "body=" + res.body());
		Assertions.assertEquals("done", res.body());
		// 双跑会各addHeader一条Set-Cookie：并发首调后必须恰好一条
		var setCookies = res.headers().map().get("set-cookie");
		Assertions.assertNotNull(setCookies, "首次请求必须Set-Cookie");
		Assertions.assertEquals(1, setCookies.size(), "并发首调只能建一个会话发一条Set-Cookie: " + setCookies);
	}
}
