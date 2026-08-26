package Benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import ClientGame.Login.BRole;
import ClientGame.Login.CreateRole;
import ClientGame.Login.GetRoleList;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.LoginQueue.BLoginToken;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
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
import Zeze.Util.TaskSpec;
import Zezex.Linkd.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// 吞吐基准：250客户端批量登录+定时器风暴。规模本身就是存在意义，功能面已被 Zezex.TestRoleTimer 小规模版覆盖。
// 从 TestRoleTimer 整体迁出（原方法 benchRoleTimer）：归 bench 车道，避免拖慢 integrationTest。
public class BenchRoleTimer {
	private static final @NotNull Logger logger = LogManager.getLogger(BenchRoleTimer.class);

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
		for (int i = 0; i < serverCount; ++i)
			servers.get(i).Start(i + 40, 20000 + i);
		harness.TestEnv.waitServerRegistered(links.getFirst().Zeze, 40, 39 + serverCount);

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

	private void stopAll() throws Exception {
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

	private static void log(String msg) {
		logger.info("======================================== {} ========================================", msg);
	}

	private static void logout(ClientGame.App app) {
		var logout = new Logout();
		logout.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, logout.getResultCode());
	}

	private static void login(ClientGame.App app, long roleId) {
		var login = new Zeze.Builtin.Game.Online.Login();
		login.Argument.setRoleId(roleId);
		login.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, login.getResultCode());
	}

	private static void auth(BLoginToken.Data token, ClientGame.App app, String account) {
		var auth = new Auth();
		auth.Argument.setAccount(account);
		auth.Argument.setLoginQueueToken(token.getToken());
		auth.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, auth.getResultCode());
	}

	private static long createRole(ClientGame.App app, String role) {
		var createRole = new CreateRole();
		createRole.Argument.setName(role);
		createRole.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, createRole.getResultCode());
		return createRole.Result.getId();
	}

	private static BRole getRole(ClientGame.App app) {
		var get = new GetRoleList();
		get.SendForWait(app.ClientService.GetSocket(), 30_000).await();
		Assertions.assertEquals(0, get.getResultCode());
		if (get.Result.getRoleList().isEmpty())
			return null;
		return get.Result.getRoleList().getFirst();
	}

	static final ConcurrentHashMap<Long, ConcurrentHashSet<Integer>> batchContext = new ConcurrentHashMap<>();
	static final TaskCompletionSource<Boolean> batchFuture = new TaskCompletionSource<>();

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

	@Test
	public void testBenchmark() throws Exception {
		Task.tryInitThreadPool();

		try {
			var clientCount = 250;
			log("batch start.");
			prepareNewEnvironment(clientCount, 1, 1);
			log("batch prepareNewEnvironment done.");

			var loginFutures = new ArrayList<Future<?>>();
			var loginRoleIds = new Vector<Long>();
			for (var loginI = 0; loginI < clientCount; ++loginI) {
				var client = clients.get(loginI);
				int finalLoginI = loginI;
				loginFutures.add(TaskSpec.ofAction(() -> {
					auth(client.onLinkConnectedFuture.get(), client, "account" + finalLoginI);
					var role = getRole(client);
					var roleId = null != role ? role.getId() : createRole(client, "role" + finalLoginI);
					login(client, roleId);
					loginRoleIds.add(roleId);
				}).name("login").submitNow());

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

			var server0 = servers.getFirst();
			var timerRole0 = server0.getZeze().getTimer().getRoleTimer();

			for (var roleId : loginRoleIds) {
				var idSet = batchContext.computeIfAbsent(roleId, (k) -> new ConcurrentHashSet<>());
				TaskSpec.ofProcedure(server0.Zeze.newProcedure(() -> {
					// 每个角色创建timer。
					for (var i = 0; i < 10; ++i) {
						idSet.add(i); // 本来应该事务成功，不过这个目前没有失败的，先这样。
						timerRole0.scheduleOnline(roleId,
							TimerSpec.ofDelay(Random.getInstance().nextInt(1000) + 100),
							TimerBatch.class, new ContextBatch(roleId, i));
					}
					return Procedure.Success;
				}, "scheduleOnlineN")).run();
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
				Assertions.assertEquals(0, future.getKey().getResultCode());
			}
			log("batch logout done.");
		} finally {
			stopAll();
		}
	}
}
