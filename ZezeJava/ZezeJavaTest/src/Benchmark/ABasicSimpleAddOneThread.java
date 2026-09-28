package Benchmark;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import demo.App;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class ABasicSimpleAddOneThread {
	// AddCount 亦被 App.adjustTableConf 引用（按它放大 Table1 缓存容量），改名/删除会破坏缓存配置
	public final static int AddCount = 1_000_000;
	public final static int Warmups = 1;
	public final static int Rounds = 5;

	@Test
	public void testBenchmark() throws Exception {
		App.Instance.Stop();
		App.Instance.Start();
		try {
			MacroBench.run("A_SimpleAddOneThread", Warmups, Rounds, AddCount, () -> {
				App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Remove, "remove").call();
				for (int i = 0; i < AddCount; ++i)
					App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Add, "Add").call();
			});
			App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Check, "check").call();
			App.Instance.Zeze.newProcedure(ABasicSimpleAddOneThread::Remove, "remove").call();
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
