package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Util.TaskSpec;
import demo.App;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class BBasicSimpleAddConcurrentWithConflict {
	// 全部事务互踩同一个 key：几乎必然冲突重做。重钉任务量对齐 performance.md 记录量级
	public static final int AddCount = 200_000;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("B_ConcurrentWithConflict", Warmups, Rounds, AddCount, () -> {
				App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Remove, "remove").call();
				// 全量提交、末尾收拢：批次 drain（每N个get同步）会制造屏障波，把吞吐压到
				// 饱和冲突吞吐的 ~1/4（实测 batch100=17万 vs 饱和=63万/s）
				var tasks = new ArrayList<Future<Long>>(AddCount);
				for (int i = 0; i < AddCount; ++i)
					tasks.add(TaskSpec.ofProcedure(
							App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Add, "Add")).submitNow());
				for (var task : tasks)
					task.get();
			});
			System.out.println(Zeze.Util.ZezeCounter.instance.collectAndReset().formattedLog());
			App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Check, "check").call();
			App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Remove, "remove").call();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long Check() {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		Assertions.assertEquals(AddCount, r.getLong2());
		return 0;
	}

	private static long Add() {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		r.setLong2(r.getLong2() + 1);
		return 0;
	}

	private static long Remove() {
		App.Instance.demo_Module1.getTable1().remove(1L);
		return 0;
	}
}
