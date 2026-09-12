package UnitTest.Zeze.Transaction;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Application;
import Zeze.Config;

/**
 * FND3-47：同实例 stop→start 是隐性残废路径——构造期组件（delayRemove/onz/takeover/
 * autoKey/timer/queueModule/historyModule/serviceManager/databases）被stop置null/反注册
 * 且start不重建：759行delayRemove.start()必NPE，绕过后也是顶着eStarted的残废应用。
 * 契约化为显式拒绝：非eUninitialized的start()抛IllegalStateException指路新建实例；
 * 全新实例（eUninitialized初始态）首次启动不受影响。
 */
@Fast
public class TestApplicationRestartContract {
	// 独立serverId避免zeze_cache_<serverId>目录与其他测试冲突。
	private static final int SERVER_ID = 7353;

	private Application app;

	private Application newApp() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(SERVER_ID);
		config.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("app_restart_contract_" + SERVER_ID);
		config.getDatabaseConfMap().put("", dbConf);
		return new Application("TestApplicationRestartContract", config);
	}

	@AfterEach
	public void after() throws Exception {
		if (app != null)
			app.stop(); // 已停实例幂等；拒绝路径无副作用
	}

	@Test
	public void testFreshStartStillWorks() throws Exception {
		app = newApp();
		Assertions.assertDoesNotThrow(app::start, "全新实例首次启动必须正常");
		Assertions.assertTrue(app.isStart());
	}

	@Test
	public void testRestartRejectedAfterStop() throws Exception {
		app = newApp();
		app.start();
		app.stop();
		// 未修复：NPE（delayRemove.start()）或其他随机异常，应用半启动顶着eStarted。
		Assertions.assertThrows(IllegalStateException.class, app::start,
				"stop后同实例重启必须显式拒绝（FND3-47），不能NPE或静默残废");
	}

	@Test
	public void testStopOnFreshAppKeepsInstanceUsable() throws Exception {
		app = newApp();
		app.stop(); // 从未启动的实例：no-op，不得解除武装
		Assertions.assertDoesNotThrow(app::start, "对未启动实例的stop不得影响后续start");
		Assertions.assertTrue(app.isStart());
		// 旧代码：stop在状态检查前置null了构造期的onz，之后start静默缺Onz服务。
		Assertions.assertNotNull(app.getOnz(), "构造期组件不得被未启动实例的stop清掉（FND3-47同族半状态）");
	}

	@Test
	public void testInterruptedLifecycleRejected() throws Exception {
		app = newApp();
		// 模拟上次start/stop中途崩溃：状态滞留eStarting（无注入钩子，反射置位）。
		var field = Application.class.getDeclaredField("startState");
		field.setAccessible(true);
		field.set(app, Application.StartState.eStarting);
		// 旧代码："stop()收尾+重走start"的假自愈——收尾stop把构造期组件置null，
		// 重走start同样落进delayRemove NPE半启动。
		Assertions.assertThrows(IllegalStateException.class, app::start,
				"生命周期未完成的实例必须显式拒绝，不能内部收尾重走start（假自愈）");
	}
}
