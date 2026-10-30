package Zeze.Game;

import java.util.ArrayList;
import java.util.List;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Config;
import Zeze.Transaction.Transaction;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOnlineLocalTransmit {
	@Test
	public void localTransmitCanExcludeOfflineTargets() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.takeoverPoolNext());
		conf.setTakeoverMode("off");
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseUrl("online_local_transmit_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", db);
		var zeze = new Application("OnlineLocalTransmit" + conf.getServerId(), conf);
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		var online = new Online(app);
		zeze.initialize(app);
		zeze.start();
		try {
			assertFalse(conf.hasGlobal());
			assertEquals(0, zeze.newProcedure(() -> {
				online.getOrAddOnlineShared(1).setLink(new BLink("link", 1, AbstractOnline.eLogined));
				online.getOrAddOnlineShared(2).setLink(new BLink("link", 2, AbstractOnline.eLinkBroken));
				online.getOrAddOnlineShared(3).setLink(new BLink("link", 3, AbstractOnline.eOffline));
				return 0;
			}, "seedTransmitTargets").call());
			var delivered = new ArrayList<Long>();
			online.getTransmitActions().put("local", (sender, target, parameter) -> {
				Transaction.whileCommit(() -> delivered.add(target));
				return 0;
			});
			var targets = List.of(1L, 2L, 3L, 4L);
			assertEquals(0, zeze.newProcedure(() -> {
				online.transmitEmbed(0, "local", targets, null, false);
				return 0;
			}, "transmitOnlineOnly").call());
			assertEquals(List.of(1L), delivered);
			delivered.clear();
			assertEquals(0, zeze.newProcedure(() -> {
				online.transmitEmbed(0, "local", targets, null, true);
				return 0;
			}, "transmitIncludingOffline").call());
			assertEquals(targets, delivered);
		} finally {
			zeze.stop();
		}
	}
}
