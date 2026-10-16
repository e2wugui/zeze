package Benchmark;

import java.util.Objects;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Util.TaskSpec;
import demo.App;
import org.junit.jupiter.api.Assertions;

// 只读事务场景：A~G 全是写事务，读多写少的服务端主形态（读锁+缓存命中+零日志零脏集的
// 提交路径）此前无覆盖。负载骨架对齐 C（同并发级、同派发形态、同批次参数），C/H 直接
// 对比即写路径开销（日志编码+脏集维护+checkpoint 分摊）。数据预热在测量轮外（写事务
// 建满 5000 key），读过程值经静态字段带出校验（返回 0，避免 defaultLogAction）。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class HReadOnlyConcurrent {
	public static final int Keys = 5_000;       // 工作集（缓存全命中，对齐 C 的 ConcurrentLevel）
	public static final int TxnCount = 1_000_000;
	public static final int Batch = 200;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		// 档一（默认）：脏记录读——记录由 prepare 逐 key 小事务写入后保持已提交未冲刷态
		// （demo 配置 checkpoint 周期实际关闭），对齐"读近期写入热数据"的真实负载档位。
		bench("H_ReadOnlyConcurrent", false);
	}

	@Test
	public void testBenchmarkClean() throws Exception {
		// 档二：干净记录读——prepare 后冲刷，读已落盘记录。两档差（实测 ~2.4×）即
		// 脏读记账成本：每个读事务的 RRS merge + checkpoint map 重登记在并发下的锁车队
		// （JFR 热点=RelativeRecordSet 公平锁）。两代代码同机制（v1.6.3 同构倒挂）。
		bench("H_ReadOnlyConcurrent_Clean", true);
	}

	private void bench(String name, boolean flushAfterPrepare) throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			prepare();
			if (flushAfterPrepare)
				App.Instance.getZeze().checkpointRun();
			MacroBench.run(name, Warmups, Rounds, TxnCount, () -> {
				var tasks = new ArrayList<Future<?>>(Batch);
				for (int i = 0; i < TxnCount; ++i) {
					final long key = i % Keys;
					tasks.add(TaskSpec.ofProcedure(
							App.Instance.getZeze().newProcedure(() -> Read(key), "read")).submitNow());
					if ((i + 1) % Batch == 0) {
						for (var task : tasks)
							task.get();
						tasks.clear();
					}
				}
				for (var task : tasks)
					task.get();
			});
			App.Instance.getZeze().newProcedure(HReadOnlyConcurrent::Check, "check").call();
			clearAll();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static final java.util.concurrent.atomic.LongAdder readSum = new java.util.concurrent.atomic.LongAdder();
	private static final java.util.concurrent.atomic.AtomicLong missCount = new java.util.concurrent.atomic.AtomicLong();

	private static long Read(long key) {
		// 真读路径：get()（可空）而非 getOrAdd()——后者带创建语义，掩盖只读锁路径。
		// 累加用 LongAdder：池线程热路径上的单 AtomicLong 会缓存行争串行化，污染吞吐
		var r = Objects.requireNonNull(App.Instance.demo_Module1).getTable1().get(key);
		if (r == null)
			missCount.incrementAndGet();
		else
			readSum.add(r.getLong2());
		return 0;
	}

	private static void prepare() throws Exception {
		clearAll();
		// 逐 key 小事务预热（对齐真实"读多写少"负载的记录来源形态）：单大事务写 5000 条会
		// 造出一个 RelativeRecordSet 大编组，此后所有读事务在该编组的公平锁上串行车队
		// （实测 31.6万 vs 逐key 79.8万，2.5×；JFR 热点=RRS 公平锁+merge）。大编组现象
		// 是真实的负载病理（全服批量事务后的并发读热点），但其度量属专门变体，不混入本场景。
		for (long key = 0; key < Keys; ++key) {
			final long k = key;
			App.Instance.getZeze().newProcedure(() -> {
				Objects.requireNonNull(App.Instance.demo_Module1).getTable1().getOrAdd(k).setLong2(k);
				return 0L;
			}, "prepare").call();
		}
	}

	private static long Check() {
		// 每 key 被读 (Warmups+Rounds)*TxnCount/Keys 次，每次读到值=key
		long reads = (long)(Warmups + Rounds) * (TxnCount / Keys);
		long expect = reads * (Keys - 1) * Keys / 2;
		Assertions.assertEquals(expect, readSum.sum());
		Assertions.assertEquals(0, missCount.get(), "预热的 key 必须全命中");
		return 0;
	}

	private static void clearAll() throws Exception {
		readSum.reset();
		App.Instance.getZeze().newProcedure(() -> {
			for (long key = 0; key < Keys; ++key)
				Objects.requireNonNull(App.Instance.demo_Module1).getTable1().remove(key);
			return 0L;
		}, "clear").call();
	}
}
