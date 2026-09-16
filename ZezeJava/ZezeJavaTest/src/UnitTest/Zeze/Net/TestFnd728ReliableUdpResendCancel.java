package UnitTest.Zeze.Net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import Zeze.Net.ReliableUdp;
import Zeze.Net.ReliableUdpHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-28回归：send()在sendWindow.put之后才写packet.resendTimerTask，Ack路径（selector线程，
 * 不持会话锁）remove后裸读该字段——按并发集合的内存一致性语义，"put之后的写"对"remove之后的
 * 读"无可见性保证，可长期读到stale null：cancel抛NPE被doHandle按包catch吞掉，
 * scheduleWithFixedDelay周期任务永不停止，已确认的包每3秒重发且Packet被闭包终身持有。
 * 修复：赋值移到put之前（先写后发布）。
 * 说明：JMM可见性缺陷本身无法从外部确定性复现（x86强内存模型下观察不到stale读）；
 * 此测试钉住可观察契约——对端Ack之后重发必须停止（cancel路径必须在Ack处理中生效）。
 */
@Fast
public class TestFnd728ReliableUdpResendCancel {

	private static final class Collector implements ReliableUdpHandle {
		@Override
		public void handle(ReliableUdp.Session session, ReliableUdp.Packet packet) {
		}
	}

	// 用裸DatagramSocket扮演对端：收包→解码generation/serialId→手工回Ack→观察其后4秒
	// （>3秒重发周期）不得再收到任何数据报。若cancel丢失（如NPE被吞），重发任务会在
	// +3s向对端重发同一数据包，此断言失败。
	@Test
	public void testAckCancelsResendTimer() throws Exception {
		Task.tryInitThreadPool();
		var client = new ReliableUdp("127.0.0.1", 0, new Collector());
		try (var peer = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0))) {
			var session = client.open("127.0.0.1", peer.getLocalPort(), new Collector());
			session.send("fnd7-28".getBytes(), 0, 7);

			var buf = new byte[2048];
			peer.setSoTimeout(10_000);
			var dg = new DatagramPacket(buf, buf.length);
			peer.receive(dg); // 首个（唯一应发出的）数据报
			var bb = ByteBuffer.Wrap(buf, dg.getLength());
			Assertions.assertEquals(ReliableUdp.TypePacket, bb.ReadUInt());
			var packet = new ReliableUdp.Packet();
			packet.decode(bb);
			Assertions.assertEquals(7, packet.length);

			// 回Ack：generation为本端（acker）代际，任意非0（对端首学为Learn不重整）；
			// peerGeneration回显被确认包的代际，serialIds含其序号。
			var ack = new ReliableUdp.Control();
			ack.command = ReliableUdp.Control.Ack;
			ack.generation = 1;
			ack.peerGeneration = packet.generation;
			ack.serialIds.add(packet.serialId);
			var out = ByteBuffer.Allocate(512);
			ack.encode(out);
			peer.send(new DatagramPacket(out.Bytes, 0, out.size(),
					client.getLocalInetAddress()));

			// Ack处理取消重发定时器后，两个重发周期内不得再收到任何数据报。
			peer.setSoTimeout(4_000);
			try {
				peer.receive(dg);
				Assertions.fail("packet resent after ack: resend timer not cancelled");
			} catch (SocketTimeoutException expected) {
			}
		} finally {
			client.close();
		}
	}
}
