package UnitTest.Zeze.Net;

import java.nio.charset.StandardCharsets;
import Zeze.Net.HaProxyHeader;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-17回归：HaProxy v1的107字节行长检查只在找不到CRLF时执行——任意垃圾后补
 * CRLF的超长行被原样接受（规范要求CRLF必须在前107字符内、整行含CRLF≤107）；
 * 端口parseInt不校验0..65535，越界值留到getter的InetSocketAddress构造抛未捕获
 * IllegalArgumentException（resolve只捕UnknownHostException）。修复：找到行后按
 * 整行判定超长即断连；端口解析处fail-fast（非数字/越界抛RuntimeException断连）；
 * resolve增捕IllegalArgumentException作纵深防御。
 */
@Fast
public class TestFnd817HaProxyV1Validation {

	private static ByteBuffer wrap(String s) {
		return ByteBuffer.Wrap(s.getBytes(StandardCharsets.ISO_8859_1));
	}

	private static void decodeExpectReject(String line) {
		var header = new HaProxyHeader("");
		var bb = wrap("PROXY " + line + "\r\n");
		Assertions.assertThrows(RuntimeException.class, () -> header.decodeHeader(bb),
				"畸形v1行必须解析处断连: " + line);
	}

	@Test
	public void testOversizedLineWithCrlfRejected() throws Exception {
		// 修复前红：检查只在无CRLF分支，超长行被原样接受（done=true）
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 " + "a".repeat(150) + " 80");
		// 无CRLF等待态检查保留：≤107继续等，>107断连
		var header = new HaProxyHeader("");
		Assertions.assertFalse(header.decodeHeader(wrap("PROXY TCP4 1.2.3.4")));
		Assertions.assertThrows(RuntimeException.class,
				() -> new HaProxyHeader("").decodeHeader(wrap("PROXY " + "a".repeat(200))));
	}

	@Test
	public void testPortValidation() throws Exception {
		// 修复前红：99999/-1/非数字被parseInt接受存入remotePort，getter抛未捕获IAE
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 99999 80");
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 80 65536");
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 -1 80");
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 abc 80");

		// 边界合法：0与65535
		var header = new HaProxyHeader("");
		var bb = wrap("PROXY TCP4 1.2.3.4 5.6.7.8 65535 0\r\n");
		Assertions.assertTrue(header.decodeHeader(bb));
		Assertions.assertEquals(65535, header.getRemoteAddress().getPort());
		Assertions.assertEquals(0, header.getTargetAddress().getPort());
	}

	@Test
	public void testValidLineStillParses() throws Exception {
		var header = new HaProxyHeader("");
		var bytes = "PROXY TCP4 1.2.3.4 5.6.7.8 12345 80\r\nnext-protocol".getBytes(StandardCharsets.ISO_8859_1);
		var bb = ByteBuffer.Wrap(bytes);
		Assertions.assertTrue(header.decodeHeader(bb));
		// 消费恰好整行（前缀+line+CRLF），后续协议数据不动
		Assertions.assertEquals("PROXY TCP4 1.2.3.4 5.6.7.8 12345 80\r\n".length(), bb.ReadIndex);
		Assertions.assertEquals("1.2.3.4", header.getRemoteAddress().getAddress().getHostAddress());
		Assertions.assertEquals(12345, header.getRemoteAddress().getPort());
		Assertions.assertEquals("5.6.7.8", header.getTargetAddress().getAddress().getHostAddress());
		Assertions.assertEquals(80, header.getTargetAddress().getPort());

		// 规范上限内的最长TCP6行（104字节≤107）照常接受
		var header6 = new HaProxyHeader("");
		var maxV6 = "PROXY TCP6 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff 65535 65535\r\n";
		Assertions.assertTrue(maxV6.length() <= 107, "测试行本身须在规范上限内: " + maxV6.length());
		Assertions.assertTrue(header6.decodeHeader(wrap(maxV6)));
		Assertions.assertEquals(65535, header6.getRemoteAddress().getPort());
	}
}
