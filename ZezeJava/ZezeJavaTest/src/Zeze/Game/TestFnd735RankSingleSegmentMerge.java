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
 * FND7-35 回归：merge(Collection,countNeed)的size==1分支直接copy()不按countNeed截断。
 * 段内允许增长到computeCount（默认2.5×rankSize）作中间数据，单段部署
 * （funcConcurrentLevel=1）时getRankTotal的快照含超容量条目：getRankPosition对
 * 第rankSize+1名之后返回具体名次而非-1（"是否在榜内"契约失真），且与多段路径
 * （会截断）行为不一致。修复：单段分支同样按countNeed截断。
 * 与其他@Fast测试错开serverId（TestRankCacheEvict用7350段、TestRankCountNeedKey用7360）。
 */
@Fast
public class TestFnd735RankSingleSegmentMerge {
	private static final int RANK_TYPE = 1;

	private static final int ServerId = 7370;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rank_single_seg_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFnd735RankSingleSegmentMerge", conf);
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
	public void testSingleSegmentTruncatedToCountNeed() throws Exception {
		var app = newApp();
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 1); // 单段部署：触发merge的size==1分支
		rank.setFuncRankSize(t -> 4);        // computeCount=4*2.5=10，单段可容纳全部8条
		app.start();
		try {
			var key = Rank.newRankKey(RANK_TYPE, 888L);
			for (long roleId = 1; roleId <= 8; ++roleId) {
				long h = roleId;
				Assertions.assertEquals(0L, app.newProcedure(() -> {
					rank.updateRank((int)h, key, h, new BValueLong(h * 10));
					return 0L;
				}, "updateRank").call());
			}

			// getRankTotal默认countNeed=rankSize=4：快照必须截断到4条（值最高的前4名）。
			var size = new int[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				size[0] = rank.getRankTotal(key).getTableValue().getRankListReadOnly().size();
				return 0L;
			}, "total").call());
			Assertions.assertEquals(4, size[0], "单段快照也必须按rankSize截断（FND7-35）");

			// 榜内名次：最高分roleId=8第1名。
			var first = new long[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				first[0] = rank.getRankPosition(key, 8);
				return 0L;
			}, "first").call());
			Assertions.assertEquals(1L, first[0], "榜内第1名");

			// 榜外契约：第5..8名（roleId 4..1，分数最低）不在榜内，必须返回-1。
			for (long roleId = 1; roleId <= 4; ++roleId) {
				long r = roleId;
				var pos = new long[1];
				Assertions.assertEquals(0L, app.newProcedure(() -> {
					pos[0] = rank.getRankPosition(key, r);
					return 0L;
				}, "pos").call());
				Assertions.assertEquals(-1L, pos[0], "超出榜容量的名次必须返回-1（roleId=" + r + "）");
			}
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
