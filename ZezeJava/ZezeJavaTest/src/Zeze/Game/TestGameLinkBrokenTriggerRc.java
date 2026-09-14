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

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(7350);

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
}
