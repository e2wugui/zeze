package Zeze.Game;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 验证跨服重登（当前服 _tlocal 中不存在该角色、角色原登录在另一 serverId）时：
 * 第一阶段补登出不得在当前服提前创建空 BLocal，也不得在当前服误触发 localRemoveEvents；
 * 第二阶段正式登录后 _tlocal 版本号与 verifyLocalCount 精确为新版本和 1。
 */
@Fast
public class TestGameOnlineCrossServerLoginLocalRemove {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_GAME_ONLINE_CROSS_SERVER_LOGIN_LOCAL_REMOVE);
	private static final String ACCOUNT = "cross-server-account";
	private static final String LINK = "cross-server-link";
	private static final long ROLE_ID = 202L;

	private static final class Env extends AppBase implements AutoCloseable {
		final Application zeze;
		final Online online;
		final SinkSocket socket;

		Env() throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(NEXT_SERVER_ID.getAndIncrement());
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("game_online_cross_server_local_" + conf.getServerId());
			conf.getDatabaseConfMap().put("", dbConf);
			zeze = new Application("GameOnlineCrossServerLocal@" + conf.getServerId(), conf);
			var providerApp = new ProviderApp(zeze);
			online = new Online(this);
			var provider = new ProviderWithOnline();
			provider.online = online;
			provider.onlineSetMap.put("", online);
			var implementation = ProviderApp.class.getDeclaredField("providerImplement");
			implementation.setAccessible(true);
			implementation.set(providerApp, provider);
			socket = new SinkSocket(providerApp.providerService);
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

		Protocol<?> buildRequest(boolean relogin, long sid) {
			var dispatch = new Dispatch();
			dispatch.setSender(socket);
			dispatch.Argument.setAccount(ACCOUNT);
			dispatch.Argument.setLinkSid(sid);
			dispatch.Argument.setOnlineSetName("");
			dispatch.Argument.setContext("");
			var session = new Session(dispatch);
			Protocol<?> rpc;
			if (relogin) {
				var req = new ReLogin();
				req.Argument.setRoleId(ROLE_ID);
				req.Argument.setOnlineSetName("");
				rpc = req;
			} else {
				var req = new Login();
				req.Argument.setRoleId(ROLE_ID);
				req.Argument.setOnlineSetName("");
				rpc = req;
			}
			rpc.setSender(socket);
			rpc.setUserState(session);
			return rpc;
		}
	}

	private static final class Session extends ProviderUserSession {
		final List<Protocol<?>> replies = new ArrayList<>();

		Session(Dispatch dispatch) {
			super(dispatch);
		}

		@Override
		public String getLinkName() {
			return LINK;
		}

		@Override
		public void respond(Protocol<?> protocol) {
			Task.runTxnAware(() -> replies.add(protocol));
		}
	}

	private static final class SinkSocket extends AsyncSocket {
		private final Connector connector = new Connector("127.0.0.1", 1) {
			@Override
			public String getName() {
				return LINK;
			}
		};

		SinkSocket(Zeze.Arch.ProviderService service) {
			super(service);
		}

		@Override
		public Connector getConnector() {
			return connector;
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			return true;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		protected void doClose(@Nullable Throwable error, boolean gracefully) {
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void crossServerReloginDoesNotFireSpuriousLocalRemoveOnTargetServer(boolean relogin) throws Exception {
		try (var env = new Env()) {
			var otherServerId = env.zeze.getConfig().getServerId() + 10000;
			// 仅写入共享在线表（模拟角色在另一台服登录），当前服 _tlocal 无记录
			assertEquals(Procedure.Success, env.zeze.newProcedure(() -> {
				var shared = env.online.getOrAddOnlineShared(ROLE_ID);
				shared.setAccount(ACCOUNT);
				shared.setLoginVersion(1L);
				shared.setLink(new BLink(LINK, 1L, AbstractOnline.eLogined));
				env.online.getOrAddOnline(ROLE_ID).setServerId(otherServerId);
				return Procedure.Success;
			}, "seedRemoteLogin").call());

			var localRemoveCount = new AtomicInteger();
			var logoutCount = new AtomicInteger();
			env.online.getLocalRemoveEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				localRemoveCount.incrementAndGet();
				return Procedure.Success;
			});
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(logoutCount::incrementAndGet);
				return Procedure.Success;
			});

			var rpc = env.buildRequest(relogin, 2L);
			var rc = relogin ? env.online.ProcessReLoginRequest((ReLogin)rpc)
					: env.online.ProcessLoginRequest((Login)rpc);
			assertEquals(Procedure.Success, rc);
			assertEquals(1, logoutCount.get(), "跨服重登应完成一阶段补登出");
			assertEquals(0, localRemoveCount.get(), "当前服原本无 _tlocal 记录，不得误触发 localRemoveEvents");
			assertEquals(1, env.online.getLocalCount(), "正式登录后 verifyLocalCount 应精确为 1");

			assertEquals(Procedure.Success, env.zeze.newProcedure(() -> {
				var local = env.online._tlocal.get(ROLE_ID);
				assertNotNull(local);
				assertEquals(2L, local.getLoginVersion());
				var shared = requireNonNull(env.online.getOnlineShared(ROLE_ID));
				assertEquals(2L, shared.getLoginVersion());
				return Procedure.Success;
			}, "verifyCrossServerLogin").call());
		}
	}
}
