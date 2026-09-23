package Zeze.Game;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Online.BLink;
import Zeze.Config;
import Zeze.Util.EventDispatcher;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-41 回归：Game.Online三处linkBrokenTrigger返回码只写info日志不外传
 * （procedureOffline/onSendError/linkBroken），断线处理器（好友、聊天下线清理等
 * RunEmbed）返回错误码时：其在本事务内已做的部分写入随外层事务一起提交（而非
 * 回滚），且流程继续推进状态机——与同文件removeLocalAndTrigger/logoutTrigger
 * 的"非0=失败=回滚"判例相反（Arch版triggerLinkBroken已在FND4-50修复检查）。
 * 本测试直接驱动linkBroken（procedureOffline/onSendError为同款一行守卫，
 * 其后续logoutTrigger路径含跨服redirect不适合@Fast，以形态核查覆盖）：
 * 注册返回错误码的RunEmbed处理器，断言外层过程失败回滚——返回码外传且
 * 状态不推进为eLinkBroken（修复前：rc=0、状态被提交推进）。
 */
@Fast
public class TestGameLinkBrokenTriggerRc {

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份
	// （zeze_cache_<serverId>）。基点须全局唯一且不落他类增长范围：曾与TestRankCacheEvict
	// (7350起)重叠7351，迁7360后又撞TestRankCountNeedKey的固定7360（30轮压测2026-09-19轮25
	// dir lock假红），迁7480独占段。
	private static final AtomicInteger NextServerId = new AtomicInteger(7480);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("link_broken_rc_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	// 不走start()：initialize创建timer（linkBroken的DelayLogout调度路径需要，
	// 修复前路径会走到schedule），RegisterProtocols/RegisterZezeTables为纯内存注册。
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
	public void testLinkBrokenTriggerFailureRollsBack() throws Exception {
		var zeze = newApp("TestLinkBrokenRc1");
		var online = newOnline(zeze);
		zeze.start();
		try {
			final var errRc = -123456;
			online.getLinkBrokenEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> errRc);

			var roleId = 777_001L;
			// 前置：独立事务提交登录态（eLogined、local版本一致）。
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOrAddOnlineShared(roleId);
				shared.setAccount("acc");
				shared.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				var local = online._tlocal.getOrAdd(roleId);
				local.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				local.setLoginVersion(shared.getLoginVersion());
				return 0;
			}, "testLinkBrokenRc.setup").call());
			// 被测：断链处理事务（处理器失败必须外传错误码并整体回滚）。
			var rc = zeze.newProcedure(() -> online.linkBroken("acc", roleId, "link1", 42L), "testLinkBrokenRc").call();
			Assertions.assertEquals(errRc, rc, "断线处理器错误码必须外传（FND5-41）");

			// 失败=整体回滚：断链状态不得随外层提交推进（修复前rc=0且状态被提交为eLinkBroken）。
			var stateAfter = new int[1];
			zeze.newProcedure(() -> {
				var shared = online.getOnlineShared(roleId);
				stateAfter[0] = shared != null ? shared.getLink().getState() : AbstractOnline.eOffline;
				return 0;
			}, "testLinkBrokenRc.verify").call();
			Assertions.assertEquals(AbstractOnline.eLogined, stateAfter[0], "失败处理器的写入不得随外层提交");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * onSendError幂等标记器契约（打穿宽限窗口修复）：onSendError只标记eLinkBroken，
	 * 不触发logout/linkBroken事件、不登出——宽限期内推送失败（群发errorSids记账必中）
	 * 不得打断DelayLogout重连窗口。旧语义assignLogoutVersion+logoutTrigger立即登出，
	 * OnlineLogoutDelay形同虚设（修复前logoutCount=1且状态eOffline）。
	 */
	@Test
	public void testOnSendErrorMarksGraceNotLogout() throws Exception {
		var zeze = newApp("TestLinkBrokenRc2");
		var online = newOnline(zeze);
		zeze.start();
		try {
			var linkBrokenCount = new AtomicInteger();
			var logoutCount = new AtomicInteger();
			online.getLinkBrokenEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> {
				linkBrokenCount.incrementAndGet();
				return 0;
			});
			online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> {
				logoutCount.incrementAndGet();
				return 0;
			});

			var roleId = 777_002L;
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOrAddOnlineShared(roleId);
				shared.setAccount("acc");
				shared.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				var local = online._tlocal.getOrAdd(roleId);
				local.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				local.setLoginVersion(shared.getLoginVersion());
				return 0;
			}, "testSendErrorGrace.setup").call());

			// linkBroken开宽限钟（DelayLogout默认60s，测试期内不会到点）。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.linkBroken("acc", roleId, "link1", 42L), "testSendErrorGrace.broken").call());
			Assertions.assertEquals(1, linkBrokenCount.get(), "断链事件恰好一次");

			// 宽限期内两次推送失败：幂等重标记，无登出、无重复事件。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.onSendError("acc", roleId, "link1", 42L), "testSendErrorGrace.se1").call());
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.onSendError("acc", roleId, "link1", 42L), "testSendErrorGrace.se2").call());

			var stateAfter = new int[1];
			var existsAfter = new boolean[1];
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOnlineShared(roleId);
				existsAfter[0] = shared != null;
				stateAfter[0] = shared != null ? shared.getLink().getState() : AbstractOnline.eOffline;
				return 0;
			}, "testSendErrorGrace.verify").call());
			Assertions.assertTrue(existsAfter[0], "宽限期内不得删除在线行");
			Assertions.assertEquals(AbstractOnline.eLinkBroken, stateAfter[0], "sendError只标记eLinkBroken");
			Assertions.assertEquals(0, logoutCount.get(), "sendError不得触发logout事件（打穿宽限即回归）");
			Assertions.assertEquals(1, linkBrokenCount.get(), "sendError不得重复触发断链事件");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 迟到的sendError不得回退已登出状态机：eOffline下同sid错误报告必须跳过——
	 * 旧语义无条件setLink(eLinkBroken)把eOffline回退成eLinkBroken，verifyLocal会对其
	 * 重走tryLogout造成重复logout事件。
	 */
	@Test
	public void testOnSendErrorAfterLogoutDoesNotRegress() throws Exception {
		var zeze = newApp("TestLinkBrokenRc4");
		var online = newOnline(zeze);
		zeze.start();
		try {
			var logoutCount = new AtomicInteger();
			online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> {
				logoutCount.incrementAndGet();
				return 0;
			});

			var roleId = 777_004L;
			// 前置：直接构造已登出终态（logoutTrigger形态：eOffline且link名/sid保留）。
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOrAddOnlineShared(roleId);
				shared.setAccount("acc");
				shared.setLink(new BLink("link1", 42L, AbstractOnline.eOffline));
				var local = online._tlocal.getOrAdd(roleId);
				local.setLink(new BLink("link1", 42L, AbstractOnline.eOffline));
				local.setLoginVersion(shared.getLoginVersion());
				return 0;
			}, "testSendErrorOffline.setup").call());

			// 同sid迟到错误报告：必须跳过，不得回退。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.onSendError("acc", roleId, "link1", 42L), "testSendErrorOffline.se").call());

			var stateAfter = new int[1];
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOnlineShared(roleId);
				stateAfter[0] = shared != null ? shared.getLink().getState() : AbstractOnline.eOffline;
				return 0;
			}, "testSendErrorOffline.verify").call());
			Assertions.assertEquals(AbstractOnline.eOffline, stateAfter[0], "迟到的sendError不得把eOffline回退成eLinkBroken");
			Assertions.assertEquals(0, logoutCount.get(), "不得对已登出状态触发任何logout路径");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * sendError先于linkBroken到达（竞速）：先标记后上钟——linkBroken不检查当前state，
	 * 迟到到达照常触发事件+调度DelayLogout，宽限链完整（原语义下sendError已立即登出，
	 * 迟到的linkBroken会在已登出状态上重跑：本测试钉住新契约的事件恰好一次+状态稳定）。
	 */
	@Test
	public void testOnSendErrorBeforeLinkBrokenStillSchedules() throws Exception {
		var zeze = newApp("TestLinkBrokenRc3");
		var online = newOnline(zeze);
		zeze.start();
		try {
			var linkBrokenCount = new AtomicInteger();
			var logoutCount = new AtomicInteger();
			online.getLinkBrokenEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> {
				linkBrokenCount.incrementAndGet();
				return 0;
			});
			online.getLogoutEvents().add(EventDispatcher.Mode.RunEmbed, (__s, __a) -> {
				logoutCount.incrementAndGet();
				return 0;
			});

			var roleId = 777_003L;
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOrAddOnlineShared(roleId);
				shared.setAccount("acc");
				shared.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				var local = online._tlocal.getOrAdd(roleId);
				local.setLink(new BLink("link1", 42L, AbstractOnline.eLogined));
				local.setLoginVersion(shared.getLoginVersion());
				return 0;
			}, "testSendErrorFirst.setup").call());

			// sendError先到：只标记，不登出、无事件。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.onSendError("acc", roleId, "link1", 42L), "testSendErrorFirst.se").call());
			Assertions.assertEquals(0, logoutCount.get());
			Assertions.assertEquals(0, linkBrokenCount.get());

			// linkBroken迟到：照常触发断链事件+调度DelayLogout（rc=0即schedule成功）。
			Assertions.assertEquals(0,
					zeze.newProcedure(() -> online.linkBroken("acc", roleId, "link1", 42L), "testSendErrorFirst.broken").call());
			Assertions.assertEquals(1, linkBrokenCount.get(), "迟到的linkBroken照常触发断链事件");
			Assertions.assertEquals(0, logoutCount.get());

			var stateAfter = new int[1];
			Assertions.assertEquals(0, zeze.newProcedure(() -> {
				var shared = online.getOnlineShared(roleId);
				stateAfter[0] = shared != null ? shared.getLink().getState() : AbstractOnline.eOffline;
				return 0;
			}, "testSendErrorFirst.verify").call());
			Assertions.assertEquals(AbstractOnline.eLinkBroken, stateAfter[0], "宽限钟在位，状态稳定eLinkBroken");
		} finally {
			try {
				zeze.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
