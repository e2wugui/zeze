package UnitTest.Zeze.Netty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND7-26回归：checkTimeout由调度线程对idleTime做get→+interval→set读改写，
 * HttpExchange.channelRead在channel的EventLoop上set(null)清零——Netty的AttributeMap
 * 单次读写原子但读-改-写序列不原子，清零落在get与set之间时被写回旧值：长时间空闲的
 * keep-alive连接恰在检查瞬间开始收新请求（只收不发，如大上传）会被误判空闲，
 * CLOSE_TIMEOUT关闭正在活跃收流的连接，违反"超时只长不短"契约。
 * 修复：检查主体投递到channel自己的EventLoop执行，与清零同队列串行。
 */
@Fast
public class TestFnd726HttpServerIdleCheck {
	private static Netty netty;
	private static AffinityServer affinityServer; // 检查间隔设为1小时：测试期间真实调度器永不tick
	private static int affinityPort;
	private static UploadServer uploadServer; // 短超时：真实调度器每秒检查
	private static int uploadPort;

	// 亲和性验证用：记录checkTimeout0实际执行的线程与accept的channel
	public static final class AffinityServer extends HttpServer {
		public volatile Thread lastCheckThread;
		public final ConcurrentLinkedQueue<Channel> accepted = new ConcurrentLinkedQueue<>();

		@Override
		protected void initChannel(@NotNull SocketChannel ch) throws Exception {
			super.initChannel(ch);
			accepted.add(ch);
		}

		@Override
		protected void checkTimeout0(@NotNull Channel channel) {
			lastCheckThread = Thread.currentThread();
			super.checkTimeout0(channel);
		}

		public void checkTimeoutForTest(@NotNull Channel channel) {
			checkTimeout(channel);
		}
	}

	public static final class UploadServer extends HttpServer {
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(2);
		affinityServer = new AffinityServer();
		affinityServer.setCheckIdleInterval(3600); // 真实调度器测试期内不tick，只留手工调用
		affinityServer.setReadIdleTimeout(7200);
		affinityServer.setWriteIdleTimeout(7200);
		affinityServer.addHandler("/ok", 8192, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendPlainText(io.netty.handler.codec.http.HttpResponseStatus.OK, "ok"));
		affinityPort = ((InetSocketAddress)affinityServer.start(netty, 0).sync().channel().localAddress()).getPort();

		uploadServer = new UploadServer();
		uploadServer.setCheckIdleInterval(1);
		uploadServer.setReadIdleTimeout(2); // 2秒无读即达读超时阈值
		uploadServer.setWriteIdleTimeout(3); // 3秒无读无写进度即关闭
		uploadServer.addHandler("/upload", 1024 * 1024, TransactionLevel.None, DispatchMode.Direct,
				x -> x.sendPlainText(io.netty.handler.codec.http.HttpResponseStatus.OK,
						Integer.toString(x.content().readableBytes())));
		uploadPort = ((InetSocketAddress)uploadServer.start(netty, 0).sync().channel().localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		uploadServer.close();
		affinityServer.close();
		netty.close();
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

	// 修复的本质：空闲检查的读-改-写必须与channelRead的清零在同一个EventLoop队列串行。
	// 手工从测试线程调用checkTimeout，检查主体必须跑到channel自己的EventLoop线程上，
	// 而不是调用方（调度线程）线程上——修复前内联执行于调用线程，此断言确定性失败。
	@Test
	public void testCheckRunsOnChannelEventLoop() throws Exception {
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + affinityPort + "/ok")).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		await("accepted channel recorded", 10_000, () -> !affinityServer.accepted.isEmpty());
		var ch = affinityServer.accepted.peek();

		final Thread[] elThread = new Thread[1];
		ch.eventLoop().submit(() -> elThread[0] = Thread.currentThread()).await();
		Assertions.assertNotNull(elThread[0]);

		// isActive前后对比（30轮压测轮24假红）：本用例验证的是"手工检查不得关闭无空闲连接"
		// （checkTimeout0在interval=3600/timeout=7200下数学上必早退）——客户端/OS侧断开
		// （网络异常窗口）是无关变量，不得使断言假红。
		var activeBefore = ch.isActive();
		affinityServer.lastCheckThread = null;
		affinityServer.checkTimeoutForTest(ch); // 模拟调度线程发起检查
		await("check executed", 10_000, () -> affinityServer.lastCheckThread != null);
		Assertions.assertSame(elThread[0], affinityServer.lastCheckThread,
				"checkTimeout必须在channel的EventLoop上执行（与活动清零串行），不得在调度/调用线程上读改写");
		if (activeBefore)
			Assertions.assertTrue(ch.isActive(), "无空闲连接不得被检查关闭");
	}

	// 行为守卫：连接以200ms间隔持续收流约4秒（超过read=2/write=3的关闭阈值全程无响应写出），
	// 期间真实调度器每秒检查——持续活跃的连接不得被误杀，请求最终正常完成并得到200。
	@Test
	public void testActiveSlowUploadNotKilled() throws Exception {
		try (var sock = new Socket("127.0.0.1", uploadPort)) {
			sock.setSoTimeout(15_000);
			var os = sock.getOutputStream();
			os.write("POST /upload HTTP/1.1\r\nHost: a\r\nContent-Length: 8000\r\n\r\n"
					.getBytes(StandardCharsets.ISO_8859_1));
			os.flush();
			var chunk = new byte[400];
			for (int i = 0; i < 20; i++) { // 20片*200ms≈4秒缓慢收流，只收不发
				//noinspection BusyWait
				Thread.sleep(200);
				os.write(chunk); // 连接若被误杀，这里抛Broken pipe/最终读不到200
				os.flush();
			}
			var br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.ISO_8859_1));
			var statusLine = br.readLine();
			Assertions.assertNotNull(statusLine, "connection killed during active upload");
			Assertions.assertEquals("HTTP/1.1 200 OK", statusLine);
			for (var line = br.readLine(); line != null && !line.isEmpty(); line = br.readLine()) {
				// 消化响应头
			}
		}
	}
}
