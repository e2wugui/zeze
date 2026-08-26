package Zezex;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Services.LoginQueue;
import Zezex.Linkd.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

/**
 * Zezex 系测试的进程内组网脚手架：clients/links/servers 集合与 LoginQueue 的启停、全链路 RPC 助手。
 * 消费方：TestRoleTimer、Benchmark.BenchRoleTimer（提取自两者的重复副本）。
 * TODO: TestGameTimer/TestOnlineSpec 另有分叉副本（websocket 拨段、roleCount、半启动只收集成功者等），
 * 	待本类长出对应钩子后再迁入，暂不强行统一。
 */
public final class ZezexTestEnv {
	private static final @NotNull Logger logger = LogManager.getLogger(ZezexTestEnv.class);

	public final ArrayList<ClientGame.App> clients = new ArrayList<>();
	public final ArrayList<Zezex.App> links = new ArrayList<>();
	public final ArrayList<Game.App> servers = new ArrayList<>();

	private LoginQueue loginQueue;

	public void prepareNewEnvironment(int clientCount, int linkCount, int serverCount) throws Exception {
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
		for (int i = 0; i < serverCount; ++i)
			servers.get(i).Start(i + 40, 20000 + i);
		for (var link : links)
			harness.TestEnv.waitServerRegistered(link.Zeze, 40, 39 + serverCount); // 等所有provider注册可见（替代盲等1秒） // 等所有provider注册可见（替代盲等1秒）

		var clientsSize = new AtomicInteger(clients.size());
		clients.parallelStream().forEach(c -> {
			try {
				c.Start("", 0); // 启用了LoginQueue以后，link参数不再使用。
				clientsSize.decrementAndGet();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		while (clientsSize.get() != 0)
			Thread.onSpinWait();
	}

	public void stopAll() throws Exception {
		try {
			for (var client : clients)
				client.Stop();
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
			clients.clear();
			links.clear();
			servers.clear();
		}
	}

	public static void auth(BLoginToken.Data token, ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.Argument.setLoginQueueToken(token.getToken());
		auth.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, auth.getResultCode());
	}

	public static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().getFirst();
	}

	public static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	public static void login(ClientGame.App app, long roleId) {
		var login = new Zeze.Builtin.Game.Online.Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, login.getResultCode());
	}

	public static void logout(ClientGame.App app, @SuppressWarnings("unused") long roleIdForLogOnly) {
		var logout = new Logout();
		logout.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, logout.getResultCode());
	}
}
