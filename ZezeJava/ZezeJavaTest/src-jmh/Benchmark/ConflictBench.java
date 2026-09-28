package Benchmark;

import Zeze.Application;
import Zeze.Config;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// 强冲突重做微基准（B 宏观场景的定位放大镜）：全部线程互踩同一个 key（Table1/BValue），
// 事务几乎必然冲突重做。跑法：-t 16 控制冲突线程数（对齐 B 的多线程形态）。
// 表用 demo_Module1 的 Table3（BValue 同 Table1，但无 RelationalMapping，裸 App 免 Schemas）。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ConflictBench {
	private Application app;
	private demo.Module1.Table3 table;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(10991);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("jmh_conflict_bench");
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("JmhConflictBench", conf);
		table = new demo.Module1.Table3();
		app.addTable(conf.getTableConf(table.getName()).getDatabaseName(), table);
		app.start();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		app.stop();
	}

	@Benchmark
	public void addSameKey(Blackhole bh) throws Exception {
		final var result = app.newProcedure(() -> {
			var r = table.getOrAdd(1L);
			r.setLong2(r.getLong2() + 1);
			return 0L;
		}, "Add").call();
		if (result != 0)
			throw new IllegalStateException("addSameKey failed: " + result);
		bh.consume(result);
	}
}
