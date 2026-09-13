package UnitTest.Zeze;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-81 回归：noDatabase模式的生命周期钩子。
 * delayRemove/checkpoint仅在!isNoDatabase()时创建，而公开钩子endStart()/
 * checkpointRun()无守卫——noDatabase应用（或serverId&lt;0测试模式）启动后调用即NPE。
 * 两个方法都是public生命周期钩子，最易被应用模板照抄。修复：按模式分流，
 * noDatabase下no-op（与无数据库语义一致）。
 */
@Fast
public class TestNoDatabaseLifecycle {
	@Test
	public void testLifecycleHooksNoNpe() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = new Application("TestNoDatabaseLifecycle", config);
		Assertions.assertTrue(app.isNoDatabase());
		Assertions.assertDoesNotThrow(app::endStart, "noDatabase模式endStart必须no-op而非NPE（FND4-81）");
		Assertions.assertDoesNotThrow(app::checkpointRun, "noDatabase模式checkpointRun必须no-op而非NPE（FND4-81）");
	}

	@Test
	public void testCheckpointRunThreadAfterStart() throws Exception {
		// FND5-01：Transaction.perform的finally按CheckpointTransactionPeriod周期调用本钩子，
		// start()在noDatabase下仍置eStarted（776行在!noDatabase块外），checkpoint::runOnce
		// 对null receiver在方法引用创建时即NPE——start后直调即确定性复现（无需30万笔）。
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var app = new Application("TestNoDatabaseLifecycleCr", config);
		app.start();
		try {
			Assertions.assertDoesNotThrow(app::checkpointRunThread,
					"noDatabase模式checkpointRunThread必须no-op而非NPE（FND5-01）");
		} finally {
			app.stop();
		}
	}
}
