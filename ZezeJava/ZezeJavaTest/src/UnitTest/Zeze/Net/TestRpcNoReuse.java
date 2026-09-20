package UnitTest.Zeze.Net;

import java.net.SocketAddress;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.TimeThrottle;
import demo.Module1.BValue;

/**
 * Rpc 实例一次性契约（FND6-11 处置转向：由「守卫收窄同实例重发竞态」改为「契约禁止重发」）：
 * Send/SendForWait 只能进入一次，发送失败也不解禁——重试请新建实例
 * （范式参考 Raft 的 RaftRpcBridge：每次发送新建桥接，原始实例永不入 rpcContexts）。
 * 三个用例：二次 Send 抛、二次 SendForWait 抛、
 * 发送失败（Send 返回 false、上下文已清理）后再发同样抛——锁定「失败也不重试」决策，
 * 防止将来有人手软加回「失败解禁」复位。
 */
@Fast
public class TestRpcNoReuse {

	public static class TestRpc extends Rpc<BValue, BValue> {
		public TestRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 1;
		}

		@Override
		public int getProtocolId() {
			return -3;
		}
	}

	/** 假socket：sendOk 控制发送成败，走通 Protocol.Send(so)→so.Send(this) 全路径（不实际发网络）。 */
	private static class FakeSocket extends AsyncSocket {
		private final boolean sendOk;

		FakeSocket(Service service) {
			this(service, true);
		}

		FakeSocket(Service service, boolean sendOk) {
			super(service);
			this.sendOk = sendOk;
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
	}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return sendOk;
		}
	}

	@Test
	public final void testSecondSendThrows() {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("TestRpcNoReuse.Send");
		var so = new FakeSocket(service);
		var rpc = new TestRpc();

		Assertions.assertTrue(rpc.Send(so, r -> Procedure.Success, 60_000), "first send");
		Assertions.assertNotEquals(0, rpc.getSessionId());

		Assertions.assertThrows(IllegalStateException.class,
				() -> rpc.Send(so, r -> Procedure.Success, 60_000), "second send must throw");
	}

	@Test
	public final void testSecondSendForWaitThrows() {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("TestRpcNoReuse.SendForWait");
		var so = new FakeSocket(service);
		var rpc = new TestRpc();

		var future = rpc.SendForWait(so, 60_000);
		Assertions.assertFalse(future.isDone());
		Assertions.assertNotEquals(0, rpc.getSessionId());

		Assertions.assertThrows(IllegalStateException.class,
				() -> rpc.SendForWait(so, 60_000), "second SendForWait must throw");
	}

	@Test
	public final void testRetryAfterSendFailAlsoThrows() {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("TestRpcNoReuse.SendFail");
		var so = new FakeSocket(service, false); // 发送恒失败
		var rpc = new TestRpc();

		Assertions.assertFalse(rpc.Send(so, r -> Procedure.Success, 60_000), "send fail");
		// 失败路径已清理上下文（无泄漏），但实例不解禁：再发必须抛。
		Assertions.assertEquals(0, service.getRpcContexts(p -> p == rpc).size());
		Assertions.assertThrows(IllegalStateException.class,
				() -> rpc.Send(so, r -> Procedure.Success, 60_000), "retry after send fail must throw");
	}
}
