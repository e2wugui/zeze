package Benchmark;
import harness.Bench;
import org.junit.jupiter.api.Test;

import demo.App;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("NewClassNamingConvention")
@Bench
public class ABasicSimpleAddOneThread {
	public final static int AddCount = 1_000_000;

	@Test

	public void testBenchmark() throws Exception {
		App.Instance.Start();
		try {
			App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Remove, "remove").call();
			System.out.println("benchmark start...");
			var b = new Zeze.Util.Benchmark();
			for (int i = 0; i < AddCount; ++i) {
				App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Add, "Add").call();
			}
			b.report(this.getClass().getName(), AddCount);
			App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Check, "check").call();
			App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Remove, "remove").call();
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
