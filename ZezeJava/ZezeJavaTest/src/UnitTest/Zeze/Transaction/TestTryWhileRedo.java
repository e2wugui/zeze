package UnitTest.Zeze.Transaction;

import java.lang.reflect.Field;
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
 * tryWhileRedo 注册的动作在 perform 重试循环中"为下一次重试"执行；
 * 最后一次尝试（tryCount==255）失败后不再重试，此时不得执行动作，
 * 否则 Rpc.handle 注册的重加上下文无人移除，泄漏在 Service.rpcContexts 中。
 */
@Fast
public class TestTryWhileRedo {
	private static Application app;

	@BeforeAll
	public static void setUp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		app = new Application("TestTryWhileRedo", config);
		// perform 要求 getCheckpoint() 非 null；NoDatabase 轻量模式不创建，注入一个未 start 的。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
	}

	@Test
	public final void testRedoActionSkippedOnFinalFailure() {
		var calls = new AtomicInteger();
		var redoActions = new AtomicInteger();
		// NoDatabase 轻量模式不走 start()，直接用 Procedure 构造器（newProcedure 有 isStart 检查）。
		var result = new Procedure(app, (FuncLong)() -> {
			calls.incrementAndGet();
			Transaction.tryWhileRedo(redoActions::incrementAndGet);
			Transaction.getCurrent().throwRedo(0, "force redo");
			return Procedure.Success;
		}, "TestTryWhileRedo.FinalFailure", null).call();

		Assertions.assertEquals(Procedure.TooManyTry, result);
		Assertions.assertEquals(256, calls.get()); // 满 256 次尝试
		// 前 255 次失败各执行一次"为重试准备"；第 256 次失败后不再重试，不执行。
		Assertions.assertEquals(255, redoActions.get());
	}

	@Test
	public final void testRedoActionRunBeforeEachRetry() {
		var calls = new AtomicInteger();
		var redoActions = new AtomicInteger();
		var result = new Procedure(app, (FuncLong)() -> {
			if (calls.incrementAndGet() < 4) {
				Transaction.tryWhileRedo(redoActions::incrementAndGet);
				Transaction.getCurrent().throwRedo(0, "force redo");
			}
			return Procedure.Success;
		}, "TestTryWhileRedo.RetryThenSuccess", null).call();

		Assertions.assertEquals(Procedure.Success, result);
		Assertions.assertEquals(4, calls.get());
		Assertions.assertEquals(3, redoActions.get()); // 每次重试前都执行
	}
}
