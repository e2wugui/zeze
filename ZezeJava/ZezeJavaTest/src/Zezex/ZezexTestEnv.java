package Zezex;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Services.LoginQueue;
import Zeze.Transaction.DatabaseMemory;
import Zezex.Linkd.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

/**
 * Zezex 系测试的进程内组网脚手架：clients/links/servers 集合与 LoginQueue 的启停、全链路 RPC 助手。
 * 消费方：TestRoleTimer、TestGameTimer、Benchmark.BenchRoleTimer、TestOnline、TestOnlineSpec。
 * 支持两种用法：
 * 1. 方法级：prepareNewEnvironment(clientCount,...) + stopAll()，每方法独立环境（TestOnline/TestOnlineSpec/BenchRoleTimer）。
 * 2. 类级共享：@BeforeAll prepareNewEnvironment(0,...)（只起 links/servers），每方法 rebuildClients(count, mode)
 *    重建客户端（provider 常驻上报后 token 秒回，client 重建 ~百ms）；方法失败 markDirty()，下一方法
 *    检测 isDirty() 后整体 stopAll+prepare 重建，保留失败隔离（TestRoleTimer/TestGameTimer）。
 */
public final class ZezexTestEnv {
	private static final @NotNull Logger logger = LogManager.getLogger(ZezexTestEnv.class);

	/** 客户端拨号口味：TCP 走 ClientService 直连；WEBSOCKET 走 Start2 的 ws 拨号（linkd 端口+10000）。 */
	public enum ClientStartMode { TCP, WEBSOCKET }

	public final ArrayList<ClientGame.App> clients = new ArrayList<>();
	public final ArrayList<Zezex.App> links = new ArrayList<>();
	public final ArrayList<Game.App> servers = new ArrayList<>();

	private LoginQueue loginQueue;
	private boolean dirty; // 类级共享用法：上一方法失败时置位，下一方法据此整体重建环境

	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
	}

	public boolean isPrepared() {
		return loginQueue != null;
	}

	public void prepareNewEnvironment(int clientCount, int linkCount, int serverCount) throws Exception {
		prepareNewEnvironment(clientCount, linkCount, serverCount, 40);
	}

	// serverIdBase：server 的 serverId 起点（provider 端口不受影响，仍从 20000 起排），默认 40；TestOnline 等旧组网用 50。
	public void prepareNewEnvironment(int clientCount, int linkCount, int serverCount, int serverIdBase) throws Exception {
		clients.clear();
		links.clear();
		servers.clear();

		// DatabaseMemory 是 JVM 级全局静态库（server.xml 的 Memory 库 DatabaseUrl 均为空串，全 JVM 共一个），
		// 不 clear 会跨环境残留三个全局键：角色名（_trolename 全局唯一索引）、命名 timerId、Online.Shared 登录状态。
		// 残留表现为：本环境 getRole 拿到上个环境的旧角色、createRole 撞全局角色名、scheduleOnlineNamed 对
		// shared 在线的遗留角色走 "not online but transmit" 假成功。clear() 把"每个环境一个干净的库"显式化。
		// integrationTest 串行执行无并发库使用者（先例：Infinite.Simulate 循环 clear）。
		DatabaseMemory.clear();

		loginQueue = new LoginQueue();
		loginQueue.start();

		// 客户端实例由 startClients 统一创建（调用前 clients 已 clear），此处不再预创建，
		// 否则 bench 的 250 客户端会翻倍成 500：多出的 250 个幽灵客户端并发抢占 LoginQueue 的
		// accept backlog 与 token，真实客户端等不到 token 卡死在 onLinkConnectedFuture.get()。
		for (int i = 0; i < linkCount; ++i)
			links.add(new Zezex.App());
		for (int i = 0; i < serverCount; ++i)
			servers.add(new Game.App());

		for (int i = 0; i < linkCount; ++i)
			links.get(i).Start(-(i + 1), 12000 + i, 15000 + i);
		for (int i = 0; i < serverCount; ++i)
			servers.get(i).Start(serverIdBase + i, 20000 + i);
		for (var link : links)
			harness.TestEnv.waitServerRegisteredRange(link.Zeze, serverIdBase, serverCount); // 等所有provider注册可见（替代盲等1秒）

		startClients(clientCount, ClientStartMode.TCP);
	}

	/** 类级共享用法：停掉旧客户端并按指定口味重建（环境 links/servers/LoginQueue 不动）。 */
	public void rebuildClients(int clientCount, ClientStartMode mode) throws Exception {
		stopClients();
		startClients(clientCount, mode);
	}

	public void stopClients() throws Exception {
		for (var client : clients)
			client.Stop();
		clients.clear();
	}

	private void startClients(int clientCount, ClientStartMode mode) throws InterruptedException {
		for (int i = 0; i < clientCount; ++i)
			clients.add(new ClientGame.App());
		var clientsSize = new AtomicInteger(clients.size());
		// 分批错峰拨号：全部并发 connect LoginQueue(5020) 会撞 Windows listen backlog 溢出被 RST
		// （Connection refused），而 LoginQueueClient 是一次性服务不重连，被拒客户端永远拿不到 token。
		// 每批之间留出 accept 消化间隙即可避免。
		final var dialBatchSize = 25;
		for (var begin = 0; begin < clients.size(); begin += dialBatchSize) {
			int from = begin, to = Math.min(begin + dialBatchSize, clients.size());
			java.util.stream.IntStream.range(from, to).parallel().forEach(i -> {
				try {
					startClient(clients.get(i), i, mode);
					clientsSize.decrementAndGet();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			if (to < clients.size())
				Thread.sleep(30);
		}
		while (clientsSize.get() != 0)
			Thread.onSpinWait();
	}

	private void startClient(ClientGame.App c, int index, ClientStartMode mode) throws Exception {
		if (mode == ClientStartMode.WEBSOCKET) {
			// Start2 的 wsUrl 实参实际未被使用（token 回调内按 LoginQueue 下发的 link 地址重建），此处构造仅为表意
			var ipPort = links.get(index % links.size()).LinkdService.getOnePassiveAddress();
			c.Start2("ws://" + ipPort.getKey() + ":" + (ipPort.getValue() + 10000) + "/websocket");
		} else {
			c.Start("", 0); // 启用了LoginQueue以后，link参数不再使用。
		}
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
			dirty = false;
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
