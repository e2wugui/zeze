package Zeze.Component;

import java.lang.reflect.InvocationTargetException;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Online;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Online.BLink;
import Zeze.Builtin.Online.BNotify;
import Zeze.Collections.Queue;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestArchReliableQueueIdentity {
	private static String name(String account, String clientId) throws Exception {
		var method = Online.class.getDeclaredMethod("reliableNotifyQueueName", String.class, String.class);
		method.setAccessible(true);
		return (String)method.invoke(null, account, clientId);
	}

	@SuppressWarnings("unchecked")
	private static Queue<BNotify> open(Online online, String account, String clientId) throws Exception {
		var method = Online.class.getDeclaredMethod("openQueue", String.class, String.class);
		method.setAccessible(true);
		try {
			return (Queue<BNotify>)method.invoke(online, account, clientId);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException cause)
				throw cause;
			throw e;
		}
	}

	@Test
	public void encodedCompoundKeysAreInjectiveAndDisjointFromEveryLegacyKey() throws Exception {
		assertEquals("Zeze.Arch.Online.ReliableNotifyQueue:account:client", name("account", "client"));
		assertNotEquals(name("a:b", "c"), name("a", "b:c"));
		assertNotEquals(name("a:b", ""), name("a:", "b"));
		assertNotEquals(name("a@\ud800", "c"), name("a@\ud801", "c"));
		assertNotEquals(name("a:b", "c"), name("c", "a:b"));
		for (var key : new String[] {name("a:b", "c"), name("a", "b:c"), name("mail@example.com", "")}) {
			assertFalse(key.contains(":"));
			assertFalse(key.contains("@"));
		}
	}

	@Test
	public void legacyPendingMessagesBlockMigrationAndNewIdentitiesDoNotClearEachOther() throws Exception {
		var app = new Application("ArchReliableQueueIdentity", TakeoverTestEnv.newConf("off", 600_000, 600_000));
		new ProviderApp(app);
		var appBase = new TakeoverTestEnv.TestAppBase(app);
		app.initialize(appBase);
		var constructor = Online.class.getDeclaredConstructor(AppBase.class);
		constructor.setAccessible(true);
		var online = constructor.newInstance(appBase);
		try {
			app.start();
			var legacy = app.getQueueModule().open("Zeze.Arch.Online.ReliableNotifyQueue:a:b:c", BNotify.class);
			assertEquals(0, app.newProcedure(() -> {
				var firstLogin = online.getOrAddOnline("a:b").getLogins().getOrAdd("c");
				firstLogin.setReliableNotifyIndex(9);
				firstLogin.setReliableNotifyConfirmIndex(4);
				var otherLogin = online.getOrAddOnline("a").getLogins().getOrAdd("b:c");
				otherLogin.setReliableNotifyIndex(13);
				legacy.add(new BNotify());
				assertThrows(IllegalStateException.class, () -> open(online, "a:b", "c"));
				assertThrows(IllegalStateException.class, () -> online.migrateLegacyReliableNotifyQueue("a:b", "c"));
				assertEquals(9, firstLogin.getReliableNotifyIndex());
				assertEquals(1, legacy.size());
				legacy.clear(); // 运维/旧版本排空；新代码不猜测旧消息归属。
				var first = open(online, "a:b", "c");
				var other = open(online, "a", "b:c");
				first.add(new BNotify());
				other.add(new BNotify());
				other.clear();
				other.remove();
				assertEquals(1, first.size());
				assertThrows(IllegalStateException.class, () -> online.migrateLegacyReliableNotifyQueue("a:b", "c"));
				first.clear();
				var link = firstLogin.getLink();
				firstLogin.setLink(new BLink(link.getLinkName(), link.getLinkSid(), Online.eLogined));
				assertThrows(IllegalStateException.class, () -> online.migrateLegacyReliableNotifyQueue("a:b", "c"));
				firstLogin.setLink(new BLink(link.getLinkName(), link.getLinkSid(), Online.eOffline));
				online.migrateLegacyReliableNotifyQueue("a:b", "c");
				assertEquals(0, firstLogin.getReliableNotifyIndex());
				assertEquals(0, firstLogin.getReliableNotifyConfirmIndex());
				assertEquals(13, otherLogin.getReliableNotifyIndex());
				var email = open(online, "mail@example.com", "client");
				email.add(new BNotify());
				assertEquals(1, email.size());
				assertTrue(legacy.isEmpty());
				return 0;
			}, "arch-isolate-and-migrate").call());
		} finally {
			app.stop();
		}
	}
}
