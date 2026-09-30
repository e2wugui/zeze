package Zeze.Arch;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Builtin.Online.BNotify;
import Zeze.Builtin.Online.Logout;
import Zeze.Builtin.Online.ReLogin;
import Zeze.Builtin.Online.SReliableNotify;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Fast
public class TestOnlineReliableLogout {
	private static final AtomicInteger NEXT_SERVER_ID =
			new AtomicInteger(FastServerIds.TEST_ONLINE_RELIABLE_LOGOUT);

	@Test
	public void finalLogoutAllowsReloginWithResetNotificationIndex() throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1L, 2L, 3L);
			var queue = env.zeze.getQueueModule().open(
					"Zeze.Arch.Online.ReliableNotifyQueue:" + ArchOnlineTestEnv.ACCOUNT
							+ ":" + ArchOnlineTestEnv.CLIENT_ID, BNotify.class);
			env.run("seedUnconfirmedNotify", () -> {
				var notify = new BNotify();
				notify.setFullEncodedProtocol(new Binary(new byte[]{1}));
				queue.add(notify);
				return Procedure.Success;
			});
			var logout = env.attach(new Logout(), env.session(1L, ArchOnlineTestEnv.CLIENT_ID));
			env.run("finalLogout", () -> env.online.ProcessLogoutRequest(logout));

			var session = env.session(2L, "");
			var relogin = env.attach(new ReLogin(), session);
			relogin.Argument.setClientId(ArchOnlineTestEnv.CLIENT_ID);
			relogin.Argument.setReliableNotifyConfirmIndex(0L);
			assertEquals(Procedure.Success, env.online.ProcessReLoginRequest(relogin),
					"A finalized login must accept the client's fresh notification index");
			env.run("verifyResetReliableState", () -> {
				var login = env.online.getLogin(ArchOnlineTestEnv.ACCOUNT, ArchOnlineTestEnv.CLIENT_ID);
				assertNotNull(login);
				assertEquals(0L, login.getReliableNotifyConfirmIndex());
				assertEquals(0L, login.getReliableNotifyIndex());
				assertEquals(0L, queue.size());
				return Procedure.Success;
			});
			var sync = (SReliableNotify)session.replies.stream()
					.filter(SReliableNotify.class::isInstance).findFirst().orElseThrow();
			assertEquals(0L, sync.Argument.getReliableNotifyIndex());
			assertEquals(0, sync.Argument.getNotifies().size());
		}
	}

	@Test
	public void duplicateReloginPreservesUnconfirmedNotifications() throws Exception {
		try (var env = new ArchOnlineTestEnv(NEXT_SERVER_ID.getAndIncrement())) {
			env.seedLogin(1L, 2L, 3L);
			var queue = env.zeze.getQueueModule().open(
					"Zeze.Arch.Online.ReliableNotifyQueue:" + ArchOnlineTestEnv.ACCOUNT
							+ ":" + ArchOnlineTestEnv.CLIENT_ID, BNotify.class);
			var encoded = new Binary(new byte[]{7});
			env.run("seedUnconfirmedNotify", () -> {
				var notify = new BNotify();
				notify.setFullEncodedProtocol(encoded);
				queue.add(notify);
				return Procedure.Success;
			});
			var session = env.session(2L, "");
			var relogin = env.attach(new ReLogin(), session);
			relogin.Argument.setClientId(ArchOnlineTestEnv.CLIENT_ID);
			relogin.Argument.setReliableNotifyConfirmIndex(2L);
			assertEquals(Procedure.Success, env.online.ProcessReLoginRequest(relogin));
			var sync = (SReliableNotify)session.replies.stream()
					.filter(SReliableNotify.class::isInstance).findFirst().orElseThrow();
			assertEquals(2L, sync.Argument.getReliableNotifyIndex());
			assertEquals(1, sync.Argument.getNotifies().size());
			assertEquals(encoded, sync.Argument.getNotifies().get(0));
			env.run("verifyRetainedReliableState", () -> {
				var login = env.online.getLogin(ArchOnlineTestEnv.ACCOUNT, ArchOnlineTestEnv.CLIENT_ID);
				assertNotNull(login);
				assertEquals(2L, login.getReliableNotifyConfirmIndex());
				assertEquals(3L, login.getReliableNotifyIndex());
				assertEquals(1L, queue.size());
				return Procedure.Success;
			});
		}
	}
}
