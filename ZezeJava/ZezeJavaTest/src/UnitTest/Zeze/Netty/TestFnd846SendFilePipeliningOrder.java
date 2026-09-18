package UnitTest.Zeze.Net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-46回归：sendFile的304/416早退分支曾context.writeAndFlush直写，绕过per-channel
 * 响应序化器——pipelining下后到的条件请求/Range请求响应先于前序请求上线，客户端按序
 * 配对时拿到错误状态码。孪生：/metrics（PrometheusCounter）经HttpResponseWithBodyStream
 * 直写共享channel的ctx，同型错序。
 * 修复后：304/416与metrics响应全部经writeResponse按请求到达序写出。
 */
@Fast
public class TestFnd846SendFilePipeliningOrder {

	public static final class TestServer extends HttpServer {
		public final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
	}

	private static void await(String what, CompletableFuture<HttpExchange> f) throws Exception {
		Assertions.assertNotNull(f.get(10, TimeUnit.SECONDS), "timeout waiting: " + what);
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	private static String readResponses(InputStream is, java.util.function.Predicate<String> done) throws Exception {
		var buf = new ByteArrayOutputStream();
		var bytes = new byte[4096];
		var deadline = System.currentTimeMillis() + 15_000;
		while (!done.test(buf.toString(StandardCharsets.ISO_8859_1))) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout reading responses");
			int n = is.read(bytes);
			if (n < 0)
				break;
			buf.write(bytes, 0, n);
		}
		return buf.toString(StandardCharsets.ISO_8859_1);
	}

	private static int indexOf(String s, String marker, String what) {
		var i = s.indexOf(marker);
		Assertions.assertTrue(i >= 0, "response missing: " + what + ", stream=" + s);
		return i;
	}

	private static void deleteRecursively(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}

	// pipelining两个请求：/slow（detach住不响应，保持队头开放）+ 文件请求（命中304/416分支）。
	// 文件分支的exchange已close（写已提交）后，前序请求才补发响应——304/416必须按请求序在其后。
	private static void assertFileEarlyExitOrdered(Function<Path, String> fileRequestHeaders,
												   String earlyExitMarker) throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var slowArrived = new CompletableFuture<HttpExchange>();
		var dir = Files.createTempDirectory("a4_fnd846");
		try {
			var file = dir.resolve("a4_file.txt");
			Files.writeString(file, "fnd846-body");
			server.addHandler("/slow", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
				x.detach(); // 前序请求保持开放（队头），响应延后由测试线程发出
				slowArrived.complete(x);
			});
			server.addFileHandler("/files/", dir.toString(), false);
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				os.write(("GET /slow HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /files/a4_file.txt HTTP/1.1\r\nHost: a\r\n"
						+ fileRequestHeaders.apply(dir) + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				await("slow handler", slowArrived);
				await("file exchange created", 10_000, () -> server.created.size() >= 2);
				var fileX = (HttpExchange)server.created.toArray()[1];
				await("file early-exit branch done (exchange closed)", 10_000, fileX::isClosed);

				// 前序请求此刻才响应：早退分支的响应必须挂在其后（修复前直写已先上线）
				var xA = slowArrived.get();
				xA.close(xA.sendPlainText(HttpResponseStatus.OK, "resp-a"));

				var stream = readResponses(sock.getInputStream(),
						s -> s.contains("resp-a") && s.contains(earlyExitMarker));
				int pA = indexOf(stream, "resp-a", "slow response");
				int pEarly = indexOf(stream, earlyExitMarker, "early-exit response");
				Assertions.assertTrue(pA < pEarly,
						"早退响应必须按pipelining请求序晚于前序响应，实际: " + stream);
			}
		} finally {
			server.close();
			netty.close();
			deleteRecursively(dir);
		}
	}

	// 304（If-Modified-Since命中）：keep-alive响应，修复前直写可在pipelining下先于前序响应
	@Test
	public void testSendFile304OrderedInPipeline() throws Exception {
		assertFileEarlyExitOrdered(dir -> {
			try {
				var file = dir.resolve("a4_file.txt");
				// 固定lastModified秒值（避开文件系统时间戳精度差异），If-Modified-Since与之相等即命中304
				var fixedSeconds = System.currentTimeMillis() / 1000 - 3600;
				Files.setLastModifiedTime(file, FileTime.fromMillis(fixedSeconds * 1000));
				return "If-Modified-Since: " + HttpServer.getDate(fixedSeconds) + "\r\n";
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, "304 Not Modified");
	}

	// 416（Range不可满足）：RFC要求回416+Content-Range，同样必须按序
	@Test
	public void testSendFile416OrderedInPipeline() throws Exception {
		assertFileEarlyExitOrdered(dir -> "Range: bytes=1000000-2000000\r\n",
				"416 Requested Range Not Satisfiable");
	}

	// 孪生：/metrics（PrometheusCounter经HttpResponseWithBodyStream写响应）曾直写共享channel的ctx
	// 插队——直接以同形态调用HttpResponseWithBodyStream验证：响应必须经序化器按请求序写出。
	// （prometheus exporter-common为主工程compileOnly、测试运行时不可用，故不经addHttpHandler注册。）
	@Test
	public void testMetricsOrderedInPipeline() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var slowArrived = new CompletableFuture<HttpExchange>();
		server.addHandler("/slow", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach();
			slowArrived.complete(x);
		});
		server.addHandler("/metrics", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			try {
				var out = Zeze.Netty.HttpResponseWithBodyStream.sendHeadersAndGetBody(x,
						HttpResponseStatus.OK, java.util.Map.of("Content-Type", "text/plain"), 0); // chunked
				out.write("metrics-body".getBytes(StandardCharsets.UTF_8), 0, 12);
				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				os.write(("GET /slow HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /metrics HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				await("slow handler", slowArrived);
				await("metrics exchange created", 10_000, () -> server.created.size() >= 2);
				var metricsX = (HttpExchange)server.created.toArray()[1];
				await("metrics response submitted (exchange closed)", 10_000, metricsX::isClosed);

				var xA = slowArrived.get();
				xA.close(xA.sendPlainText(HttpResponseStatus.OK, "resp-a"));

				var stream = readResponses(sock.getInputStream(), s -> s.contains("resp-a")
						&& s.contains("metrics-body") && s.contains("0\r\n\r\n"));
				int pA = indexOf(stream, "resp-a", "slow response");
				int pMetrics = indexOf(stream, "metrics-body", "metrics response");
				Assertions.assertTrue(pA < pMetrics,
						"/metrics响应必须按pipelining请求序晚于前序响应，实际: " + stream);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
