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

// 缓存抖动微基准（G 宏观场景的定位放大镜）：工作集 10 万 key 远超缓存容量 1000，
// 顺序轮询保证每次访问都是 miss——完整走过 装载（Database 拷贝+decode）→ 修改 →
// 驱逐（脏记录 flush=encode+写回）。裸 Application，checkpoint 关闭（驱逐自带 flush）。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CacheThrashBench {
	private static final int Keys = 100_000;
	private static final int Capacity = 1_000;

	private Application app;
	private demo.Module1.Table3 table;
	private int cursor;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(10992);
		conf.setCheckpointPeriod(Integer.MAX_VALUE);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config必设：AutoKey注册读默认表配置
		var tc = new Config.TableConf();
		tc.setCacheCapacity(Capacity);
		tc.setCacheFactor(1.0f);
		conf.getTableConfMap().put("demo_Module1_Table3", tc);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("jmh_cache_thrash");
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("JmhCacheThrashBench", conf);
		table = new demo.Module1.Table3();
		app.addTable(conf.getTableConf(table.getName()).getDatabaseName(), table);
		app.start();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		app.stop();
	}

	@Benchmark
	public void thrash(Blackhole bh) throws Exception {
		final long key = cursor++ % Keys;
		final var result = app.newProcedure(() -> {
			var r = table.getOrAdd(key);
			r.setLong2(r.getLong2() + 1);
			return 0L;
		}, "thrash").call();
		if (result != 0)
			throw new IllegalStateException("thrash failed: " + result);
		bh.consume(result);
	}
}
