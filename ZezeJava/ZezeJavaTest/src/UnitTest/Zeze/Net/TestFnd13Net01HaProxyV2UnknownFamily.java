package UnitTest.Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.HaProxyHeader;
import Zeze.Serialize.ByteBuffer;

/**
 * FND13 net-01：v2 PROXY command 携带不支持的 transport family（UDP4/UNIX 等）必须按畸形协议
 * 拒绝（fail-closed，对齐 cmd 非法值与 checkV2AddressLength 判例）。修复前整个头被静默消费、
 * remote/target 地址保持 null、连接按直连继续（fail-open）——LB 提供的真实客户端 IP 静默丢失。
 */
@Fast
public class TestFnd13Net01HaProxyV2UnknownFamily {

	// fam=0x12（AF_INET+SOCK_DGRAM，spec 定义但本实现不支持）：修复前静默接受且不设地址（本用例红）。
	@Test
	public final void testUdp4FamilyRejected() {
		var header = new HaProxyHeader(null);
		Assertions.assertThrows(RuntimeException.class, () -> header.decodeHeader(buildV2((byte)0x12)));
	}

	// fam=0x31（AF_UNIX+SOCK_STREAM）：同上拒绝。
	@Test
	public final void testUnixStreamFamilyRejected() {
		var header = new HaProxyHeader(null);
		Assertions.assertThrows(RuntimeException.class, () -> header.decodeHeader(buildV2((byte)0x31)));
	}

	// 已支持的 0x11（TCPv4）不受影响（回归）。
	@Test
	public final void testTcp4StillAccepted() throws Exception {
		var header = new HaProxyHeader(null);
		Assertions.assertTrue(header.decodeHeader(buildV2((byte)0x11)));
		Assertions.assertNotNull(header.getRemoteAddress());
	}

	private static ByteBuffer buildV2(byte fam) {
		var bb = ByteBuffer.Allocate(28);
		bb.Append(HaProxyHeader.v2sig, 0, HaProxyHeader.v2sig.length); // 裸写：WriteBytes 会先写长度前缀（序列化约定）
		bb.WriteByte(0x21); // version 2, command PROXY
		bb.WriteByte(fam);
		bb.WriteByte(0);
		bb.WriteByte(12); // length=12（按TCPv4口径声明，fam判定先于地址读取，长度仅用于帧边界）
		while (bb.size() < 28)
			bb.WriteByte(0);
		return bb;
	}
}
