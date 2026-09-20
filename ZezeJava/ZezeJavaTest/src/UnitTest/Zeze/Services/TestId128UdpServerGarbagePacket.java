package UnitTest.Zeze.Services;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpServer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SM1-F2 回归：畸形UDP报文的解码异常（Rpc.decode对纯垃圾抛IllegalStateException，非
 * IllegalArgumentException）原落入全栈error按包记录——FND6-28日志洪水防护在最易构造的
 * 攻击向量上失效。
 * 修复：rpc.decode单独包一层catch(RuntimeException)→限频单行并丢弃剩余报文；worker
 * 不死、后续合法请求仍被正常服务（同报文中先于垃圾帧处理的合法帧仍应答）。
 */
@Fast
public class TestId128UdpServerGarbagePacket {
	@Test
	public void testGarbagePacketsNeitherKillWorkerNorBreakService() throws Exception {
		var server = new Id128UdpServer();
		server.start();
		try (var udp = new DatagramSocket()) {
			udp.setSoTimeout(5000);
			var addr = new InetSocketAddress("127.0.0.1", server.getLocalPort());
			// 0xFF填充：首字节familyclass校验必失败（IllegalStateException），确定性触发解码拒绝路径
			var garbage = new byte[64];
			Arrays.fill(garbage, (byte)0xff);
			for (int i = 0; i < 100; i++)
				udp.send(new DatagramPacket(garbage, garbage.length, addr));

			// 垃圾风暴后合法请求仍被服务：worker存活且decode隔离不影响后续报文处理
			var r = new AllocateId128();
			r.Argument.setName("wt5-garbage-test");
			r.Argument.setCount(100);
			var bbr = ByteBuffer.Allocate();
			r.encode(bbr);
			udp.send(new DatagramPacket(bbr.Bytes, bbr.ReadIndex, bbr.size(), addr));

			var buf = new byte[2048];
			var p = new DatagramPacket(buf, buf.length);
			udp.receive(p); // worker死亡或合法请求被吞则5s超时失败
			var bb = ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength());
			var rr = new AllocateId128();
			rr.decode(bb);
			Assertions.assertEquals(100, rr.Result.getCount(), "垃圾风暴后合法分配请求必须仍被服务");
			Assertions.assertNotNull(rr.Result.getStartId());
		} finally {
			server.stop();
		}
	}
}
