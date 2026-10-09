package Zeze.Trans;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Transaction.perform 对业务过程抛出的 AssertionError 一律重抛（单测断言语义优先），
 * 但 whileCommit/whileRollback 回调经 triggerActions 重抛出的 AssertionError 在
 * finalCommit/finalRollback 的收尾 catch(Throwable) 中被吞：提交路径静默返回 Success、
 * 回滚路径静默返回错误码——测试断言失效时单测假绿，与业务断言路径表现不一致。
 *
 * 修复：final* 收尾对 AssertionError 先于 Throwable 兜底重抛；perform 调 finalCommit
 * 处单独接住重抛（数据已落库，不进 halt 路径）向上传播。
 */
@Fast
public class TestFinalCallbackAssertionError {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（16374，growth=1；上限16383）。
	private static final int SERVER_ID = FastServerIds.TEST_FINAL_CALLBACK_ASSERTION;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("final_cb_assert_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFinalCallbackAssertionError", conf);
	}

	@Test
	public void testCommitCallbackAssertionPropagates() throws Exception {
		var app = newApp();
		app.start();
		try {
			var ex = Assertions.assertThrows(AssertionError.class, () ->
					app.newProcedure(() -> {
						Transaction.whileCommit(() -> {
							throw new AssertionError("assert in whileCommit");
						});
						return Procedure.Success;
					}, "commitAe").call());
			Assertions.assertEquals("assert in whileCommit", ex.getMessage());
		} finally {
			app.stop();
		}
	}

	@Test
	public void testRollbackCallbackAssertionPropagates() throws Exception {
		var app = newApp();
		app.start();
		try {
			var ex = Assertions.assertThrows(AssertionError.class, () ->
					app.newProcedure(() -> {
						Transaction.whileRollback(() -> {
							throw new AssertionError("assert in whileRollback");
						});
						return Procedure.Exception; // 走finalRollback终态
					}, "rollbackAe").call());
			Assertions.assertEquals("assert in whileRollback", ex.getMessage());
		} finally {
			app.stop();
		}
	}
}
