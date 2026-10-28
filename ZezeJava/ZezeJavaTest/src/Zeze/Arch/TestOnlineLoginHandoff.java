package Zeze.Arch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.Logout;
import Zeze.Builtin.Online.ReLogin;
import Zeze.Net.Protocol;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TaskSpec;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOnlineLoginHandoff {

	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_ONLINE_LOGIN_HANDOFF);
	private static final long LOGIN_REJECTED = 123_456;

	@Test
	public void loginWithoutOldSessionCompletesDirectly() throws Exception {
		verifyDirectLogin(false);
	}

	@Test
	public void reloginWithoutOldSessionCompletesDirectly() throws Exception {
		verifyDirectLogin(true);
	}

	@Test
	public void loginCommitsOldLogoutBeforeStartingNewLogin() throws Exception {
		verifyOrdinaryHandoff(false);
	}

	@Test
	public void reloginCommitsOldLogoutBeforeStartingNewLogin() throws Exception {
		verifyOrdinaryHandoff(true);
	}

	@Test
	public void loginPreservesRivalSessionWhenCompletionTransactionRedoes() throws Exception {
		verifyRivalLogin(false, false);
	}

	@Test
	public void reloginPreservesRivalSessionWhenCompletionTransactionRedoes() throws Exception {
		verifyRivalLogin(true, false);
	}

	@Test
	public void loginContinuesWhenAnotherClientChangesAccountVersion() throws Exception {
		verifyRivalLogin(false, true);
	}

	@Test
	public void reloginContinuesWhenAnotherClientChangesAccountVersion() throws Exception {
		verifyRivalLogin(true, true);
	}

	@Test
	public void failedLoginKeepsCommittedOldLogout() throws Exception {
		verifyLoginFailure(false);
	}

	@Test
	public void failedReloginKeepsCommittedOldLogout() throws Exception {
		verifyLoginFailure(true);
	}

	@Test
	public void loginPreparationRedoDoesNotPublishAbortedLogout() throws Exception {
		verifyPreparationRedo(false);
	}

	@Test
	public void reloginPreparationRedoDoesNotPublishAbortedLogout() throws Exception {
		verifyPreparationRedo(true);
	}

	@Test
	public void loginPreparationRedoDoesNotLogOutNewerGeneration() throws Exception {
		verifyPreparationSuperseded(false);
	}

	@Test
	public void reloginPreparationRedoDoesNotLogOutNewerGeneration() throws Exception {
		verifyPreparationSuperseded(true);
	}

	@Test
	public void loginRejectsNestedTransactionBeforeChangingSession() throws Exception {
		verifyNestedRejection(false, false);
	}

	@Test
	public void reloginRejectsNestedTransactionBeforeChangingSession() throws Exception {
		verifyNestedRejection(true, false);
	}

	@Test
	public void loginRejectsCommitCallbackBeforeChangingSession() throws Exception {
		verifyNestedRejection(false, true);
	}

	@Test
	public void reloginRejectsCommitCallbackBeforeChangingSession() throws Exception {
		verifyNestedRejection(true, true);
	}

	private static void verifyDirectLogin(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			var logouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				logouts.incrementAndGet();
				return Procedure.Success;
			});
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			assertEquals(Procedure.Success, invoke(env, rpc));
			assertEquals(0, logouts.get());
			assertEquals(1, env.online.getLoginTimes());
			assertEquals(1, session.replies.stream().filter(reply -> reply == rpc).count());
			assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 1, 2);
		}
	}

	private static void verifyOrdinaryHandoff(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var committedLogouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(committedLogouts::incrementAndGet);
				return Procedure.Success;
			});
			loginEvents(env, relogin).add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				assertEquals(1, committedLogouts.get(), "Old logout must commit before the new login event");
				return Procedure.Success;
			});
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			assertEquals(Procedure.Success, invoke(env, rpc));
			assertEquals(1, committedLogouts.get());
			assertEquals(1, env.online.getLoginTimes());
			assertEquals(1, session.replies.stream().filter(reply -> reply == rpc).count());
			assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 2, 2);
		}
	}

	private static void verifyRivalLogin(boolean relogin, boolean anotherClient) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var targetThread = Thread.currentThread();
			var committedLogouts = new AtomicInteger();
			var completionAttempts = new AtomicInteger();
			var enterCompletion = new CountDownLatch(1);
			var rivalTask = new AtomicReference<@Nullable Future<Long>>();
			var rivalClient = anotherClient ? "another-client" : ArchOnlineTestEnv.CLIENT_ID;
			var rivalRpc = request(env, env.session(3, ""), false, rivalClient);
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == targetThread) {
					Transaction.whileCommit(() -> {
						committedLogouts.incrementAndGet();
						rivalTask.set(TaskSpec.ofFunc0(() -> {
							assertTrue(enterCompletion.await(5, TimeUnit.SECONDS));
							return invoke(env, rivalRpc);
						}).name("ArchOnlineHandoff.rival").submitNow());
					});
				}
				return Procedure.Success;
			});
			loginEvents(env, relogin).add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == targetThread) {
					completionAttempts.incrementAndGet();
					// The target already read and changed the account row, but owns no commit locks.
					// Let a real rival write commit, forcing the target's completion transaction to redo.
					enterCompletion.countDown();
					var rival = rivalTask.get();
					assertNotNull(rival);
					assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
				}
				return Procedure.Success;
			});
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			try {
				var result = invoke(env, rpc);
				assertEquals(1, committedLogouts.get(), "A request must not log out the rival that superseded it");
				if (anotherClient) {
					assertEquals(Procedure.Success, result);
					assertTrue(completionAttempts.get() >= 2, "The completion must genuinely redo after the rival write");
					assertEquals(1, session.replies.stream().filter(reply -> reply == rpc).count());
					assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 3, 2);
					assertActive(env, rivalClient, 2, 3);
					assertEquals(2, env.online.getLoginTimes());
				} else {
					assertEquals(env.online.errorCode(Online.ResultCodeLoginSuperseded), result);
					assertEquals(1, completionAttempts.get(), "The redo must stop before invoking another login event");
					assertTrue(session.replies.isEmpty(), "Aborted completion must not publish a success response");
					assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 2, 3);
					assertEquals(1, env.online.getLoginTimes());
				}
			} finally {
				enterCompletion.countDown();
				var rival = rivalTask.get();
				if (rival != null)
					assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
			}
		}
	}

	private static void verifyLoginFailure(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var committedLogouts = new AtomicInteger();
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				Transaction.whileCommit(committedLogouts::incrementAndGet);
				return Procedure.Success;
			});
			loginEvents(env, relogin).add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> LOGIN_REJECTED);
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			assertEquals(LOGIN_REJECTED, invoke(env, rpc));
			assertEquals(1, committedLogouts.get());
			assertEquals(0, env.online.getLoginTimes());
			assertTrue(session.replies.isEmpty());
			env.run("verifyCommittedLogoutAfterLoginFailure", () -> {
				var account = env.online.getOnline(ArchOnlineTestEnv.ACCOUNT);
				assertNotNull(account);
				var login = account.getLogins().get(ArchOnlineTestEnv.CLIENT_ID);
				assertNotNull(login);
				assertEquals(1, login.getLoginVersion());
				assertEquals(1, login.getLogoutVersion());
				assertEquals(AbstractOnline.eOffline, login.getLink().getState());
				assertNull(env.online._tlocal.get(ArchOnlineTestEnv.ACCOUNT));
				return Procedure.Success;
			});
		}
	}

	private static void verifyPreparationRedo(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var targetThread = Thread.currentThread();
			var compete = new AtomicBoolean(true);
			var committedTargetLogouts = new AtomicInteger();
			var rivalLogout = env.attach(new Logout(), env.session(1, ArchOnlineTestEnv.CLIENT_ID));
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == targetThread) {
					Transaction.whileCommit(committedTargetLogouts::incrementAndGet);
					if (compete.getAndSet(false)) {
						var rival = TaskSpec.ofFunc0(() -> env.zeze.newProcedure(
								() -> env.online.ProcessLogoutRequest(rivalLogout), "ArchOnlineHandoff.rivalLogout").call())
								.submitNow();
						assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
					}
				}
				return Procedure.Success;
			});
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			assertEquals(Procedure.Success, invoke(env, rpc),
					"A preparation that redoes into a direct login must not retain an aborted logout continuation");
			assertFalse(compete.get(), "The rival logout must have been arranged");
			assertEquals(0, committedTargetLogouts.get(), "Only the rival logout committed");
			assertEquals(1, session.replies.stream().filter(reply -> reply == rpc).count());
			assertEquals(1, env.online.getLoginTimes());
			assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 2, 2);
		}
	}

	private static void verifyPreparationSuperseded(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var targetThread = Thread.currentThread();
			var compete = new AtomicBoolean(true);
			var committedTargetLogouts = new AtomicInteger();
			var rivalLogin = request(env, env.session(3, ""), false, ArchOnlineTestEnv.CLIENT_ID);
			env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == targetThread) {
					Transaction.whileCommit(committedTargetLogouts::incrementAndGet);
					if (compete.getAndSet(false)) {
						var rival = TaskSpec.ofFunc0(() -> invoke(env, rivalLogin)).submitNow();
						assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
					}
				}
				return Procedure.Success;
			});
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			assertEquals(env.online.errorCode(Online.ResultCodeLoginSuperseded), invoke(env, rpc));
			assertFalse(compete.get());
			assertEquals(0, committedTargetLogouts.get(), "The preparation redo must preserve the rival generation");
			assertTrue(session.replies.isEmpty());
			assertEquals(1, env.online.getLoginTimes());
			assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 2, 3);
		}
	}

	private static void verifyNestedRejection(boolean relogin, boolean commitCallback) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1, 0, 0);
			var session = env.session(2, "");
			var rpc = request(env, session, relogin, ArchOnlineTestEnv.CLIENT_ID);
			env.run("rejectNestedOnlineHandoff", () -> {
				if (commitCallback)
					Transaction.whileCommit(() -> assertThrows(IllegalStateException.class, () -> invoke(env, rpc)));
				else
					assertThrows(IllegalStateException.class, () -> invoke(env, rpc));
				return Procedure.Success;
			});
			assertTrue(session.replies.isEmpty());
			assertEquals(0, env.online.getLoginTimes());
			assertActive(env, ArchOnlineTestEnv.CLIENT_ID, 1, 1);
		}
	}

	private static EventDispatcher loginEvents(ArchOnlineTestEnv env, boolean relogin) {
		return relogin ? env.online.getReloginEvents() : env.online.getLoginEvents();
	}

	private static Protocol<?> request(ArchOnlineTestEnv env, ArchOnlineTestEnv.Session session,
			boolean relogin, String clientId) {
		if (relogin) {
			var rpc = env.attach(new ReLogin(), session);
			rpc.Argument.setClientId(clientId);
			return rpc;
		}
		var rpc = env.attach(new Login(), session);
		rpc.Argument.setClientId(clientId);
		return rpc;
	}

	private static long invoke(ArchOnlineTestEnv env, Protocol<?> rpc) {
		return rpc instanceof Login login
				? env.online.ProcessLoginRequest(login)
				: env.online.ProcessReLoginRequest((ReLogin)rpc);
	}

	private static void assertActive(ArchOnlineTestEnv env, String clientId, long version, long linkSid) {
		env.run("verifyActiveLoginHandoff", () -> {
			var login = env.online.getLogin(ArchOnlineTestEnv.ACCOUNT, clientId);
			assertNotNull(login);
			assertEquals(version, login.getLoginVersion());
			assertEquals(AbstractOnline.eLogined, login.getLink().getState());
			assertEquals(ArchOnlineTestEnv.LINK_NAME, login.getLink().getLinkName());
			assertEquals(linkSid, login.getLink().getLinkSid());
			var local = env.online._tlocal.get(ArchOnlineTestEnv.ACCOUNT);
			assertNotNull(local);
			var localLogin = local.getLogins().get(clientId);
			assertNotNull(localLogin);
			assertEquals(version, localLogin.getLoginVersion());
			return Procedure.Success;
		});
	}
}
