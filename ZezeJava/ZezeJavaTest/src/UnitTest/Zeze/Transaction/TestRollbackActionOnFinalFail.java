package UnitTest.Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;

/**
 * FND-T1-1 回归：perform 的 Abort / ErrorSavepoint / TooManyTry 终局回滚必须触发 whileRollback 回调。
 * 修复前这些路径调用单参 finalRollback（executeRollbackAction=false），回调被静默丢弃——
 * Timer 恢复链的节点推进、HotManager 热更回滚、Online 回滚通知全部落空。
 * TooManyTry 更进一步：每轮 redo 的 reuseTransactionForRedo 已清空回调，修复以
 * redoRollbackActions 暂存最近一轮的回调，终局失败时补触发（重做成功的轮次自然作废）。
 */
@Fast
public class TestRollbackActionOnFinalFail {
	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestRollbackActionOnFinalFail", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
	}

	@Test
	public final void testRollbackActionOnAbort() {
		var rollbacks = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
			Transaction.getCurrent().throwAbort("force abort after rollback action registered", null);
			return Procedure.Success; // 不可达：throwAbort 必抛
		}, "TestRollbackActionOnFinalFail.Abort", null).call();

		Assertions.assertEquals(Procedure.AbortException, result);
		// Abort 终局回滚也要触发回调（对齐错误码路径 finalRollback(procedure, true)）
		Assertions.assertEquals(1, rollbacks.get());
	}

	@Test
	public final void testRollbackActionOnErrorSavepoint() {
		var rollbacks = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
			Transaction.getCurrent().begin(); // Begin/End 不配对：错误码路径留下残留 savepoint
			return 100L;
		}, "TestRollbackActionOnFinalFail.ErrorSavepoint", null).call();

		Assertions.assertEquals(Procedure.ErrorSavepoint, result);
		// 残留 savepoint 中的回调并入事务级 actions 统一触发
		Assertions.assertEquals(1, rollbacks.get());
	}

	@Test
	public final void testRollbackActionOnTooManyTry() {
		var rollbacks = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
			Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success; // 不可达：throwRedo 必抛
		}, "TestRollbackActionOnFinalFail.TooManyTry", null).call();

		Assertions.assertEquals(Procedure.TooManyTry, result);
		// 每轮重做会重新注册回调（作废轮次的不执行），只有最近一轮的回调在终局回滚时触发一次
		Assertions.assertEquals(1, rollbacks.get());
	}

	@Test
	public final void testRollbackActionDroppedWhenRedoEndsInSuccess() {
		var rollbacks = new AtomicInteger();
		var commits = new AtomicInteger();
		var calls = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			calls.incrementAndGet();
			Transaction.getCurrent().runWhileRollback(rollbacks::incrementAndGet);
			Transaction.getCurrent().runWhileCommit(commits::incrementAndGet);
			if (calls.get() < 3)
				Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		}, "TestRollbackActionOnFinalFail.RedoThenSuccess", null).call();

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(3, calls.get());
		// 作废轮次与成功轮次注册的 rollback 回调都不执行；成功轮次的 commit 回调执行一次
		Assertions.assertEquals(0, rollbacks.get());
		Assertions.assertEquals(1, commits.get());
	}
}
