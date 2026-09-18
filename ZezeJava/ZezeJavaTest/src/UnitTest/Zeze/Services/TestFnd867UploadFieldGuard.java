package UnitTest.Zeze.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.ReloadClassServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND8-67 回归：multipart不含目标字段（getBodyHttpData返回null）或放同名文本字段
 * （返回MemoryAttribute）时，原无守卫强转分别NPE/CCE，异常穿透后请求无应答挂起
 * （仅TaskOneByOne ERROR日志）。
 * 修复：instanceof模式匹配守卫，两种畸形形态均答400并携带字段名便于诊断。
 */
@Fast
public class TestFnd867UploadFieldGuard {
	private static final String TOKEN = "a5-fnd867-token";
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
		new ReloadClassServer(new StubApp(server), "/a5/upload", uploadDir.toString(), "patch", TOKEN);
		var channel = server.start(netty, 0).sync().channel();
		port = ((java.net.InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	/** 构造只含文本字段（无任何文件字段）的multipart body。 */
	private static byte[] multipartTextOnly(String boundary, String name, String value) {
		return ("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
				+ value + "\r\n"
				+ "--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
	}

	private static HttpResponse<String> postMultipart(byte[] body, String boundary) throws Exception {
		return client.send(HttpRequest.newBuilder(
						URI.create("http://127.0.0.1:" + port + "/a5/upload"))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.header("X-Zeze-Token", TOKEN)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	public void testMalformedMultipartRejectedWith400() throws Exception {
		// 形态一：表单完全没有目标字段（原NPE路径）
		var boundary = "a5fnd867b1";
		var res = postMultipart(multipartTextOnly(boundary, "other", "x"), boundary);
		Assertions.assertEquals(400, res.statusCode(), "缺字段必须400（原为无应答挂起+NPE）");
		Assertions.assertTrue(res.body().contains("patch"), "应答携带字段名便于诊断: " + res.body());

		// 形态二：目标字段是文本而非文件（原ClassCastException路径）
		var boundary2 = "a5fnd867b2";
		res = postMultipart(multipartTextOnly(boundary2, "patch", "not-a-file"), boundary2);
		Assertions.assertEquals(400, res.statusCode(), "同名文本字段必须400（原为无应答挂起+CCE）");
		Assertions.assertTrue(res.body().contains("patch"), "应答携带字段名便于诊断: " + res.body());
	}
}
