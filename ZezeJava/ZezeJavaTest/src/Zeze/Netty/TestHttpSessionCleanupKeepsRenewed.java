package Zeze.Netty;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 会话清理的删除事务必须重验过期条件：ExpiredTimer的walk按快照选中候选键，
 * RemoveBatch另开事务删除——扫描到删除的窗口内会话可能被合法续期
 * （CookieSession.setExpireTime提交，expireTime推进到cutoff之后），
 * 无条件删除会让在线会话连同属性/登录态一起消失。
 * 批次只保存候选主键与单一扫描cutoff，删除时按当前行重验。
 */
@Fast
public class TestHttpSessionCleanupKeepsRenewed {



	@Test
	public void renewedSessionSurvivesConcurrentCleanupBatch() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.takeoverPoolNext());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("http_session_cleanup_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestHttpSessionCleanup" + conf.getServerId(), conf);
		var tSession = new Zeze.Builtin.HttpSession.tSession();
		app.addTable("", tSession);
		app.start();
		try {
			var now = System.currentTimeMillis();
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				tSession.getOrAdd("renewed").setExpireTime(now - 1000);
				tSession.getOrAdd("expired").setExpireTime(now - 1000);
				return 0L;
			}, "seed").call());

			// 扫描读的是后端存储：提交的脏记录需先flush落库walk才可见
			app.checkpointRun();

			// 扫描快照：两个都过期入批
			var batch = new HttpSession.RemoveBatch(tSession, now);
			tSession.walk((key, value) -> {
				if (value.getExpireTime() <= now)
					batch.add(key);
				return true;
			});

			// 窗口内"renewed"被合法续期并提交
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				tSession.get("renewed").setExpireTime(now + 3600_000);
				return 0L;
			}, "renew").call());

			batch.tryPerform();

			Assertions.assertEquals(0L, app.newProcedure(() -> {
				Assertions.assertNotNull(tSession.get("renewed"), "续期后的会话不得被清理批次删除");
				Assertions.assertNull(tSession.get("expired"), "未续期的过期会话照常清理");
				return 0L;
			}, "verify").call());
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
