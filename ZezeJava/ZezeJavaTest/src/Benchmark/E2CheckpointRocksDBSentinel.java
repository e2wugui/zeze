package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Transaction.CheckpointFlushMode;
import demo.App;

// RocksDB 回归哨兵：同 E 场景但默认库切 RocksDB（bench-results/rocksdb-e2，gitignored）。
// 定位不是精确对比（开发机磁盘 IO 噪声大），而是防“Memory 口径上的优化在真实落盘路径
// 变差”——只看数量级是否塌（如掉 30% 报警），不进 before/after 显著性判定。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class E2CheckpointRocksDBSentinel {
	public static final int Records = 10_000;
	public static final int Warmups = 2;
	public static final int Rounds = 3;

	@Test
	public void benchFlushMultiThreadMerge() throws Exception {
		App.Instance.Stop();
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(Integer.MAX_VALUE);
		cfg.setCheckpointFlushMode(CheckpointFlushMode.MultiThreadMerge);
		var rocks = new Config.DatabaseConf();
		rocks.setDatabaseType(Config.DbType.RocksDb);
		rocks.setDatabaseUrl("bench-results/rocksdb-e2");
		cfg.getDatabaseConfMap().put("", rocks);
		// RocksDb 与 GlobalCacheManager 互斥（Config.createDatabase 校验 hasGlobal），哨兵单进程清掉
		cfg.setGlobalCacheManagerHostNameOrAddress("");
		App.Instance.Start(cfg);
		try {
			var round = new int[]{0};
			MacroBench.runTimed("E2_CheckpointRocksDB_MultiThreadMerge", Warmups, Rounds, Records, () -> {
				dirtyRecords(++round[0]);
				long begin = System.nanoTime();
				App.Instance.Zeze.checkpointRun();
				return (System.nanoTime() - begin) / 1e9;
			});
		} finally {
			//App.Instance.Stop();
		}
	}

	private static void dirtyRecords(int roundNo) throws Exception {
		for (long k = 0; k < Records; ++k) {
			final long key = k;
			final long value = key + roundNo;
			App.Instance.Zeze.newProcedure(() -> {
				App.Instance.demo_Module1.getTable1().getOrAdd(key).setLong2(value);
				return 0L;
			}, "dirty").call();
		}
	}
}
