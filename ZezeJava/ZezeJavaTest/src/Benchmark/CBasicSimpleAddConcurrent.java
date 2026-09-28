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
public class CBasicSimpleAddConcurrent {
	public final static int AddCount = 1_000_000;
	public final static int ConcurrentLevel = 5_000;
	public static final int Batch = 200;
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("C_Concurrent", Warmups, Rounds, AddCount, () -> {
				for (long k = 0; k < ConcurrentLevel; ++k) {
					final long rk = k;
					App.Instance.Zeze.newProcedure(() -> Remove(rk), "remove").call();
				}
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
			App.Instance.Zeze.newProcedure(CBasicSimpleAddConcurrent::Check, "check").call();
			for (long k = 0; k < ConcurrentLevel; ++k) {
				final long rk = k;
				App.Instance.Zeze.newProcedure(() -> Remove(rk), "remove").call();
			}
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long Check() {
		long sum = 0;
		for (long i = 0; i < ConcurrentLevel; ++i) {
			var r = App.Instance.demo_Module1.getTable1().getOrAdd(i);
			sum += r.getLong2();
		}
		Assertions.assertEquals(AddCount, sum);
		return 0;
	}

	@SuppressWarnings("unused")
	private static long Add() {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		r.setLong2(r.getLong2() + 1);
		return 0;
	}

	private static long Add(long key) {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(key);
		r.setLong2(r.getLong2() + 1);
		return 0;
	}

	private static long Remove(long key) {
		App.Instance.demo_Module1.getTable1().remove(key);
		return 0;
	}
}
