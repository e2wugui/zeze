package UnitTest.Zeze.Net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.DatagramService;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ReplayAttackGrowRange;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.junit.Assert;
import org.junit.Test;

public class TestDatagram {
	@Test
	public void testSendDispatch() throws IOException, InterruptedException {
		Task.tryInitThreadPool(null, null, null);

		var service = new TestDatagramService();
		service.addFactoryHandle(ProtoValue.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProtoValue::new, this::processServerPValue, TransactionLevel.None, DispatchMode.Normal
				));

		byte[] securityKey = new byte[] {1, 2, 3, 4};
		// server
		var server = service.bind(new InetSocketAddress(4000));
		server.createSession(null, 1, securityKey, ReplayAttackPolicy.AllowDisorder);
		// client
		var session = service.createSession(
				new InetSocketAddress(0),
				new InetSocketAddress("127.0.0.1", 4000),
				1, securityKey, ReplayAttackPolicy.AllowDisorder);
		var p = new ProtoValue();
		p.Argument.setString3("hello");
		session.send(p);
		while (helloNumber.get() < 3)
			Thread.sleep(1);
	}

	AtomicLong helloNumber = new AtomicLong();

	private long processServerPValue(ProtoValue p) throws Exception {
		helloNumber.incrementAndGet();
		p.DatagramSession.send(p);
		System.out.println(p.Argument.getString3());
		return 0;
	}

	@Test
	public void testReplay() {
		var r = new ReplayAttackGrowRange(2);
		Assert.assertFalse(r.replay(1));
		System.out.println(r);

		Assert.assertFalse(r.replay(3));
		System.out.println(r);

		Assert.assertFalse(r.replay(2));
		System.out.println(r);

		Assert.assertTrue(r.replay(2));

		Assert.assertFalse(r.replay(4));
		Assert.assertFalse(r.replay(5));
		Assert.assertFalse(r.replay(6));
		Assert.assertFalse(r.replay(7));
		System.out.println(r);
		Assert.assertFalse(r.replay(8));
		System.out.println(r);
		Assert.assertFalse(r.replay(15));
		System.out.println(r);
		Assert.assertFalse(r.replay(16));
		System.out.println(r);
		Assert.assertFalse(r.replay(19));
		System.out.println(r);
	}
}

class TestDatagramService extends DatagramService {
	public TestDatagramService() {
		super("test.datagram.service");
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