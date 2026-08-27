package Zezex;

import UnitTest.Zeze.Component.TestBean;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

/**
 * Role Online/Offline Timer 走 Game.App 全链路的测试。类级共享环境（单server，online timer 要求登录驻留在本server）：
 * 方法1 用 TCP 客户端、方法2 用 websocket 客户端（ZezexTestEnv.ClientStartMode），每方法只重建客户端。
 */
@SuppressWarnings("CallToPrintStackTrace")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestGameTimer {

	static final ZezexTestEnv env = new ZezexTestEnv();

	/** 上一方法失败（dirty）则整体重建保留失败隔离；绿路径只重建客户端（~百ms）。单server：online timer 要求登录驻留在本server，双server会走transmit转发且不触发（原方法1即(2,2,1)）。 */
	private static void prepareMethod(ZezexTestEnv.ClientStartMode mode) throws Exception {
		if (!env.isPrepared() || env.isDirty()) {
			if (env.isPrepared())
				env.stopAll();
			env.prepareNewEnvironment(0, 2, 1);
		}
		env.rebuildClients(2, mode);
	}

	@AfterAll
	static void teardownEnv() throws Exception {
		env.stopAll();
	}

	private static void testContent(TimerContext context) {
		TestBean bean = (TestBean)context.customData;
		//noinspection DataFlowIssue
		if (bean.checkLiving())
			bean.addValue();
		System.out.println(">> Name: " + context.timerName
				+ " ID: " + context.timerId
				+ " Now: " + System.currentTimeMillis()
				+ " Expected: " + context.expectedTimeMills
				+ " Next: " + context.nextExpectedTimeMills);
	}

	private static class TestOnlineTimerHandle implements TimerHandle {
		private TestOnlineTimerHandle() {
		}

		@Override
		public void onTimer(@NotNull TimerContext context) {
			testContent(context);
		}
	}

	@Test
	public void testRoleTimer1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			prepareMethod(ZezexTestEnv.ClientStartMode.TCP);

			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.getFirst();
			var timer0 = server0.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = null != role ? role.getId() : ZezexTestEnv.createRole(client0, "role0");
			ZezexTestEnv.login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();
			TestBean bean = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOnline(roleId, 1, 1, 5, System.currentTimeMillis() + 2000, TestOnlineTimerHandle.class, bean);
				timerRole0.scheduleOnline(roleId, TimerSpec.ofDelay(1).period(1).times(5).endTime(System.currentTimeMillis() + 2000),
					TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			// bean 5次触发实际ms级完成：50ms轮询（上限3s），timer真卡住仍会失败
			for (int i = 0; i < 60 && bean.getTestValue() < 5; ++i)
				Thread.sleep(50);
			Assertions.assertTrue(bean.getTestValue() >= 5);
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);
			Thread.sleep(200); // 负向断言稳定窗：被踢的timer若残留触发应在此窗内出现
			Assertions.assertEquals(5, bean.getTestValue()); // 确保客户端0的timer被踢掉了【不变】
			log("测试二通过");

			TestBean namedBean = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 5, System.currentTimeMillis() + 2000, TestOnlineTimerHandle.class, namedBean);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(100).period(100).times(5).endTime(System.currentTimeMillis() + 2000),
					TestOnlineTimerHandle.class, namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			Assertions.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 5, System.currentTimeMillis() + 5000, TestOnlineTimerHandle.class, newNamedBean1);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(100).period(100).times(5).endTime(System.currentTimeMillis() + 5000),
					TestOnlineTimerHandle.class, newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// namedBean 5×100ms理论500ms：轮询到位后再留200ms负向稳定窗（错注册的newNamedBean1若有触发应在此窗内出现）
			for (int i = 0; i < 60 && namedBean.getTestValue() < 5; ++i)
				Thread.sleep(50);
			Thread.sleep(200);

			Assertions.assertEquals(5, namedBean.getTestValue());
			Assertions.assertEquals(0, newNamedBean1.getTestValue());

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 10, System.currentTimeMillis() + 5000, TestOnlineTimerHandle.class, newNamedBean2);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(100).period(100).times(10).endTime(System.currentTimeMillis() + 5000),
					TestOnlineTimerHandle.class, newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 100ms 周期 ×10 次理论约 1 秒，盲等 1.1 秒余量太薄易抖：改为有界轮询（50ms 间隔，最多 3 秒），timer 真卡住仍会失败
			for (int i = 0; i < 60 && newNamedBean2.getTestValue() < 10; ++i)
				Thread.sleep(50);
			Assertions.assertTrue(newNamedBean2.getTestValue() >= 10);
			log("测试三通过");
		} catch (Throwable e) {
			env.markDirty();
			e.printStackTrace();
			throw e;
		}
	}

	private static class TestOfflineTimerHandle implements TimerHandle {
		private TestOfflineTimerHandle() {
		}

		@Override
		public void onTimer(@NotNull TimerContext context) {
			testContent(context);
		}
	}

	@Test
	public void testRoleTimer2() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Offline Timer 测试启动");

			// 初始化环境（websocket 客户端）
			prepareMethod(ZezexTestEnv.ClientStartMode.WEBSOCKET);

			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.getFirst();
			var timer0 = server0.getZeze().getTimer();

			var timerRole0 = timer0.getRoleTimer();

			// 注册登录客户端0
			log("注册登录客户端0");
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account1");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = null != role ? role.getId() : ZezexTestEnv.createRole(client0, "role1");
			ZezexTestEnv.login(client0, roleId);

			// 角色下线时注册定时器
			ZezexTestEnv.logout(client0, roleId);

			TestBean bean = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOffline(roleId, 100, 100, 10, System.currentTimeMillis() + 5000, TestOfflineTimerHandle.class, bean);
				timerRole0.scheduleOffline(roleId, TimerSpec.ofDelay(100).period(100).times(10).endTime(System.currentTimeMillis() + 5000),
					TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());
			// 轮询确认offline timer真的在角色下线期间触发（此前该方法对timer触发零覆盖）
			for (int i = 0; i < 60 && bean.getTestValue() < 1; ++i)
				Thread.sleep(50);
			Assertions.assertTrue(bean.getTestValue() >= 1);

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account1");
			ZezexTestEnv.login(client1, roleId);
		} catch (Throwable e) {
			env.markDirty();
			e.printStackTrace();
			throw e;
		}
	}

	private static void log(String msg) {
		System.out.println("======================================== " + msg + " ========================================");
	}
}
