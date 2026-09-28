package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.TaskSpec;
import demo.App;
import org.junit.jupiter.api.Assertions;

// 调度形态：同负载（5000 key 一般并发）经线程池 submitNow 派发的吞吐。
// performance.md 的 GlobalAsync>50w/s、虚拟线程>15w/s 在仓内无落成基准，本场景补齐。
// 跑法：gradle benchCore（默认虚拟线程池）+ gradle benchCorePlatform（-DuseVirtualThread=false
// 平台线程池，gradle 任务按 *FDispatch* 过滤只跑本类）。两配置各写一份 JSON
// （F_DispatchPool_Virtual / F_DispatchPool_Platform），吞吐差即锁适配虚拟线程的代价。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class FDispatchPoolThroughput {
	public static final int AddCount = 500_000;
	public static final int ConcurrentLevel = 5_000;
	public static final int Batch = 200;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		var virtual = PropertiesHelper.getBool("useVirtualThread", true);
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("F_DispatchPool_" + (virtual ? "Virtual" : "Platform"), Warmups, Rounds, AddCount, () -> {
				removeAll();
				var tasks = new ArrayList<Future<Long>>(Batch);
				for (int i = 0; i < AddCount; ++i) {
					final long key = i % ConcurrentLevel;
					tasks.add(TaskSpec.ofProcedure(
							App.Instance.Zeze.newProcedure(() -> Add(key), "Add")).submitNow());
					if ((i + 1) % Batch == 0) {
						for (var task : tasks)
							task.get();
						tasks.clear();
					}
				}
				for (var task : tasks)
					task.get();
			});
			System.out.println(Zeze.Util.ZezeCounter.instance.collectAndReset().formattedLog());
			App.Instance.Zeze.newProcedure(FDispatchPoolThroughput::Check, "check").call();
			removeAll();
		} finally {
			//App.Instance.Stop();
		}
	}

	// ofAction 派发（生产协议处理器的真实路径；ofProcedure 的 statsKey 为 null 不走
	// addTaskRunTime 统计路径，本变体补该覆盖）。任务体不含事务，纯派发+统计开销。
	@Test
	public void testActionDispatch() throws Exception {
		var virtual = PropertiesHelper.getBool("useVirtualThread", true);
		MacroBench.run("F_DispatchAction_" + (virtual ? "Virtual" : "Platform"), Warmups, Rounds, AddCount, () -> {
			var tasks = new ArrayList<Future<?>>(Batch);
			var counter = new java.util.concurrent.atomic.AtomicLong();
			for (int i = 0; i < AddCount; ++i) {
				tasks.add(TaskSpec.ofAction(counter::incrementAndGet).name("dispatchAction").submitNow());
				if ((i + 1) % Batch == 0) {
					for (var task : tasks)
						task.get();
					tasks.clear();
				}
			}
			for (var task : tasks)
				task.get();
			Assertions.assertEquals(AddCount, counter.get());
		});
	}

	private static long Check() {
		long sum = 0;
		for (long key = 0; key < ConcurrentLevel; ++key) {
			var r = App.Instance.demo_Module1.getTable1().getOrAdd(key);
			sum += r.getLong2();
		}
		Assertions.assertEquals(AddCount, sum);
		return 0;
	}

	private static long Add(long key) {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(key);
		r.setLong2(r.getLong2() + 1);
		return 0;
	}

	private static void removeAll() throws Exception {
		for (long key = 0; key < ConcurrentLevel; ++key) {
			final long rk = key;
			App.Instance.Zeze.newProcedure(() -> Remove(rk), "remove").call();
		}
	}

	private static long Remove(long key) {
		App.Instance.demo_Module1.getTable1().remove(key);
		return 0;
	}
}
