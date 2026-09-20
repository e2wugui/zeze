package UnitTest.Zeze.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * S3-F2 回归：热更失败路径原无应答且不清理已落盘补丁——坏补丁毒化uploadDir使
 * linkd启动链（start()对目录内唯一补丁无条件reloadClasses）下次必失败。
 * 修复：catch罩住ZipFile构造与reloadClasses，失败回500并删除destFile，维持uploadDir
 * "只留可用补丁"不变式（redefineClasses单调用原子，删文件不留半热更状态）。
 */
@Fast
public class TestReloadClassBadPatchCleanup {
	private static final String TOKEN = "wt5-s3f2-token";
	private static Netty netty;
	private static HttpServer server;
	private static int port;
	private static Path reloadDir;
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
		reloadDir = uploadDir.resolve("reload");
		var app = new StubApp(server);
		new ReloadClassServer(app, "/wt5/reload", reloadDir.toString(), "patch", TOKEN);
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

	/** 坏补丁1：非zip文件——ZipFile构造即抛（裁决明确要求的覆盖形态）。 */
	@Test
	public void testNonZipPatchReplies500AndDeleted() throws Exception {
		var res = post("/wt5/reload?patch=bad1.zip", "this is not a zip file".getBytes(StandardCharsets.ISO_8859_1));
		Assertions.assertEquals(500, res.statusCode(), "坏补丁必须应答500（原异常穿透无应答）");
		Assertions.assertFalse(Files.exists(reloadDir.resolve("bad1.zip")),
				"失败补丁必须删除，维持uploadDir只留可用补丁（否则linkd下次启动必失败）");
	}

	/** 坏补丁2：zip可打开但class条目损坏——reloadClasses解析失败。 */
	@Test
	public void testCorruptClassZipReplies500AndDeleted() throws Exception {
		var out = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(out)) {
			zip.putNextEntry(new ZipEntry("no/such/TestClass.class"));
			zip.write(new byte[64]); // 垃圾字节码：常量池解析必抛
			zip.closeEntry();
		}
		var res = post("/wt5/reload?patch=bad2.zip", out.toByteArray());
		Assertions.assertEquals(500, res.statusCode(), "损坏class的zip必须应答500");
		Assertions.assertFalse(Files.exists(reloadDir.resolve("bad2.zip")), "失败补丁必须删除");
	}
}
