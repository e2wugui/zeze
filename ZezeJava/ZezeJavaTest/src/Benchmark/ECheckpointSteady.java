package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Transaction.CheckpointFlushMode;
import demo.App;

// checkpoint 稳态（收编原 CheckpointFlush.java，多轮+JSON）：每轮先把 Records 条记录弄脏
// （计时段外），再计时 checkpointRun 的完整 flush（序列化+提交，Memory 库无磁盘 IO）。
// 旧三场景测量窗口（4~7s）内 CheckpointPeriod(60s) 根本不触发，本场景补该路径覆盖。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class ECheckpointSteady {
	public static final int Records = 10_000;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void benchFlushMultiThreadMerge() throws Exception {
		bench("E_CheckpointFlush_MultiThreadMerge", CheckpointFlushMode.MultiThreadMerge);
	}

	@Test
	public void benchFlushSingleThread() throws Exception {
		bench("E_CheckpointFlush_SingleThread", CheckpointFlushMode.SingleThread);
	}

	private static void bench(String name, CheckpointFlushMode mode) throws Exception {
		App.Instance.Stop();
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(Integer.MAX_VALUE); // 关自动 flush，轮内手动 checkpointRun
		cfg.setCheckpointFlushMode(mode);
		App.Instance.Start(cfg);
		try {
			var round = new int[]{0}; // 每轮换写值保证记录必脏
			MacroBench.runTimed(name, Warmups, Rounds, Records, () -> {
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
