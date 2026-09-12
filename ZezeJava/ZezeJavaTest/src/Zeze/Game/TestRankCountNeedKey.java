package Zeze.Game;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-79 回归：getRankTotal(key,countNeed)缓存命中路径忽略countNeed。
 * 缓存仅按keyHint索引时，默认getRankSize构建的截断快照会被更大countNeed的
 * 请求命中——按countNeed语义取榜（如发前500名奖励）静默拿到截断数据。
 * 修复：countNeed进入缓存键，不同需求各存快照。
 * 多并发段下merge按countNeed截断（单段忽略countNeed返回全量），故测试设
 * concurrentLevel=2使截断语义生效。
 */
@Fast
public class TestRankCountNeedKey {
	private static final int RANK_TYPE = 1;

	// 与其他@Fast测试错开serverId（TestRankCacheEvict用7350段）。
	private static final int ServerId = 7360;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rank_count_need_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestRankCountNeedKey", conf);
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
	public void testCountNeedInCacheKey() throws Exception {
		var app = newApp();
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2);
		rank.setFuncRankSize(t -> 4); // computeCount=4*2.5=10，每段可存全部8条
		app.start();
		try {
			var key = Rank.newRankKey(RANK_TYPE, 777L);
			for (long roleId = 1; roleId <= 8; ++roleId) {
				long h = roleId;
				Assertions.assertEquals(0L, app.newProcedure(() -> {
					rank.updateRank((int)h, key, h, new BValueLong(h * 10));
					return 0L;
				}, "updateRank").call());
			}

			// 单参版本countNeed=rankSize=4：建立4条截断快照（默认timeout内保持新鲜）。
			var sizeSmall = new int[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				sizeSmall[0] = rank.getRankTotal(key).getTableValue().getRankListReadOnly().size();
				return 0L;
			}, "small").call());
			Assertions.assertEquals(4, sizeSmall[0], "单参版本按rankSize截断");

			// 大countNeed查询同一key：不得命中countNeed=4的截断快照。
			var sizeBig = new int[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				sizeBig[0] = rank.getRankTotal(key, 8).getTableValue().getRankListReadOnly().size();
				return 0L;
			}, "big").call());
			Assertions.assertEquals(8, sizeBig[0], "countNeed=8不得命中countNeed=4的截断快照（FND4-79）");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
