package UnitTest.Zeze.Game;

import java.io.File;
import java.util.ArrayList;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import UnitTest.Zeze.Game.MyTasks.MyTask01;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Builtin.Game.TaskBase.BSpecificTaskEvent;
import Zeze.Builtin.Game.TaskBase.BTConditionNPCTalkEvent;
import Zeze.Builtin.Game.TaskBase.TriggerTaskEvent;
import Zeze.Game.TaskBase;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zezex.Linkd.Auth;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Ignore;

@Ignore
public class TestTask extends TestCase {
	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();
	final static int ClientCount = 1;
	final static int LinkCount = 1;
	final static int ServerCount = 1;
	final static int RoleCount = 1;

	private void start() throws Exception {
		for (int i = 0; i < ClientCount; ++i) {
			var client = new ClientGame.App();
			clients.add(client);
		}
		for (int i = 0; i < LinkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < ServerCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < LinkCount; ++i)
			links.get(i).Start(-(i+1), 12000 + i, 15000 + i);
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

	private void stop() throws Exception {
		for (var client : clients)
			client.Stop();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
	}

	// ======================================== 测试用例1：每日任务 ========================================

	static class TaskClient {
		public long TaskId;
		public int state; // 0: 不可接取, 1: 可接取, 2: 已接取, 3: 已完成未提交 4: 已提交
	}
	static ConcurrentHashSet<TaskClient> tasksInfoClient = new ConcurrentHashSet<>(); // 模拟一份在客户端的任务数据

	public void test1() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();

		try {
			start();

			System.out.println("=============== 在Client0注册Role0 ===============");
			var client0 = clients.get(0);
			auth(client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var server0 = servers.get(0);

			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {

				var module = server0.taskModule;
				module.registerTask(MyTask01.class);
				// ==================== 读取任务列表：DailyTask ====================
				String task1JsonPath = new File("").getAbsolutePath();
				task1JsonPath = task1JsonPath.replace('\\', '/');
				task1JsonPath += "/src/UnitTest/Zeze/Game/MyJson/daily_task.json";
				module.loadJson(task1JsonPath);

				registerRole(client0, roleId); // 注册角色
				acceptTask(client0, roleId, 1); // 接受任务1
				killMonster(client0, roleId, 1, 1001, 10); // 杀死10只怪物1001
				selectOption(client0, roleId, 1, "npc01dialog10001", 1); // 选择对话选项1
				selectOption(client0, roleId, 1, "npc01dialog10002", 2); // 选择对话选项2
				finishTalk(client0, roleId, 1); // 完成对话

				return Procedure.Success;
			}, "Daily Task - Test").call());
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			stop();
		}
	}

	/**
	 * 当角色创建时，会自动赋给这个角色整个任务列表。在返回的ChangedTasks中，会有整个任务列表的信息。
	 */
	private static void registerRole(ClientGame.App app, long roleId) {
		TriggerTaskEvent event = new TriggerTaskEvent();
		event.Argument.setRoleId(roleId);
		event.SendForWait(app.ClientService.GetSocket()).await();
		var resCode = event.Result.getResultCode();
		var tasksInfo = event.Result.getChangedTasks();
		tasksInfoClient.clear();
		for (var taskInfo : tasksInfo) {
			var task = new TaskClient();
			task.TaskId = taskInfo.getTaskId();
			task.state = taskInfo.getTaskState();
			tasksInfoClient.add(task);
		}

		var success = (resCode & TaskBase.Module.TaskResultSuccess);
		Assert.assertNotEquals(0, success);
		var roleTasksCreated = (resCode & TaskBase.Module.TaskResultNewRoleTasksCreated);
		Assert.assertNotEquals(0, roleTasksCreated);
	}

	/**
	 * 接任务
	 */
	private static void acceptTask(ClientGame.App app, long roleId, long taskId) {

	}

	/**
	 * 完成任务之后交任务
	 */
	private static void commitTask(ClientGame.App app, long roleId, long taskId) {

	}

	private static void killMonster(ClientGame.App app, long roleId, long taskId, int monsterId, int count) {

	}

	private static void selectOption(ClientGame.App app, long roleId, long taskId, String dialogId, int optionId) {
		TriggerTaskEvent taskEvent = new TriggerTaskEvent();
		taskEvent.Argument.setRoleId(roleId);
		var bean = new BTConditionNPCTalkEvent();
		bean.setFinished(false);
		bean.setDialogId(dialogId);
		bean.setDialogOption(optionId);

		taskEvent.Argument.setEventType(new BSpecificTaskEvent(taskId));
		taskEvent.Argument.getEventBean().setBean(bean);

		taskEvent.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, taskEvent.getResultCode());
	}

	private static void finishTalk(ClientGame.App app, long roleId, long taskId) {
		TriggerTaskEvent taskEvent = new TriggerTaskEvent();
		taskEvent.Argument.setRoleId(roleId);
		var bean = new BTConditionNPCTalkEvent();
		bean.setFinished(true); // 在对话结束时，发一条这个事件

		taskEvent.Argument.setEventType(new BSpecificTaskEvent(taskId));
		taskEvent.Argument.getEventBean().setBean(bean);

		taskEvent.SendForWait(app.ClientService.GetSocket()).await();
		Assert.assertEquals(0, taskEvent.getResultCode());
	}

	// ======================================== 用户登录相关 ========================================

	// 全局角色登录状态函数
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
