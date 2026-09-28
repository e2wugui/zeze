package Zeze.Netty;

import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
 * FND12 net-01回归：PrefaceDetector原以"攒满24字节魔数"为唯一判定量下限，在攒够
 * 之前不向后续handler传递任何字节——总长不足24字节的完整HTTP请求（极简HTTP/1.0、
 * 探活）永远到不了HttpRequestDecoder，零响应直至writeIdleTimeout（默认60s）被关。
 * 修复=增量比对已到字节：任何一位与魔数前缀不符即判定h1并自移除透传；h2完整魔数
 * 判定语义不变（全部24字节相符才换栈）。裸socket发20字节的HTTP/1.0请求必须秒级
 * 拿到应答（修复前挂到soTimeout超时）。
 */
@Fast
public class TestFnd12Net01ShortHttpRequestPassesPrefaceDetector {

	@Test
	public void testShortHttp10RequestGetsResponse() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/ok", 8192, TransactionLevel.None, DispatchMode.Normal,
				x -> x.sendPlainText(HttpResponseStatus.OK, "ok"));
		try {
			var port = ((java.net.InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			try (var sock = new Socket("127.0.0.1", port)) {
				sock.setSoTimeout(10_000);
				// 20字节 < 24字节魔数：修复前探测器永远攒不够、请求到不了解码器
				sock.getOutputStream().write("GET /ok HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
				sock.getOutputStream().flush();
				var buf = new byte[512];
				var n = sock.getInputStream().read(buf);
				Assertions.assertTrue(n > 0, "short http/1.0 request must get a response");
				var head = new String(buf, 0, Math.min(n, 64), StandardCharsets.US_ASCII);
				Assertions.assertTrue(head.startsWith("HTTP/"), "unexpected response head: " + head);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
