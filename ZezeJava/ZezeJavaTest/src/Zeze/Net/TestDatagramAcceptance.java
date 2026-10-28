package Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ReplayAttackPolicy;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDatagramAcceptance {
	private static final InetSocketAddress ORIGINAL = new InetSocketAddress("127.0.0.1", 1);
	private static final InetSocketAddress REBOUND = new InetSocketAddress("127.0.0.1", 2);

	private static ByteBuffer packet(long token, long serial) {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong8s(token, serial);
		return bb;
	}

	@Test
	void invalidTailDoesNotCommitEarlierFrameOrReceiveState() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("datagram-validation");
		var dispatched = new AtomicInteger();
		service.AddFactoryHandle(ProtoValue.TypeId_, new Service.ProtocolFactoryHandle<>(ProtoValue::new,
				p -> { dispatched.incrementAndGet(); return 0; }, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			for (var policy : ReplayAttackPolicy.values()) {
				var session = socket.createSessionServer(ORIGINAL, null, policy);
				var invalid = packet(session.getTokenId(), 1_000_000);
				new ProtoValue().encodeWithHead(invalid);
				invalid.WriteByte(42); // 不完整的下一帧头
				session.onProcessDatagram(REBOUND, invalid);
				assertEquals(ORIGINAL, session.getRemoteAddress());
				assertEquals(0, dispatched.getAndSet(0));
				var valid = packet(session.getTokenId(), 1);
				new ProtoValue().encodeWithHead(valid);
				var duplicate = ByteBuffer.Wrap(valid.Copy());
				session.onProcessDatagram(REBOUND, valid);
				assertEquals(REBOUND, session.getRemoteAddress());
				assertEquals(1, dispatched.getAndSet(0));
				session.onProcessDatagram(ORIGINAL, duplicate);
				assertEquals(REBOUND, session.getRemoteAddress());
				assertEquals(0, dispatched.get());
				var newer = packet(session.getTokenId(), 3);
				new ProtoValue().encodeWithHead(newer);
				session.onProcessDatagram(REBOUND, newer);
				assertEquals(1, dispatched.getAndSet(0));
				var disorder = packet(session.getTokenId(), 2);
				new ProtoValue().encodeWithHead(disorder);
				session.onProcessDatagram(REBOUND, disorder);
				assertEquals(policy == ReplayAttackPolicy.AllowDisorder ? 1 : 0, dispatched.getAndSet(0));
			}
		} finally {
			service.stop();
		}
	}

	@Test
	void malformedProtocolBodyDoesNotConsumeTheSerial() throws Exception {
		Task.tryInitThreadPool();
		var service = new Service("datagram-body-validation");
		var dispatched = new AtomicInteger();
		service.AddFactoryHandle(ProtoValue.TypeId_, new Service.ProtocolFactoryHandle<>(ProtoValue::new,
				p -> { dispatched.incrementAndGet(); return 0; }, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = service.bindUdp(new InetSocketAddress(0));
			var session = socket.createSessionServer(ORIGINAL, null, ReplayAttackPolicy.IncreasingOnly);
			var invalid = packet(session.getTokenId(), 10);
			invalid.WriteInt4(0);
			invalid.WriteInt4(ProtoValue.ProtocolId_);
			invalid.WriteInt4(0); // 声明完整的空body，但已知协议至少需要family头
			session.onProcessDatagram(REBOUND, invalid);
			assertEquals(ORIGINAL, session.getRemoteAddress());
			var valid = packet(session.getTokenId(), 10);
			new ProtoValue().encodeWithHead(valid);
			session.onProcessDatagram(REBOUND, valid);
			assertEquals(1, dispatched.get());
		} finally {
			service.stop();
		}
	}
}
