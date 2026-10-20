package Benchmark;

import java.util.Objects;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import Zeze.Config;
import Zeze.Transaction.CheckpointFlushMode;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import demo.App;
import org.junit.jupiter.api.Assertions;

// K 提交洪水下的 checkpoint 竞争探针（生产形态）：E 稳态的脏化不走派发池、flush 与提交
// 时间零重叠，覆盖不了"并发提交者与 flusher 争 RRS 锁"的形态。本场景派发池饱和提交
// （全量提交无批次屏障，对齐 B 形态）持续冲刷 Keys 个热 key 制造 RRS 合并竞争，同时
// flusher 线程连续 checkpointRun 抢同一批锁。观测口：RRS.flushLockWait 聚合分布、
// FlushSet 取锁告警（zeze.rrsFlushLockWaitWarnMs）、flush 迭代计数与单轮最大耗时——
// RRS 非公平锁取舍下 flusher 饿死（=持久化停摆）风险的实证探针。JUnit 默认固定 VT 池；
// 加 -DuseUnlimitedVirtualThread=true 对齐生产无界 VT 形态。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class KCheckpointUnderCommitFlood {
	public static final int Keys = 64;          // 热 key 子集：冲突集中到少量 RRS 编组
	public static final int TxnCount = 500_000;
	public static final int InFlight = 10_000;  // 在飞上限：约束无界 VT 形态下的 VT 驻留总量
	public static final int Warmups = Integer.getInteger("bench.flood.warmups", 1);
	public static final int Rounds = Integer.getInteger("bench.flood.rounds", 3);

	@Test
	public void benchFlood() throws Exception {
		App.Instance.Stop();
		var cfg = Config.load("zeze.xml");
		cfg.setCheckpointPeriod(Integer.MAX_VALUE); // 关自动周期，flush 节奏由本场景显式驱动
		cfg.setCheckpointFlushMode(CheckpointFlushMode.MultiThreadMerge);
		App.Instance.Start(cfg);
		try {
			prepare();
			var completed = new LongAdder();
			var flushIterations = new AtomicLong();
			var flushMaxNanos = new AtomicLong();
			MacroBench.run("K_CheckpointUnderCommitFlood", Warmups, Rounds, TxnCount, () -> {
				var stop = new AtomicBoolean();
				var flusher = new Thread(() -> {
					while (!stop.get()) {
						var t0 = System.nanoTime();
						try {
							App.Instance.getZeze().checkpointRun();
						} catch (Exception e) {
							throw Task.forceThrow(e);
						}
						var d = System.nanoTime() - t0;
						flushIterations.incrementAndGet();
						flushMaxNanos.accumulateAndGet(d, Math::max);
					}
				}, "K-flood-flusher");
				flusher.setDaemon(true);
				flusher.start();
				try {
					var permits = new Semaphore(InFlight);
					var tasks = new ArrayList<Future<?>>(TxnCount);
					for (int i = 0; i < TxnCount; ++i) {
						final long key = i % Keys;
						permits.acquire();
						tasks.add(TaskSpec.ofProcedure(App.Instance.getZeze().newProcedure(() -> {
							try {
								var r = Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(key);
								r.setLong2(r.getLong2() + 1);
							} finally {
								completed.increment();
								permits.release();
							}
							return 0L;
						}, "floodWrite")).submitNow());
					}
					for (var task : tasks)
						task.get();
				} finally {
					stop.set(true);
					flusher.join();
				}
			});
			System.out.printf("K flusher: iterations=%d maxFlushMs=%.3f%n",
					flushIterations.get(), flushMaxNanos.get() / 1e6);
			System.out.println(ZezeCounter.instance.collectAndReset().formattedLog());
			// 活性断言：任何一轮里 flusher 都必须持续推进（迭代数随轮次单调增长是软信号；
			// 硬故障形态是饥饿导致的停顿，由上面的 maxFlushMs 与 flushLockWait 分布呈报）。
			Assertions.assertTrue(flushIterations.get() >= Rounds, "flusher 整轮未推进，疑似饿死");
			App.Instance.getZeze().newProcedure(KCheckpointUnderCommitFlood::Check, "check").call();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long Check() {
		// 每 key 每轮被 +1 的次数 = TxnCount 按 Keys 取模分配（前 TxnCount%Keys 个 key 多一次），
		// 与 B 的 Check 同理
		long base = TxnCount / Keys, rem = TxnCount % Keys;
		for (long k = 0; k < Keys; ++k) {
			long expectPerKey = (long)(Warmups + Rounds) * (base + (k < rem ? 1 : 0));
			Assertions.assertEquals(expectPerKey, Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(k).getLong2(),
					"key=" + k);
		}
		return 0;
	}

	private static void prepare() throws Exception {
		// 逐 key 小事务建立热 key 集（对齐 H 教训：单大事务预热会造 RRS 大编组污染形态），
		// 随后冲散，让测量轮从干净编组状态开始。
		for (long key = 0; key < Keys; ++key) {
			final long k = key;
			App.Instance.getZeze().newProcedure(() -> {
				Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(k).setLong2(0L);
				return 0L;
			}, "prepare").call();
		}
		App.Instance.getZeze().checkpointRun();
	}
}
