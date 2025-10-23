package Zezex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import UnitTest.Zeze.Component.TestBean;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Services.LoginQueue;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zezex.Linkd.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRoleTimer {
	private static final @NotNull Logger logger = LogManager.getLogger(TestRoleTimer.class);

	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();
	LoginQueue loginQueue;

	private void prepareNewEnvironment(int clientCount, int linkCount, int serverCount) throws Exception {
		clients.clear();
		links.clear();
		servers.clear();

		loginQueue = new LoginQueue();
		loginQueue.start();

		for (int i = 0; i < clientCount; ++i)
			clients.add(new ClientGame.App());
		for (int i = 0; i < linkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < serverCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < linkCount; ++i)
			links.get(i).Start(-(i + 1), 12000 + i, 15000 + i);
		for (int i = 0; i < serverCount; ++i) {
			servers.get(i).Start(i + 40, 20000 + i);
			//servers.get(i).getZeze().getTimer().initializeOnlineTimer(servers.get(i).ProviderApp);
			//servers.get(i).getZeze().getTimer().start();
		}
		Thread.sleep(2000);
		for (int i = 0; i < clientCount; ++i) {
			var link = links.get(i % linkCount);
			var ipPort = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipPort.getKey(), ipPort.getValue());
		}
		//for (int i = 0; i < clientCount; ++i)
		//	clients.get(i).Connector.WaitReady();
	}

	private void stopAll() throws Exception {
		for (var client : clients)
			client.Stop();
		for (var server : servers)
			server.stopBeforeModules();
		for (var server : servers)
			server.Stop();
		for (var link : links)
			link.Stop();
		loginQueue.stop();
		loginQueue = null;
		clients.clear();
		links.clear();
		servers.clear();
	}

	private static void testContent(TimerContext context) {
		TestBean bean = (TestBean)context.customData;
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
		public void onTimer(TimerContext context) {
			testContent(context);
		}
	}

	static final TaskCompletionSource<Boolean> timerFuture = new TaskCompletionSource<>();

	public static class NullCustomDataHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			timerFuture.setResult(true);
		}
	}

	@Test
	public void testRoleTimer1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			prepareNewEnvironment(2, 2, 1);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
			var link0 = links.get(0);
			var link1 = links.get(1);
			var server0 = servers.get(0);
			var timer0 = server0.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOnline(roleId, 1, -1, -1, -1, NullCustomDataHandle.class, null);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			timerFuture.get();
			System.out.println("NullCustomDataHandle Done!");
			TestBean bean = new TestBean();
			bean.resetFuture(5);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOnline(roleId, 1, 1, 5, -1, TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			bean.getFuture().await();
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);
			Assert.assertTrue(bean.getTestValue() > 0); // 确保客户端0的timer被踢掉了
			log("测试二通过");

			TestBean namedBean = new TestBean();
			namedBean.resetFuture(5);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 5, 5, 5, -1, TestOnlineTimerHandle.class, namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			newNamedBean1.resetFuture(5);
			Assert.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			namedBean.getFuture().await();
			Assert.assertEquals(0, newNamedBean1.getTestValue());

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			newNamedBean2.resetFuture(5);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			newNamedBean2.getFuture().await();
			log("测试三通过");

			logout(client1, roleId);
			sleep(200, 5);
		} finally {
			stopAll();
		}
	}

	@Test
	public void testRoleTimerCron1() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Online Timer 初始化测试环境");
			prepareNewEnvironment(2, 2, 1);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
			var link0 = links.get(0);
			var link1 = links.get(1);
			var server0 = servers.get(0);
			var timer0 = server0.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = role != null ? role.getId() : createRole(client0, "new_role0");
			login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();

			TestBean bean = new TestBean();
			bean.resetFuture(2);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOnline(roleId, "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			bean.getFuture().await();
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);
			sleep(1000, 1);
			Assert.assertTrue(bean.getTestValue() > 0); // 确保客户端0的timer被踢掉了
			log("测试二通过");

			TestBean namedBean = new TestBean();
			namedBean.resetFuture(2);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, namedBean);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			// 在过程中完后注册同名NamedTimer，应该失败
			TestBean newNamedBean1 = new TestBean();
			newNamedBean1.resetFuture(2);
			Assert.assertEquals(Procedure.Exception, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", "*/1 * * * * ?", 2, -1, TestOnlineTimerHandle.class, newNamedBean1);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			namedBean.getFuture().await();

			// 在执行完后注册同名NamedTimer，应该成功
			TestBean newNamedBean2 = new TestBean();
			newNamedBean2.resetFuture(2);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var res = timerRole0.scheduleOnlineNamed(roleId, "MyNamedTimer", 1, 1, 5, -1, TestOnlineTimerHandle.class, newNamedBean2);
				return res ? Procedure.Success : Procedure.Exception;
			}, "testOnlineWithBean").call());
			newNamedBean2.getFuture().await();
			log("测试三通过");

			logout(client1, roleId);
			sleep(200, 5);
		} finally {
			stopAll();
		}
	}

	public static class TestOfflineTimerHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			testContent(context);
		}
	}

	@Test
	public void testRoleTimer2() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Offline Timer 测试启动");

			// 初始化环境
			prepareNewEnvironment(2, 2, 2);

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
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role1");
			login(client0, roleId);

			sleep(200, 1);

			// 角色下线时注册定时器
			logout(client0, roleId);

			TestBean bean = new TestBean();
			bean.resetFuture(5);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOffline(roleId, 1, 1, 5, -1, TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());
			bean.getFuture().await();

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);

			sleep(200, 1);

			logout(client1, roleId);
			sleep(200, 1);
		} finally {
			stopAll();
		}
	}

	@Test
	public void testRoleTimerCron2() throws Exception {
		Task.tryInitThreadPool();

		try {
			log("Role Offline Timer 测试启动");

			// 初始化环境
			prepareNewEnvironment(2, 2, 2);

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
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = role != null ? role.getId() : createRole(client0, "new_role1");
			login(client0, roleId);

			sleep(200, 1);

			// 角色下线时注册定时器
			logout(client0, roleId);

			TestBean bean = new TestBean();
			bean.resetFuture(2);
			Assert.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				timerRole0.scheduleOffline(roleId, "*/1 * * * * ?", 2, -1, TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());

			bean.getFuture().await();
			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);

			sleep(200, 1);
			logout(client1, roleId);
			sleep(200, 1);
		} finally {
			stopAll();
		}
	}

	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new Zeze.Builtin.Game.Online.ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assert.assertEquals(0, relogin.getResultCode());
	}

	private static void logout(ClientGame.App app, long roleIdForLogOnly) {
		var logout = new Zeze.Builtin.Game.Online.Logout();
		logout.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assert.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Zeze.Builtin.Game.Online.Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		//System.out.println("login result: " + login.getResultCode());
		Assert.assertEquals(0, login.getResultCode());
	}

	private static void auth(BLoginToken.Data token, ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.Argument.setLoginQueueToken(token.getToken());
		auth.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assert.assertEquals(0, auth.getResultCode());
	}

	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assert.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket(), 30_000).await();
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
		logger.info("======================================== {} ========================================", msg);
	}

	@Test
	public void benchRoleTimer() throws Exception {
		Task.tryInitThreadPool();

		try {
			var clientCount = 1000;
			log("batch start.");
			prepareNewEnvironment(clientCount, 1, 1);
			log("batch prepareNewEnvironment done.");

			var loginFutures = new ArrayList<Future<?>>();
			var loginRoleIds = new Vector<Long>();
			for (var loginI = 0; loginI < clientCount; ++loginI) {
				var client = clients.get(loginI);
				int finalLoginI = loginI;
				loginFutures.add(Task.runUnsafe(() -> {
					auth(client.onLinkConnectedFuture.get(), client, "account" + finalLoginI);
					var role = getRole(client);
					var roleId = null != role ? role.getId() : createRole(client, "role" + finalLoginI);
					login(client, roleId);
					loginRoleIds.add(roleId);
				}, "login"));

				// 为了防止Task把线程全部占完，造成线程饥饿，这里每150个任务就等待完成一次。
				if ((loginI + 1) % 150 == 0) {
					for (var future : loginFutures)
						future.get();
					loginFutures.clear();
				}
			}
			for (var future : loginFutures)
				future.get();
			// ---- 当getRole出现超时时，这里的size居然是0，一个都没有登录成功！ ---
			log("batch login " + loginRoleIds.size() + " complete.");

			var server0 = servers.get(0);
			var timer0 = server0.getZeze().getTimer();
			var timerRole0 = timer0.getRoleTimer();

			for (var roleId : loginRoleIds) {
				var idSet = batchContext.computeIfAbsent(roleId, (k) -> new ConcurrentHashSet<>());
				Task.run(server0.Zeze.newProcedure(() -> {
					// 每个角色创建20个timer。
					for (var i = 0; i < 20; ++i) {
						idSet.add(i); // 本来应该事务成功，不过这个目前没有失败的，先这样。
						timerRole0.scheduleOnline(roleId, Random.getInstance().nextInt(3000), -1, -1, -1, TimerBatch.class, new ContextBatch(roleId, i));
					}
					return Procedure.Success;
				}, "scheduleOnlineN"));
			}
			if (!loginRoleIds.isEmpty())
				batchFuture.await();
			log("batch future done.");

			// 这里应该对成功login才logout，或者忽略logout结果，目前把这个错误暴露出来不忽略。
			var logoutFutures = new HashMap<Logout, TaskCompletionSource<EmptyBean>>();
			for (var client : clients) {
				var logout = new Logout();
				logoutFutures.put(logout, logout.SendForWait(client.ClientService.GetSocket(), 30_000));
			}
			for (var future : logoutFutures.entrySet()) {
				future.getValue().await();
				Assert.assertEquals(0, future.getKey().getResultCode());
			}
			log("batch logout done.");
		} finally {
			stopAll();
		}
	}

	private static final ConcurrentHashMap<Long, ConcurrentHashSet<Integer>> batchContext = new ConcurrentHashMap<>();
	private static final TaskCompletionSource<Boolean> batchFuture = new TaskCompletionSource<>();

	public static class ContextBatch extends Bean {
		private long roleId;
		private int id;

		public ContextBatch() {

		}

		public ContextBatch(long roleId, int id) {
			this.roleId = roleId;
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public long getRoleId() {
			return roleId;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong(roleId);
			bb.WriteInt(id);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			roleId = bb.ReadLong();
			id = bb.ReadInt();
		}
	}

	public static class TimerBatch implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			var ctxBean = (ContextBatch)context.customData;
			assert ctxBean != null;
			var idSet = batchContext.get(ctxBean.getRoleId());
			if (null != idSet) {
				idSet.remove(ctxBean.getId());
				if (idSet.isEmpty()) {
					batchContext.remove(ctxBean.getRoleId());
					if (batchContext.isEmpty())
						batchFuture.setResult(true);
				}
			} else if (batchContext.isEmpty())
				batchFuture.setResult(true);
		}
	}
}
