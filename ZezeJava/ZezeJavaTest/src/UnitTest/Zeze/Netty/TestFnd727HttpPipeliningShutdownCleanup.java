package UnitTest.Zeze.Netty;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
 * FND7-27回归：HTTP pipelining下新请求的exchanges.put无条件覆盖旧表项（HttpServer.channelRead），
 * 被逐出的exchange只能靠已入队task11Executor的endStream任务收尾；HttpServer.close()的
 * shutdown(true)丢弃未运行任务，而fireEndStreamHandle没有挂onCancel（对照fireStreamContentHandle/
 * fireWebSocket都挂了）——被逐出exchange的retain的request与累积content（池化内存）在停机窗口
 * 静默泄漏，正常运行期无泄漏。修复：任务被丢弃时补偿close释放资源。
 */
@Fast
public class TestFnd727HttpPipeliningShutdownCleanup {

	public static final class TestServer extends HttpServer {
		public final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
	}

	private static void await(String what, long timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	// 三个请求pipelining到一个连接：r0的endStream任务先被认领并阻塞在handler里（保证r1/r2的
	// 任务只排队未认领），r1的exchange被r2的put覆盖逐出（停机清扫扫不到），r2的在表内。
	// server.close()的shutdown(true)丢弃r1/r2的排队任务——被逐出的r1必须由onCancel补偿close，
	// 否则它连同retain的request永不释放。
	@Test
	public void testEvictedExchangeReleasedOnShutdown() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var entered = new CountDownLatch(1);
		var block = new CountDownLatch(1);
		server.addHandler("/block", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			entered.countDown();
			block.await(); // 阻塞第一个派发任务：后续任务（同channel.id同队列）排队不认领
		});
		server.addHandler("/ok", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /block HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /ok HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /ok HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				// r0的任务已认领运行 + 三个exchange都已创建（r0/r1被pipelining覆盖逐出，表内只剩r2）
				Assertions.assertTrue(entered.await(10, TimeUnit.SECONDS), "blocked handler not entered");
				await("3 pipelined exchanges created", 10_000, () -> server.created.size() >= 3);
				var x1 = (HttpExchange)server.created.toArray()[1]; // 被逐出的中间那个
				var x2 = (HttpExchange)server.created.toArray()[2]; // 表内那个

				// close会阻塞在waitComplete直到阻塞的handler返回，放后台线程
				var closeThread = new Thread(server::close, "fnd7-27-close");
				closeThread.start();
				// r1的exchange此刻只可能由onCancel补偿关闭（它已被逐出表外，任务被shutdown丢弃，
				// 同队列前序任务阻塞着不可能执行）——等补偿发生后再放行阻塞任务。
				await("evicted exchange closed by shutdown cancel", 10_000, x1::isClosed);
				block.countDown();
				closeThread.join(15_000);
				Assertions.assertFalse(closeThread.isAlive(), "server.close() must return");

				// 三个exchange（含被逐出的）都必须结束并释放retain的request（closeInEventLoop置null）
				for (var x : server.created)
					await("exchange released: " + x, 10_000, () -> x.isClosed() && x.request() == null);
				Assertions.assertTrue(x2.isClosed());
			}
		} finally {
			block.countDown();
			server.close();
			netty.close();
		}
	}
}
