package Zeze.Component;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Timer.BAccountClientId;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestTimerCancelBothFamilies {
	private static final class ArchOnline extends Zeze.Arch.Online {
		ArchOnline(AppBase app) {
			super(app);
		}
	}

	private static final class RemoteRoleTimers extends TimerRole {
		boolean forwarded;
		boolean offlineCanceled;

		RemoteRoleTimers(Zeze.Game.Online online) {
			super(online);
		}

		@Override Long getLocalLoginVersion(Long id) { return null; }
		@Override Long getSharedLoginVersion(Long id) { return 1L; }
		@Override void transmitCancel(Long id, String timerId, long version) { forwarded = true; }
		@Override public boolean cancelOffline(String timerId, long id) {
			offlineCanceled = true;
			return true;
		}
	}

	private static final class RemoteAccountTimers extends TimerAccount {
		boolean forwarded;
		boolean offlineCanceled;

		RemoteAccountTimers(Zeze.Arch.Online online) {
			super(online);
		}

		@Override Long getLocalLoginVersion(BAccountClientId id) { return null; }
		@Override Long getSharedLoginVersion(BAccountClientId id) { return 1L; }
		@Override void transmitCancel(BAccountClientId id, String timerId, long version) { forwarded = true; }
		@Override public boolean cancelOffline(String timerId, String account, String clientId) {
			offlineCanceled = true;
			return true;
		}
	}

	@Test
	public void forwardedOnlineCancelAlsoAttemptsOfflineFamily() throws Exception {
		var config = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		var app = new Application("CancelBothFamilies" + config.getServerId(), config);
		try {
			new ProviderApp(app);
			var base = new TakeoverTestEnv.TestAppBase(app);
			var role = new RemoteRoleTimers(new TimerTestEnv.TestOnline(base));
			var account = new RemoteAccountTimers(new ArchOnline(base));
			Assertions.assertTrue(role.cancel("offline-role", 7));
			Assertions.assertTrue(role.forwarded);
			Assertions.assertTrue(role.offlineCanceled, "forwarding is not proof that an offline timer was canceled");
			Assertions.assertTrue(account.cancel("offline-account", "account", "client"));
			Assertions.assertTrue(account.forwarded);
			Assertions.assertTrue(account.offlineCanceled);
		} finally {
			app.stop();
		}
	}
}
