package UnitTest.Zeze.Game;

import java.util.function.LongUnaryOperator;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BRankValue;
import Zeze.Game.Rank;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.ConcurrentHashSet;
import demo.SimpleApp;
import junit.framework.TestCase;

public class TestRank extends TestCase {
	private static final int CONC_LEVEL = 100;
	private static final int APP_COUNT = 3;
	private static final int ROLE_ID_BEGIN = 1000;
	private static final int SERVER_ID_BEGIN = 1;
	private static final int RANK_TYPE = 1;

	private final SimpleApp[] apps = new SimpleApp[APP_COUNT];

	private boolean disableTest = false;

	@Override
	protected void setUp() {
		var config = Zeze.Config.load();
		if (config.getGlobalCacheManagerHostNameOrAddress().contains(".xml")) {
			System.out.println("Test Rank Disable On GlobL-Raft.");
			disableTest = true;
			return;
		}

		System.out.println("------ setUp begin");
		try {
			for (int i = 0; i < APP_COUNT; i++)
				(apps[i] = new SimpleApp(SERVER_ID_BEGIN + i)).start();

			System.out.println("Begin Thread.sleep");
			Thread.sleep(2000); // wait connected
			for (int i = 0; i < APP_COUNT; i++) {
				System.out.format("End Thread.sleep app%d:%n", SERVER_ID_BEGIN + i);
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

	@Override
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

	public void testRank() throws Exception {
		if (disableTest)
			return;

		try {
			System.out.println("------ testRank begin");
			SimpleApp app = apps[0]; // 可以随便取一个, 不过都是对称的, 应该不用都测

			app.rank.funcConcurrentLevel = rankType -> CONC_LEVEL;
			int concLevel = app.rank.getConcurrentLevel(RANK_TYPE);
			var rankKey = Rank.newRankKey(RANK_TYPE, BConcurrentKey.TimeTypeTotal);
			LongUnaryOperator roleId2Value = roleId -> roleId * 10;

			for (int hash = 0; hash < concLevel; hash++) {
				int h = hash;
				long roleId = ROLE_ID_BEGIN + h;
				app.getZeze().newProcedure(() -> {
					app.rank.updateRank(h, rankKey, roleId, roleId2Value.applyAsLong(roleId), Binary.Empty).await().onSuccess(r -> {
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
					assertEquals(roleId2Value.applyAsLong(rank.getRoleId()), rank.getValue());
				}
				return Procedure.Success;
			}, "getRankDirect").call();

			var hashSet1 = new ConcurrentHashSet<Integer>();
			var hashSet2 = new ConcurrentHashSet<Integer>();
			app.getZeze().newProcedure(() -> {
				app.rank.getRankAll(rankKey).onResult(r -> {
					assertNotNull(r);
//					System.out.format("--- getRankAll onResult: hash=%d, resultCode=%d, rankList=%s%n",
//							r.getHash(), r.getResultCode(), r.rankList);
					assertTrue(r.getHash() >= 0 && r.getHash() < concLevel);
					assertTrue(hashSet1.add(r.getHash()));
					assertEquals(0, r.getResultCode());
					assertEquals(1, r.rankList.getRankList().size());
					var rank = r.rankList.getRankList().get(0);
					assertTrue(rank.getRoleId() >= ROLE_ID_BEGIN && rank.getRoleId() < ROLE_ID_BEGIN + concLevel);
					assertEquals(roleId2Value.applyAsLong(rank.getRoleId()), rank.getValue());
				}).onAllDone(ctx -> {
					assertNotNull(ctx);
					var results = ctx.getAllResults();
//					System.out.format("--- getRankAll onAllDone: timeout=%b, results.size=%d, concurrent=%d%n",
//							ctx.isTimeout(), results.size(), ctx.getConcurrentLevel());
					assertFalse(ctx.isTimeout());
					assertEquals(concLevel, results.size());
					assertEquals(concLevel, ctx.getConcurrentLevel());
					results.foreachValue(r -> {
//						System.out.format("        hash=%d, resultCode=%d, rankList=[%d]:%s%n",
//								r.getHash(), r.getResultCode(), r.rankList.getRankList().size(), r.rankList);
						assertTrue(r.getHash() >= 0 && r.getHash() < concLevel);
						assertTrue(hashSet2.add(r.getHash()));
						assertEquals(0, r.getResultCode());
						assertEquals(1, r.rankList.getRankList().size());
						var rank = r.rankList.getRankList().get(0);
						assertTrue(rank.getRoleId() >= ROLE_ID_BEGIN && rank.getRoleId() < ROLE_ID_BEGIN + concLevel);
						assertEquals(roleId2Value.applyAsLong(rank.getRoleId()), rank.getValue());
					});
				}).await();
				return Procedure.Success;
			}, "getRankAll").call();
			assertEquals(concLevel, hashSet1.size());
			assertEquals(concLevel, hashSet2.size());
		} catch (Throwable e) {
			// print stacktrace.
			e.printStackTrace();
			throw e;
		} finally {
			System.out.println("------ testRank end");
		}
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
