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
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.junit.Test;

public class TestDatagram {
	@Test
	public void testSendDispatch() throws IOException, InterruptedException {
		Task.tryInitThreadPool(null, null, null);

		var service = new TestDatagramService();
		service.addFactoryHandle(ProtoValue.TypeId_, new Service.ProtocolFactoryHandle<>(
				ProtoValue::new, this::processServerPValue, TransactionLevel.None, DispatchMode.Normal
				));

		// server
		service.bind(new InetSocketAddress(4000));
		// client
		var session = service.openSession(new InetSocketAddress(0), new InetSocketAddress("127.0.0.1", 4000), 1);
		var p = new ProtoValue();
		p.Argument.setString3("hello");
		session.send(p);
		while (helloNumber.get() < 3)
			Thread.sleep(1);
	}

	AtomicLong helloNumber = new AtomicLong();

	private long processServerPValue(ProtoValue p) throws Exception {
		helloNumber.incrementAndGet();
		p.DatagramSession.send(p.Remote, p);
		System.out.println(p.Argument.getString3());
		return 0;
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