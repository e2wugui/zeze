package Task;

import java.util.ArrayList;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Game.Task;
import Zeze.Game.TaskPhase;
import Zezex.Linkd.Auth;
import demo.Server;
import org.junit.Assert;

/**
 * 在Zeze外面（模拟服务器环境）测试Task系统。其中，需要测试的需求有：
 * Gameplay视角：
 * 1. 创建角色后，允许客户端角色使用方法访问自己RoleId的任务列表。以一个金币获取任务为例：
 * 2. 允许客户端角色接取任务。
 * 3. 允许客户端角色推进任务进度。
 * 4. 允许客户端角色放弃任务。
 * 5. 允许客户端角色完成任务。
 * Designer视角
 * 1. 允许设计者创建任务：对Zeze.Game.Task进行扩展。
 * 2. 允许设计者创建任务进度：对Zeze.Game.TaskProgress进行扩展。
 * 3. 允许设计者创建任务进度变更事件：对Zeze.Game.TaskProgressChangeEvent进行扩展。
 * 4. 允许设计者创建任务进度变更事件处理器：对Zeze.Game.TaskProgressChangeEventHandler进行扩展。
 */
public class TestTask {

	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();
	final static int ClientCount = 1;
	final static int LinkCount = 1;
	final static int ServerCount = 1;
	final static int RoleCount = 1;

	private void start() throws Throwable {
		for (int i = 0; i < ClientCount; ++i) {
			var client = new ClientGame.App();
			clients.add(client);
		}
		for (int i = 0; i < LinkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < ServerCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < LinkCount; ++i)
			links.get(i).Start(10000 + i, 15000 + i);
		for (int i = 0; i < ServerCount; ++i)
			servers.get(i).Start(i, 20000 + i);
		Thread.sleep(2000); // wait server ready
		for (int i = 0; i < ClientCount; ++i) {
			var link = links.get(i % LinkCount); // 按顺序选择link
			var ipport = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipport.getKey(), ipport.getValue());
			// wait client connected
			clients.get(i).Connector.WaitReady();
		}
	}

	private void stop() throws Throwable {
		for (var client : clients)
			client.Stop();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
	}

	public void test1() throws Throwable {
		Zeze.Util.Task.tryInitThreadPool(null, null, null);

		try {
			start();

			System.out.println("=============== 在Client0注册Role0 ===============");
			var client0 = clients.get(0);
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var server0 = servers.get(0);
			var task1 = server0.getZeze().getTaskModule().open("Task01-GetGold");
			TaskPhase phase1 = task1.newPhase();
			TaskPhase phase2 = task1.newPhase();
			TaskPhase phase3 = task1.newPhase();
			ConditionNamedCount goldCondition = new ConditionNamedCount("Gold", 100);
			phase1.addCondition(goldCondition);
			phase2.addCondition(goldCondition);
			phase3.addCondition(goldCondition);
			task1.linkPhase(phase1, phase2);
			task1.linkPhase(phase2, phase3);
			task1.setupTask();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			stop();
		}
	}

	// 一个简单的任务，用于测试。
	static void collectACoin(ClientGame.App app, long roleId) {

	}

	// 全局的一些辅助函数

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
}
