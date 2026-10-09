package Zeze.Netty;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;

import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * h2 大文件下载跨写缓冲水位（writability 翻转）必须完整送达：
 * channelWritabilityChanged 的背压日志不得解引用 transport 私有句柄——
 * h2 子stream的 channel 没有 TCP ChannelOutboundBuffer（unsafe().outboundBuffer()
 * 返回 null），日志表达式 NPE 会被 exceptionCaught 当协议异常处理，
 * 关闭合法下载流（大文件下载在背压窗口处中断）。
 */
@Fast
public class TestHttp2SendFileBackpressure {

	// 收集全部exchange的生命周期观察服务器（残留断言范式）
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
	public void largeSendFileCrossingWritabilityWindowCompletes() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new CollectingServer();
		var file = Files.createTempFile("h2backpressure", ".bin");
		try {
			// 4MB远超默认写缓冲水位（64KB）：sendFile期间h2子stream必然经历
			// unwritable→writable翻转，背压期间流必须保持存活。
			var expected = new byte[4 * 1024 * 1024];
			for (int i = 0; i < expected.length; i++)
				expected[i] = (byte)(i % 251);
			Files.write(file, expected);
			server.addHandler("/big", 8192, TransactionLevel.None, DispatchMode.Normal,
					x -> x.sendFile(file.toFile(), 0));
			try (var client = new H2TestClient()) {
				var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
				client.connect(port);
				var result = client.request(HttpMethod.GET, "/big", null).await();
				Assertions.assertEquals(HttpResponseStatus.OK, result.status());
				Assertions.assertArrayEquals(expected, result.body(), "跨背压窗口的下载必须字节级完整");
				awaitAllReleased(server);
			}
		} finally {
			Files.deleteIfExists(file);
			server.close();
			netty.close();
		}
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
