package UnitTest.Zeze.Net;

import java.net.SocketAddress;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Procedure;
import Zeze.Util.Reflect;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TimeThrottle;
import demo.Module1.BValue;

/**
 * FND2-N1-3 回归：Rpc 同实例重发必须移除旧上下文条目。
 * 原实现在 Send 三参与 SendReturnVoid 入口处保留了被注释的 remove（"sessionId还没生成，
 * 没法remove"）——实际上重发入口时旧 sessionId 仍在字段里。旧条目残留导致旧超时定时器
 * 触发时按旧 id 移除到本实例，把新请求的 future/isTimeout/resultCode 错误置为超时，
 * 新应答到达时 future 已异常完成无法生效（假超时+应答丢失）。
 * 修复为：入口先取 old sessionId，新上下文注册后 removeRpcContext(old, this)，
 * 旧定时器此后只能移除到 null 直接返回。
 * 两个测试（Send 走假socket、SendReturnVoid 走 so=null）均同实例连发两次：
 * 第一次短超时(200ms)、第二次立即重发长超时；等待旧定时器触发后断言未被 clobber。
 * 修复前：isTimeout=true、resultCode=Timeout、future 被置 RpcTimeoutException（红）。
 */
@Fast
public class TestRpcResendRemoveOldContext {

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

	/** 假socket：Send 恒成功，走通 Protocol.Send(so)→so.Send(this) 全路径（不实际发网络）。 */
	private static final class FakeSocket extends AsyncSocket {
		FakeSocket(Service service) {
			super(service);
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
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true; // 假装发送成功
		}
	}

	@Test
	public final void testResendSendNotClobberedByOldTimer() throws Exception {
		Assumptions.assumeFalse(Reflect.inDebugMode, "debug 模式下 Rpc.schedule 将超时放宽10分钟，等待断言无意义");
		var service = new Service("TestRpcResendRemoveOldContext.Send");
		var so = new FakeSocket(service);
		var rpc = new TestRpc();
		// 模拟 SendForWait 重发场景：重发前挂上新future，旧定时器错误触发时会把它置RpcTimeoutException
		var future = new TaskCompletionSource<BValue>();
		rpc.setFuture(future);

		Assertions.assertTrue(rpc.Send(so, r -> Procedure.Success, 200), "first send");
		var firstSessionId = rpc.getSessionId();
		Assertions.assertTrue(rpc.Send(so, r -> Procedure.Success, 60_000), "resend"); // 立即同实例重发
		Assertions.assertNotEquals(firstSessionId, rpc.getSessionId());
		// 修复点：重发即移除旧上下文条目，表中只剩新上下文
		Assertions.assertEquals(1, service.getRpcContexts(p -> p == rpc).size());

		Thread.sleep(1000); // 旧定时器(200ms)已触发

		// 旧定时器只能移除到null：不得置超时/错误码/异常future
		Assertions.assertFalse(rpc.isTimeout(), "old timer must not clobber new request");
		Assertions.assertEquals(0, rpc.getResultCode());
		Assertions.assertFalse(future.isDone(), "old timer must not complete new future");
		// 新上下文仍完好，等待应答或新超时(60s)
		Assertions.assertEquals(1, service.getRpcContexts(p -> p == rpc).size());
	}

	@Test
	public final void testResendSendReturnVoidNotClobberedByOldTimer() throws Exception {
		Assumptions.assumeFalse(Reflect.inDebugMode, "debug 模式下 Rpc.schedule 将超时放宽10分钟，等待断言无意义");
		var service = new Service("TestRpcResendRemoveOldContext.SendReturnVoid");
		var rpc = new TestRpc();

		// so=null：SendReturnVoid 只注册上下文+超时任务（Protocol.Send(null) 返回 false，不实际发送）
		rpc.SendReturnVoid(service, null, r -> Procedure.Success, 200);
		var firstSessionId = rpc.getSessionId();
		rpc.SendReturnVoid(service, null, r -> Procedure.Success, 60_000); // 立即同实例重发
		Assertions.assertNotEquals(firstSessionId, rpc.getSessionId());
		Assertions.assertEquals(1, service.getRpcContexts(p -> p == rpc).size());

		Thread.sleep(1000); // 旧定时器(200ms)已触发

		Assertions.assertFalse(rpc.isTimeout(), "old timer must not clobber new request");
		Assertions.assertEquals(0, rpc.getResultCode());
		Assertions.assertEquals(1, service.getRpcContexts(p -> p == rpc).size());
	}
}
