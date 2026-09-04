package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-N2-4回归:close()把task11Executor shutdown(true)后不可逆,但scheduler置null允许再start()。
 * 重启表面完全成功(重新bind、重新起调度),之后非Direct请求的派发submit命中isShutdown队列被
 * 静默丢弃——全部无响应黑洞(连接挂到读空闲超时),无任何日志线索。修复后start()检测队列已死则重建。
 */
@Fast
public class TestHttpServerRestart {
	private Netty netty;
	private HttpServer server;

	// Normal模式(noProcedure):请求经task11Executor派发执行,正是重启后会被黑洞的路径
	private static HttpServer newServer() {
		var s = new HttpServer();
		s.addHandler("/ok", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
		return s;
	}

	private static int startAndGetPort(HttpServer s, Netty n) throws Exception {
		return ((InetSocketAddress)s.start(n, 0).sync().channel().localAddress()).getPort();
	}

	private static String httpGet(int port) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder()
						.uri(URI.create("http://127.0.0.1:" + port + "/ok"))
						.timeout(Duration.ofSeconds(10)) // 修复前黑洞:请求永远无响应,这里超时失败
						.GET().build(),
				HttpResponse.BodyHandlers.ofString()).body();
	}

	// 同一HttpServer实例close→start重启:非Direct请求必须仍能正常响应。
	// 修复前task11Executor已死,重启后的请求被静默丢弃,httpGet超时。
	@Test
	public void testRestartAfterClose() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = newServer();
		try {
			Assertions.assertEquals("ok", httpGet(startAndGetPort(server, netty))); // 首次启动正常

			server.close();
			Assertions.assertEquals("ok", httpGet(startAndGetPort(server, netty))); // 重启后必须仍正常
		} finally {
			server.close(); // 幂等:调度已停时直接返回
		}
	}

	@AfterEach
	public void tearDown() {
		if (server != null)
			server.close(); // 幂等
		if (netty != null)
			netty.close();
	}
}
