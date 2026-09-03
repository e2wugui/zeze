package UnitTest.Zeze.Net;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.GoBackZeze;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.Reflect;
import demo.Module1.BValue;

/**
 * FND-N1-1 回归：Rpc/manualContext 的超时清理必须与注册的即时性对称。
 * 注册（addRpcContext / manualContexts.putIfAbsent）不随事务回滚撤销，但原实现的清理用事务感知
 * schedule（回滚即丢弃注册）→ 事务内发送后回滚时超时任务永不注册，应答丢失则上下文永驻、
 * SendForWait 永久挂起。修复为 scheduleNow（不等提交立即注册）后，回滚不影响超时兜底。
 * 两个测试均在事务内注册上下文（模拟 dispatchProtocol 把 handler 包进过程）后强制 Abort：
 * 修复前超时后条目仍在（红），修复后被超时任务清理并回调（绿）。
 */
@Fast
public class TestRpcTimeoutScheduleOnRollback {
	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestRpcTimeoutScheduleOnRollback", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
	}

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
			return -2;
		}
	}

	private static void callInProcedureAndAbort(FuncLong action) {
		// throwAbort 置事务 state=Abort，perform 返回 Procedure.AbortException 错误码，
		// call() 以非零返回码结束（不抛出）——过程已回滚正是要构造的终局。
		var rc = new Procedure(app, action, "TestRpcTimeoutScheduleOnRollback.Rollback", null).call();
		Assertions.assertNotEquals(Procedure.Success, rc, "procedure should abort");
	}

	private static void awaitUntil(long timeoutMs, BooleanSupplier stillPresent) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (stillPresent.getAsBoolean() && System.currentTimeMillis() < deadline)
			Thread.sleep(20);
	}

	@Test
	public final void testManualContextTimeoutSurvivesRollback() throws Exception {
		var service = new Service("TestRpcTimeoutScheduleOnRollback.Manual");
		var removed = new AtomicReference<Service.ManualContext>();
		var removedByTimeout = new AtomicBoolean();
		var context = new Service.ManualContext() {
			@Override
			public void onRemoved() {
				removed.set(this);
				removedByTimeout.set(isTimeout());
			}
		};
		var sessionId = new long[1];
		callInProcedureAndAbort(() -> {
			sessionId[0] = service.addManualContextWithTimeout(context, 200);
			Transaction.getCurrent().throwAbort("force abort after manual context registered", null);
			return Procedure.Success; // 不可达：throwAbort 必抛
		});
		// 注册立即生效，不随回滚撤销
		Assertions.assertNotNull(service.tryGetManualContext(sessionId[0]));
		// 修复点：超时清理的注册不得随回滚丢弃——超时后条目被移除、onRemoved(isTimeout=true) 被回调
		awaitUntil(5000, () -> service.tryGetManualContext(sessionId[0]) != null);
		Assertions.assertNull(service.tryGetManualContext(sessionId[0]));
		Assertions.assertSame(context, removed.get());
		Assertions.assertTrue(removedByTimeout.get());
	}

	@Test
	public final void testRpcContextTimeoutSurvivesRollback() throws Exception {
		Assumptions.assumeFalse(Reflect.inDebugMode, "debug 模式下 Rpc.schedule 将超时放宽10分钟，等待断言无意义");
		var service = new Service("TestRpcTimeoutScheduleOnRollback.Rpc");
		var rpc = new TestRpc();
		var sessionId = new long[1];
		callInProcedureAndAbort(() -> {
			// so=null：SendReturnVoid 只注册上下文+超时任务（Protocol.Send(null) 返回 false，不实际发送）
			rpc.SendReturnVoid(service, null, r -> Procedure.Success, 200);
			sessionId[0] = rpc.getSessionId();
			Transaction.getCurrent().throwAbort("force abort after rpc context registered", null);
			return Procedure.Success; // 不可达：throwAbort 必抛
		});
		// 注册立即生效，不随回滚撤销（Protocol 基类无 sessionId，用恒等匹配定位注册的 rpc 上下文）
		Assertions.assertFalse(service.getRpcContexts(p -> p == rpc).isEmpty());
		// 修复点：回滚不丢弃超时兜底——超时后条目被超时任务消费移除并标记 isTimeout
		awaitUntil(5000, () -> !service.getRpcContexts(p -> p == rpc).isEmpty());
		Assertions.assertTrue(service.getRpcContexts(p -> p == rpc).isEmpty());
		Assertions.assertTrue(rpc.isTimeout());
	}
}
