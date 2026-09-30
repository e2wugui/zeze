package Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Application;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import demo.Bean1;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/** 同步日志回调失败不能再次回滚已结束的子过程保存点。 */
@Fast
@Isolated
public class TestProcedureLogActionFailureIsolation {
	@Test
	public void testThrowingChildLogActionPreservesParentCommitAndChildResult() throws Exception {
		var conf = TakeoverTestEnv.newConf("off", 60_000, 60_000);
		var app = new Application("TestProcedureLogActionFailureIsolation@" + conf.getServerId(), conf);
		var table = new tMemorySize();
		app.addTable("", table);
		var originalLogAction = Procedure.logAction;
		try {
			app.start();
			var parentCommits = new AtomicInteger();
			var parentRollbacks = new AtomicInteger();
			var childCommits = new AtomicInteger();
			var childRollbacks = new AtomicInteger();
			var logCalls = new AtomicInteger();
			var childResult = new AtomicLong(Procedure.Unknown);
			var childName = "TestProcedureLogActionFailureIsolation.child";
			Procedure.logAction = (ex, result, procedure, message) -> {
				if (result == Procedure.LogicError && childName.equals(procedure.getActionName())) {
					logCalls.incrementAndGet();
					throw new IllegalStateException("expected child log failure");
				}
			};

			var parentResult = app.newProcedure(() -> {
				Transaction.whileCommit(parentCommits::incrementAndGet);
				Transaction.whileRollback(parentRollbacks::incrementAndGet);
				var parentValue = new Bean1();
				parentValue.setV1(42);
				table.put(1L, parentValue);
				childResult.set(app.newProcedure(() -> {
					Transaction.whileCommit(childCommits::incrementAndGet);
					Transaction.whileRollback(childRollbacks::incrementAndGet);
					var childValue = new Bean1();
					childValue.setV1(99);
					table.put(1L, childValue);
					return Procedure.LogicError;
				}, childName).call());
				return Procedure.Success;
			}, "TestProcedureLogActionFailureIsolation.parent").call();

			Assertions.assertEquals(1, logCalls.get(), "必须实际触发同步日志回调异常");
			Assertions.assertEquals(Procedure.LogicError, childResult.get(), "日志失败不得替换子过程业务结果");
			Assertions.assertEquals(Procedure.Success, parentResult, "子过程日志失败不得回滚父过程保存点");
			Assertions.assertEquals(1, parentCommits.get());
			Assertions.assertEquals(0, parentRollbacks.get());
			Assertions.assertEquals(0, childCommits.get());
			Assertions.assertEquals(1, childRollbacks.get());
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var value = table.get(1L);
				Assertions.assertNotNull(value);
				Assertions.assertEquals(42, value.getV1(), "父过程写入应提交，子过程写入应回滚");
				return Procedure.Success;
			}, "TestProcedureLogActionFailureIsolation.verify").call());
		} finally {
			Procedure.logAction = originalLogAction;
			app.stop();
		}
	}
}
