package Zeze.Arch;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Online.Login;
import Zeze.Builtin.Online.ReLogin;
import Zeze.Transaction.Procedure;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestOnlineLoginValidation {
	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_ONLINE_LOGIN_VALIDATION);

	@Test
	public void loginRejectsEmptyClientIdWithoutCreatingOnlineState() throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			var rpc = env.attach(new Login(), env.session(1L, ""));
			assertEquals(Procedure.LogicError, env.online.ProcessLoginRequest(rpc));
			assertNoOnlineState(env);
		}
	}

	@Test
	public void reloginRejectsEmptyClientIdWithoutCreatingOnlineState() throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			var rpc = env.attach(new ReLogin(), env.session(1L, ""));
			assertEquals(Procedure.LogicError, env.online.ProcessReLoginRequest(rpc));
			assertNoOnlineState(env);
		}
	}

	private static void assertNoOnlineState(ArchOnlineTestEnv env) {
		env.run("verifyNoGhostOnlineState", () -> {
			assertNull(env.online.getOnline(ArchOnlineTestEnv.ACCOUNT));
			assertNull(env.online._tlocal.get(ArchOnlineTestEnv.ACCOUNT));
			return Procedure.Success;
		});
		assertEquals(0L, env.online.getLoginTimes());
	}
}
