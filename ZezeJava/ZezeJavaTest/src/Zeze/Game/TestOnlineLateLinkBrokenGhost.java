package Zeze.Game;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * local缺失的断链善后（provider重启后_tlocal内存表丢失）必须先核对当前链：
 * 重登换链后迟到的旧linkName/linkSid断链报告不得把当前登录链改写成旧值、
 * 也不得为当前loginVersion安排登出。
 * 修复前ghost分支早于owner核对执行setLink(报告值)+DelayLogout(当前版本)，
 * 迟到旧报告直接破坏当前会话；修复后先核对linkName/linkSid，不匹配即no-op。
 * 另钉住先查后建：善后路径不创建共享行（迟到报告对不存在的角色不得残留空行）。
 */
@Fast
public class TestOnlineLateLinkBrokenGhost {

	private static final int ServerId = FastServerIds.TEST_ONLINE_LATE_LINK_BROKEN_GHOST;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("online_ghost_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestOnlineGhost", conf);
	}

	private static Online newOnline(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		zeze.initialize(app);
		return new Online(app);
	}

	@Test
	public void lateOldLinkReportDoesNotRewriteCurrentChain() throws Exception {
		var zeze = newApp();
		var online = newOnline(zeze);
		zeze.start();
		try {
			final var roleId = 91L;
			// 当前会话：new-link/999，登录版本42，归属本机；不建_tlocal（provider重启丢失形态）。
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOrAddOnlineShared(roleId);
				shared.setAccount("account");
				shared.setLoginVersion(42L);
				shared.setLink(new BLink("new-link", 999L, AbstractOnline.eLogined));
				online.getOrAddOnline(roleId).setServerId(ServerId);
				return 0;
			}, "seedCurrentChain").call());

			// 迟到的旧链断链报告（old-link/123）。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.linkBroken("account", roleId, "old-link", 123L), "lateBroken").call());

			var linkNameAfter = new String[1];
			var linkSidAfter = new long[1];
			var stateAfter = new int[1];
			var versionAfter = new long[1];
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOnlineShared(roleId);
				linkNameAfter[0] = shared.getLink().getLinkName();
				linkSidAfter[0] = shared.getLink().getLinkSid();
				stateAfter[0] = shared.getLink().getState();
				versionAfter[0] = shared.getLoginVersion();
				return 0;
			}, "verify").call());
			Assertions.assertEquals("new-link", linkNameAfter[0], "当前登录链不得被迟到的旧报告改写");
			Assertions.assertEquals(999L, linkSidAfter[0], "当前linkSid不得被迟到的旧报告改写");
			Assertions.assertEquals(AbstractOnline.eLogined, stateAfter[0], "当前会话不得被旧报告推进到eLinkBroken");
			Assertions.assertEquals(42L, versionAfter[0], "loginVersion不变");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/** 迟到报告对从未在线的角色不得创建共享行（善后路径不创建状态）。 */
	@Test
	public void lateReportDoesNotCreateSharedRow() throws Exception {
		var zeze = newApp();
		var online = newOnline(zeze);
		zeze.start();
		try {
			final var roleId = 92L;
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.linkBroken("nobody", roleId, "old-link", 123L), "lateBrokenNoRow").call());
			var exists = new boolean[1];
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				exists[0] = online.getOnlineShared(roleId) != null;
				return 0;
			}, "verifyNoRow").call());
			Assertions.assertFalse(exists[0], "善后路径不得创建共享行");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
