package UnitTest.Zeze.Trans;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Transaction;
import Zeze.Util.TaskSpec;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-02 回归：finalCommit/finalRollback 对 logActions 无逐项异常隔离。
 * 任一 logAction（如用户替换的 Procedure.logAction 注册的异常记录动作）抛错
 * 会吞掉后续 logActions、ChangeListener 通知与全部 whileCommit/whileRollback 回调
 * （典型是 Rpc 应答，丢失即静默丢语义）。修复后逐项 try-catch（与 triggerActions 同型）。
 */
@Fast
public class TestFnd702FinalActionsIsolation {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（T1组：120起）。
	private static final AtomicInteger nextServerId = new AtomicInteger(120);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("t1_fnd702_finalactions_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFnd702FinalActionsIsolation@" + conf.getServerId(), conf);
	}

	@Test
	public void testThrowingLogActionNotSwallowCommitCallbacks() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			var ran = new AtomicInteger();
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var t = Transaction.getCurrent();
				// 第一个logAction抛错，第二个必须仍执行。
				t.addLogAction(() -> {
					throw new RuntimeException("logAction boom-commit");
				});
				t.addLogAction(ran::incrementAndGet);
				Transaction.whileCommit(ran::incrementAndGet);
				table.put(1L, new demo.Bean1());
				return 0L;
			}, "TestFnd702.commit")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(2, ran.get(), "抛错的logAction不得吞掉后续logAction与whileCommit回调");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testThrowingLogActionNotSwallowRollbackCallbacks() throws Exception {
		var app = newApp();
		var table = new tMemorySize();
		app.addTable("", table);
		app.start();
		try {
			var ran = new AtomicInteger();
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var t = Transaction.getCurrent();
				t.addLogAction(() -> {
					throw new RuntimeException("logAction boom-rollback");
				});
				t.addLogAction(ran::incrementAndGet);
				Transaction.whileRollback(ran::incrementAndGet);
				table.put(2L, new demo.Bean1());
				return 1L; // 非0：rollback → finalRollback
			}, "TestFnd702.rollback")).call();
			Assertions.assertEquals(1L, rc);
			Assertions.assertEquals(2, ran.get(), "抛错的logAction不得吞掉后续logAction与whileRollback回调");
		} finally {
			app.stop();
		}
	}
}
