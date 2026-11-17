package Zeze.Game;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Config;
import Zeze.Util.EventDispatcher;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestStaleLocalOfflineOwnership {
	@Test
	void staleLocalCleanupCannotLogoutANewerLoginOnTheSameLink() throws Exception {
		Task.tryInitThreadPool();
		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_STALE_LOCAL_OFFLINE_OWNERSHIP);
		config.setTakeoverMode("off");
		config.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseUrl("stale_local_offline_" + config.getServerId());
		config.getDatabaseConfMap().put("", database);
		var zeze = new Application("StaleLocalOfflineOwnership", config);
		var app = new AppBase() {
			@Override public Application getZeze() { return zeze; }
		};
		var providerApp = new ProviderApp(zeze);
		zeze.initialize(app);
		var online = new Online(app);
		var provider = new ProviderWithOnline();
		provider.online = online;
		provider.onlineSetMap.put("", online);
		var implementation = ProviderApp.class.getDeclaredField("providerImplement");
		implementation.setAccessible(true);
		implementation.set(providerApp, provider);
		var brokenEvents = new AtomicInteger();
		online.getLinkBrokenEvents().add(EventDispatcher.Mode.RunEmbed, (sender, argument) -> {
			brokenEvents.incrementAndGet();
			return -18791L; // Current-owner control below proves that the handler is reachable.
		});
		var offline = Online.class.getDeclaredMethod("procedureOffline", Long.class,
				Zeze.Builtin.Game.Online.BLocal.class, boolean.class);
		offline.setAccessible(true);
		zeze.start();
		try {
			for (long roleId = 1; roleId <= 3; roleId++) {
				final long id = roleId;
				assertEquals(0, zeze.newProcedure(() -> {
					var shared = online.getOrAddOnlineShared(id);
					shared.setAccount("account");
					shared.setLoginVersion(2);
					shared.setLogoutVersion(1);
					shared.setLink(new BLink("same-link", 37, AbstractOnline.eLogined));
					online.getOrAddOnline(id).setServerId(config.getServerId());
					var local = online._tlocal.getOrAdd(id);
					local.setLoginVersion(id == 3 ? 2 : 1);
					local.setLink(new BLink("same-link", 37, AbstractOnline.eLogined));
					return 0;
				}, "staleLocal.seed").call());
				long result = zeze.newProcedure(() -> {
					try {
						return (Long)offline.invoke(online, id, online._tlocal.get(id), id == 1);
					} catch (InvocationTargetException e) {
						throw Task.forceThrow(e.getCause());
					}
				}, "staleLocal.offline").call();
				assertEquals(id == 3 ? -18791 : 0, result);
				assertEquals(0, zeze.newProcedure(() -> {
					var shared = online.getOnlineShared(id);
					assertNotNull(shared);
					assertEquals(AbstractOnline.eLogined, shared.getLink().getState());
					assertEquals(2, shared.getLoginVersion());
					assertEquals(1, shared.getLogoutVersion());
					if (id != 3)
						assertNull(online._tlocal.get(id));
					return 0;
				}, "staleLocal.verify").call());
			}
			assertEquals(1, brokenEvents.get(), "only the current local login owns the shared session");
		} finally {
			zeze.stop();
		}
	}
}
