package Zeze.Component;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.BNotify;
import Zeze.Collections.Queue;
import Zeze.Game.Online;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOnlineSetReliableQueue {
	@Test
	@SuppressWarnings("unchecked")
	public void clearingAnotherSetDoesNotDeletePendingNotificationsAndMigrationRequiresDraining() throws Exception {
		var app = new Application("OnlineSetReliableQueue", TakeoverTestEnv.newConf("off", 600_000, 600_000));
		new ProviderApp(app);
		var appBase = new TakeoverTestEnv.TestAppBase(app);
		app.initialize(appBase);
		var defaultSet = new TimerTestEnv.TestOnline(appBase);
		var constructor = Online.class.getDeclaredConstructor(AppBase.class, String.class);
		constructor.setAccessible(true);
		var other = constructor.newInstance(appBase, "other:set");
		var queueMethod = Online.class.getDeclaredMethod("openQueue", long.class);
		queueMethod.setAccessible(true);
		try {
			app.start();
			var oldQueue = (Queue<BNotify>)queueMethod.invoke(defaultSet, 42L);
			var otherQueue = (Queue<BNotify>)queueMethod.invoke(other, 42L);
			assertEquals("Zeze.Game.Online.ReliableNotifyQueue:42", oldQueue.getName());
			assertNotEquals(oldQueue.getName(), otherQueue.getName());
			assertEquals(0, app.newProcedure(() -> {
				oldQueue.add(new BNotify());
				otherQueue.add(new BNotify());
				otherQueue.clear(); // 其他集合Login会走此路径。
				assertEquals(1, oldQueue.size());
				otherQueue.remove(); // 其他集合最终Logout会走此路径。
				assertEquals(1, oldQueue.size());
				other.getOrAddOnline(42).setReliableNotifyIndex(10);
				assertThrows(IllegalStateException.class, () -> other.migrateLegacyReliableNotifyQueue(42));
				assertEquals(10, other.getOnline(42).getReliableNotifyIndex());
				oldQueue.clear();
				other.migrateLegacyReliableNotifyQueue(42);
				assertEquals(0, other.getOnline(42).getReliableNotifyIndex());
				return 0;
			}, "isolate-and-migrate").call());
		} finally {
			app.stop();
		}
	}
}
