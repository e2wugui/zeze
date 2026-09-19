package UnitTest.Zeze.Net;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import Zeze.Netty.HttpResponseWithBodyStream;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ChunkedBodyStream的OutputStream缓冲复用契约回归：write(byte[],int,int)返回后调用方即可
 * 复用/改写缓冲——GZIPOutputStream（Prometheus /metrics gzip路径）的deflate循环正是反复用
 * 同一个内部buf调用out.write。修复前write经sendStream零拷贝包装调用方数组，chunk在
 * EventLoop上编码/写socket之前数组已被下一轮deflate覆写，响应损坏（gunzip ZipException）。
 * 修复后write先拷贝再送出。Normal派发复现真实形态：handler运行在派发线程，编码必然滞后于write返回。
 */
@Fast
public class TestChunkedBodyStreamGzip {

	@Test
	public void testGzipChunkedBufferReuseNotCorrupted() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		var pattern = new byte[8192];
		for (int i = 0; i < pattern.length; i++)
			pattern[i] = (byte)(i * 31 + 7);
		int blocks = 32; // 256KB：gzip输出远超其512B内部buf，多次复用必然发生
		var expected = new ByteArrayOutputStream();
		for (int i = 0; i < blocks; i++)
			expected.write(pattern);

		server.addHandler("/gz", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach();
			try (var gzip = new GZIPOutputStream(HttpResponseWithBodyStream.sendHeadersAndGetBody(x,
					HttpResponseStatus.OK, Map.of("Content-Type", "application/octet-stream"), 0))) {
				for (int i = 0; i < blocks; i++)
					gzip.write(pattern); // gzip内部复用buf逐块写出，契约内合法
				gzip.finish();
			}
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(30_000);
				sock.getOutputStream().write("GET /gz HTTP/1.1\r\nHost: a\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
				sock.getOutputStream().flush();

				var raw = new ByteArrayOutputStream(); // 读到chunked终结符为止（keep-alive无EOF）
				var bytes = new byte[8192];
				var deadline = System.currentTimeMillis() + 30_000;
				while (System.currentTimeMillis() < deadline) {
					int n = sock.getInputStream().read(bytes);
					if (n < 0)
						break;
					raw.write(bytes, 0, n);
					if (new String(raw.toByteArray(), StandardCharsets.ISO_8859_1).contains("\r\n0\r\n\r\n"))
						break;
				}
				var s = new String(raw.toByteArray(), StandardCharsets.ISO_8859_1);
				int h = s.indexOf("\r\n\r\n");
				Assertions.assertTrue(h >= 0, "response header terminator not found");
				Assertions.assertTrue(s.contains("\r\n0\r\n\r\n"), "chunked terminator not found");

				var gunzipped = new byte[0];
				try (var gin = new GZIPInputStream(new java.io.ByteArrayInputStream(
						dechunk(java.util.Arrays.copyOfRange(raw.toByteArray(), h + 4, raw.size()))))) {
					gunzipped = gin.readAllBytes();
				} catch (Exception e) {
					Assertions.fail("gunzip failed (corrupted chunked payload): " + e);
				}
				Assertions.assertArrayEquals(expected.toByteArray(), gunzipped, "gzip body corrupted");
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 极简chunked解码：hex长度行 + 数据 + CRLF，0长度终结（与TestBackpressureSlowClient同型）
	private static byte[] dechunk(byte[] framed) throws Exception {
		var out = new ByteArrayOutputStream();
		int p = 0;
		for (;;) {
			int lineEnd = p;
			while (lineEnd + 1 < framed.length && !(framed[lineEnd] == '\r' && framed[lineEnd + 1] == '\n'))
				lineEnd++;
			Assertions.assertTrue(lineEnd + 2 <= framed.length, "chunk size line truncated");
			int size = Integer.parseInt(new String(framed, p, lineEnd - p, StandardCharsets.ISO_8859_1).trim(), 16);
			p = lineEnd + 2;
			if (size == 0)
				return out.toByteArray();
			Assertions.assertTrue(p + size + 2 <= framed.length, "chunk body truncated");
			out.write(framed, p, size);
			p += size + 2;
		}
	}
}
