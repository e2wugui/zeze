package UnitTest.Zeze.Net;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
 * 背压回归：慢客户端+大响应不得被断连。原先channelWritabilityChanged在outbound缓冲越过水位
 * （writePendingLimit）时error+flush().close()直接杀连接——慢读客户端叠加文件/流式大响应必然
 * 触发，合法流量被误杀。修复后：越水位只是背压信号（info日志），ChunkedBodyStream越水位阻塞
 * 等待积压排出（awaitWritable），持续拥塞由写空闲超时兜底。watermark压到32KB加速触发。
 */
@Fast
public class TestBackpressureSlowClient {

	public static final class TestServer extends HttpServer {
		{
			writePendingLimit = 32 * 1024; // 低水位加速触发（initChannel读取，须在start前设置）
		}
	}

	// 慢读客户端：每次最多读16KB然后sleep，直到完成条件满足或流结束
	private static byte[] readSlowly(InputStream is, java.util.function.Predicate<byte[]> done, int chunkSleepMillis)
			throws Exception {
		var buf = new ByteArrayOutputStream();
		var bytes = new byte[16 * 1024];
		var deadline = System.currentTimeMillis() + 30_000;
		while (!done.test(buf.toByteArray())) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout, got " + buf.size());
			int n = is.read(bytes);
			if (n < 0)
				break;
			buf.write(bytes, 0, n);
			//noinspection BusyWait
			Thread.sleep(chunkSleepMillis);
		}
		return buf.toByteArray();
	}

	// 固定长度模式：单次1MB响应写向慢读客户端——越过水位后连接必须存活并完整送达。
	// 修复前：1MB写入即越过32KB水位，writability事件直接close，客户端读到EOF数据不全。
	@Test
	public void testFixedLengthSurvivesSlowClient() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var total = 1024 * 1024;
		var done = new CompletableFuture<Boolean>();
		server.addHandler("/big", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach();
			try {
				var out = HttpResponseWithBodyStream.sendHeadersAndGetBody(x,
						HttpResponseStatus.OK, Map.of("Content-Type", "application/octet-stream"), total);
				var block = new byte[4096];
				for (int i = 0; i < total / block.length; i++) {
					java.util.Arrays.fill(block, (byte)i);
					out.write(block, 0, block.length);
				}
				out.close();
				done.complete(true);
			} catch (Exception e) {
				done.completeExceptionally(e);
			}
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(30_000);
				sock.getOutputStream().write("GET /big HTTP/1.1\r\nHost: a\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
				sock.getOutputStream().flush();

				var raw = readSlowly(sock.getInputStream(),
						r -> headerEnd(r) >= 0 && r.length - headerEnd(r) - 4 >= total, 20);
				Assertions.assertTrue(done.get(10, TimeUnit.SECONDS), "server side stream must complete");
				var body = stripHeader(raw, total);
				Assertions.assertEquals(total, body.length, "full body must arrive, got " + body.length);
				// 校验内容：第i个4KB块的字节值恒为(byte)i
				for (int i = 0; i < total / 4096; i++)
					for (int j = 0; j < 4096; j += 997) // 抽样校验
						Assertions.assertEquals((byte)i, body[i * 4096 + j], "block " + i + " byte " + j);
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 分块模式：客户端先2秒不读（服务端写满水位后awaitWritable阻塞、连接存活——修复前这里就被杀），
	// 然后慢读全部数据，解chunked帧后校验总量与内容。
	@Test
	public void testChunkedSurvivesSlowClient() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new TestServer();
		var chunks = 256;
		var chunkSize = 4096;
		var total = chunks * chunkSize;
		var done = new CompletableFuture<Boolean>();
		server.addHandler("/chunked", 8192, TransactionLevel.None, DispatchMode.Normal, x -> {
			x.detach();
			try {
				var out = HttpResponseWithBodyStream.sendHeadersAndGetBody(x,
						HttpResponseStatus.OK, Map.of("Content-Type", "application/octet-stream"), 0);
				var block = new byte[chunkSize];
				for (int i = 0; i < chunks; i++) {
					java.util.Arrays.fill(block, (byte)i);
					out.write(block, 0, chunkSize); // 越水位时awaitWritable阻塞等待
				}
				out.close();
				done.complete(true);
			} catch (Exception e) {
				done.completeExceptionally(e);
			}
		});
		try {
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(30_000);
				sock.getOutputStream().write("GET /chunked HTTP/1.1\r\nHost: a\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
				sock.getOutputStream().flush();

				Thread.sleep(2000); // 客户端完全不读：服务端写满32KB水位后必须阻塞存活而非断连
				var term = "\r\n0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
				var raw = readSlowly(sock.getInputStream(),
						r -> contains(r, term), 10);
				Assertions.assertTrue(done.get(10, TimeUnit.SECONDS), "server side stream must complete");
				var body = dechunk(stripHeader(raw, raw.length));
				Assertions.assertEquals(total, body.length, "full body must arrive, got " + body.length);
				for (int i = 0; i < chunks; i++)
					Assertions.assertEquals((byte)i, body[i * chunkSize], "chunk " + i + " first byte");
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 字节序列包含判断
	private static boolean contains(byte[] haystack, byte[] needle) {
		outer:
		for (int i = 0; i + needle.length <= haystack.length; i++) {
			for (int j = 0; j < needle.length; j++)
				if (haystack[i + j] != needle[j])
					continue outer;
			return true;
		}
		return false;
	}

	// 响应头终结符(\r\n\r\n)位置，未收全返回-1
	private static int headerEnd(byte[] raw) {
		for (int i = 0; i + 3 < raw.length; i++)
			if (raw[i] == '\r' && raw[i + 1] == '\n' && raw[i + 2] == '\r' && raw[i + 3] == '\n')
				return i;
		return -1;
	}

	// 跳过HTTP响应头（到第一个空行）
	private static byte[] stripHeader(byte[] raw, int atLeast) {
		int i = headerEnd(raw);
		Assertions.assertTrue(i + 4 <= raw.length, "response header terminator not found");
		var body = new byte[raw.length - i - 4];
		System.arraycopy(raw, i + 4, body, 0, body.length);
		return body;
	}

	// 极简chunked解码：hex长度行 + 数据 + CRLF，0长度终结
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
			Assertions.assertTrue(p + size + 2 <= framed.length,
					"chunk body truncated p=" + p + " size=" + size + " len=" + framed.length
							+ " head=" + java.util.HexFormat.of().formatHex(framed, 0, Math.min(64, framed.length))
							+ " tail=" + java.util.HexFormat.of().formatHex(framed, Math.max(0, framed.length - 64), framed.length));
			out.write(framed, p, size);
			p += size + 2; // 数据 + CRLF
		}
	}
}
