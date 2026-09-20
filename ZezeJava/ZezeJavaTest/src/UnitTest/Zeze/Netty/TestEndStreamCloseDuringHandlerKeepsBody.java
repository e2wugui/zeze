package UnitTest.Zeze.Netty;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NY1-F1回归：endStream任务运行期间close仍会提前release request/content。
 * 修复前endStreamTaskPending在任务入口清位，onEndStream用户回调执行期间并发close的
 * closeInEventLoop观察到pending==false即releaseTerminal——迟到的回调读到空body/空request。
 * 修复：清位移到finally的releaseTerminal()之后。本测试在回调执行中途（latch卡住）并发
 * closeConnectionNow，回调随后读取的content必须完整。
 */
@Fast
public class TestEndStreamCloseDuringHandlerKeepsBody {

	private static final int BodySize = 4096;

	private Netty netty;
	private HttpServer server;
	private int port;

	@BeforeEach
	public void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new ExchangeCollectingServer();
	}

	@AfterEach
	public void tearDown() {
		server.close();
		netty.close();
	}

	static final class ExchangeCollectingServer extends HttpServer {
		final java.util.concurrent.ConcurrentLinkedQueue<HttpExchange> created = new java.util.concurrent.ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
	}

	@Test
	public void testCloseDuringEndStreamHandlerKeepsContent() throws Exception {
		var handlerEntered = new CountDownLatch(1);
		var releaseHandler = new CountDownLatch(1);
		var handlerDone = new CountDownLatch(1);
		var bodySeen = new AtomicReference<Integer>();
		var requestSeen = new AtomicReference<Boolean>();

		// Normal模式：onEndStream经task11Executor派发（正是endStreamTaskPending保护的排队窗口）；
		// 非流模式body由框架缓冲在content
		server.addHandler("/slow", 64 * 1024, TransactionLevel.None, DispatchMode.Normal, x -> {
			handlerEntered.countDown();
			try {
				Assertions.assertTrue(releaseHandler.await(15, TimeUnit.SECONDS), "handler must be released");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			bodySeen.set(x.content().readableBytes());
			requestSeen.set(x.request() != null);
			handlerDone.countDown();
		});
		port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

		try (var sock = new Socket("127.0.0.1", port)) {
			sock.setSoTimeout(15_000);
			var os = sock.getOutputStream();
			var body = new byte[BodySize];
			java.util.Arrays.fill(body, (byte)'z');
			os.write(("POST /slow HTTP/1.1\r\nHost: a\r\nContent-Length: " + BodySize + "\r\n\r\n")
					.getBytes(StandardCharsets.ISO_8859_1));
			os.write(body);
			os.flush();

			// 等onEndStream回调开始执行（任务已出队、正在运行——修复前此刻pending已被清位）
			Assertions.assertTrue(handlerEntered.await(10, TimeUnit.SECONDS), "handler must start");

			// 并发close（任意线程）：closeInEventLoop在EL上执行，与运行中的回调并发
			((ExchangeCollectingServer)server).created.peek().closeConnectionNow();

			// 放行回调：此刻修复前content已被releaseTerminal清空（读到0/null）
			releaseHandler.countDown();
			Assertions.assertTrue(handlerDone.await(10, TimeUnit.SECONDS));

			Assertions.assertEquals(BodySize, bodySeen.get(),
					"close不得提前release：运行中的onEndStream回调必须读到完整body");
			Assertions.assertTrue(requestSeen.get(), "close不得提前release：运行中的回调必须读到request");
		}
	}
}
