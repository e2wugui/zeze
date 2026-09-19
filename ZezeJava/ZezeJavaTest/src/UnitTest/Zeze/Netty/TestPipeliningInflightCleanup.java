package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * pipelining在途exchange清理（janitor）与迟到写策略回归。
 * 修复前：channelInactive/exceptionCaught/写超时/停机四个清理点只关exchanges表内最新一个exchange
 * （表每channel单槽，pipelining下前序exchange被后续请求覆盖出表），detach后遗弃的前序exchange
 * 无人close——retain的request、累积content、挂起响应写全部泄漏（参考Armeria的unfinishedRequests
 * 全量登记+cleanup）。修复后：清理点遍历序化器在途列表全部关闭。
 * 迟到写：exchange出队后的writeResponse原先"直发并告警"放行——此刻本响应终结符已写出、后继可能
 * 正持笔直写，插队字节会污染下一响应的帧。修复后：失败并释放（对齐Armeria Http1ObjectEncoder
 * 对id&lt;currentId写一律丢弃的语义）。
 */
@Fast
public class TestPipeliningInflightCleanup {

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

	// 两个pipelined请求都detach住不响应，客户端断开连接：channelInactive的janitor必须把两个
	// exchange全部close。修复前只有最新一个被关，前序exchange（detach遗弃）永久泄漏。
	@Test
	public void testChannelInactiveClosesAllInFlight() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		server.addHandler("/hang", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach(); // 挂起不响应，模拟慢/遗弃handler
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /hang HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /hang HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				await("both exchanges created", 10_000, () -> server.created.size() >= 2);

				// abortive断开（RST）：普通FIN只走ChannelInputShutdownReadComplete（标记最新exchange的
				// willCloseConnection后挂起等响应/空闲超时），不清在途；RST走exceptionCaught→janitor。
				sock.setSoLinger(true, 0);
				sock.close();
			}
			var first = (HttpExchange)server.created.toArray()[0];
			var second = (HttpExchange)server.created.toArray()[1];
			await("first (superseded) exchange closed by janitor", 10_000, first::isClosed);
			await("second (latest) exchange closed by janitor", 10_000, second::isClosed);
		} finally {
			server.close();
			netty.close();
		}
	}

	// close之后的迟到写：writeResponse必须失败promise（ClosedChannelException）且不向连接写字节，
	// 不得再"直发放行"。确定性依据：pipelining下second的响应字节只有在本exchange（first）出队后
	// 才可能上线（序化器推进过entry1后second才持笔直写）——客户端收到"second"即证明first已出队，
	// 此后对first的写必为迟到写。
	@Test
	public void testLateWriteAfterCloseFails() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var secondArrived = new java.util.concurrent.CompletableFuture<HttpExchange>();
		server.addHandler("/first", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.close(x.sendPlainText(HttpResponseStatus.OK, "resp-first")));
		server.addHandler("/second", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach(); // 由测试线程在first出队后补发响应
			secondArrived.complete(x);
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /first HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /second HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				var second = secondArrived.get(10, TimeUnit.SECONDS);
				var first = (HttpExchange)server.created.toArray()[0];
				Assertions.assertNotSame(first, second);

				// first正常响应；等它上线后补发second——second字节到达即证明first已出队
				var buf = new java.io.ByteArrayOutputStream();
				var bytes = new byte[4096];
				second.close(second.sendPlainText(HttpResponseStatus.OK, "resp-second"));
				while (!buf.toString(StandardCharsets.ISO_8859_1).contains("resp-second")) {
					int n = sock.getInputStream().read(bytes);
					Assertions.assertTrue(n >= 0, "connection closed unexpectedly");
					buf.write(bytes, 0, n);
				}
				Assertions.assertTrue(buf.toString(StandardCharsets.ISO_8859_1).contains("resp-first"),
						"pipelining order broken: " + buf);

				var f = first.sendPlainText(HttpResponseStatus.OK, "late"); // 已出队exchange的迟到写
				Assertions.assertTrue(f.awaitUninterruptibly(3000), "late write future must complete");
				Assertions.assertFalse(f.isSuccess(), "late write must fail");
				Assertions.assertInstanceOf(java.nio.channels.ClosedChannelException.class, f.cause());

				// 迟到写不得产生任何响应字节：短soTimeout内应无任何数据到达
				sock.setSoTimeout(1000);
				Assertions.assertThrows(java.net.SocketTimeoutException.class, () -> sock.getInputStream().read(),
						"no response bytes may be written after exchange finished");
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 终结后未出队的迟到写：first持笔挂起（detach不响应），second在其后正常close——entry已finished
	// 但未出队，此后对second的写必须失败且不产生任何字节。修复前该写被排进挂起队列，drainPending
	// 冲刷时排在second终结符之后，插进下一响应的帧前（正是迟到写要防的污染，原先只在出队后拒绝）。
	// 确定性：stray写在close返回后由handler同线程发出，EL任务序上必然排在close的release任务之后。
	@Test
	public void testLateWriteAfterFinishBeforeDequeueFails() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var strayWrite = new java.util.concurrent.CompletableFuture<ChannelFuture>();
		server.addHandler("/first", 8192, TransactionLevel.None, DispatchMode.Normal, HttpExchange::detach);
		server.addHandler("/second", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.close(x.sendPlainText(HttpResponseStatus.OK, "resp-second"));
			strayWrite.complete(x.sendPlainText(HttpResponseStatus.OK, "late")); // close后再写即误用
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /first HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /second HTTP/1.1\r\nHost: a\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();

				var f = strayWrite.get(10, TimeUnit.SECONDS);
				Assertions.assertTrue(f.awaitUninterruptibly(3000), "stray write future must complete");
				Assertions.assertFalse(f.isSuccess(), "stray write must fail");
				Assertions.assertInstanceOf(java.nio.channels.ClosedChannelException.class, f.cause());

				// 释放持笔：first响应+close→advance冲刷second的挂起响应；客户端按序收到两个响应，无stray字节
				var first = (HttpExchange)server.created.toArray()[0];
				first.close(first.sendPlainText(HttpResponseStatus.OK, "resp-first"));

				var buf = new java.io.ByteArrayOutputStream();
				var bytes = new byte[4096];
				long deadline = System.currentTimeMillis() + 10_000;
				while (!buf.toString(StandardCharsets.ISO_8859_1).contains("resp-second")) {
					Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout: " + buf);
					int n = sock.getInputStream().read(bytes);
					Assertions.assertTrue(n >= 0, "connection closed unexpectedly");
					buf.write(bytes, 0, n);
				}
				var s = buf.toString(StandardCharsets.ISO_8859_1);
				Assertions.assertTrue(s.contains("resp-first"), "pipelining order broken: " + s);
				Assertions.assertFalse(s.contains("late"), "stray bytes must not reach the client: " + s);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
