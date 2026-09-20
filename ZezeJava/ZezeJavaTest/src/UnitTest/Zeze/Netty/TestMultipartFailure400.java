package UnitTest.Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpMultipartHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.handler.codec.http.multipart.Attribute;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * NY1-F3回归：multipart解码/回调中途失败后仍答200 OK（静默数据丢失）。onStreamContent的
 * 异常被任务框架吞掉，onEndStream无条件进默认onEndRequest发200。修复：catch内立即取走销毁
 * decoder（onEndStream因attr已空跳过200）、回400并断连。
 * 用onAttribute抛异常构造确定性红（catch覆盖解码与回调两类异常）；修复前此处收到200。
 */
@Fast
public class TestMultipartFailure400 {
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure
		server.addHandler("/mp", TransactionLevel.None, DispatchMode.Normal, new HttpMultipartHandle() {
			@Override
			public void onAttribute(@NotNull HttpExchange x, @NotNull Attribute attr) throws Exception {
				throw new RuntimeException("wt4: simulated multipart callback failure");
			}
		});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	// 发送原始请求字节并读到服务器关闭连接(EOF)为止,返回收到的完整响应
	private static @NotNull String sendRawUntilClose(@NotNull String raw) throws IOException {
		try (var sock = connectWithRetry()) {
			sock.setSoTimeout(15000);
			var os = sock.getOutputStream();
			os.write(raw.getBytes(StandardCharsets.ISO_8859_1));
			os.flush();
			var out = new ByteArrayOutputStream();
			var buf = new byte[4096];
			var in = sock.getInputStream();
			int n;
			while ((n = in.read(buf)) >= 0)
				out.write(buf, 0, n);
			return out.toString(StandardCharsets.ISO_8859_1);
		}
	}

	// Windows满负载下loopback connect偶发SYN无应答：显式短超时+有界重试（对齐TestHttpServerUploadLimit）
	private static Socket connectWithRetry() throws IOException {
		var target = new InetSocketAddress("127.0.0.1", port);
		for (int attempt = 1; ; ++attempt) {
			var sock = new Socket();
			try {
				sock.connect(target, 5_000);
				return sock;
			} catch (SocketTimeoutException | java.net.ConnectException e) {
				sock.close();
				if (attempt >= 3)
					throw e;
			}
		}
	}

	@Test
	public void testMultipartCallbackFailureReturns400AndCloses() throws Exception {
		var body = "--B\r\n"
				+ "Content-Disposition: form-data; name=\"field1\"\r\n"
				+ "\r\n"
				+ "value1\r\n"
				+ "--B--\r\n";
		var raw = "POST /mp HTTP/1.1\r\nHost: a\r\n"
				+ "Content-Type: multipart/form-data; boundary=B\r\n"
				+ "Content-Length: " + body.length() + "\r\n\r\n" + body;
		var res = sendRawUntilClose(raw);
		// 修复前：异常被吞，onEndRequest默认回200 OK（静默数据丢失）——断言失败于此；
		// 修复后：立即回400并断连
		Assertions.assertTrue(res.startsWith("HTTP/1.1 400"), "multipart失败必须回400并断连: " + res);
	}
}
