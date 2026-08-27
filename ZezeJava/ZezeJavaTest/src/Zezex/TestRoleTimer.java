package Zezex;

import java.util.concurrent.TimeUnit;
import UnitTest.Zeze.Component.TestBean;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestRoleTimer {
	private static final @NotNull Logger logger = LogManager.getLogger(TestRoleTimer.class);

	final ZezexTestEnv env = new ZezexTestEnv();

	private static void testContent(TimerContext context) {
		TestBean bean = (TestBean)context.customData;
		Assertions.assertNotNull(bean);
		if (bean.checkLiving())
			bean.addValue();
		System.out.println(">> Name: " + context.timerName
				+ " ID: " + context.timerId
				+ " Now: " + System.currentTimeMillis()
				+ " Expected: " + context.expectedTimeMills
				+ " Next: " + context.nextExpectedTimeMills);
	}

	public static class TestOnlineTimerHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			testContent(context);
		}
	}

	static final TaskCompletionSource<Boolean> timerFuture = new TaskCompletionSource<>();

	public static class NullCustomDataHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			timerFuture.setResult(true);
		}
	}

	@Test
	public void testRoleTimer1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			env.prepareNewEnvironment(2, 2, 1);

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
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOnline(roleId, 1, -1, -1, -1, NullCustomDataHandle.class, null);
				timerRole0.scheduleOnline(roleId, TimerSpec.ofDelay(1), NullCustomDataHandle.class, null);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			timerFuture.get(30, TimeUnit.SECONDS);
			System.out.println("NullCustomDataHandle Done!");
			TestBean bean = new TestBean();
			bean.resetFuture(5);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOnline(roleId, 1, 1, 5, -1, TestOnlineTimerHandle.class, bean);
				timerRole0.scheduleOnline(roleId, TimerSpec.ofDelay(1).period(1).times(5), TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			bean.getFuture().get(30, TimeUnit.SECONDS);
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);
			Assertions.assertTrue(bean.getTestValue() > 0); // 确保客户端0的timer被踢掉了
			log("测试二通过");

			TestBean namedBean = new TestBean();
			namedBean.resetFuture(5);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 5, 5, 5, -1, TestOnlineTimerHandle.class, namedBean);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(5).period(5).times(5), TestOnlineTimerHandle.class, namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			newNamedBean1.resetFuture(5);
			Assertions.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean1);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(1).period(1).times(5), TestOnlineTimerHandle.class, newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			namedBean.getFuture().get(30, TimeUnit.SECONDS);
			Assertions.assertEquals(0, newNamedBean1.getTestValue());

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			newNamedBean2.resetFuture(5);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean2);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(1).period(1).times(5), TestOnlineTimerHandle.class, newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			newNamedBean2.getFuture().get(30, TimeUnit.SECONDS);
			log("测试三通过");

			ZezexTestEnv.logout(client1, roleId);
		} finally {
			env.stopAll();
		}
	}

	@Test
	public void testRoleTimerCron1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			env.prepareNewEnvironment(2, 2, 1);

			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.getFirst();
			var timer0 = server0.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = role != null ? role.getId() : ZezexTestEnv.createRole(client0, "new_role0");
			ZezexTestEnv.login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();

			TestBean bean = new TestBean();
			bean.resetFuture(2);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOnline(roleId, "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, bean);
				timerRole0.scheduleOnline(roleId,
					TimerSpec.ofCron("*/1 * * * * ?").times(2),
					TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			bean.getFuture().get(30, TimeUnit.SECONDS);
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);
			Assertions.assertTrue(bean.getTestValue() > 0); // 确保客户端0的timer被踢掉了
			log("测试二通过");

			TestBean namedBean = new TestBean();
			namedBean.resetFuture(2);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, namedBean);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofCron("*/1 * * * * ?").times(2),
					TestOnlineTimerHandle.class, namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			newNamedBean1.resetFuture(2);
			Assertions.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, newNamedBean1);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofCron("*/1 * * * * ?").times(2),
					TestOnlineTimerHandle.class, newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			namedBean.getFuture().get(30, TimeUnit.SECONDS);

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			newNamedBean2.resetFuture(2);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean2);
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer",
					TimerSpec.ofDelay(1).period(1).times(5),
					TestOnlineTimerHandle.class, newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			newNamedBean2.getFuture().get(30, TimeUnit.SECONDS);
			log("测试三通过");

			ZezexTestEnv.logout(client1, roleId);
		} finally {
			env.stopAll();
		}
	}

	public static class TestOfflineTimerHandle implements TimerHandle {
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

			// 初始化环境
			env.prepareNewEnvironment(2, 2, 2);

			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.get(0);
			var timer0 = server0.getZeze().getTimer();

			var timerRole0 = timer0.getRoleTimer();

			// 注册登录客户端0
			log("注册登录客户端0");
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = null != role ? role.getId() : ZezexTestEnv.createRole(client0, "role1");
			ZezexTestEnv.login(client0, roleId);

			// 角色下线时注册定时器
			ZezexTestEnv.logout(client0, roleId);

			TestBean bean = new TestBean();
			bean.resetFuture(5);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOffline(roleId, 1, 1, 5, -1, TestOfflineTimerHandle.class, bean);
				timerRole0.scheduleOffline(roleId, TimerSpec.ofDelay(1).period(1).times(5),
					TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());
			bean.getFuture().get(30, TimeUnit.SECONDS);

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);

			ZezexTestEnv.logout(client1, roleId);
		} finally {
			env.stopAll();
		}
	}

	@Test
	public void testRoleTimerCron2() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Offline Timer 测试启动");

			// 初始化环境
			env.prepareNewEnvironment(2, 2, 2);

			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.get(0);
			var timer0 = server0.getZeze().getTimer();

			var timerRole0 = timer0.getRoleTimer();

			// 注册登录客户端0
			log("注册登录客户端0");
			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = ZezexTestEnv.getRole(client0);
			var roleId = role != null ? role.getId() : ZezexTestEnv.createRole(client0, "new_role1");
			ZezexTestEnv.login(client0, roleId);

			// 角色下线时注册定时器
			ZezexTestEnv.logout(client0, roleId);

			TestBean bean = new TestBean();
			bean.resetFuture(2);
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOffline(roleId, "*/1 * * * * ?", 2, -1, TestOfflineTimerHandle.class, bean);
				timerRole0.scheduleOffline(roleId, TimerSpec.ofCron("*/1 * * * * ?").times(2),
					TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());

			bean.getFuture().get(30, TimeUnit.SECONDS);
			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			ZezexTestEnv.login(client1, roleId);

			ZezexTestEnv.logout(client1, roleId);
		} finally {
			env.stopAll();
		}
	}

	private static void log(String msg) {
		logger.info("======================================== {} ========================================", msg);
	}
}
