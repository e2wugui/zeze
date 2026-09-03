package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-1回归:HttpServer连接异常/失活路径必须清理exchanges。
 * 修复前exchanges.remove只在HttpExchange.close()里发生:畸形请求(如"GET /%zz")让channelRead抛出异常,
 * exceptionCaught只get不删,channelInactive只清channels,该连接的HttpExchange连同retain的request
 * 和累积的content(池化direct内存)永久滞留在exchanges中泄漏。
 */
@Fast
public class TestHttpServerCleanup {
	private static Netty netty;
	private static TestServer server;
	private static int port;

	// 暴露基类protected的exchanges给断言用
	public static final class TestServer extends HttpServer {
		public @NotNull ConcurrentHashMap<ChannelId, HttpExchange> exchangesView() {
			return exchanges;
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new TestServer();
		server.addHandler("/ok", 8192, TransactionLevel.Serializable, DispatchMode.Direct,
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	// 发送原始请求字节并读到服务器关闭连接(EOF)为止,返回收到的完整响应。
	// read返回-1即证明连接被服务器关闭;若服务器一直不关,soTimeout超时会让本测试失败。
	private static @NotNull String sendRawUntilClose(@NotNull String raw) throws IOException {
		try (var sock = new Socket("127.0.0.1", port)) {
			sock.setSoTimeout(15000);
			var os = sock.getOutputStream();
			os.write(raw.getBytes(StandardCharsets.ISO_8859_1));
			os.flush();
			var out = new ByteArrayOutputStream();
			var buf = new byte[4096];
			var in = sock.getInputStream();
			int n;
			while ((n = in.read(buf)) >= 0)
				out.write(buf, 0, n);
			return out.toString(StandardCharsets.ISO_8859_1);
		}
	}

	// 连接关闭后exchanges最终必须为空(留出异步清理的时间);修复前畸形请求的exchange永不移除,这里会等满超时失败
	private static void awaitExchangesEmpty() throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!server.exchangesView().isEmpty()) {
			Assertions.assertTrue(System.nanoTime() < deadline,
					"exchanges not cleaned up after connection closed: " + server.exchangesView().keySet());
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}

	// 触发变体:畸形uri的百分号编码。HttpRequestDecoder不校验"%zz",HttpExchange.channelRead里
	// path()->urlDecode抛IllegalArgumentException,异常走exceptionCaught(回500并关连接)。
	// 此时exchange已put进exchanges,永远等不到正常的close(),必须被主动清理并释放retain的request。
	// 重复多次让泄漏的累积效果可见:修复前每个连接泄漏一个exchange,exchanges无限增长。
	@Test
	public void testMalformedUriCleanup() throws Exception {
		for (int i = 0; i < 10; i++) {
			var res = sendRawUntilClose("GET /%zz HTTP/1.1\r\nHost: a\r\n\r\n");
			Assertions.assertTrue(res.startsWith("HTTP/1.1 500"), res); // 异常以500回报(默认HttpServer.sendStackTrace=1)
		}
		awaitExchangesEmpty();
	}

	// 回归:正常请求路径行为不变,响应正常,完成后exchange同样不在exchanges里残留
	@Test
	public void testNormalRequest() throws Exception {
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/ok")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		Assertions.assertEquals("ok", res.body());
		awaitExchangesEmpty();
	}
}
