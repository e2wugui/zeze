package Zeze.Net;

import java.nio.charset.StandardCharsets;
import Zeze.Net.HaProxyHeader;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND15 net-02 回归：HaProxy v1头解析区分"未出现CRLF"（等待更多数据）与"空行"
 * （"PROXY \r\n"畸形头）——空行停留在等待态会使连接永久挂在头解析占槽；
 * 同族：家族行字段缺失与不支持家族的5-token行不得静默按直连放行。
 */
@Fast
public class TestFnd15Net02HaProxyV1EmptyLine {

	private static ByteBuffer wrap(String s) {
		return ByteBuffer.Wrap(s.getBytes(StandardCharsets.ISO_8859_1));
	}

	private static void decodeExpectReject(String line) {
		var header = new HaProxyHeader("");
		var bb = wrap("PROXY " + line + "\r\n");
		Assertions.assertThrows(RuntimeException.class, () -> header.decodeHeader(bb),
				"畸形v1行必须解析处断连: " + line);
	}

	// 空行（8字节攻击串）：修复前红——findV1Line返回""走"等待更多数据"，decodeHeader恒false。
	@Test
	public void testEmptyLineRejected() {
		decodeExpectReject("");
	}

	// 家族行字段缺失（修复前红：静默接受done=true、地址null按直连放行）
	@Test
	public void testIncompleteFamilyLineRejected() {
		decodeExpectReject("TCP4");
		decodeExpectReject("TCP4 1.2.3.4 5.6.7.8 80");
	}

	// 不支持家族的5-token行（对齐v2分支default拒绝）
	@Test
	public void testUnsupportedFamilyRejected() {
		decodeExpectReject("SCTP 1.2.3.4 5.6.7.8 12345 80");
	}

	// 守护：未出现CRLF的部分行继续等待（真实LB连接建立瞬间都经过此状态，不得误杀）。
	@Test
	public void testPartialLineStillWaits() throws Exception {
		var header = new HaProxyHeader("");
		Assertions.assertFalse(header.decodeHeader(wrap("PROXY TCP4 1.2.3.4")),
				"无CRLF的部分行必须等待更多数据");
		Assertions.assertFalse(header.decodeHeader(wrap("PROXY")),
				"仅前缀也等待（不足8字节甚至不进本分支）");
	}

	// 守护："PROXY UNKNOWN\r\n"（规范唯一合法短行）仍被接受。
	@Test
	public void testUnknownLineStillAccepted() throws Exception {
		var header = new HaProxyHeader("");
		Assertions.assertTrue(header.decodeHeader(wrap("PROXY UNKNOWN\r\n")));
	}
}
