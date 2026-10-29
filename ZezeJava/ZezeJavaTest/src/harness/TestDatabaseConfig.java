package harness;

import Zeze.Config;
import org.jspecify.annotations.Nullable;

/**
 * 测试应用在创建 Application 前显式选择后端；生产 Config 不读取测试属性。
 * 仅切换空 url 的 Memory 配置，独立内存桶和其他后端保持原配置。
 * Memory 语义测试直接使用 Config，不调用本助手；不要依赖 setter 调用历史区分配置。
 */
public final class TestDatabaseConfig {

	private TestDatabaseConfig() {
	}

	public static Config apply(Config config) {
		return apply(config, System.getProperty("zeze.test.database"), System.getProperty("zeze.test.database.url"));
	}

	static Config apply(Config config, @Nullable String backend, @Nullable String url) {
		if (backend == null)
			return config;
		var databases = config.getDatabaseConfMap().values().stream()
				.filter(db -> db.getDatabaseType() == Config.DbType.Memory && db.getDatabaseUrl().isEmpty())
				.toList();
		if (databases.isEmpty())
			return config;
		for (var db : databases) {
			var jdbc = jdbcConf(backend, url);
			db.setDatabaseType(jdbc.getDatabaseType());
			db.setDatabaseUrl(jdbc.getDatabaseUrl());
			db.setDruidConf(jdbc.getDruidConf());
		}
		return config;
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
