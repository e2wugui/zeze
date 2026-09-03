package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.HaProxyHeader;
import Zeze.Serialize.ByteBuffer;

/**
 * FND-N1-3 回归：HaProxyHeader v1 的地址解析移出 selector 线程（懒解析）。
 * decodeHeader 只记录主机名/端口，不再调用 InetAddress.getByName——非字面量主机名会同步DNS解析
 * （默认可达5-30秒），攻击者可用伪造的PROXY行卡死整个Selector线程上的所有连接。
 * 地址推迟到 getRemoteAddress/getTargetAddress 首次访问时解析。
 */
@Fast
public class TestHaProxyHeader {

	private static ByteBuffer buf(String s) {
		return ByteBuffer.Wrap(s.getBytes(StandardCharsets.ISO_8859_1));
	}

	// v1 主机名token：decodeHeader 必须不抛异常立即完成。
	// 修复前：这里同步DNS解析阻塞后抛 UnknownHostException（本用例红）。
	@Test
	public final void testV1HostnameDecodeNoResolve() throws Exception {
		var header = new HaProxyHeader(null);
		var bb = buf("PROXY TCP4 some.nonexistent.host.example 1.2.3.4 1 2\r\nX");
		Assertions.assertTrue(header.decodeHeader(bb));
		Assertions.assertEquals(1, bb.size()); // 只剩尾随数据'X'，头已消费
		// 刻意不调用getter：避免测试环境产生真实DNS依赖
	}

	// v1 字面量IP：getter懒解析得到正确地址（字面量不查DNS）。
	@Test
	public final void testV1LiteralLazyResolve() throws Exception {
		var header = new HaProxyHeader(null);
		var bb = buf("PROXY TCP4 127.0.0.1 127.0.0.2 111 222\r\n");
		Assertions.assertTrue(header.decodeHeader(bb));
		Assertions.assertEquals(0, bb.size());
		var remote = header.getRemoteAddress();
		var target = header.getTargetAddress();
		Assertions.assertNotNull(remote);
		Assertions.assertNotNull(target);
		Assertions.assertEquals(InetAddress.getByName("127.0.0.1"), remote.getAddress());
		Assertions.assertEquals(111, remote.getPort());
		Assertions.assertEquals(InetAddress.getByName("127.0.0.2"), target.getAddress());
		Assertions.assertEquals(222, target.getPort());
	}

	// v2 回归：字节字面量地址仍在decode时直接解析（不经懒解析路径）。
	@Test
	public final void testV2Regression() throws Exception {
		var bb = ByteBuffer.Allocate(28);
		bb.Append(HaProxyHeader.v2sig, 0, HaProxyHeader.v2sig.length); // 裸写：WriteBytes 会先写长度前缀（序列化约定）
		bb.WriteByte(0x21); // version 2, command PROXY
		bb.WriteByte(0x11); // AF_INET + SOCK_STREAM
		bb.WriteByte(0); bb.WriteByte(12); // length=12（4+4+2+2）
		bb.WriteInt4BE(0x7f000001); // src 127.0.0.1
		bb.WriteInt4BE(0x7f000002); // dst 127.0.0.2
		bb.WriteByte(111 >> 8); bb.WriteByte(111 & 0xff);
		bb.WriteByte(222 >> 8); bb.WriteByte(222 & 0xff);
		var header = new HaProxyHeader(null);
		Assertions.assertTrue(header.decodeHeader(bb));
		Assertions.assertEquals(0, bb.size());
		var remote = header.getRemoteAddress();
		var target = header.getTargetAddress();
		Assertions.assertNotNull(remote);
		Assertions.assertNotNull(target);
		Assertions.assertEquals(InetAddress.getByName("127.0.0.1"), remote.getAddress());
		Assertions.assertEquals(111, remote.getPort());
		Assertions.assertEquals(InetAddress.getByName("127.0.0.2"), target.getAddress());
		Assertions.assertEquals(222, target.getPort());
	}

	// v1 超长行仍按原逻辑拒绝（回归保护）。
	@Test
	public final void testV1LineTooLongStillThrows() {
		var header = new HaProxyHeader(null);
		var sb = new StringBuilder("PROXY ");
		for (int i = 0; i < 120; i++)
			sb.append('a');
		Assertions.assertThrows(RuntimeException.class, () -> header.decodeHeader(buf(sb.toString())));
	}
}
