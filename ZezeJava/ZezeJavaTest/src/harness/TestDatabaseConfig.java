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
		var dbType = switch (backend) {
			case "MySql" -> Config.DbType.MySql;
			case "PostgreSQL" -> Config.DbType.PostgreSQL;
			default -> throw new IllegalStateException("zeze.test.database unsupported type: " + backend);
		};
		if (url == null || url.isBlank())
			throw new IllegalStateException("zeze.test.database=" + backend + " requires zeze.test.database.url");
		for (var db : databases) {
			var druid = new Config.DruidConf();
			druid.driverClassName = dbType == Config.DbType.MySql ? "com.mysql.cj.jdbc.Driver" : "org.postgresql.Driver";
			// 多应用测试不常驻闲置连接，保留并发余量；回收时间须大于 Druid 默认清理周期 60s。
			druid.initialSize = 0;
			druid.minIdle = 0;
			druid.maxActive = 8;
			druid.minEvictableIdleTimeMillis = 61_000L;
			druid.maxWait = 10_000L;
			db.setDatabaseType(dbType);
			db.setDatabaseUrl(url);
			db.setDruidConf(druid);
		}
		return config;
	}
}
