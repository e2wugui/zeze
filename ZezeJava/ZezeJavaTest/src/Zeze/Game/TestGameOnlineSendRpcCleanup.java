package Zeze.Game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

/**
 * 验证 Online.sendOnlineRpc 发送失败（如目标角色离线）时立即原子回收 RpcContext 与超时定时器：
 * 1. sendOnlineRpc 返回 false 后 providerService.getRpcContext 立即为 null，超时后不误触 responseHandle；
 * 2. sendRpcForWait 失败返回异常 future 的同时不再滞留 RpcContext 与超时定时器。
 */
@Fast
public class TestGameOnlineSendRpcCleanup {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_GAME_ONLINE_SEND_RPC_CLEANUP);

	private static final class Env extends AppBase implements AutoCloseable {
		final Application zeze;
		final ProviderApp providerApp;
		final Online online;

		Env() throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(NEXT_SERVER_ID.getAndIncrement());
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("game_online_send_rpc_cleanup_" + conf.getServerId());
			conf.getDatabaseConfMap().put("", dbConf);
			zeze = new Application("GameOnlineSendRpcCleanup@" + conf.getServerId(), conf);
			providerApp = new ProviderApp(zeze);
			online = new Online(this);
			var provider = new ProviderWithOnline();
			provider.online = online;
			provider.onlineSetMap.put("", online);
			var implementation = ProviderApp.class.getDeclaredField("providerImplement");
			implementation.setAccessible(true);
			implementation.set(providerApp, provider);
			zeze.initialize(this);
			zeze.start();
		}

		@Override
		public Application getZeze() {
			return zeze;
		}

		@Override
		public void close() throws Exception {
			zeze.stop();
		}
	}

	@Test
	public void sendOnlineRpcFailureRemovesContextAndSuppressesTimeoutCallback() throws Exception {
		try (var env = new Env()) {
			var offlineRoleId = 999_001L;
			var callbackCount = new AtomicInteger();
			var rpc = new Login();
			rpc.Argument.setRoleId(offlineRoleId);

			var sent = env.online.sendOnlineRpc(offlineRoleId, rpc, r -> {
				callbackCount.incrementAndGet();
				return 0L;
			}, 120, true);

			assertFalse(sent, "对离线角色发送 RPC 应返回 false");
			assertNull(env.providerApp.providerService.getRpcContext(rpc.getSessionId()),
					"发送失败必须立即移除 RpcContext，不得滞留到超时");

			Thread.sleep(280);
			assertEquals(0, callbackCount.get(), "发送返回 false 后超时定时器必须已取消，不得误触 responseHandle");
			assertFalse(rpc.isTimeout(), "已取消的 RPC 不得被标记为超时");
		}
	}

	@Test
	public void sendRpcForWaitFailureRemovesContextAndCancelsTimeout() throws Exception {
		try (var env = new Env()) {
			var offlineRoleId = 999_002L;
			var rpc = new Login();
			rpc.Argument.setRoleId(offlineRoleId);

			var future = OnlineSpec.ofRole(env.online, offlineRoleId)
					.quietWhenAbsent(true)
					.timeout(120)
					.sendRpcForWait(rpc);

			assertTrue(future.isDone(), "发送失败时 future 应立即完成");
			var ex = assertThrows(CompletionException.class, future::get);
			assertInstanceOf(IllegalStateException.class, ex.getCause());
			assertNull(env.providerApp.providerService.getRpcContext(rpc.getSessionId()),
					"sendRpcForWait 发送失败必须立即移除 RpcContext");

			Thread.sleep(280);
			assertFalse(rpc.isTimeout(), "sendRpcForWait 失败后超时定时器必须已取消");
		}
	}
}
