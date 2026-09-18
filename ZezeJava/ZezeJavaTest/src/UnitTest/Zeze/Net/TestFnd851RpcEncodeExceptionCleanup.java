package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.TimeThrottle;
import demo.Module1.BValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-51回归：Rpc.Send/SendReturnVoid在addRpcContext之后、schedule之前，编码/传输异常
 * 直接逃逸——上下文既不清理也无超时兜底（超时定时器从未注册），rpcContexts条目永久泄漏、
 * responseHandle回调永不触发，违背"上下文必须有超时兜底"的设计契约与SendReturnVoid自身
 * javadoc。孪生：SendReturnVoid同型。
 * 修复后：schedule前移到发送之前（对齐Online.sendOnlineRpc先例）——异常路径同样有
 * 超时回收与Timeout回调；false路径双参remove与超时定时器互斥恰好一次。
 */
@Fast
public class TestFnd851RpcEncodeExceptionCleanup {

	/** encode必抛的Argument bean：制造addRpcContext之后、super.Send内部的确定性异常源。 */
	public static class ThrowingBean implements Serializable {
		protected boolean throwOnEncode() {
			return true;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			if (throwOnEncode())
				throw new IllegalStateException("boom-encode-fnd851");
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
		}
	}

	public static class EncodeFailRpc extends Rpc<ThrowingBean, BValue> {
		public static final long TypeId_ = Protocol.makeTypeId(94, 51);

		public EncodeFailRpc() {
			Argument = new ThrowingBean();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 94;
		}

		@Override
		public int getProtocolId() {
			return 51;
		}
	}

	/** 直发即成功返回true的桩socket：encode在AsyncSocket.Send(p)内先抛，不触达传输。 */
	private static final class StubSocket extends AsyncSocket {
		StubSocket(Service service) {
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
			return true;
		}
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	private static final class HandleResult {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicLong code = new AtomicLong(Long.MIN_VALUE);
	}

	// Send：编码异常逃逸后上下文必须有超时兜底——Timeout回调触发且上下文回收
	@Test
	public void testSendEncodeExceptionHasTimeoutFallback() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("test.fnd851.a");
		var handleResult = new HandleResult();
		service.AddFactoryHandle(EncodeFailRpc.TypeId_, new Service.ProtocolFactoryHandle<>(EncodeFailRpc::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var stub = new StubSocket(service);
			var rpc = new EncodeFailRpc();
			Assertions.assertThrows(IllegalStateException.class,
					() -> rpc.Send(stub, r -> {
						handleResult.code.set(r.getResultCode());
						handleResult.latch.countDown();
						return 0;
					}, 200), "编码异常必须照常同步抛给直接调用方");

			// 修复前：schedule不可达——上下文永驻、回调永不触发；修复后：200ms超时兜底
			Assertions.assertTrue(handleResult.latch.await(10, TimeUnit.SECONDS),
					"编码异常路径的超时兜底必须派发responseHandle回调");
			Assertions.assertEquals(Procedure.Timeout, handleResult.code.get(), "晚到通知为Timeout码");
			await("context cleaned by timeout", 10_000, () -> service.getRpcContextsToSender(stub).isEmpty());
		} finally {
			service.Stop();
		}
	}

	// 孪生：SendReturnVoid同型——异常路径必须兑现"总是建立RpcContext……Timeout后回调"
	@Test
	public void testSendReturnVoidEncodeExceptionHasTimeoutFallback() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("test.fnd851.b");
		var handleResult = new HandleResult();
		service.AddFactoryHandle(EncodeFailRpc.TypeId_, new Service.ProtocolFactoryHandle<>(EncodeFailRpc::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var stub = new StubSocket(service);
			var rpc = new EncodeFailRpc();
			Assertions.assertThrows(IllegalStateException.class,
					() -> rpc.SendReturnVoid(service, stub, r -> {
						handleResult.code.set(r.getResultCode());
						handleResult.latch.countDown();
						return 0;
					}, 200), "编码异常必须照常同步抛给直接调用方");

			Assertions.assertTrue(handleResult.latch.await(10, TimeUnit.SECONDS),
					"SendReturnVoid异常路径必须按javadoc在Timeout后回调");
			Assertions.assertEquals(Procedure.Timeout, handleResult.code.get());
			await("context cleaned by timeout", 10_000, () -> service.getRpcContextsToSender(stub).isEmpty());
		} finally {
			service.Stop();
		}
	}

	// 回归红线：Send返回false的正常失败路径行为不变——上下文即时清理，超时定时器到期空转不双发
	@Test
	public void testSendFalsePathStillCleansContext() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var service = new Service("test.fnd851.c");
		var callbacks = new CountDownLatch(1);
		service.AddFactoryHandle(EncodeFailRpc.TypeId_, new Service.ProtocolFactoryHandle<>(EncodeFailRpc::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var stub = new AsyncSocket(service) {
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
					return false; // 传输失败：正常false路径
				}
			};
			var rpc = new EncodeFailRpc();
			rpc.Argument = new ThrowingBean() {
				@Override
				protected boolean throwOnEncode() {
					return false; // 本用例编码成功、传输层返回false
				}
			};
			Assertions.assertFalse(rpc.Send(stub, r -> {
				callbacks.countDown();
				return 0;
			}, 200), "false路径返回值不变");
			Assertions.assertTrue(service.getRpcContextsToSender(stub).isEmpty(), "false路径上下文即时清理");
			// 超时定时器到期后双参remove失败跳过——回调不得派发（恰好一次语义）
			//noinspection BusyWait
			Thread.sleep(600);
			Assertions.assertEquals(1, callbacks.getCount(), "超时定时器不得在上下文已清理后派发回调");
		} finally {
			service.Stop();
		}
	}
}
