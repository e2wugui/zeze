package Benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// 吞吐基准：250客户端批量登录+定时器风暴。规模本身就是存在意义，功能面已被 Zezex.TestRoleTimer 小规模版覆盖。
// 从 TestRoleTimer 整体迁出（原方法 benchRoleTimer）：归 bench 车道，避免拖慢 integrationTest。
// 组网与RPC复用 Zezex.ZezexTestEnv 脚手架。
public class BenchRoleTimer {
	private static final @NotNull Logger logger = LogManager.getLogger(BenchRoleTimer.class);

	private final Zezex.ZezexTestEnv env = new Zezex.ZezexTestEnv();

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

	private static void log(String msg) {
		logger.info("======================================== {} ========================================", msg);
	}

		@Test
		public void testBenchmark() throws Exception {
			Task.tryInitThreadPool();

			try {
				var clientCount = 250;
				log("batch start.");
				env.prepareNewEnvironment(clientCount, 1, 1);
				log("batch prepareNewEnvironment done.");

				var loginFutures = new ArrayList<Future<?>>();
				var loginRoleIds = new Vector<Long>();
				for (var loginI = 0; loginI < clientCount; ++loginI) {
					var client = env.clients.get(loginI);
					int finalLoginI = loginI;
					loginFutures.add(TaskSpec.ofAction(() -> {
						Zezex.ZezexTestEnv.auth(client.onLinkConnectedFuture.get(), client, "account" + finalLoginI);
						var role = Zezex.ZezexTestEnv.getRole(client);
						var roleId = null != role ? role.getId() : Zezex.ZezexTestEnv.createRole(client, "role" + finalLoginI);
						Zezex.ZezexTestEnv.login(client, roleId);
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

				var server0 = env.servers.getFirst();
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
				for (var client : env.clients) {
					var logout = new Logout();
					logoutFutures.put(logout, logout.SendForWait(client.ClientService.GetSocket(), 30_000));
				}
				for (var future : logoutFutures.entrySet()) {
					future.getValue().await();
					Assertions.assertEquals(0, future.getKey().getResultCode());
				}
				log("batch logout done.");
			} finally {
				env.stopAll();
			}
		}
	}
