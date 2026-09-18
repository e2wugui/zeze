package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpFileUploadHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-58回归：HttpServer.close()的shutdown(true)清扫取消流式任务时，fireEndStreamHandle
 * 的onCancel补偿只close exchange——channel attr上的multipart解码器/上传缓冲（堆数据、
 * 临时文件、未关fileChannel）从不销毁，永久驻留static工厂的requestFileDeleteMap，
 * 跨close→start重启累积无上界（正常断连路径经closeInEventLoop重新派发onEndStream会销毁，
 * 泄漏只在停机窗口）。
 * 修复后：cancel补偿内做资源级销毁（attr getAndSet(null)"取走即负责"，Netty destroy()
 * 非幂等），不跑用户回调；defaultHttpDataFactory补deleteOnExit(true)的JVM退出兜底。
 */
@Fast
public class TestFnd858ShutdownUploadCleanup {

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
			Thread.sleep(5);
		}
	}

	// 停机清扫在途raw上传：cancel补偿必须释放channel attr上的MixedFileUpload（取走即销毁）
	@Test
	public void testShutdownCleansInflightUpload() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		server.addHandler("/upload", TransactionLevel.None, DispatchMode.Normal,
				new HttpFileUploadHandle() { }); // multipart重载：begin/content/end全接，流模式生效 // Normal模式：收尾任务走队列，可被shutdown(true)清扫
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				// raw上传（非multipart）：onBeginStream把MixedFileUpload装入fileUploadKey attr
				os.write(("POST /upload?filename=a4_fnd858.bin HTTP/1.1\r\nHost: a\r\n"
						+ "Content-Type: application/octet-stream\r\n"
						+ "Content-Length: 100000\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				os.write(new byte[65536]); // 部分body（>MemoryBufSize，混合缓冲落盘），不发LastHttpContent
				os.flush();

				await("upload exchange created", 10_000, () -> !server.created.isEmpty());
				var x = server.created.peek();
				await("fileUpload in channel attr", 10_000,
						() -> x.channel().attr(HttpFileUploadHandle.fileUploadKey).get() != null);

				// 停机：closeConnectionNow→closeInEventLoop→fireEndStreamHandle提交即被shutdown清扫→
				// 内联onCancel。修复前cancel只close，attr永驻；修复后getAndSet取走并release。
				server.close();
				await("fileUpload released by cancel compensation", 10_000,
						() -> x.channel().attr(HttpFileUploadHandle.fileUploadKey).get() == null);
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 正常完结的上传不受影响：onEndStream先行取走销毁，停机cancel拿到null为no-op，无双跑
	@Test
	public void testCompletedUploadThenShutdownNoError() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		server.addHandler("/upload", TransactionLevel.None, DispatchMode.Normal,
				new HttpFileUploadHandle() { }); // multipart重载：begin/content/end全接，流模式生效
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				OutputStream os = sock.getOutputStream();
				os.write(("POST /upload?filename=a4_fnd858b.bin HTTP/1.1\r\nHost: a\r\n"
						+ "Content-Type: application/octet-stream\r\n"
						+ "Content-Length: 4\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.write("abcd".getBytes(StandardCharsets.ISO_8859_1)); // 完整body，正常完结路径
				os.flush();

				await("upload exchange created", 10_000, () -> !server.created.isEmpty());
				var x = server.created.peek();
				// onEndStream的getAndSet(null)先取走销毁（200 OK后attr必空）
				await("upload completed and attr cleaned by normal path", 10_000,
						() -> x.channel().attr(HttpFileUploadHandle.fileUploadKey).get() == null);
				// 收尾响应到达（200），连接未因停机误杀而异常
				InputStream is = sock.getInputStream();
				var buf = new byte[4096];
				var got = new StringBuilder();
				while (got.indexOf("200") < 0) {
					int n = is.read(buf);
					Assertions.assertTrue(n >= 0, "connection closed before response");
					got.append(new String(buf, 0, n, StandardCharsets.ISO_8859_1));
				}

				server.close(); // 正常路径已清空attr：cancel为no-op，不双destroy
				//noinspection BusyWait
				Thread.sleep(300); // 观察窗：无异常即通过（destroy二次调用会抛checkDestroyed异常进error日志）
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
