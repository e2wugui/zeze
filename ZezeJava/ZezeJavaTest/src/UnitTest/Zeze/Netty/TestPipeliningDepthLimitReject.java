package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 深度pipelining超限（MaxResponseOrderDepth=128）拒绝路径回归：第129个在途请求触发
 * registerResponseOrder拒绝→closeConnectionNow。修复前拒绝后仍继续x.channelRead(msg)：
 * request被retain进已终结的exchange——closeInEventLoop在登记失败时已先行（request字段
 * 尚为null），close族被CAS挡住不会重跑、janitor也扫不到未登记条目，retain永久泄漏
 *（观测点：被拒exchange的request()永不为null；健康exchange由closeInEventLoop释放并置null）。
 * 修复后registerResponseOrder返回false，channelRead立即丢弃（finally释放原始msg），
 * 并顺带保证任何消息形态（如未来引入aggregator的FullHttpRequest）都不会派发handler。
 */
@Fast
public class TestPipeliningDepthLimitReject {

	public static final class TestServer extends HttpServer {
		public final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
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

	@Test
	public void testRejectedRequestNotProcessed() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var handled = new AtomicInteger();
		server.addHandler("/hang", 0, TransactionLevel.None, DispatchMode.Normal, x -> {
			handled.incrementAndGet();
			x.detach(); // 挂住不响应：前128个保持登记在途
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var sb = new StringBuilder();
				for (int i = 0; i <= 128; i++) // 129条：第129条触发超限拒绝
					sb.append("GET /hang HTTP/1.1\r\nHost: a\r\n\r\n");
				sock.getOutputStream().write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
				sock.getOutputStream().flush();

				await("connection closed by depth limit", 10_000, () -> {
					try {
						return sock.getInputStream().read() < 0;
					} catch (java.io.IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
			// 前128个handler照常执行（在途请求不被误伤）；被拒的第129个不得派发handler
			await("first 128 handlers ran", 10_000, () -> handled.get() >= 128);
			//noinspection BusyWait
			Thread.sleep(300);
			Assertions.assertEquals(128, handled.get(), "rejected request must not dispatch handler");
			// 善后：129个exchange（128在途+被拒的1个）全部终结，且无任何exchange残留retain的request
			Assertions.assertEquals(129, server.created.size());
			await("all exchanges closed", 10_000,
					() -> server.created.stream().allMatch(HttpExchange::isClosed));
			var leaked = server.created.stream().filter(x -> x.request() != null).toList();
			Assertions.assertTrue(leaked.isEmpty(),
					"rejected request must not be retained into the closed exchange (leak): " + leaked.size());
		} finally {
			server.close();
			netty.close();
		}
	}
}
