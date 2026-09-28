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

// Collections 微基准：裸 Application（ServiceManager disable、Memory 库）+ 单表 tMap2Bean1，
// 每次调用 = 一个完整存储过程（getOrAdd + PMap2 容器操作），单线程。
// 覆盖 delta-log 容器路径的单操作成本（宏观 D 场景的定位放大镜）。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CollectionsBench {
	private static final int Keys = 1_000; // 工作集（全缓存命中）

	private Application app;
	private demo.web.tMap2Bean1 table;
	private int cursor;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(10990); // 独立 serverId，避免与测试族的缓存目录互撞
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("jmh_collections_bench");
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("JmhCollectionsBench", conf);
		table = new demo.web.tMap2Bean1();
		// addTable 必须在 start 之前：Database.open（建 TableCache）在 start 内遍历已注册表
		app.addTable(conf.getTableConf(table.getName()).getDatabaseName(), table);
		app.start();
		// 预填工作集：每 key 一个 4 项 V2 的 Bean1
		for (long k = 0; k < Keys; ++k) {
			final long key = k;
			var ret = app.newProcedure(() -> {
				table.getOrAdd(key).getMap2().put((int)key, newBean1((int)key));
				return 0L;
			}, "prefill").call();
			if (ret != 0)
				throw new IllegalStateException("prefill failed: " + ret);
		}
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		app.stop();
	}

	private static demo.Bean1 newBean1(int seed) {
		var b = new demo.Bean1();
		b.setV1(seed);
		for (int j = 0; j < 4; ++j)
			b.getV2().put(j, seed + j);
		return b;
	}

	@Benchmark
	public long emptyProcedure() throws Exception {
		return app.newProcedure(() -> 0L, "empty").call();
	}

	@Benchmark
	public void containerPutEntry(Blackhole bh) throws Exception {
		final int key = (cursor++ % Keys);
		final var result = app.newProcedure(() -> {
			table.getOrAdd((long)key).getMap2().put(key, newBean1(key));
			return 0L;
		}, "putEntry").call();
		if (result != 0)
			throw new IllegalStateException("putEntry failed: " + result);
		bh.consume(result);
	}

	@Benchmark
	public void containerGetEntry(Blackhole bh) throws Exception {
		final int key = (cursor++ % Keys);
		// 过程返回值必须为 0：非零返回码会触发 Procedure.defaultLogAction 逐次记日志
		final var result = app.newProcedure(() -> {
			var b1 = table.getOrAdd((long)key).getMap2().get(key);
			bh.consume(b1 != null ? b1.getV1() : -1);
			return 0L;
		}, "getEntry").call();
		if (result != 0)
			throw new IllegalStateException("getEntry failed: " + result);
	}
}
