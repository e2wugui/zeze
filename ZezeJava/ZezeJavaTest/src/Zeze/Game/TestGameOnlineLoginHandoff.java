package Zeze.Game;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Game.Online.BNotify;
import Zeze.Builtin.Game.Online.Login;
import Zeze.Builtin.Game.Online.Logout;
import Zeze.Builtin.Game.Online.ReLogin;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Builtin.Provider.Dispatch;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.FuncLong;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestGameOnlineLoginHandoff {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_GAME_ONLINE_LOGIN_HANDOFF);
	private static final String ACCOUNT = "game-handoff-account";
	private static final String LINK = "game-handoff-link";
	private static final long ROLE_ID = 101L;

	private record Snapshot(long version, long logoutVersion, int state, long sid,
						boolean hasLocal, long localVersion) {
	}

	private record Request(Protocol<?> rpc, Session session, boolean relogin) {
		long invoke(Env env) {
			return relogin ? env.online.ProcessReLoginRequest((ReLogin)rpc)
					: env.online.ProcessLoginRequest((Login)rpc);
		}

		long committedReplies() {
			return session.replies.stream().filter(reply -> reply == rpc).count();
		}
	}

	private static final class Env extends AppBase implements AutoCloseable {
		final Application zeze;
		final Online online;
		final Online otherSet;
		final SinkSocket socket;

		Env() throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setServerId(NEXT_SERVER_ID.getAndIncrement());
			conf.setTakeoverMode("off");
			conf.setDefaultTableConf(new Config.TableConf());
			var database = new Config.DatabaseConf();
			database.setDatabaseUrl("game_online_handoff_" + conf.getServerId());
			conf.getDatabaseConfMap().put("", database);
			zeze = new Application("GameOnlineHandoff" + conf.getServerId(), conf);
			var providerApp = new ProviderApp(zeze);
			online = new Online(this);
			var provider = new ProviderWithOnline();
			provider.online = online;
			provider.onlineSetMap.put("", online);
			// The lightweight ProviderApp has no implementation; install the real Game router.
			var implementation = ProviderApp.class.getDeclaredField("providerImplement");
			implementation.setAccessible(true);
			implementation.set(providerApp, provider);
			otherSet = online.createOnlineSet(this, "other");
			provider.onlineSetMap.put("other", otherSet);
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

		void run(String name, FuncLong action) {
			assertEquals(Procedure.Success, zeze.newProcedure(action, "gameHandoff." + name).call());
		}

		void seed(Online target, long version) {
			run("seed", () -> {
				var shared = target.getOrAddOnlineShared(ROLE_ID);
				shared.setAccount(ACCOUNT);
				shared.setLoginVersion(version);
				shared.setLink(new BLink(LINK, 1L, AbstractOnline.eLogined));
				target.getOrAddOnline(ROLE_ID).setServerId(zeze.getConfig().getServerId());
				var local = target._tlocal.getOrAdd(ROLE_ID);
				local.setLoginVersion(version);
				local.setLink(new BLink(LINK, 1L, AbstractOnline.eLogined));
				return Procedure.Success;
			});
		}

		Session session(long sid, String setName, boolean bound) {
			var dispatch = new Dispatch();
			dispatch.setSender(socket);
			dispatch.Argument.setAccount(ACCOUNT);
			dispatch.Argument.setLinkSid(sid);
			dispatch.Argument.setOnlineSetName(setName);
			dispatch.Argument.setContext(bound ? Long.toString(ROLE_ID) : "");
			return new Session(dispatch);
		}

		Request request(boolean relogin, long sid) {
			return request(relogin, sid, "", 0L);
		}

		Request request(boolean relogin, long sid, String setName, long confirmIndex) {
			var session = session(sid, setName, false);
			Protocol<?> rpc;
			if (relogin) {
				var request = new ReLogin();
				request.Argument.setRoleId(ROLE_ID);
				request.Argument.setOnlineSetName(setName);
				request.Argument.setReliableNotifyConfirmIndex(confirmIndex);
				rpc = request;
			} else {
				var request = new Login();
				request.Argument.setRoleId(ROLE_ID);
				request.Argument.setOnlineSetName(setName);
				rpc = request;
			}
			rpc.setSender(socket);
			rpc.setUserState(session);
			return new Request(rpc, session, relogin);
		}

		long logout(long sid) {
			var rpc = new Logout();
			rpc.setSender(socket);
			rpc.setUserState(session(sid, "", true));
			return zeze.newProcedure(() -> online.ProcessLogoutRequestOnlineSet(rpc),
					"gameHandoff.rivalLogout").call();
		}

		Snapshot snapshot(Online target) {
			var result = new OutObject<Snapshot>();
			run("snapshot", () -> {
				var shared = target.getOnlineShared(ROLE_ID);
				assertNotNull(shared);
				var local = target._tlocal.get(ROLE_ID);
				result.value = new Snapshot(shared.getLoginVersion(), shared.getLogoutVersion(),
						shared.getLink().getState(), shared.getLink().getLinkSid(),
						local != null, local != null ? local.getLoginVersion() : 0L);
				return Procedure.Success;
			});
			return requireNonNull(result.value);
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

	private static void assertCurrent(Env env, long version, long sid) {
		var snapshot = env.snapshot(env.online);
		assertEquals(version, snapshot.version());
		assertEquals(AbstractOnline.eLogined, snapshot.state());
		assertEquals(sid, snapshot.sid());
		assertTrue(snapshot.hasLocal());
		assertEquals(version, snapshot.localVersion());
	}

	private static void assertOffline(Env env, long version) {
		var snapshot = env.snapshot(env.online);
		assertEquals(version, snapshot.version());
		assertEquals(version, snapshot.logoutVersion());
		assertEquals(AbstractOnline.eOffline, snapshot.state());
		assertFalse(snapshot.hasLocal());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void freshSessionCompletesWithoutCompensatingLogout(boolean relogin) throws Exception {
		try (var env = new Env()) {
			var logouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(logouts::incrementAndGet);
				return Procedure.Success;
			});
			var request = env.request(relogin, 2L);
			assertEquals(Procedure.Success, request.invoke(env));
			assertEquals(0, logouts.get());
			assertEquals(1L, request.committedReplies());
			assertCurrent(env, 1L, 2L);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void handoffCommitsLogoutBeforeLoginEvent(boolean relogin) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var logouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(logouts::incrementAndGet);
				return Procedure.Success;
			});
			var events = relogin ? env.online.getReloginEvents() : env.online.getLoginEvents();
			events.add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				assertEquals(1, logouts.get(), "The old logout must be committed before the login event");
				return Procedure.Success;
			});
			var request = env.request(relogin, 2L);
			assertEquals(Procedure.Success, request.invoke(env));
			assertEquals(1, logouts.get());
			assertEquals(1L, request.committedReplies());
			assertCurrent(env, 2L, 2L);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void rivalLoginSurvivesSecondPhaseConflict(boolean relogin) throws Exception {
		verifySecondPhaseRival(relogin, false);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void rivalLoginThenLogoutStillSupersedesOldRequest(boolean relogin) throws Exception {
		verifySecondPhaseRival(relogin, true);
	}

	private static void verifySecondPhaseRival(boolean relogin, boolean rivalLogsOut) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var handlerThread = Thread.currentThread();
			var targetReachedLogin = new CountDownLatch(1);
			var pendingRival = new AtomicReference<@Nullable Future<Long>>();
			var compensatingLogouts = new AtomicInteger();
			var rival = env.request(false, 3L);
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == handlerThread) {
					Transaction.whileCommit(() -> {
						compensatingLogouts.incrementAndGet();
						pendingRival.set(TaskSpec.ofFunc0(() -> {
							var rc = rival.invoke(env);
							if (rc != 0 || !rivalLogsOut)
								return rc;
							return env.logout(3L);
						}).name("gameHandoff.rival").submitNow());
					});
				}
				return Procedure.Success;
			});
			EventDispatcher.EventHandle overlap = (sender, arg) -> {
				if (Thread.currentThread() == handlerThread) {
					targetReachedLogin.countDown();
					assertEquals(Procedure.Success, requireNonNull(pendingRival.get()).get(5, TimeUnit.SECONDS));
				} else
					assertTrue(targetReachedLogin.await(5, TimeUnit.SECONDS));
				return Procedure.Success;
			};
			env.online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, overlap);
			env.online.getReloginEvents().add(EventDispatcher.Mode.RunEmbed, overlap);
			var target = env.request(relogin, 2L);
			try {
				assertEquals(env.online.errorCode(Online.ResultCodeLoginSuperseded), target.invoke(env));
				assertEquals(1, compensatingLogouts.get(), "The losing request must not log out its rival");
				assertEquals(0L, target.committedReplies(), "A rolled-back login attempt must not send success");
				assertEquals(1L, rival.committedReplies());
				assertEquals(0L, requireNonNull(pendingRival.get()).get(5, TimeUnit.SECONDS));
				if (rivalLogsOut)
					assertOffline(env, 2L);
				else
					assertCurrent(env, 2L, 3L);
			} finally {
				targetReachedLogin.countDown();
				var pending = pendingRival.get();
				if (pending != null)
					pending.get(5, TimeUnit.SECONDS);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void loginFailureKeepsCommittedOldLogout(boolean relogin) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var logouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(logouts::incrementAndGet);
				return Procedure.Success;
			});
			var events = relogin ? env.online.getReloginEvents() : env.online.getLoginEvents();
			events.add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> -771L);
			var request = env.request(relogin, 2L);
			assertEquals(-771L, request.invoke(env));
			assertEquals(1, logouts.get());
			assertEquals(0L, request.committedReplies());
			assertOffline(env, 1L);
		}
	}

	@Test
	public void logoutFailureRollsBackAndDoesNotStartLogin() throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var committedLogouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(committedLogouts::incrementAndGet);
				return -772L;
			});
			env.online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				fail("Failed logout must not proceed to login");
				return Procedure.Success;
			});
			var request = env.request(false, 2L);
			assertEquals(-772L, request.invoke(env));
			assertEquals(0, committedLogouts.get());
			assertEquals(0L, request.committedReplies());
			assertCurrent(env, 1L, 1L);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void firstPhaseRedoDoesNotPublishRolledBackContinuation(boolean relogin) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var handlerThread = Thread.currentThread();
			var raced = new AtomicBoolean();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == handlerThread && raced.compareAndSet(false, true)) {
					assertEquals(Procedure.Success, TaskSpec.ofFunc0(() -> env.logout(1L))
							.name("gameHandoff.competingLogout").submitNow().get(5, TimeUnit.SECONDS));
				}
				return Procedure.Success;
			});
			var request = env.request(relogin, 2L);
			assertEquals(Procedure.Success, request.invoke(env));
			assertTrue(raced.get());
			assertEquals(1L, request.committedReplies());
			assertCurrent(env, 2L, 2L);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void firstPhaseRedoDoesNotLogOutNewGeneration(boolean relogin) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var handlerThread = Thread.currentThread();
			var raced = new AtomicBoolean();
			var rival = env.request(false, 3L);
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == handlerThread && raced.compareAndSet(false, true)) {
					assertEquals(Procedure.Success, TaskSpec.ofFunc0(() -> rival.invoke(env))
							.name("gameHandoff.firstPhaseRival").submitNow().get(5, TimeUnit.SECONDS));
				}
				return Procedure.Success;
			});
			var request = env.request(relogin, 2L);
			assertEquals(env.online.errorCode(Online.ResultCodeLoginSuperseded), request.invoke(env));
			assertTrue(raced.get());
			assertEquals(0L, request.committedReplies());
			assertEquals(1L, rival.committedReplies());
			assertCurrent(env, 2L, 3L);
		}
	}

	@Test
	public void anotherOnlineSetDoesNotSupersedeSameRole() throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var handlerThread = Thread.currentThread();
			var pendingRival = new AtomicReference<@Nullable Future<Long>>();
			var rival = env.request(false, 3L, "other", 0L);
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(() -> pendingRival.set(TaskSpec.ofFunc0(() -> rival.invoke(env))
						.name("gameHandoff.otherSet").submitNow()));
				return Procedure.Success;
			});
			env.online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == handlerThread)
					assertEquals(Procedure.Success, requireNonNull(pendingRival.get()).get(5, TimeUnit.SECONDS));
				return Procedure.Success;
			});
			var request = env.request(false, 2L);
			assertEquals(Procedure.Success, request.invoke(env));
			assertEquals(1L, request.committedReplies());
			assertEquals(1L, rival.committedReplies());
			assertCurrent(env, 2L, 2L);
			var other = env.snapshot(env.otherSet);
			assertEquals(1L, other.version());
			assertEquals(3L, other.sid());
			assertEquals(AbstractOnline.eLogined, other.state());
		}
	}

	@Test
	public void reloginRetainsUnconfirmedReliableNotifications() throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var queue = env.zeze.getQueueModule().open(
					"Zeze.Game.Online.ReliableNotifyQueue:" + ROLE_ID, BNotify.class);
			var encoded = new Binary(new byte[]{7});
			env.run("seedReliable", () -> {
				var row = env.online.getOrAddOnline(ROLE_ID);
				row.setReliableNotifyConfirmIndex(2L);
				row.setReliableNotifyIndex(3L);
				var notify = new BNotify();
				notify.setFullEncodedProtocol(encoded);
				queue.add(notify);
				return Procedure.Success;
			});
			var request = env.request(true, 2L, "", 2L);
			assertEquals(Procedure.Success, request.invoke(env));
			assertEquals(1L, request.committedReplies());
			var sync = (SReliableNotify)request.session.replies.stream()
					.filter(SReliableNotify.class::isInstance).findFirst().orElseThrow();
			assertEquals(2L, sync.Argument.getReliableNotifyIndex());
			assertEquals(List.of(encoded), new ArrayList<>(sync.Argument.getNotifies()));
			env.run("verifyReliable", () -> {
				var row = env.online.getOrAddOnline(ROLE_ID);
				assertEquals(2L, row.getReliableNotifyConfirmIndex());
				assertEquals(3L, row.getReliableNotifyIndex());
				assertEquals(1L, queue.size());
				return Procedure.Success;
			});
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	public void nestedHandlerCallRejectedBeforeMutation(boolean relogin) throws Exception {
		try (var env = new Env()) {
			env.seed(env.online, 1L);
			var request = env.request(relogin, 2L);
			env.run("nestedHandler", () -> {
				assertThrows(IllegalStateException.class, () -> request.invoke(env));
				return Procedure.Success;
			});
			assertEquals(0L, request.committedReplies());
			assertCurrent(env, 1L, 1L);
		}
	}
}
