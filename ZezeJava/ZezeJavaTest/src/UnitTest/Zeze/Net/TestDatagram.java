package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.AsyncSocket;
import Zeze.Net.DatagramSession;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ReplayAttackGrowRange;
import Zeze.Util.ReplayAttackGrowRange2;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestDatagram {
	@Test
	public void testSendDispatch() throws Exception {
		Task.tryInitThreadPool();

		var service = new TestDatagramService();
		service.AddFactoryHandle(ProtoValue.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProtoValue::new, this::processServerPValue, TransactionLevel.None, DispatchMode.Normal
		));

		byte[] securityKey = new byte[]{1, 2, 3, 4};
		// server
		var server = service.bindUdp(new InetSocketAddress(0));
		var client = service.bindUdp(new InetSocketAddress(0));
		var sessionServer = server.createSessionServer(
				new InetSocketAddress("127.0.0.1", client.getLocal().getPort()),
				securityKey, ReplayAttackPolicy.AllowDisorder);
		// client
		var session = client.createSession(
				new InetSocketAddress("127.0.0.1", server.getLocal().getPort()),
				sessionServer.getTokenId(), securityKey, ReplayAttackPolicy.AllowDisorder);
		assert session != null;
		var p = new ProtoValue();
		p.Argument.setString3("hello");
		session.Send(p);
		while (helloNumber.get() < 3) {
			//noinspection BusyWait
			Thread.sleep(1);
		}
		service.Stop();
	}

	private final AtomicLong helloNumber = new AtomicLong();

	private long processServerPValue(ProtoValue p) {
		if (helloNumber.incrementAndGet() < 3)
			p.getSender().Send(p);
		System.out.println(p.Argument.getString3());
		return 0;
	}

	// N-3缺陷: isClosed()语义颠倒——存活返回true、close()之后返回false,
	// 对照TcpSocket/Websocket全家族(closed != 0),所有按!isClosed()判断的调用方语义反转。
	@Test
	public void testIsClosed() throws Exception {
		Task.tryInitThreadPool();
		var service = new TestDatagramService();
		var server = service.bindUdp(new InetSocketAddress(0));
		var session = server.createSessionServer(
				new InetSocketAddress("127.0.0.1", server.getLocal().getPort()),
				null, ReplayAttackPolicy.AllowDisorder);
		Assertions.assertFalse(session.isClosed(), "alive session reported closed");
		session.close(null);
		Assertions.assertTrue(session.isClosed(), "closed session reported alive");
		service.Stop();
	}

	// P2缺口: DatagramSession.close()从不回调Service.OnSocketClose（对照TcpSocket/WebsocketClient家族），
	// 用户挂在OnSocketClose上的清理对UDP会话永不执行；且必须exactly-once，重入close不能双发回调。
	@Test
	public void testSessionCloseCallback() throws Exception {
		Task.tryInitThreadPool();
		var service = new TestDatagramOnCloseService();
		var server = service.bindUdp(new InetSocketAddress(0));
		var session = server.createSessionServer(
				new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
		Assertions.assertEquals(0, service.closedSessions.size());
		session.close(null);
		Assertions.assertEquals(1, service.closedSessions.size(), "OnSocketClose not fired on session close");
		Assertions.assertSame(session, service.closedSessions.get(0));
		session.close(null); // 重入：回调不能双发
		Assertions.assertEquals(1, service.closedSessions.size(), "OnSocketClose fired twice");
		Assertions.assertTrue(session.isClosed());
		service.Stop();
	}

	// P2缺口: DatagramSocket.close()只关channel不清tokens表，会话对象永久残留、
	// socket整体关闭的OnSocketClose级联缺失、isClosed恒false。
	@Test
	public void testSocketCloseCascade() throws Exception {
		Task.tryInitThreadPool();
		var service = new TestDatagramOnCloseService();
		var server = service.bindUdp(new InetSocketAddress(0));
		var s1 = server.createSessionServer(
				new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
		var s2 = server.createSessionServer(
				new InetSocketAddress("127.0.0.1", 1), null, ReplayAttackPolicy.AllowDisorder);
		server.close();
		Assertions.assertEquals(2, service.closedSessions.size(), "sessions not cascaded on socket close");
		Assertions.assertTrue(s1.isClosed() && s2.isClosed(), "sessions still alive after socket close");
		Assertions.assertFalse(server.containsSession(s1) || server.containsSession(s2), "tokens not cleared");
		server.close(); // 重入幂等
		Assertions.assertEquals(2, service.closedSessions.size());
		service.Stop();
	}

	@Test
	public void testReplay() {
		var r = new ReplayAttackGrowRange(2);
		Assertions.assertFalse(r.replay(1));
		Assertions.assertEquals("010 pos=1 max=1", r.toString());
		System.out.println(r);

		Assertions.assertFalse(r.replay(3));
		Assertions.assertEquals("01010 pos=3 max=3", r.toString());
		System.out.println(r);

		Assertions.assertFalse(r.replay(2));
		Assertions.assertEquals("01110 pos=3 max=3", r.toString());
		System.out.println(r);

		Assertions.assertTrue(r.replay(2));

		Assertions.assertFalse(r.replay(4));
		Assertions.assertFalse(r.replay(5));
		Assertions.assertFalse(r.replay(6));
		Assertions.assertFalse(r.replay(7));
		Assertions.assertEquals("011111110 pos=7 max=7", r.toString());
		System.out.println(r);
		Assertions.assertFalse(r.replay(8));
		Assertions.assertEquals("011111111 pos=8 max=8", r.toString());
		System.out.println(r);
		Assertions.assertFalse(r.replay(15));
		Assertions.assertEquals("0111111110000001 pos=15 max=15", r.toString());
		System.out.println(r);
		Assertions.assertFalse(r.replay(16));
		Assertions.assertEquals("1111111110000001 pos=0 max=16", r.toString());
		System.out.println(r);
		Assertions.assertFalse(r.replay(19));
		Assertions.assertEquals("1001111110000001 pos=3 max=19", r.toString());
		System.out.println(r);
		Assertions.assertFalse(r.replay(14));
		Assertions.assertEquals("1001111110000011 pos=3 max=19", r.toString());
		System.out.println(r);
		Assertions.assertTrue(r.replay(3));
	}

	@Test
	public void testReplay2() {
		var r = new ReplayAttackGrowRange2(16);
		Assertions.assertFalse(r.replay(1));
		System.out.println(r);
		Assertions.assertEquals("01 max=1 win=16 bits=[2]", r.toString());

		Assertions.assertFalse(r.replay(3));
		System.out.println(r);
		Assertions.assertEquals("0101 max=3 win=16 bits=[2]", r.toString());

		Assertions.assertFalse(r.replay(2));
		System.out.println(r);
		Assertions.assertEquals("0111 max=3 win=16 bits=[2]", r.toString());

		Assertions.assertTrue(r.replay(2));

		Assertions.assertFalse(r.replay(4));
		Assertions.assertFalse(r.replay(5));
		Assertions.assertFalse(r.replay(6));
		Assertions.assertFalse(r.replay(7));
		System.out.println(r);
		Assertions.assertEquals("01111111 max=7 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(8));
		System.out.println(r);
		Assertions.assertEquals("011111111 max=8 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(15));
		System.out.println(r);
		Assertions.assertEquals("0111111110000001 max=15 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(16));
		System.out.println(r);
		Assertions.assertEquals("1111111100000011 max=16 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(19));
		System.out.println(r);
		Assertions.assertEquals("1111100000011001 max=19 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(20));
		System.out.println(r);
		Assertions.assertEquals("1111000000110011 max=20 win=16 bits=[2]", r.toString());
		Assertions.assertFalse(r.replay(35));
		System.out.println(r);
		Assertions.assertEquals("1000000000000001 max=35 win=16 bits=[2]", r.toString());
		Assertions.assertTrue(r.replay(18));
		Assertions.assertTrue(r.replay(20));
		Assertions.assertFalse(r.replay(100));
		System.out.println(r);
		Assertions.assertEquals("0000000000000001 max=100 win=16 bits=[2]", r.toString());
	}
}

class TestDatagramService extends Service {
	public TestDatagramService() {
		super("test.datagram.service");
	}
}

class TestDatagramOnCloseService extends Service {
	public final java.util.List<DatagramSession> closedSessions = new java.util.concurrent.CopyOnWriteArrayList<>();

	public TestDatagramOnCloseService() {
		super("test.datagram.onclose.service");
	}

	@Override
	public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
		if (so instanceof DatagramSession ds)
			closedSessions.add(ds);
		super.OnSocketClose(so, e);
	}
}

class ProtoValue extends Protocol<BValue> {
	public static final int ProtocolId_ = Bean.hash32(ProtoValue.class.getName()); // 1138220698
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1138220698

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public ProtoValue() {
		Argument = new BValue();
	}
}
