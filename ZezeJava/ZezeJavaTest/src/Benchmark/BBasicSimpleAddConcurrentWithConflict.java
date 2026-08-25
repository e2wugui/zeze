package Benchmark;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Util.PerfCounter;
import Zeze.Util.TaskSpec;
import demo.App;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("NewClassNamingConvention")
public class BBasicSimpleAddConcurrentWithConflict {
	public static final int AddCount = 10_000;

	@Test

	public void testBenchmark() throws Exception {
		App.Instance.Start();
		try {
			App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Remove, "remove").call();
			ArrayList<Future<Long>> tasks = new ArrayList<>(AddCount);
			System.out.println("benchmark start...");
			var b = new Zeze.Util.Benchmark();
			for (int i = 0; i < AddCount; ++i) {
				tasks.add(TaskSpec.ofProcedure(App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Add, "Add")).submitNow());
				if ((i + 1) % 10 == 0) {
					for (var task : tasks)
						task.get();
					tasks.clear();
				}
			}
			//b.Report(this.getClass().getName(), AddCount);
			for (var task : tasks) {
				task.get();
			}
			b.report(this.getClass().getName(), AddCount);
			System.out.println(PerfCounter.instance().getLogAndReset());
			App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Check, "check").call();
			App.Instance.Zeze.newProcedure(BBasicSimpleAddConcurrentWithConflict::Remove, "remove").call();
		} finally {
			//App.Instance.Stop();
		}
	}

	private static long Check() {
		var r = App.Instance.demo_Module1.getTable1().getOrAdd(1L);
		Assertions.assertEquals(AddCount, r.getLong2());
		//System.out.println(r.getLong2());
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
