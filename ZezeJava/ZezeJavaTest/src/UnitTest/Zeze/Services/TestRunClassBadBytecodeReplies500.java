package UnitTest.Zeze.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.RunClassServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * S3-F3 回归：上传class的执行段（defineClass/Runnable/Callable/main三分支）原无守卫，
 * 异常穿透则请求无HTTP应答即断连。修复：catch Throwable（裁决修订——defineClass对损坏
 * 字节码抛ClassFormatError，仅Exception盖不住最常见触发形态）回500带异常摘要。
 */
@Fast
public class TestRunClassBadBytecodeReplies500 {
	private static final String TOKEN = "wt5-s3f3-token";
	private static Netty netty;
	private static HttpServer server;
	private static int port;
	private static final HttpClient client = HttpClient.newHttpClient();

	private static final class StubApp extends Zeze.AppBase {
		private final HttpServer httpServer;

		StubApp(HttpServer httpServer) {
			this.httpServer = httpServer;
		}

		@Override
		public Zeze.Application getZeze() {
			return null;
		}

		@Override
		public HttpServer getHttpServer() {
			return httpServer;
		}
	}

	@BeforeAll
	public static void setUp(@TempDir Path uploadDir) throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer();
		var app = new StubApp(server);
		new RunClassServer(app, "/wt5/run", uploadDir.resolve("run").toString(), "patch", TOKEN);
		var channel = server.start(netty, 0).sync().channel();
		port = ((java.net.InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	private static HttpResponse<String> post(String path, byte[] body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
				.header("X-Zeze-Token", TOKEN)
				.header("Content-Type", "application/octet-stream")
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** 垃圾字节码：defineClass抛ClassFormatError（Error子类）——catch Throwable的裁决要点。 */
	@Test
	public void testGarbageBytecodeReplies500WithSummary() throws Exception {
		var res = post("/wt5/run?patch=bad.class", new byte[64]);
		Assertions.assertEquals(500, res.statusCode(), "垃圾字节码必须应答500（原无应答即断连）");
		Assertions.assertTrue(res.body().contains("run failed"), "应答须带异常摘要: " + res.body());
	}

	/** 合法class但无main/Runnable/Callable：getMethod("main")抛NoSuchMethodException（Exception路径）。 */
	@Test
	public void testNoEntryPointClassReplies500() throws Exception {
		var classBytes = getClass().getResourceAsStream(
				"TestRunClassBadBytecodeReplies500$NoEntryPoint.class").readAllBytes();
		var res = post("/wt5/run?patch=NoEntryPoint.class", classBytes);
		Assertions.assertEquals(500, res.statusCode(), "无入口点的class必须应答500");
		Assertions.assertTrue(res.body().contains("run failed"), "应答须带异常摘要: " + res.body());
	}

	/** 供上传执行的目标：合法class文件但非Runnable/Callable且无main。 */
	public static final class NoEntryPoint {
		public int value = 1;
	}
}
