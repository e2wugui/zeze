package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Transaction.DispatchMode;
import Zeze.Util.PerfCounter;
import Zeze.Util.ZezeCounter;
import demo.App;
import junit.framework.TestCase;
import org.junit.Assert;

@SuppressWarnings("NewClassNamingConvention")
public class CBasicSimpleAddConcurrent extends TestCase {
	public final static int AddCount = 500_000;
	public final static int ConcurrentLevel = 5_000;

	public void testBenchmark() throws Exception {
		App.Instance.Start();
		try {
			for (int i = 0; i < ConcurrentLevel; ++i) {
				final long k = i;
				App.Instance.Zeze.newProcedure(() -> Remove(k), "remove").call();
			}
			ArrayList<Future<Long>> tasks = new ArrayList<>(AddCount);
			System.out.println("benchmark start...");
			var b = new Zeze.Util.Benchmark();
			for (int i = 0; i < AddCount; ++i) {
				final int c = i % ConcurrentLevel;
				tasks.add(Zeze.Util.Task.runUnsafe(App.Instance.Zeze.newProcedure(() -> Add(c), "Add"), DispatchMode.Normal));
				//tasks.add(Zeze.Util.Task.Create(App.Instance.Zeze.newProcedure(this::Add, "Add"), null, null));
			}
			//b.Report(this.getClass().getName(), AddCount);
			for (var task : tasks) {
				task.get();
			}
			b.report(this.getClass().getName(), AddCount);
			System.out.println(((PerfCounter)ZezeCounter.instance).getLogAndReset());
			App.Instance.Zeze.newProcedure(CBasicSimpleAddConcurrent::Check, "check").call();
			for (long i = 0; i < ConcurrentLevel; ++i) {
				final long k = i;
				App.Instance.Zeze.newProcedure(() -> Remove(k), "remove").call();
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
		Assert.assertEquals(AddCount, sum);
		//System.out.println(r.getLong2());
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
		//System.out.println("Add=" + key);
		return 0;
	}

	private static long Remove(long key) {
		App.Instance.demo_Module1.getTable1().remove(key);
		return 0;
	}
}
