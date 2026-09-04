package UnitTest.Zeze.Services;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import harness.Fast;

/**
 * FND2-S2-2：Id128UdpServer.process 入口校验对端可控的 count 与 name。
 * <p>
 * 修复前 count 无校验：count&lt;0 使号段回退（current倒退）→ 同name空间向两个客户端发放
 * 重复tid；count=0 同一startId发给两个客户端；巨量count每唯一name写盘巨量预留。
 * 唯一name无界则撑爆cache/RocksDB。
 * <p>
 * 用真实loopback Id128UdpServer（table=null，纯内存）按协议收发：
 * 非法报文被run()内层catch记日志丢弃整包（无应答，客户端soTimeout）；
 * 诚实报文照常工作，号段不因攻击报文回退；边界值（name恰好128字节、count==1）仍合法。
 * 伪造非法count的rpc直接走process——无需TCP/SM环境。
 */
@Fast
public class TestId128UdpServerValidation {
	private static final String NAME = "UnitTest.FND2_S2_2.Validate";
	private static Id128UdpServer server;
	private static DatagramSocket client;
	private static InetSocketAddress serverAddress;

	@BeforeAll
	public static void setUp() throws Exception {
		server = new Id128UdpServer(); // table=null：纯内存发号，自包含。
		server.start();
		client = new DatagramSocket();
		client.setSoTimeout(1500);
		serverAddress = new InetSocketAddress("127.0.0.1", server.getLocalPort());
	}

	@AfterAll
	public static void tearDown() throws Exception {
		client.close();
		server.stop();
	}

	/** 发一个请求并等待应答（每个报文恰好一个rpc，应答也恰好一个）。 */
	private static AllocateId128 alloc(String name, int count) throws Exception {
		var r = new AllocateId128();
		r.Argument.setName(name);
		r.Argument.setCount(count);
		var bb = ByteBuffer.Allocate();
		r.encode(bb);
		client.send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), serverAddress));
		return receiveOne();
	}

	/** 发一个非法请求：应被入口校验拒绝（整包丢弃，无应答）。 */
	private static void allocExpectRejected(String name, int count) throws Exception {
		var r = new AllocateId128();
		r.Argument.setName(name);
		r.Argument.setCount(count);
		var bb = ByteBuffer.Allocate();
		r.encode(bb);
		client.send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), serverAddress));
		Assertions.assertThrows(SocketTimeoutException.class, TestId128UdpServerValidation::receiveOne,
				"invalid request must be dropped without response: count=" + count);
	}

	private static AllocateId128 receiveOne() throws Exception {
		var buf = new byte[2048];
		var p = new DatagramPacket(buf, buf.length);
		client.receive(p);
		var r = new AllocateId128();
		r.decode(ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength()));
		return r;
	}

	@Test
	public void testIllegalCountRejectedAndNoRegression() throws Exception {
		var r1 = alloc(NAME, 100);
		Assertions.assertEquals(100, r1.Result.getCount());
		var start1 = r1.Result.getStartId();

		// count<0：修复前会使号段回退→下一个诚实客户端拿到与r1重叠的号段（重复tid）。
		allocExpectRejected(NAME, -100);
		allocExpectRejected(NAME, 0);
		// count巨大：每唯一name写盘巨量预留。
		allocExpectRejected(NAME, Tid128Cache.ALLOCATE_COUNT_MAX + 1);
		allocExpectRejected(NAME, Integer.MAX_VALUE);

		// 攻击报文全部被丢弃后，同name的下一次分配严格顺延r1的号段（无回退、无重复）。
		var r2 = alloc(NAME, 10);
		Assertions.assertEquals(10, r2.Result.getCount());
		Assertions.assertEquals(start1.add(100), r2.Result.getStartId());
	}

	@Test
	public void testNameLengthLimit() throws Exception {
		// 超长name：无界唯一name撑cache/RocksDB，拒绝。
		allocExpectRejected("n".repeat(129), 1);
		allocExpectRejected("x".repeat(256), 1);
		// 边界：恰好128字节仍合法（应答到达即通过，无soTimeout抛出）。
		var r = alloc("b".repeat(128), 1);
		Assertions.assertEquals(1, r.Result.getCount());
	}

	@Test
	public void testBadRpcDroppedGoodRpcInSamePacketAnswered() throws Exception {
		// 一个报文内[好,坏]：好的先处理并应答，坏的触发校验异常丢弃，worker存活。
		var good = new AllocateId128();
		good.Argument.setName(NAME);
		good.Argument.setCount(1);
		var bad = new AllocateId128();
		bad.Argument.setName(NAME);
		bad.Argument.setCount(0);
		var bb = ByteBuffer.Allocate();
		good.encode(bb);
		bad.encode(bb);
		client.send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), serverAddress));

		var resp = receiveOne(); // 只有good的应答。
		Assertions.assertEquals(1, resp.Result.getCount());
		Assertions.assertThrows(SocketTimeoutException.class, TestId128UdpServerValidation::receiveOne,
				"bad rpc in the same packet must be dropped without response");

		// worker未被坏报文杀掉：后续正常分配照常工作。
		var r = alloc(NAME, 1);
		Assertions.assertEquals(1, r.Result.getCount());
	}
}
