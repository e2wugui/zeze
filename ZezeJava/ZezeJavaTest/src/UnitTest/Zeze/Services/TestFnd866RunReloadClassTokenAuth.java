package UnitTest.Zeze.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.ReloadClassServer;
import Zeze.Services.RunClassServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND8-66 回归：ReloadClass/RunClass 两端点原为零鉴权任意代码执行——任何能建立TCP
 * 连接者即可热替换任意类或执行任意字节码，仅靠类注释部署纪律约束。
 * 修复：构造器可空token参数（null自动生成随机token并打日志，默认强制鉴权、零配置
 * 可用），onEndRequest首行校验X-Zeze-Token头或token查询参数（常量时间比较），
 * 失配403并记录来源。
 */
@Fast
public class TestFnd866RunReloadClassTokenAuth {
	private static final String TOKEN = "a5-fnd866-token";
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
		new ReloadClassServer(app, "/a5/reload", uploadDir.resolve("reload").toString(), "patch", TOKEN);
		new RunClassServer(app, "/a5/run", uploadDir.resolve("run").toString(), "patch", TOKEN);
		// 零配置形态：null token自动生成——反射断言非空随机
		var auto = new ReloadClassServer(app, "/a5/reload2", uploadDir.resolve("reload2").toString(), "patch", null);
		var tokenField = ReloadClassServer.class.getDeclaredField("token");
		tokenField.setAccessible(true);
		var autoToken = (String)tokenField.get(auto);
		Assertions.assertNotNull(autoToken);
		Assertions.assertFalse(autoToken.isBlank(), "null token必须自动生成随机token");

		var channel = server.start(netty, 0).sync().channel();
		port = ((java.net.InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	private static HttpResponse<String> post(String path, String tokenHeader, String tokenQuery,
											  String contentType, byte[] body) throws Exception {
		var url = "http://127.0.0.1:" + port + path;
		if (tokenQuery != null)
			url += (path.contains("?") ? "&" : "?") + "token=" + tokenQuery;
		var builder = HttpRequest.newBuilder(URI.create(url))
				.POST(HttpRequest.BodyPublishers.ofByteArray(body));
		if (contentType != null)
			builder.header("Content-Type", contentType);
		if (tokenHeader != null)
			builder.header("X-Zeze-Token", tokenHeader);
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** 空zip（reloadClasses无条目可替换，安全） */
	private static byte[] emptyZip() throws IOException {
		var out = new ByteArrayOutputStream();
		try (var ignore = new ZipOutputStream(out)) {
		}
		return out.toByteArray();
	}

	@Test
	public void testUnauthorizedRejected() throws Exception {
		// 无token/错token/他端点token：均不得进入执行路径（403）
		var zip = emptyZip();
		Assertions.assertEquals(403, post("/a5/reload?patch=x.zip", null, null,
				"application/octet-stream", zip).statusCode(), "无token必须403");
		Assertions.assertEquals(403, post("/a5/reload?patch=x.zip", "wrong-token", null,
				"application/octet-stream", zip).statusCode(), "错token必须403");
		Assertions.assertEquals(403, post("/a5/run?patch=x.class", null, "wrong-token",
				"application/octet-stream", new byte[]{1}).statusCode(), "错token(query)必须403");
		Assertions.assertEquals(403, post("/a5/reload2?patch=x.zip", null, null,
				"application/octet-stream", zip).statusCode(), "自动生成token的端点同样强制鉴权");
	}

	@Test
	public void testAuthorizedAccepted() throws Exception {
		// ReloadClass：正确token（头携带）+空zip→200
		Assertions.assertEquals(200, post("/a5/reload?patch=x.zip", TOKEN, null,
				"application/octet-stream", emptyZip()).statusCode(), "正确token必须放行");

		// RunClass：正确token（查询参数携带）+一个带main的真实class字节码→200执行
		var tinyMain = getClass().getResourceAsStream(
				"TestFnd866RunReloadClassTokenAuth$TinyMain.class").readAllBytes();
		var res = post("/a5/run?patch=TinyMain.class", null, TOKEN,
				"application/octet-stream", tinyMain);
		Assertions.assertEquals(200, res.statusCode(), "正确token(query)必须放行并执行");
	}

	/** 供RunClass端点defineClass执行的 benign 目标：只留痕。 */
	public static final class TinyMain {
		public static volatile boolean ran;

		public static void main(String[] args) {
			ran = true;
		}
	}
}
