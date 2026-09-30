package Zeze.Arch;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.ReLogin;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.EventDispatcher;
import Zeze.Util.TaskSpec;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestOnlineLoginRetryLimit {
	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_ONLINE_LOGIN_RETRY_LIMIT);

	@Test
	public void loginStopsAfterRepeatedCompetingLogins() throws Exception {
		verifyCompetingLoginBound(false);
	}

	@Test
	public void reloginStopsAfterRepeatedCompetingLogins() throws Exception {
		verifyCompetingLoginBound(true);
	}

	private static void verifyCompetingLoginBound(boolean relogin) throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1L, 0L, 0L);
			var handlerThread = Thread.currentThread();
			var compensatingLogouts = new AtomicInteger();
			var pendingRival = new AtomicReference<Future<Long>>();
			var rivalLogin = env.attach(new Login(), env.session(1L, ""));
			rivalLogin.Argument.setClientId(ArchOnlineTestEnv.CLIENT_ID);
			var logoutHook = env.online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (sender, arg) -> {
				if (Thread.currentThread() == handlerThread) {
					Transaction.whileCommit(() -> {
						// A finite rival storm: old unbounded handlers eventually succeed after
						// the fourth rival, so a regression fails without leaving a runaway task.
						if (compensatingLogouts.incrementAndGet() <= 4)
							pendingRival.set(TaskSpec.ofFunc0(() -> env.online.ProcessLoginRequest(rivalLogin))
									.name("competingOnlineLogin").submitNow());
					});
				}
				return Procedure.Success;
			});
			EventDispatcher.EventHandle waitForRival = (sender, arg) -> {
				if (Thread.currentThread() == handlerThread) {
					// If the next target transaction reads Offline before the rival commits,
					// let the rival finish while this transaction has not acquired commit locks.
					// Its genuine write conflict forces a redo into the compensating logout branch.
					var rival = pendingRival.get();
					if (rival != null)
						assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
				}
				return Procedure.Success;
			};
			var loginHook = env.online.getLoginEvents().add(EventDispatcher.Mode.RunEmbed, waitForRival);
			var reloginHook = env.online.getReloginEvents().add(EventDispatcher.Mode.RunEmbed, waitForRival);
			try {
				long result;
				if (relogin) {
					var rpc = env.attach(new ReLogin(), env.session(2L, ""));
					rpc.Argument.setClientId(ArchOnlineTestEnv.CLIENT_ID);
					result = env.online.ProcessReLoginRequest(rpc);
				} else {
					var rpc = env.attach(new Login(), env.session(2L, ""));
					rpc.Argument.setClientId(ArchOnlineTestEnv.CLIENT_ID);
					result = env.online.ProcessLoginRequest(rpc);
				}
				assertEquals(Procedure.LogicError, result,
						"Repeated competing logins must exhaust a finite handler retry budget");
				assertTrue(compensatingLogouts.get() <= 3, "The handler must stop within three transactions");
			} finally {
				logoutHook.cancel();
				loginHook.cancel();
				reloginHook.cancel();
				var rival = pendingRival.get();
				if (rival != null)
					assertEquals(Procedure.Success, rival.get(5, TimeUnit.SECONDS));
			}
		}
	}
}
