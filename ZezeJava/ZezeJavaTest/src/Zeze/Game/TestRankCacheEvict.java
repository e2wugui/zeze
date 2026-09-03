package Zeze.Game;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Game.Rank.BConcurrentKey;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import harness.Fast;

/**
 * FND-G1-7 回归：Rank.rankCached 只增不减——周期榜（Day/Week/...）每个新周期 key、
 * 自定义榜（TimeTypeCustomize）每个 customizeId 都会在首次 getRankTotal 时新建 RankTotal
 * 并持有全量合并快照，旧 key 不再被查询却常驻进程结束。
 * 修复后 getRankTotal 在缓存超过容量上限时执行淘汰：先淘汰已过期条目（下次访问自动重建），
 * 仍超限则按 BuildTime 淘汰最旧的（近似 LRU），当前正在服务的条目（keep）受保护。
 * 自包含（内存库 + disable ServiceManager，无外部进程），标 @Fast。
 */
@Fast
public class TestRankCacheEvict {

	private static final int RANK_TYPE = 1;

	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(7350);

	private static Application newApp(String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rank_cache_evict_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	// fake ProviderApp 仅建立 zeze.redirect（Rank.Initialize 读 redirect.providerApp），
	// RegisterProtocols 为生成空实现。
	private static Rank newRank(Application zeze) {
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		new ProviderApp(zeze);
		var rank = new Rank(app); // protected 构造：测试与被测同包 Zeze.Game
		rank.Initialize(app);     // RegisterZezeTables（RegisterProtocols 为空）
		return rank;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<BConcurrentKey, Rank.RankTotal> rankCachedOf(Rank rank) throws Exception {
		var field = Rank.class.getDeclaredField("rankCached");
		field.setAccessible(true);
		return (ConcurrentHashMap<BConcurrentKey, Rank.RankTotal>)field.get(rank);
	}

	private static BConcurrentKey customizeKey(long customizeId) {
		return Rank.newRankKey(RANK_TYPE, customizeId);
	}

	private static long getRankTotalInProcedure(Application app, Rank rank, BConcurrentKey key) {
		return app.newProcedure(() -> {
			rank.getRankTotal(key);
			return 0L;
		}, "TestRankCacheEvict.getRankTotal").call();
	}

	@Test
	public void testEvictOverCapacityKeepNewest() throws Exception {
		var app = newApp("TestRankCacheEvict1");
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2); // 减少重建时合并的段数
		rank.setFuncRankCacheCapacity(t -> 2);
		app.start();
		try {
			// 默认 rankCacheTimeout=5min：测试期间条目不会自然过期，走"最旧淘汰"分支

			for (long id = 1; id <= 5; ++id) {
				Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(id)));
				Thread.sleep(2); // BuildTime 毫秒级：区分新旧，避免同毫秒平局使淘汰顺序不定
			}

			var cached = rankCachedOf(rank);
			// 修复前：5 个 key 全部滞留（size==5）；修复后：上限 2
			Assertions.assertTrue(cached.size() <= 2, "rankCached must be bounded, size=" + cached.size());
			Assertions.assertFalse(cached.containsKey(customizeKey(1)), "最旧条目应被淘汰");
			Assertions.assertTrue(cached.containsKey(customizeKey(4)));
			Assertions.assertTrue(cached.containsKey(customizeKey(5)), "最新条目应保留");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}

	@Test
	public void testEvictExpiredFirst() throws Exception {
		var app = newApp("TestRankCacheEvict2");
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2);
		rank.setFuncRankCacheCapacity(t -> 2);
		rank.setFuncRankCacheTimeout(t -> 0L); // 恒过期：淘汰时过期分支可先行清理
		app.start();
		try {

			for (long id = 1; id <= 4; ++id)
				Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(id)));

			var cached = rankCachedOf(rank);
			Assertions.assertTrue(cached.size() <= 2, "rankCached must be bounded, size=" + cached.size());
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}

	@Test
	public void testKeepProtectedAndRebuildAfterEvict() throws Exception {
		var app = newApp("TestRankCacheEvict3");
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2);
		rank.setFuncRankCacheCapacity(t -> 2);
		app.start();
		try {
			// 默认 timeout：条目在测试期间保持新鲜

			Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(1)));
			Thread.sleep(2);
			Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(2)));
			Thread.sleep(2);
			// 第三次访问：新建的 keep(key3) 受保护，淘汰最旧的 key1
			Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(3)));
			var cached = rankCachedOf(rank);
			Assertions.assertEquals(2, cached.size());
			Assertions.assertFalse(cached.containsKey(customizeKey(1)));
			Assertions.assertTrue(cached.containsKey(customizeKey(2)));
			Assertions.assertTrue(cached.containsKey(customizeKey(3)));

			// 再访问已淘汰的 key1：重建（BuildTime 更新），此时 key2 成为最旧被淘汰
			Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(1)));
			cached = rankCachedOf(rank);
			Assertions.assertEquals(2, cached.size());
			Assertions.assertTrue(cached.containsKey(customizeKey(1)), "被淘汰条目再次访问必须可重建");
			Assertions.assertFalse(cached.containsKey(customizeKey(2)));
			Assertions.assertTrue(cached.containsKey(customizeKey(3)));
			Assertions.assertTrue(cached.get(customizeKey(1)).getBuildTime() > 0, "重建后 BuildTime 应已设置");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}

	@Test
	public void testEvictedEntryRebuildKeepsData() throws Exception {
		var app = newApp("TestRankCacheEvict4");
		var rank = newRank(app);
		rank.setFuncConcurrentLevel(t -> 2);
		rank.setFuncRankCacheCapacity(t -> 1);
		app.start();
		try {

			// 写入榜单：roleId 越大 value 越大（LongOnlyCompactor 降序）
			var keyA = customizeKey(100);
			for (long roleId = 1; roleId <= 5; ++roleId) {
				long h = roleId;
				Assertions.assertEquals(0L, app.newProcedure(() -> {
					rank.updateRank((int)h, keyA, h, new BValueLong(h * 10)); // 本地直调，返回已完成 future
					return 0L;
				}, "updateRank").call());
			}

			// 用 keyB 挤掉 keyA（容量 1），再访问 keyA 触发重建，数据必须完整
			Assertions.assertEquals(0L, getRankTotalInProcedure(app, rank, customizeKey(200)));
			var cached = rankCachedOf(rank);
			Assertions.assertEquals(1, cached.size());
			Assertions.assertFalse(cached.containsKey(keyA));

			var position = new long[1];
			Assertions.assertEquals(0L, app.newProcedure(() -> {
				position[0] = rank.getRankPosition(keyA, 5L);
				return 0L;
			}, "getRankPosition").call());
			Assertions.assertEquals(1L, position[0], "value 最大的 roleId=5 应排第一");
			cached = rankCachedOf(rank);
			Assertions.assertTrue(cached.containsKey(keyA), "重建后 keyA 应回到缓存");
		} finally {
			try {
				app.stop();
			} catch (Exception ignored) {
			}
		}
	}
}
