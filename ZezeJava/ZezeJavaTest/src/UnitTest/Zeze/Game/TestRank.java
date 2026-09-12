package UnitTest.Zeze.Game;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.LongUnaryOperator;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BRankValue;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Game.Rank;
import Zeze.Transaction.Procedure;
import demo.SimpleApp;
import harness.TestEnv;

public class TestRank {
	private static final int CONC_LEVEL = 100;
	private static final int APP_COUNT = 3;
	private static final int ROLE_ID_BEGIN = 1000;
	private static final int SERVER_ID_BEGIN = 1;
	private static final int RANK_TYPE = 1;

	private final SimpleApp[] apps = new SimpleApp[APP_COUNT];

	private boolean disableTest = false;

	@BeforeEach
	protected void setUp() {
		var config = Zeze.Config.load();
		if (config.hasGlobalRaft()) {
			System.out.println("Test Rank Disable On GlobL-Raft.");
			disableTest = true;
			return;
		}

		System.out.println("------ setUp begin");
		try {
			for (int i = 0; i < APP_COUNT; i++)
				(apps[i] = new SimpleApp(SERVER_ID_BEGIN + i)).start();

			// 等待每个 app 的 ServiceManager 订阅推送看到全部 server 注册（Rank 按 serverId 重定向到其他 app），替代盲等 2s
			for (var app : apps)
				TestEnv.waitServerRegisteredRange(app.getZeze(), SERVER_ID_BEGIN, APP_COUNT);
			for (int i = 0; i < APP_COUNT; i++) {
				System.out.format("waitServerRegistered app%d done:%n", SERVER_ID_BEGIN + i);
				apps[i].getZeze().getServiceManager().getSubscribeStates().forEach((name, state) -> {
					System.out.format("  '%s':%n", name);
					state.getLocalStates().forEach((k, v) -> System.out.format("    { %s, %s }%n", k, v));
				});
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("------ setUp end");
		}
	}

	@AfterEach
	protected void tearDown() {
		if (disableTest)
			return;
		System.out.println("------ tearDown begin");
		try {
			for (int i = 0; i < APP_COUNT; i++)
				apps[i].stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println("------ tearDown end");
	}

	@Test
	public void testRank() {
		if (disableTest)
			return;

		try {
			System.out.println("------ testRank begin");
			SimpleApp app = apps[0]; // 可以随便取一个, 不过都是对称的, 应该不用都测

			app.rank.setFuncConcurrentLevel(rankType -> CONC_LEVEL);
			int concLevel = app.rank.getConcurrentLevel(RANK_TYPE);
			var rankKey = Rank.newRankKey(RANK_TYPE, BConcurrentKey.TimeTypeTotal);
			LongUnaryOperator roleId2Value = roleId -> roleId * 10;

			for (int hash = 0; hash < concLevel; hash++) {
				int h = hash;
				long roleId = ROLE_ID_BEGIN + h;
				app.getZeze().newProcedure(() -> {
					app.rank.updateRank(h, rankKey, roleId, new BValueLong(roleId2Value.applyAsLong(roleId))).await().onSuccess(r -> {
						assertNotNull(r);
						assertEquals(Procedure.Success, r.longValue());
					}).onFail(e -> {
						e.printStackTrace();
						fail();
					});
					return Procedure.Success;
				}, "updateRank").call();
			}

			app.getZeze().newProcedure(() -> {
				// 直接从数据库读取并合并
				var result = app.rank.getRankDirect(rankKey);
//				System.out.format("--- getRankDirect: concurrent=%d, rankList=[%d]:%s%n",
//						concLevel, result.getRankList().size(), result);
				assertEquals(concLevel, result.getRankList().size());
				for (BRankValue rank : result.getRankList()) {
					assertTrue(rank.getRoleId() >= ROLE_ID_BEGIN && rank.getRoleId() < ROLE_ID_BEGIN + concLevel);
					assertEquals(roleId2Value.applyAsLong(rank.getRoleId()), ((BValueLong)rank.getDynamic().getBean()).getValue());
				}
				return Procedure.Success;
			}, "getRankDirect").call();
		} catch (Throwable e) {
			// print stacktrace.
			e.printStackTrace();
			throw e;
		} finally {
			System.out.println("------ testRank end");
		}
	}

	// FND4-77：deleteRank/mergeRank 不失效 rankCached——查询过（缓存建立）→删榜→立即查询
	// 命中未过期缓存返回已删旧榜（窗口期=RankCacheTimeout默认5分钟）。
	// 修复后：写路径按rankType整类失效缓存（提交后），删除/合并后立即查询必须重建。
	@Test
	public void testDeleteAndMergeRankInvalidateCache() {
		if (disableTest)
			return;
		var app = apps[0];
		app.rank.setFuncConcurrentLevel(rankType -> CONC_LEVEL);
		var rankKey = Rank.newRankKey(RANK_TYPE, BConcurrentKey.TimeTypeTotal);

		app.getZeze().newProcedure(() -> {
			// 造榜+建缓存：非空快照
			updateOk(app, 0, rankKey, ROLE_ID_BEGIN, 100);
			assertFalse(app.rank.getRankTotal(rankKey).getTableValue().getRankListReadOnly().isEmpty(),
					"预置：缓存建立且非空");
			// 删榜：写路径必须失效缓存
			app.rank.deleteRank(rankKey);
			return Procedure.Success;
		}, "FND4_77.setupAndDelete").call();

		// 红断言（新事务）：删除后立即查询必须空榜（原实现命中未过期缓存返回旧榜）
		app.getZeze().newProcedure(() -> {
			assertTrue(app.rank.getRankTotal(rankKey).getTableValue().getRankListReadOnly().isEmpty(),
					"deleteRank后不得返回旧榜缓存（FND4-77）");
			return Procedure.Success;
		}, "FND4_77.getRankTotalAfterDelete").call();

		// mergeRank 同理：from/to同rankType不同offset，合并后to的缓存必须失效重建
		var fromKey = new BConcurrentKey(RANK_TYPE, 0, BConcurrentKey.TimeTypeTotal, 2026, 111);
		var toKey = new BConcurrentKey(RANK_TYPE, 0, BConcurrentKey.TimeTypeTotal, 2026, 222);
		app.getZeze().newProcedure(() -> {
			updateOk(app, 0, toKey, ROLE_ID_BEGIN, 50);
			assertFalse(app.rank.getRankTotal(toKey).getTableValue().getRankListReadOnly().isEmpty(),
					"预置：to榜缓存建立且非空");
			updateOk(app, 0, fromKey, ROLE_ID_BEGIN + 1, 200);
			updateOk(app, 0, fromKey, ROLE_ID_BEGIN + 2, 300);
			app.rank.mergeRank(fromKey, toKey);
			return Procedure.Success;
		}, "FND4_77.mergeRank").call();

		// 红断言（新事务）：合并后立即查询必须包含from成员（原实现返回合并前旧快照）
		app.getZeze().newProcedure(() -> {
			var merged = app.rank.getRankTotal(toKey).getTableValue().getRankListReadOnly();
			assertEquals(3, merged.size(), "mergeRank后不得返回旧快照（FND4-77）");
			boolean hasFrom = false;
			for (var v : merged)
				hasFrom |= v.getRoleId() == ROLE_ID_BEGIN + 2;
			assertTrue(hasFrom, "from成员必须出现在合并后的to榜");
			return Procedure.Success;
		}, "FND4_77.getRankTotalAfterMerge").call();
	}

	private void updateOk(SimpleApp app, int hash, BConcurrentKey key, long roleId, long value) {
		app.rank.updateRank(hash, key, roleId, new BValueLong(value)).await().onSuccess(r ->
				assertEquals(Procedure.Success, r.longValue())).onFail(e -> {
			e.printStackTrace();
			fail();
		});
	}

	// 用于生成Redirect代码
	public static void main(String[] args) throws Exception {
		TestRank testRank = new TestRank();
		try {
			testRank.setUp();
			testRank.testRank();
		} finally {
			testRank.tearDown();
		}
	}
}
