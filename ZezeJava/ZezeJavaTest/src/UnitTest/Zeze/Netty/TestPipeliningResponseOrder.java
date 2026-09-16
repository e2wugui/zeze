package UnitTest.Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * HTTP/1.1 pipelining响应乱序（FND7轮N①草案终局处置）：HttpExchange的响应写直达channel，
 * 派发任务（task11按channel串行）先返回但响应异步延迟发出的形态下，后到的请求可以先写出响应——
 * pipelining客户端按请求序匹配响应，乱序字节流对每个请求都是错误应答（静默数据错乱）。
 * 修复：per-channel FIFO响应序化——exchange首次响应写占位（先到达者先写），close CAS释放，
 * 头部未就绪（前面的exchange未完成响应）时后续exchange的响应写挂起，按请求到达序冲刷。
 * 回归红线：非pipelining（单在途请求）路径行为不变——exchange即队头，写直达。
 */
@Fast
public class TestPipeliningResponseOrder {

	private static void await(String what, CompletableFuture<HttpExchange> f) throws Exception {
		Assertions.assertNotNull(f.get(10, TimeUnit.SECONDS), "timeout waiting: " + what);
	}

	// 读取直至完成条件满足（状态行计数只是进度信号：流式响应头部与chunk分次flush，
	// 仅按状态行计数会在末响应chunk到达前提前返回），返回原始字节流的ISO-8859-1视图
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

	private static int countOccurrences(String s, String sub) {
		int c = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1))
			c++;
		return c;
	}

	private static int indexOf(String s, String marker, String what) {
		var i = s.indexOf(marker);
		Assertions.assertTrue(i >= 0, "response body missing: " + what + ", stream=" + s);
		return i;
	}

	// 三请求pipelining到一个连接，各handler登记exchange（detach阻止派发任务finally的自动close）
	// 后立即返回；测试线程按r3→r2→r1的乱序完成发出响应——字节流必须按请求序r1,r2,r3写出。
	// 之后同一连接上发第4个请求必须得到正确应答（连接协议流未被乱序写损坏）。
	@Test
	public void testFullResponsesOutOfOrderCompletionInOrderBytes() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		@SuppressWarnings("unchecked")
		var arrived = (CompletableFuture<HttpExchange>[])new CompletableFuture<?>[3];
		for (int i = 0; i < 3; i++)
			arrived[i] = new CompletableFuture<>();
		for (int i = 0; i < 3; i++) {
			var idx = i;
			server.addHandler("/r" + (i + 1), 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
				x.detach(); // 异步两段式响应形态：响应延迟到派发任务之外发出，close由响应方负责
				arrived[idx].complete(x);
			});
		}
		server.addHandler("/r4", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK, "resp-4"));
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				os.write(("GET /r1 HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /r2 HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /r3 HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				await("r1 handler", arrived[0]);
				await("r2 handler", arrived[1]);
				await("r3 handler", arrived[2]);
				var x1 = arrived[0].get();
				var x2 = arrived[1].get();
				var x3 = arrived[2].get();

				// 乱序完成：r3先发，r2次之，r1最后
				x3.close(x3.sendPlainText(HttpResponseStatus.OK, "resp-3"));
				x2.close(x2.sendPlainText(HttpResponseStatus.OK, "resp-2"));
				x1.close(x1.sendPlainText(HttpResponseStatus.OK, "resp-1"));

				var stream = readResponses(sock.getInputStream(),
						s -> countOccurrences(s, "resp-1") + countOccurrences(s, "resp-2")
								+ countOccurrences(s, "resp-3") >= 3);
				int p1 = indexOf(stream, "resp-1", "r1");
				int p2 = indexOf(stream, "resp-2", "r2");
				int p3 = indexOf(stream, "resp-3", "r3");
				Assertions.assertTrue(p1 < p2 && p2 < p3,
						"pipelining响应必须按请求序写出（r1<r2<r3），实际顺序错乱: " + stream);

				// 连接协议流完整性：第4个请求在乱序恢复后的连接上得到正确应答
				os.write("GET /r4 HTTP/1.1\r\nHost: a\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				var stream4 = readResponses(sock.getInputStream(), s -> s.contains("resp-4"));
				indexOf(stream4, "resp-4", "r4");
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 流式（chunked）响应同型：beginStream/sendStream/endStream全部经由序化器，乱序完成按序写出。
	@Test
	public void testStreamResponsesOutOfOrderCompletionInOrderBytes() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		@SuppressWarnings("unchecked")
		var arrived = (CompletableFuture<HttpExchange>[])new CompletableFuture<?>[3];
		for (int i = 0; i < 3; i++)
			arrived[i] = new CompletableFuture<>();
		for (int i = 0; i < 3; i++) {
			var idx = i;
			server.addHandler("/s" + (i + 1), 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
				x.detach();
				arrived[idx].complete(x);
			});
		}
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				os.write(("GET /s1 HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /s2 HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /s3 HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				await("s1 handler", arrived[0]);
				await("s2 handler", arrived[1]);
				await("s3 handler", arrived[2]);

				// 乱序完成流式响应：s3先完整收尾，s2次之，s1最后
				streamRespond(arrived[2].get(), "body-s3");
				streamRespond(arrived[1].get(), "body-s2");
				streamRespond(arrived[0].get(), "body-s1");

				var stream = readResponses(sock.getInputStream(),
						s -> countOccurrences(s, "0\r\n\r\n") >= 3); // 三个chunked响应全部终结
				int p1 = indexOf(stream, "body-s1", "s1");
				int p2 = indexOf(stream, "body-s2", "s2");
				int p3 = indexOf(stream, "body-s3", "s3");
				Assertions.assertTrue(p1 < p2 && p2 < p3,
						"pipelining流式响应必须按请求序写出（s1<s2<s3），实际顺序错乱: " + stream);
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	private static void streamRespond(HttpExchange x, String body) {
		x.beginStream(HttpResponseStatus.OK, HttpServer.setDate(new DefaultHttpHeaders())
				.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE));
		x.sendStream(body.getBytes(StandardCharsets.UTF_8));
		x.endStream();
	}
}
