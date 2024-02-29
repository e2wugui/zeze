package Zezex;

import java.util.ArrayList;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import UnitTest.Zeze.Component.TestBean;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zezex.Linkd.Auth;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGameTimer {

	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();

	private static void waitLinkdProvider(Zezex.App linkd) throws InterruptedException {
		while (true) {
			System.out.println(linkd.LinkdApp.zeze.getServiceManager().getSubscribeStates().values());
			if (!linkd.LinkdApp.zeze.getServiceManager().getSubscribeStates().isEmpty())
				break;
			System.out.println("wait Linkd Provider.");
			//noinspection BusyWait
			Thread.sleep(1000);
		}
	}

	private void prepareNewEnvironment(int clientCount, int linkCount, int serverCount, int roleCount) throws Exception {
		clients.clear();
		links.clear();
		servers.clear();

		for (int i = 0; i < clientCount; ++i)
			clients.add(new ClientGame.App());
		for (int i = 0; i < linkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < serverCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < linkCount; ++i)
			links.get(i).Start(-(i+1), 12000 + i, 15000 + i);
		for (int i = 40; i < serverCount + 40; ++i) {
			servers.get(i - 40).Start(i, 20000 + i - 40);
			//servers.get(i - 40).getZeze().getTimer().initializeOnlineTimer(servers.get(i - 40).ProviderApp);
			//servers.get(i - 40).getZeze().getTimer().start();
		}
		for (var link : links) {
			waitLinkdProvider(link);
		}
		for (int i = 0; i < clientCount; ++i) {
			var link = links.get(i % linkCount);
			var ipport = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipport.getKey(), ipport.getValue());
			clients.get(i).Connector.WaitReady();
		}
	}

	private void stopAll() throws Exception {
		for (var client : clients)
			client.Stop();
		Thread.sleep(100); // 防止client断开连接的时候，下面的provider关闭太快执行异常。这个异常实际上无所谓。
		for (var server : servers)
			server.stopBeforeModules();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
	}

	private static void testContent(TimerContext context) throws Exception {
		TestBean bean = (TestBean)context.customData;
		if (bean.checkLiving())
			bean.addValue();
		System.out.println(">> Name: " + context.timerName + " ID: " + context.timerId + " Now: " + context.curTimeMills + " Expected: " + context.expectedTimeMills + " Next: " + context.nextExpectedTimeMills);
	}

	public static class TestOnlineTimerHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			testContent(context);
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	@Test
	public void testRoleTimer1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			prepareNewEnvironment(2, 2, 1, 2);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
			var link0 = links.get(0);
			var link1 = links.get(1);
			var server0 = servers.get(0);
//			var server1 = servers.get(1);
			var timer0 = server0.getZeze().getTimer();
//			var timer1 = server1.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();
//			var timerRole1 = timer1.getRoleTimer();

			TestBean bean = new TestBean();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOnline(roleId, 1, 1, 5, System.currentTimeMillis() + 2000, new TestOnlineTimerHandle(), bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			sleep(100, 6);
			Assert.assertTrue(bean.getTestValue() >= 5);
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			auth(client1, "account0");
			login(client1, roleId);
			sleep(100, 6);
			Assert.assertEquals(5, bean.getTestValue()); // 确保客户端0的timer被踢掉了【不变】
			log("测试二通过");

			TestBean namedBean = new TestBean();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 5, System.currentTimeMillis() + 2000, new TestOnlineTimerHandle(), namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			Assert.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 5, System.currentTimeMillis() + 5000, new TestOnlineTimerHandle(), newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			sleep(100, 10);

			Assert.assertEquals(5, namedBean.getTestValue());
			Assert.assertEquals(0, newNamedBean1.getTestValue());

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 100, 100, 10, System.currentTimeMillis() + 5000, new TestOnlineTimerHandle(), newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			sleep(100, 11);
			Assert.assertTrue(newNamedBean2.getTestValue() >= 10);
			log("测试三通过");
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		} finally {
			stopAll();
		}
	}

	public static class TestOfflineTimerHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) throws Exception {
			testContent(context);
		}

		@Override
		public void onTimerCancel() throws Exception {

		}
	}

	@Test
	public void testRoleTimer2() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Offline Timer 测试启动");

			// 初始化环境
			prepareNewEnvironment(2, 2, 2, 2);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
			var link0 = links.get(0);
			var link1 = links.get(1);
			var server0 = servers.get(0);
			var server1 = servers.get(1);
			var timer0 = server0.getZeze().getTimer();
			var timer1 = server1.getZeze().getTimer();

			var timerRole0 = timer0.getRoleTimer();
			var timerRole1 = timer1.getRoleTimer();

			// 注册登录客户端0
			log("注册登录客户端0");
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			sleep(200, 1);

			// 角色下线时注册定时器
			logout(client0, roleId);

			TestBean bean = new TestBean();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOffline(roleId, 100, 100, 10, System.currentTimeMillis() + 5000, TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());

			sleep(100, 5);

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			auth(client1, "account0");
			login(client1, roleId);

			sleep(100, 5);
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		} finally {
			stopAll();
		}
	}

	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new Zeze.Builtin.Game.Online.ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, relogin.getResultCode());
	}

	private static void logout(ClientGame.App app, long roleIdForLogOnly) {
		var logout = new Zeze.Builtin.Game.Online.Logout();
		logout.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Zeze.Builtin.Game.Online.Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, login.getResultCode());
	}

	private static void auth(ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, auth.getResultCode());
	}

	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assert.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().get(0);
	}

	private static void sleep(long gap, int times) {
		try {
			for (int i = 0; i < times; ++i) {
				Thread.sleep(gap);
				System.out.println("-- sleep " + i);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void log(String msg) {
		System.out.println("======================================== " + msg + " ========================================");
	}
}
