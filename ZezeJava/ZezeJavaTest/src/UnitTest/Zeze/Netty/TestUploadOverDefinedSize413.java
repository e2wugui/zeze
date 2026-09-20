package UnitTest.Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpFileUploadHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * NY1-F4回归：raw上传超过声明大小（Content-Range声明的total）时addContent抛IOException被
 * 任务框架吞掉——onEndRequest永不执行、invokeEndStream的close(null)空写无响应体，客户端零字节
 * 挂到空闲超时。修复：onStreamContent/onEndStream捕获尺寸超限转413+closeConnectionOnFlush
 * （对齐streamContentTotal超限的既有处置模板）。
 * chunked body声明Content-Range: bytes 0-99/100（definedSize=100）实发8192字节——修复前
 * sendRawUntilClose挂到soTimeout；修复后收到413并断连。
 */
@Fast
public class TestUploadOverDefinedSize413 {
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure；默认maxUploadSize=256MB不干扰本路径
		server.addHandler("/raw", TransactionLevel.None, DispatchMode.Normal, new HttpFileUploadHandle() {
		});
		var channel = server.start(netty, 0).sync().channel();
		port = ((InetSocketAddress)channel.localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

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
	public void testRawUploadOverDefinedSizeReturns413AndCloses() throws Exception {
		// 一个8192字节的chunked块（>声明的100字节）：首个onStreamContent即超限
		var chunk = new StringBuilder().append(Integer.toHexString(8192)).append("\r\n")
				.append("x".repeat(8192)).append("\r\n").toString();
		var raw = "POST /raw?filename=a4_ny1f4.bin HTTP/1.1\r\nHost: a\r\n"
				+ "Content-Type: application/octet-stream\r\n"
				+ "Content-Range: bytes 0-99/100\r\n" // definedSize=100（parseRange取r[2]）
				+ "Transfer-Encoding: chunked\r\n\r\n" + chunk;
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), "raw上传超声明大小必须回413并断连: " + res);
	}
}
