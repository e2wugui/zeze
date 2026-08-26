package Zezex;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import ClientZezex.Linkd.Cs;
import UnitTest.Zeze.Component.TestBean;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Net.Protocol;
import Zeze.Services.LoginQueue;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zezex.Linkd.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("CallToPrintStackTrace")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestGameTimer {
	private static final @NotNull Logger logger = LogManager.getLogger();
	final ArrayList<ClientGame.App> clients = new ArrayList<>();
	final ArrayList<Zezex.App> links = new ArrayList<>();
	final ArrayList<Game.App> servers = new ArrayList<>();
	LoginQueue loginQueue;

	@SuppressWarnings({"SameParameterValue", "unused"})
	private void prepareNewEnvironment(int clientCount, int linkCount, int serverCount, int roleCount) throws Exception {
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
		for (int i = 40; i < serverCount + 40; ++i) {
			servers.get(i - 40).Start(i, 20000 + i - 40);
			//servers.get(i - 40).getZeze().getTimer().initializeOnlineTimer(servers.get(i - 40).ProviderApp);
			//servers.get(i - 40).getZeze().getTimer().start();
		}
		for (var link : links)
			harness.TestEnv.waitServerRegistered(link.Zeze, 40, 39 + serverCount); // 等所有provider注册可见（100ms就绪轮询）
		for (int i = 0; i < clientCount; ++i) {
			var link = links.get(i % linkCount);
			var ipPort = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start(ipPort.getKey(), ipPort.getValue());
		}
		/*
		var req = new Cs();
		req.Argument.setAccount("Request");
		req.Send(clients.get(0).ClientService.GetSocket());
		*/
		HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
				URI.create("ws://127.0.0.1:" + 22000 + "/websocket"), new WebSocket.Listener() {
					@Override
					public void onOpen(WebSocket webSocket) {
						webSocket.request(1);
						var cs = new Cs();
						cs.Argument.setAccount("RequestWeb");
						var bb = cs.encode();
						var buf = ByteBuffer.wrap(bb.Bytes, bb.ReadIndex, bb.size());
						webSocket.sendBinary(buf, true);
						logger.info("Cs Web {}", cs.Argument);
					}

					final Zeze.Serialize.ByteBuffer input = Zeze.Serialize.ByteBuffer.Allocate();

					@Override
					public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
						webSocket.request(1);
						var n = data.remaining();
						input.EnsureWrite(n);
						data.get(input.Bytes, input.WriteIndex, n);
						input.WriteIndex += n;
						if (last) {
							var sc = Protocol.decode(clients.getFirst().ClientService, input);
							logger.info("Sc Web {}", sc != null ? sc.Argument : null);
							input.Compact();
						}
						return null;
					}
				});
	}

	@SuppressWarnings({"unused", "SameParameterValue"})
	private void prepareNewEnvironment2(int clientCount, int linkCount, int serverCount, int roleCount) throws Exception {
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
		for (int i = 40; i < serverCount + 40; ++i) {
			servers.get(i - 40).Start(i, 20000 + i - 40);
			//servers.get(i - 40).getZeze().getTimer().initializeOnlineTimer(servers.get(i - 40).ProviderApp);
			//servers.get(i - 40).getZeze().getTimer().start();
		}
		for (var link : links)
			harness.TestEnv.waitServerRegistered(link.Zeze, 40, 39 + serverCount); // 等所有provider注册可见（100ms就绪轮询）
		for (int i = 0; i < clientCount; ++i) {
			var link = links.get(i % linkCount);
			var ipPort = link.LinkdService.getOnePassiveAddress();
			clients.get(i).Start2("ws://" + ipPort.getKey() + ":" + (ipPort.getValue() + 10000) + "/websocket");
		}
	}

	private void stopAll() throws Exception {
		try {
			for (var client : clients)
				client.Stop();
			Thread.sleep(100); // 防止client断开连接的时候，下面的provider关闭太快执行异常。这个异常实际上无所谓。
			for (var server : servers) {
				if (server.Zeze != null) // 半启动（Start 中途失败/未调用）的 server，跳过避免 NPE 掩盖真正的失败原因
					server.stopBeforeModules();
			}
			for (var server : servers) {
				if (server.Zeze != null)
					server.Stop();
			}
			for (var link : links) {
				try {
					link.Stop();
				} catch (Exception e) {
					logger.error("stop link failed", e);
				}
			}
		} finally {
			// 无论如何都要释放 5020/5021 端口，否则会毒化后续使用 LoginQueue 的测试
			if (loginQueue != null) {
				loginQueue.stop();
				loginQueue = null;
			}
		}
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
			prepareNewEnvironment(2, 2, 1, 2);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
//			var link0 = links.get(0);
//			var link1 = links.get(1);
			var server0 = servers.getFirst();
//			var server1 = servers.get(1);
			var timer0 = server0.getZeze().getTimer();
//			var timer1 = server1.getZeze().getTimer();

			log("测试 Role Online Timer ");
			log("在客户端0登录role0");
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			var timerRole0 = timer0.getRoleTimer();
//			var timerRole1 = timer1.getRoleTimer();

			TestBean bean = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOnline(roleId, 1, 1, 5, System.currentTimeMillis() + 2000, TestOnlineTimerHandle.class, bean);
				timerRole0.scheduleOnline(roleId, TimerSpec.ofDelay(1).period(1).times(5).endTime(System.currentTimeMillis() + 2000),
					TestRoleTimer.TestOnlineTimerHandle.class, bean);
				return Procedure.Success;
			}, "testOnlineWithBean").call());
			sleep(100, 6);
			Assertions.assertTrue(bean.getTestValue() >= 5);
			log("测试一通过");

			log("在客户端1登录role0，踢掉客户端0的登录");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);
			sleep(100, 6);
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
			sleep(100, 10);

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
			sleep(100, 11);
			Assertions.assertTrue(newNamedBean2.getTestValue() >= 10);
			log("测试三通过");
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		} finally {
			stopAll();
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

			// 初始化环境
			prepareNewEnvironment2(2, 2, 2, 2);

			var client0 = clients.get(0);
			var client1 = clients.get(1);
//			var link0 = links.get(0);
//			var link1 = links.get(1);
			var server0 = servers.getFirst();
//			var server1 = servers.get(1);
			var timer0 = server0.getZeze().getTimer();
//			var timer1 = server1.getZeze().getTimer();

			var timerRole0 = timer0.getRoleTimer();
//			var timerRole1 = timer1.getRoleTimer();

			// 注册登录客户端0
			log("注册登录客户端0");
			auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role = getRole(client0);
			var roleId = null != role ? role.getId() : createRole(client0, "role0");
			login(client0, roleId);

			sleep(200, 1);

			// 角色下线时注册定时器
			logout(client0, roleId);

			TestBean bean = new TestBean();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				//timerRole0.scheduleOffline(roleId, 100, 100, 10, System.currentTimeMillis() + 5000, TestOfflineTimerHandle.class, bean);
				timerRole0.scheduleOffline(roleId, TimerSpec.ofDelay(100).period(100).times(10).endTime(System.currentTimeMillis() + 5000),
					TestOfflineTimerHandle.class, bean);
				return Procedure.Success;
			}, "test1").call());

			sleep(100, 5);

			// 注册登录客户端1，踢掉客户端0的登录
			log("注册登录客户端1");
			auth(client1.onLinkConnectedFuture.get(), client1, "account0");
			login(client1, roleId);

			sleep(100, 5);
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		} finally {
			stopAll();
		}
	}

	@SuppressWarnings("unused")
	private static void relogin(ClientGame.App app, long roleId) {
		var relogin = new Zeze.Builtin.Game.Online.ReLogin();
		relogin.Argument.setRoleId(roleId);
		relogin.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, relogin.getResultCode());
	}

	@SuppressWarnings("unused")
	private static void logout(ClientGame.App app, long roleIdForLogOnly) {
		var logout = new Zeze.Builtin.Game.Online.Logout();
		logout.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Zeze.Builtin.Game.Online.Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, login.getResultCode());
	}

	@SuppressWarnings("SameParameterValue")
	private static void auth(BLoginToken.Data token, ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.Argument.setLoginQueueToken(token.getToken());
		auth.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, auth.getResultCode());
	}

	@SuppressWarnings("SameParameterValue")
	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket(), 10_000).await();
		Assertions.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().getFirst();
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
