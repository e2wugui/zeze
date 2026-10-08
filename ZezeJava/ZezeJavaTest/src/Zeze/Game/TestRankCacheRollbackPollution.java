package Zeze.Game;

import harness.FastServerIds;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Transaction.Procedure;

/**
 * FND15 game-01 回归：getRankTotal 进程级快照缓存被回滚事务污染。
 * 重建快照含本事务读己之写（单服redirect本地回环同事务），BuildTime原在事务内立即盖章——
 * 调用方事务回滚后未提交数据在缓存超时窗口内被当已提交排名提供，且updateRank不失效缓存无自愈。
 * 修复（方案D）：value立即写保留读己之写，BuildTime经whileCommit延迟到提交后盖章——
 * 未提交/回滚条目永不获freshness，后续访问判过期重建自动覆盖。
 */
@Fast
public class TestRankCacheRollbackPollution {
	private static final int RANK_TYPE = 1;

	// 与其他@Fast测试错开serverId（TestRankCacheEvict用7350段、TestRankCountNeedKey用7360、
	// TestRankSingleSegmentMerge用7370；7490与TestClearTableCacheTimers的7490起段撞，
	// 同JVM并发时FileMutex互斥失败）。
	private static final int ServerId = FastServerIds.TEST_RANK_CACHE_ROLLBACK_POLLUTION;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rank_g1_rollback_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestRankCacheRollback", conf);
	}

	private static Rank newRank(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		var rank = new Rank(app);
		rank.Initialize(app);
		return rank;
	}

	@Test
	public void testRollbackNotStampedAndNotServed() throws Exception {
		var app = newApp();
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2);
		rank.setFuncRankSize(t -> 4);
		app.start();
		try {
			var key = Rank.newRankKey(RANK_TYPE, 888L);
			final long roleId = 1001L;

			// 污染事务：进榜 + 查询触发缓存重建 + 返回错误强制回滚。
			var seenOwnWrite = new boolean[1];
			Assertions.assertEquals(Procedure.LogicError, app.newProcedure(() -> {
				rank.updateRank((int)roleId, key, roleId, new BValueLong(9999));
				for (var r : rank.getRankTotal(key).getTableValue().getRankListReadOnly()) {
					if (r.getRoleId() == roleId) {
						seenOwnWrite[0] = true;
						break;
					}
				}
				return Procedure.LogicError;
			}, "pollute").call(), "污染事务必须以回滚收场");
			Assertions.assertTrue(seenOwnWrite[0], "同事务读己之写：进榜必须立即可见（方案D保留此语义）");

			// 核心（修复前红）：回滚后新事务查询不得命中污染快照。
			// 修复前：BuildTime事务内已盖章→freshness命中→返回含roleId的未提交榜。
			// 修复后：BuildTime未推进→判过期→以已提交读视图重建→roleId不在榜。
			var polluted = new boolean[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				for (var r : rank.getRankTotal(key).getTableValue().getRankListReadOnly()) {
					if (r.getRoleId() == roleId) {
						polluted[0] = true;
						break;
					}
				}
				return 0L;
			}, "verify").call());
			Assertions.assertFalse(polluted[0], "回滚后未提交的进榜不得被当作已提交排名提供（game-01）");

			// 提交路径守护：已提交事务的查询必须推进BuildTime（提交后窗口语义保留）。
			// verify事务已提交（其defer盖章生效），本事务应freshness命中且BuildTime>0。
			var buildTime = new long[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				buildTime[0] = rank.getRankTotal(key).getBuildTime();
				return 0L;
			}, "stamp").call());
			Assertions.assertTrue(buildTime[0] > 0, "已提交事务的查询必须推进BuildTime");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
