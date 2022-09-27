package Zezex;

import java.util.ArrayList;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import UnitTest.Zeze.Component.TestBean;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zezex.Linkd.Auth;
import junit.framework.TestCase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestOnlineTimer {

	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();

	private void prepareNewEnvironment(int clientCount, int linkCount, int serverCount, int roleCount) throws Throwable {
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
			links.get(i).Start(10000 + i, 15000 + i);
		for (int i = 0; i < serverCount; ++i)
			servers.get(i).Start(i, 20000 + i);
		Thread.sleep(2000);
		for (int i = 0; i < clientCount; ++i) {
			var link = links.get(i % linkCount);
			var ipport = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipport.getKey(), ipport.getValue());
			clients.get(i).Connector.WaitReady();
		}
	}

	private void stopAll() throws Throwable {
		for (var client : clients)
			client.Stop();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
	}

	@Test
	public void testRoleTimer() throws Throwable {
		Task.tryInitThreadPool(null, null, null);

		try {
			log("Role Timer 测试启动");

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

			// 注册登录客户端0
			log("注册登录客户端0");
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			timer0.initializeOnlineTimer(server0.ProviderApp);
			timer1.initializeOnlineTimer(server1.ProviderApp);
			var timerRole0 = timer0.getRoleTimer();
			var timerRole1 = timer1.getRoleTimer();

			TestBean bean = new TestBean();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOnline(roleId, 200, 200, 15, System.currentTimeMillis() + 5000, new TimerHandle() {
					@Override
					public void onTimer(TimerContext context) throws Throwable {
						System.out.println(">> Name: " + context.timerName + " ID: " + context.timerId + " Now: " + context.curTimeMills + " Expected: " + context.expectedTimeMills + " Next: " + context.nextExpectedTimeMills);
					}
				}, bean);
				return Procedure.Success;
			}, "test1").call());

			sleep(200, 10);

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			auth(client1, "account0");
			login(client1, roleId);

			sleep(200, 20);

			// 客户端0在另一个Server登录

//			log("连接断开");
//			client0.ClientService.Stop();
//			sleep(200, 10);
//			log("连接恢复");
//			client0.ClientService.Start();
//			client0.Connector.WaitReady();
//			auth(client0, "account0");
//			relogin(client0, roleId0);
//			sleep(200, 10);

//			// Test: 重新登录
//			log("账号登出");
//			logout(client0, roleId0);
//
//			sleep(200, 6);
//
//			log("账号重新登录");
//			auth(client0, "account0");
//			var new_role0 = getRole(client0);
//			var new_roleId0 = null != new_role0 ? new_role0.getId() : createRole(client0, "new_role0");
//			assert new_role0 != null;
//			Assert.assertEquals(roleId0, new_role0.getId());
//			login(client0, new_roleId0);
//
//			sleep(200, 12);

//			logger.info("=== test3 - 1");
//			var client0 = clients.get(0);
//			auth(client0, "account0");
//			var role = getRole(client0);
//			var roleId = null != role ? role.getId() : createRole(client0, "role0");
//			login(client0, roleId);

//			// testcase relogin
//			logger.info("=== test3 - 2");
//			client0.ClientService.Stop();
//			client0.ClientService.Start();
//			client0.Connector.WaitReady();
//			auth(client0, "account0");
//			relogin(client0, roleId);
//
//			// testcase kick
//			logger.info("=== test3 - 3");
//			var client1 = clients.get(1);
//			auth(client1, "account0");
//			login(client1, roleId);
//
//			// logout client1: client0 被踢了
//			logger.info("=== test3 - 4");
//			logout(client1, roleId);
		} finally {
			stopAll();
		}
	}

	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, relogin.getResultCode());
	}

	private static void logout(ClientGame.App app, long roleIdForLogOnly) {
		var logout = new Logout();
		logout.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, login.getResultCode());
	}

	private static void auth(ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, auth.getResultCode());
	}

	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().get(0);
	}

	private static void sleep(long gap, int times) {
		try {
			for (int i = 0; i < times; ++i) {
				Thread.sleep(gap);
				System.out.println("--- sleep --- " + i);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void log(String msg) {
		System.out.println("======================================== " + msg + " ========================================");
	}
}