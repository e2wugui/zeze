package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import demo.App;
import org.junit.jupiter.api.Assertions;

// 缓存压力场景：工作集（10万 key）远超缓存容量（1000），顺序轮询访问保证每次命中都是
// miss——完整走过 装载（Database 拷贝+bean decode）→ 修改 → 驱逐（脏记录 flush=encode+写回）
// 的 cache-thrash 循环。A~F 全场景缓存全命中，performance.md 自称"性能核心是 Cache 命中率"，
// 本场景补 miss/装载/LRU 路径覆盖（grill 拍板的二期空位）。
// checkpoint 周期关掉：驱逐路径自带脏记录 flush，测的就是逐记录装载+写回。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class GCacheThrashing {
	public static final int Keys = 100_000;  // 工作集
	public static final int Capacity = 1_000; // 缓存容量（factor=1，真实容量同值）
	public static final int TxnCount = 200_000;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(Integer.MAX_VALUE); // 驱逐自带 flush，不需要周期 checkpoint
		var tc = new Config.TableConf();
		tc.setCacheCapacity(Capacity);
		tc.setCacheFactor(1.0f);
		cfg.getTableConfMap().put("demo_Module1_Table1", tc);
		App.Instance.Start(cfg);
		try {
			clearAll();
			MacroBench.run("G_CacheThrashing", Warmups, Rounds, TxnCount, () -> {
				for (int i = 0; i < TxnCount; ++i) {
					final long key = i % Keys;
					App.Instance.Zeze.newProcedure(() -> Add(key), "Add").call();
				}
			});
			check();
			clearAll();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long Add(long key) {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(key);
		r.setLong2(r.getLong2() + 1);
		return 0;
	}

	private static volatile long checkSum;

	private static void check() throws Exception {
		// 全量校验在测量轮外：每 key 应得 (Warmups+Rounds)*TxnCount/Keys 次自增
		//（校验本身再触发一轮全量 miss 装载）。过程返回 0，结果经字段带出——
		// 非零返回码会触发 defaultLogAction 逐次记日志。
		App.Instance.Zeze.newProcedure(() -> {
			long sum = 0;
			for (long key = 0; key < Keys; ++key)
				sum += App.Instance.demo_Module1.getTable1().getOrAdd(key).getLong2();
			checkSum = sum;
			return 0L;
		}, "check").call();
		long perKey = (long)(Warmups + Rounds) * (TxnCount / Keys);
		Assertions.assertEquals(perKey * Keys, checkSum);
	}

	private static void clearAll() throws Exception {
		App.Instance.Zeze.newProcedure(() -> {
			for (long key = 0; key < Keys; ++key)
				App.Instance.demo_Module1.getTable1().remove(key);
			return 0L;
		}, "clear").call();
		App.Instance.Zeze.checkpointRun();
	}
}
