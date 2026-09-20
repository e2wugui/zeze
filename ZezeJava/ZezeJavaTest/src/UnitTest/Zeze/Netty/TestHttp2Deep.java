package UnitTest.Zeze.Netty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * h2深度测试族（PR3）：并发stream乱序完成交织（h2的核心价值——h1序化器下同场景是
 * B等A，h2下B先走）、多帧上传累积（100KB跨多个DATA帧）、sendFile的h2分块路径
 * （HttpFileService手写分块，无零拷贝）、exchange生命周期防漏
 * （createHttpExchange收集+轮询isClosed，对齐Fnd727范式）。
 */
@Fast
public class TestHttp2Deep {

	// 收集全部exchange的生命周期观察服务器（对齐Fnd727的残留断言范式）
	static final class CollectingServer extends HttpServer {
		final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}
	}

	@Test
	public void testConcurrentStreamsOutOfOrderCompletion() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new CollectingServer();
		// detach+延迟补发：A等500ms、B等50ms——B必须先完成且互不串流（h2允许乱序；
		// 同场景h1下B的字节必须排在A后，是序化器的领地）
		server.addHandler("/slow", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach();
			// path()只含纯路径（query被剥），延迟参数从request().uri()取
			var uri = x.request().uri();
			var delay = Long.parseLong(uri.substring(uri.indexOf('=') + 1));
			TaskSpec.ofAction(() -> x.close(x.sendPlainText(HttpResponseStatus.OK,
					"done-" + delay))).scheduleNow(delay);
		});
		try (var client = new H2TestClient()) {
			var port = start(server, netty);
			client.connect(port);
			var a = client.request(HttpMethod.GET, "/slow?ms=500", null);
			var b = client.request(HttpMethod.GET, "/slow?ms=50", null);
			var resultB = b.await();
			Assertions.assertEquals("done-50", resultB.bodyText(), "B（50ms）的响应体");
			Assertions.assertFalse(a.done().isDone(), "B完成时A（500ms）必须仍在途——乱序完成是h2合法形态");
			Assertions.assertEquals("done-500", a.await().bodyText(), "A（500ms）的响应体");
			awaitAllReleased(server);
		} finally {
			server.close();
			netty.close();
		}
	}

	@Test
	public void testMultiFrameUpload() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new CollectingServer();
		// 100KB体跨多个DATA帧（客户端默认16KB帧上限），验证累积路径content()的完整性
		server.addHandler("/up", 256 * 1024, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK,
						x.content().readableBytes() + ":" + x.content().getByte(0) + ":" + x.content().getByte(x.content().readableBytes() - 1)));
		try (var client = new H2TestClient()) {
			var port = start(server, netty);
			client.connect(port);
			var body = new byte[100 * 1024];
			for (int i = 0; i < body.length; i++)
				body[i] = (byte)(i % 251);
			body[0] = 0x5a;
			body[body.length - 1] 	= 0x61;
			var result = client.request(HttpMethod.POST, "/up", body).await();
			Assertions.assertEquals(HttpResponseStatus.OK, result.status());
			Assertions.assertEquals((100 * 1024) + ":90:97", result.bodyText(),
					"上传体长度与首尾字节（0x5a=90, 0x61=97）");
			awaitAllReleased(server);
		} finally {
			server.close();
			netty.close();
		}
	}

	@Test
	public void testSendFileChunkedOverH2() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new CollectingServer();
		var file = Files.createTempFile("h2chunk", ".bin");
		try {
			// 60KB模式文件：跨4个16KB DATA帧（多帧覆盖）。限制在默认流控窗内——裸netty自栈的
			// 测试客户端不回补接收窗，>64KB会与服务端发送窗死锁（协议正确性无碍，真实h2客户端
			// 按RFC必回窗；跨窗边界的下载验证待真实h2客户端可用时补充）。
			var expected = new byte[60 * 1024];
			for (int i = 0; i < expected.length; i++)
				expected[i] = (byte)(i % 241);
			Files.write(file, expected);
			server.addHandler("/file", 8192, TransactionLevel.None, DispatchMode.Normal,
					x -> x.sendFile(file.toFile(), 0));
			try (var client = new H2TestClient()) {
				var port = start(server, netty);
				client.connect(port);
				var result = client.request(HttpMethod.GET, "/file", null).await();
				Assertions.assertEquals(HttpResponseStatus.OK, result.status());
				Assertions.assertArrayEquals(expected, result.body(), "分块传输必须字节级一致");
				awaitAllReleased(server);
			}
		} finally {
			Files.deleteIfExists(file);
			server.close();
			netty.close();
		}
	}

	private static int start(HttpServer server, Netty netty) throws Exception {
		var port = ((java.net.InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
		return port;
	}

	private static void awaitAllReleased(CollectingServer server) throws Exception {
		var deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			var allClosed = server.created.stream().allMatch(HttpExchange::isClosed);
			if (allClosed && server.created.stream().allMatch(x -> x.request() == null))
				return;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		Assertions.fail("h2 exchange未全部释放: " + server.created.size());
	}
}
