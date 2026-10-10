package Benchmark;

import Zeze.Application;
import Zeze.Config;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// 只读事务微基准（H 宏观场景的定位放大镜）：多线程读 N 个互不冲突的 key（对齐 C 的并发形态），
// 对照 ConflictBench.addSameKey 的写路径同口径分配/吞吐。宏观实测只读比写慢 ~2.5×（反直觉，
// 两代同构），本基准拆解根因：TableKey/RecordAccessed 逐读分配、accessed-map 登记、
// 读锁与快照路径成本。跑法：jmh -PjmhFilter='ReadOnlyBench.*' -PjmhExtraArgs='-prof gc'。
// 表用 demo_Module1 的 Table3（BValue 同 Table1，但无 RelationalMapping，裸 App 免 Schemas）。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ReadOnlyBench {
	private Application app;
	private demo.Module1.Table3 table;

	@Param({"64"})
	public int keys; // 工作集（全缓存命中，隔离装载路径）

	@Setup(Level.Trial)
	public void setup() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(10992);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("jmh_readonly_bench");
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("JmhReadOnlyBench", conf);
		table = new demo.Module1.Table3();
		app.addTable(conf.getTableConf(table.getName()).getDatabaseName(), table);
		app.start();
		// 预热数据在测量外：先写满工作集再开始读
		app.newProcedure(() -> {
			for (long k = 0; k < keys; ++k)
				table.getOrAdd(k).setLong2(k);
			return 0L;
		}, "prepare").call();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		app.newProcedure(() -> {
			for (long k = 0; k < keys; ++k)
				table.remove(k);
			return 0L;
		}, "clear").call();
		app.stop();
	}

	private long counter;

	@Benchmark
	public void readKey(Blackhole bh) throws Exception {
		final long key = (counter++ & 0x7fffffff) % keys;
		final long[] box = new long[1]; // 过程必须返回 0（非零触发 defaultLogAction 逐次日志，基准三坑之二），值经盒子带出
		final var result = app.newProcedure(() -> {
			var r = table.get(key);
			if (r == null)
				throw new IllegalStateException("missing key: " + key);
			box[0] = r.getLong2();
			return 0L;
		}, "Read").call();
		if (result != 0 || box[0] != key)
			throw new IllegalStateException("read mismatch: result=" + result + " value=" + box[0] + " key=" + key);
		bh.consume(box[0]);
	}
}
