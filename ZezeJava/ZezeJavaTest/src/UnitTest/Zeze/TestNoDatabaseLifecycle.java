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
}
