package Zeze.Game;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Config;

/**
 * 多段榜单归并结果是独立快照：修改查询结果的元素不得改写表内数据。
 * <p>
 * merge(Collection,countNeed)的单段分支copy，多段分支新建BRankList却直接追加输入
 * BRankValue引用，最终返回current——源码注释自称"最后Copy一次"但没做。结果未受管而
 * 元素仍属原表：业务在事务中修改查询结果元素即改写原记录（受管bean的修改被日志记录
 * 并随提交落表）；缓存持有相同引用时buildTime与内容也无法解释为稳定快照。
 * 修复：中间归并只读借用、top-K截断后唯一出口copy一次，零/单/多段一致；缓存发布
 * 私有copy与局部结果隔离。mergeRank的两参merge仍借用、落表前copy不变。
 * 独立内存库（url=前缀+serverId派生），不验证持久库与跨节点缓存。
 */
@Fast
public class TestRankMergeSnapshotIndependent {
	private static final int SERVER_ID = FastServerIds.TEST_RANK_MERGE_SNAPSHOT_INDEPENDENT;

	private Application zeze;
	private Rank rank;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var db = new Config.DatabaseConf();
		db.setDatabaseType(Config.DbType.Memory);
		db.setDatabaseUrl("rank_merge_snapshot_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", db);
		zeze = new Application("TestRankMergeSnapshotIndependent", conf);
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		rank = new Rank(app);
		rank.Initialize(app);
		rank.setFuncConcurrentLevel(t -> 2); // 多段归并路径
		zeze.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (zeze.getStartState() != Application.StartState.eStopped)
			zeze.stop();
	}

	@Test
	public void testMultiSegmentResultElementsAreIndependentSnapshot() throws Exception {
		var key = Rank.newRankKey(1, 9971L);
		var rc = zeze.newProcedure(() -> {
			rank.updateRank(0, key, 77, new BValueLong(100));
			return 0L;
		}, "seed-rank").call();
		Assertions.assertEquals(0L, rc, "种子事务必须成功");

		// 多段归并结果：元素必须是独立快照（修复前：直接引用表内受管bean，isManaged=true）。
		// getRankDirect触碰表，须在事务内调用。
		rc = zeze.newProcedure(() -> {
			var direct = rank.getRankDirect(key);
			Assertions.assertFalse(direct.getRankList().isEmpty(), "榜单必须有条目");
			Assertions.assertFalse(direct.getRankList().get(0).isManaged(),
					"查询结果元素不得是表内受管bean（修复前多段分支直接引用表行）");
			return 0L;
		}, "check-direct").call();
		Assertions.assertEquals(0L, rc);
		// 事务中修改查询结果元素并提交：表内数据必须保持原值（修复前：写穿到原表）。
		rc = zeze.newProcedure(() -> {
			var result = rank.getRankDirect(key);
			result.getRankList().get(0).setRoleId(999);
			return 0L;
		}, "mutate-result").call();
		Assertions.assertEquals(0L, rc);
		rc = zeze.newProcedure(() -> {
			var reRead = rank.getRankDirect(key);
			Assertions.assertEquals(77L, reRead.getRankList().get(0).getRoleId(),
					"修改查询结果不得改写原表（修复前：下一事务读到999）");
			return 0L;
		}, "verify-table").call();
		Assertions.assertEquals(0L, rc);
	}

	@Test
	public void testOldSnapshotStaysStableAfterTableUpdate() throws Exception {
		var key = Rank.newRankKey(1, 9972L);
		final Rank.RankTotal[] holder = new Rank.RankTotal[1];
		Assertions.assertEquals(0L, zeze.newProcedure(() -> {
			rank.updateRank(0, key, 88, new BValueLong(200));
			return 0L;
		}, "seed-rank-stable").call());
		// 建立榜单快照并保留引用（getRankTotal对外只暴露只读视图，元素不可变——
		// 缓存与局部结果的隔离由merge出口copy+发布私有copy结构性保证）。
		Assertions.assertEquals(0L, zeze.newProcedure(() -> {
			holder[0] = rank.getRankTotal(key);
			return 0L;
		}, "build-snapshot").call());

		// 更新原表：同roleId换新值（updateRank按remove/re-add替换行对象）。
		Assertions.assertEquals(0L, zeze.newProcedure(() -> {
			rank.updateRank(0, key, 88, new BValueLong(999));
			return 0L;
		}, "update-table").call());

		// 旧快照引用保持旧值：快照与表彻底别名隔离，表更新不改变已发出的快照。
		var oldEntry = holder[0].getTableValue().getRankListReadOnly().get(0);
		Assertions.assertEquals(200L, ((BValueLong)oldEntry.getDynamicReadOnly().getBean()).getValue(),
				"旧快照内容不得随表更新变化");
		Assertions.assertEquals(88L, oldEntry.getRoleId());
	}
}
