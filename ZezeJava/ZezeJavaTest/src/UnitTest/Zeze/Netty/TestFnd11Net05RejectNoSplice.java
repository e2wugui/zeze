package UnitTest.Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND11 net-05回归：rejectAndClose（停机503/创建策略503/解码失败400共用）曾无条件
 * 豁免序化器直写拒绝响应——序化器头部在途entry的响应已开始写（流式响应字节在途）时，
 * 拒绝响应拼接进前序响应体中途（帧腐蚀，违反"宁断连不错序"）。
 * 修复后分级：头部已开始写→抑制拒绝响应仅关连接；未开始写/无在途→维持直写
 * （串行客户端常态路径；pipelining错位归属保留为已知限制）。
 */
@Fast
public class TestFnd11Net05RejectNoSplice {

	private static String readAll(InputStream is) throws Exception {
		var buf = new ByteArrayOutputStream();
		var bytes = new byte[4096];
		int n;
		while ((n = is.read(bytes)) >= 0)
			buf.write(bytes, 0, n);
		return buf.toString(StandardCharsets.ISO_8859_1);
	}

	private static int openPort(HttpServer server, Netty netty) throws Exception {
		return ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
	}

	// Direct派发保证序化器head的started先于畸形请求的解码失败处理（同EL任务序）：
	// /stream的响应已写出（200+partial，未endStream）+ /bad的非法chunk size触发400拒绝。
	@Test
	public void testStreamingHeadSuppressesReject() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/stream", 8192, TransactionLevel.None, DispatchMode.Direct, x -> {
			x.detach(); // 保持exchange开放（队头），响应未完成
			x.beginStream(HttpResponseStatus.OK, new DefaultHttpHeaders()); // 状态行+chunked头已写
			x.sendStream("partial".getBytes(StandardCharsets.ISO_8859_1)); // 未endStream：流在途
		});
		try {
			var port = openPort(server, netty);
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /stream HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /bad HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
						+ "XXXX\r\n").getBytes(StandardCharsets.ISO_8859_1)); // XXXX=非法chunk size
				os.flush();
				var received = readAll(sock.getInputStream());
				Assertions.assertTrue(received.contains("200"), "stream response must be started: " + received);
				Assertions.assertTrue(received.contains("partial"), "stream bytes must be flushed: " + received);
				Assertions.assertFalse(received.contains("400"),
						"reject response must be suppressed when head started (splice): " + received);
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 对照：head未开始写（/pending保持开放且未写任何响应字节）时拒绝响应仍直写——
	// 串行客户端的常态路径（被拒请求自身即head）不能被过度抑制。
	@Test
	public void testUnstartedHeadStillGetsReject() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/pending", 8192, TransactionLevel.None, DispatchMode.Direct, x -> x.detach());
		try {
			var port = openPort(server, netty);
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(15_000);
				var os = sock.getOutputStream();
				os.write(("GET /pending HTTP/1.1\r\nHost: a\r\n\r\n"
						+ "GET /bad HTTP/1.1\r\nHost: a\r\nTransfer-Encoding: chunked\r\n\r\n"
						+ "XXXX\r\n").getBytes(StandardCharsets.ISO_8859_1));
				os.flush();
				var received = readAll(sock.getInputStream());
				Assertions.assertTrue(received.contains("400"), "unstarted head must still get reject: " + received);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
