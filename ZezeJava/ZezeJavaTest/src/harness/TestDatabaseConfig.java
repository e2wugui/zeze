package harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import Zeze.Config;
import org.jspecify.annotations.Nullable;

/**
 * 测试应用在创建 Application 前显式选择后端；生产 Config 不读取测试属性。
 * 仅切换空 url 的 Memory 配置，独立内存桶和其他后端保持原配置。
 * Memory 语义测试直接使用 Config，不调用本助手；不要依赖 setter 调用历史区分配置。
 * RocksDb 目标为每个数据库生成独立目录（最贴近 Memory 的不共享状态语义），不接受 url；
 * 多 App 测试用 keepOriginal 豁免——RocksDb 目录锁不支持同进程多 App。
 */
public final class TestDatabaseConfig {

	private TestDatabaseConfig() {
	}

	/** 豁免重定向的Config登记：弱引用跟随Config生命周期，不阻碍回收。 */
	private static final Set<Config> keepOriginals =
			Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

	/** 多App测试豁免后端重定向：RocksDb 等后端不支持同进程多 App（目录锁），登记后保持原配置。 */
	public static Config keepOriginal(Config config) {
		keepOriginals.add(config);
		return config;
	}

	public static Config apply(Config config) {
		return apply(config, System.getProperty("zeze.test.database"), System.getProperty("zeze.test.database.url"));
	}

	static Config apply(Config config, @Nullable String backend, @Nullable String url) {
		if (backend == null || keepOriginals.contains(config))
			return config;
		var databases = config.getDatabaseConfMap().values().stream()
				.filter(db -> db.getDatabaseType() == Config.DbType.Memory && db.getDatabaseUrl().isEmpty())
				.toList();
		if (databases.isEmpty())
			return config;
		if (Config.DbType.RocksDb.name().equals(backend)) {
			// RocksDb非JDBC：每个数据库独立目录（Memory式不共享状态，目录锁不互斥），
			// 不动druidConf（DatabaseRocksDb不消费连接池，显式RocksDb测试同款惯例）。
			if (url != null && !url.isBlank())
				throw new IllegalStateException("zeze.test.database=RocksDb 不接受 zeze.test.database.url"
						+ "：每个数据库自动独立目录，保持 Memory 式不共享状态");
			for (var db : databases) {
				db.setDatabaseType(Config.DbType.RocksDb);
				db.setDatabaseUrl(newRocksDbDir());
			}
			// 连Global配置一起摘：RocksDb与GlobalCacheManager互斥（Config.createDatabase硬闸），
			// hasGlobal/hasGlobalRaft均由本字段派生，置空即单机本地锁模式。
			// 依赖分布式锁语义的测试（Takeover/跨App对撞/多App）必须keepOriginal豁免。
			config.setGlobalCacheManagerHostNameOrAddress(null);
			return config;
		}
		for (var db : databases) {
			var jdbc = jdbcConf(backend, url);
			db.setDatabaseType(jdbc.getDatabaseType());
			db.setDatabaseUrl(jdbc.getDatabaseUrl());
			db.setDruidConf(jdbc.getDruidConf());
		}
		return config;
	}

	/** 独立RocksDb目录：build/下集中存放（gitignored、gradle clean可扫除；rocksdb数据留取证，对齐cleanTestResidue惯例）。 */
	private static String newRocksDbDir() {
		try {
			var root = Path.of("build", "test-rocksdb");
			Files.createDirectories(root);
			return Files.createTempDirectory(root, "db_").toString();
		} catch (IOException e) {
			throw new IllegalStateException("create rocksdb test directory failed", e);
		}
	}

	/** 直接测试数据库适配器时也使用同一选择；null表示未选择本后端，调用方须明确中止测试。 */
	public static Config.@Nullable DatabaseConf jdbcTestConf(Config.DbType expected) {
		return jdbcTestConf(expected, System.getProperty("zeze.test.database"),
				System.getProperty("zeze.test.database.url"));
	}

	static Config.@Nullable DatabaseConf jdbcTestConf(Config.DbType expected, @Nullable String backend,
			@Nullable String url) {
		if (backend == null) {
			if (url != null && !url.isBlank())
				throw new IllegalStateException("zeze.test.database.url requires zeze.test.database");
			return null;
		}
		var conf = jdbcConf(backend, url); // 先验证显式配置，不能把错误配置当成未选择后端。
		return conf.getDatabaseType() == expected ? conf : null;
	}

	static Config.DatabaseConf jdbcConf(String backend, @Nullable String url) {
		var selected = JdbcTestBackend.parse(backend);
		if (url == null || url.isBlank())
			throw new IllegalStateException("zeze.test.database=" + backend + " requires zeze.test.database.url");
		if (!url.startsWith(selected.urlPrefix))
			throw new IllegalStateException("zeze.test.database=" + backend + " requires URL prefix " + selected.urlPrefix);
		var druid = new Config.DruidConf();
		druid.driverClassName = selected.driverClassName;
		// 多应用测试不常驻闲置连接，保留并发余量；回收时间须大于 Druid 默认清理周期60s。
		druid.initialSize = 0;
		druid.minIdle = 0;
		druid.maxActive = 4;
		druid.minEvictableIdleTimeMillis = 61_000L;
		druid.maxWait = 10_000L;
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(selected.type);
		conf.setDatabaseUrl(url);
		conf.setDruidConf(druid);
		return conf;
	}
}
