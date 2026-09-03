package UnitTest.Zeze.Netty;

import harness.Fast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-5回归:流模式上传(multipart/raw)不受handler.MaxContentLength约束,
 * multipart默认实现(DefaultHttpDataFactory全堆内,maxSize=Long.MAX_VALUE)可被无限大body打爆堆,
 * raw上传(MixedFileUpload definedSize=0,未setMaxSize)超16KB落盘后无限增长耗尽磁盘。
 * 修复后流模式请求body总量以server.maxUploadSize兜底,超限回413并断开连接。
 */
@Fast
public class TestHttpServerUploadLimit {
	private static final int MaxUpload = 8192;
	private static Netty netty;
	private static HttpServer server;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer(); // 无zeze, noProcedure, Direct派发inline执行
		server.setMaxUploadSize(MaxUpload);
		// 流模式上传handler:不缓冲content(边收边丢),结束时回200
		server.addHandler("/upload", TransactionLevel.None, DispatchMode.Direct,
				(x, from, to, size) -> { },
				(x, c) -> { },
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
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
		try (var sock = new Socket("127.0.0.1", port)) {
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

	// 只发count块、不带终止块:让服务器在读完全部已发数据后恰好触发超限关闭,
	// 避免接收缓冲残留未读数据导致close时内核发RST抹掉413响应
	private static @NotNull String chunkedBodyNoEnd(int chunkSize, int chunkCount) {
		var sb = new StringBuilder();
		var chunk = new String(new char[chunkSize]).replace('\0', 'x');
		for (int i = 0; i < chunkCount; i++)
			sb.append(Integer.toHexString(chunkSize)).append("\r\n").append(chunk).append("\r\n");
		return sb.toString();
	}

	// 正常路径:总量恰等于上限(不超),完整处理返回200
	@Test
	public void testUploadWithinLimit() throws Exception {
		var body = new byte[MaxUpload];
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/upload"))
				.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, res.statusCode());
		Assertions.assertEquals("ok", res.body());
	}

	// chunked上传超限(无Content-Length,raw上传definedSize=0的场景):回413并关闭连接
	@Test
	public void testChunkedUploadOverLimit() throws Exception {
		// 3*4096=12288 > 8192,第3块触发超限
		var raw = "POST /upload HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
				+ chunkedBodyNoEnd(4096, 3);
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
	}

	// Content-Length声明大小同样受总量上限约束(chunked可谎报/省略声明,检查必须按实际接收字节累计)
	@Test
	public void testContentLengthUploadOverLimit() throws Exception {
		var body = new String(new char[MaxUpload * 2]).replace('\0', 'y');
		var raw = "POST /upload HTTP/1.1\r\nHost: a\r\nContent-Length: " + body.length()
				+ "\r\n\r\n" + body;
		var res = sendRawUntilClose(raw);
		Assertions.assertTrue(res.startsWith("HTTP/1.1 413"), res);
	}
}
