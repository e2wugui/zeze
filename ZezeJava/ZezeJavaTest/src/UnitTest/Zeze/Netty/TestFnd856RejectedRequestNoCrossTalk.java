package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-56回归：createHttpExchange返回null后裸return——pipelining下被拒请求的后续
 * HttpContent/LastHttpContent帧按channelId路由进仍在表内的前一个exchange：body串包、
 * onEndStream二次派发、经late-write直发重复响应。契约注释"通常要回复状态并关闭连接"
 * 是建议语气，且钩子签名拿不到HttpRequest，拒绝决策只能是连接级的。
 * 修复后：null分支照搬停机503分支同构处置——框架代回503+Connection:close+移除在途
 * exchange并CLOSE_ON_FLUSH善后；契约注释升级为框架强制事实描述。
 */
@Fast
public class TestFnd856RejectedRequestNoCrossTalk {

	/** 同一连接的第2个请求起拒绝（连接级策略形态，模拟过载限流子类；计数门确定生效）。 */
	private static final class RejectingServer extends HttpServer {
		private final ConcurrentHashMap<ChannelId, AtomicInteger> counts = new ConcurrentHashMap<>();

		@Override
		public @Nullable HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var count = counts.computeIfAbsent(context.channel().id(), k -> new AtomicInteger()).incrementAndGet();
			return count >= 2 ? null : super.createHttpExchange(context);
		}
	}

	// pipelining[A(POST带body,handler异步滞留) → B(被拒)]：B必须得到503且连接关闭，
	// A的onEndStream恰好派发一次、body不被B的帧污染
	@Test
	public void testRejectedRequestNotRoutedIntoPreviousExchange() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new RejectingServer();
		var dispatched = new AtomicInteger();
		var bodySeen = new CompletableFuture<String>();
		var aArrived = new CompletableFuture<HttpExchange>();
		server.addHandler("/a", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			dispatched.incrementAndGet(); // onEndStream派发计数（修复前会被B的LastHttpContent二次触发）
			bodySeen.complete(x.content().toString(StandardCharsets.UTF_8));
			x.detach(); // A滞留在表内（异步完成形态），制造串包窗口
			aArrived.complete(x);
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				// A：POST带body（handler派发后滞留）；B：第2个请求（同连接计数≥2被拒）。
				// B带body，验证其帧不得累进A的content（修复前body串包+LastHttpContent二次派发）。
				os.write(("POST /a HTTP/1.1\r\nHost: a\r\nContent-Length: 4\r\n\r\nAAAA"
						+ "POST /b HTTP/1.1\r\nHost: a\r\nContent-Length: 4\r\n\r\nBBBB").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				Assertions.assertNotNull(aArrived.get(10, TimeUnit.SECONDS), "request A dispatched");
				Assertions.assertEquals("AAAA", bodySeen.get(5, TimeUnit.SECONDS),
						"A的body必须只含自身内容（修复前被B的body串包）");

				// B被拒：客户端必须收到503（框架代回）；随后连接关闭
				InputStream is = sock.getInputStream();
				var buf = new ByteArrayOutputStream();
				var bytes = new byte[4096];
				var deadline = System.currentTimeMillis() + 15_000;
				while (System.currentTimeMillis() < deadline) {
					int n = is.read(bytes);
					if (n < 0)
						break;
					buf.write(bytes, 0, n);
					if (buf.toString(StandardCharsets.ISO_8859_1).contains("503"))
						break;
				}
				Assertions.assertTrue(buf.toString(StandardCharsets.ISO_8859_1).contains("503 Service Unavailable"),
						"被拒请求必须得到503（框架代回），实际: " + buf);

				// 观察窗：A的onEndStream不得被B的LastHttpContent二次派发（修复前B帧到达即二次触发）
				//noinspection BusyWait
				Thread.sleep(500);
				Assertions.assertEquals(1, dispatched.get(),
						"被拒请求的后续帧不得再次派发前一个exchange（修复前二次触发onEndStream），dispatched="
								+ dispatched.get());
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 非pipelining（无在途exchange）下的null拒绝同样得到503+断连，行为一致
	@Test
	public void testRejectedRequestStandalone() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new RejectingServer();
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				// 第1个请求正常处理，第2个请求被拒
				os.write(("GET /any HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /any HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				InputStream is = sock.getInputStream();
				var buf = new ByteArrayOutputStream();
				var bytes = new byte[4096];
				int n;
				while ((n = is.read(bytes)) >= 0) {
					buf.write(bytes, 0, n);
					if (buf.toString(StandardCharsets.ISO_8859_1).contains("503"))
						break;
				}
				Assertions.assertTrue(buf.toString(StandardCharsets.ISO_8859_1).contains("503 Service Unavailable"),
						"实际: " + buf);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
