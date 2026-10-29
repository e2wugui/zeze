package harness;

import java.util.function.Function;
import Zeze.Config;
import Zeze.Transaction.DatabaseJdbc;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabasePostgreSQL;
import Zeze.Transaction.DatabaseSqlServer;

/** 测试支持的JDBC后端：连接协议、驱动和适配器构造在同一处维护。 */
public enum JdbcTestBackend {
	MySql(Config.DbType.MySql, "jdbc:mysql:", "com.mysql.cj.jdbc.Driver", c -> new DatabaseMySql(null, c)),
	PostgreSQL(Config.DbType.PostgreSQL, "jdbc:postgresql:", "org.postgresql.Driver", c -> new DatabasePostgreSQL(null, c)),
	SqlServer(Config.DbType.SqlServer, "jdbc:sqlserver:", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
			c -> new DatabaseSqlServer(null, c));

	public final Config.DbType type;
	public final String urlPrefix;
	public final String driverClassName;
	private final Function<Config.DatabaseConf, DatabaseJdbc> factory;

	JdbcTestBackend(Config.DbType type, String urlPrefix, String driverClassName,
			Function<Config.DatabaseConf, DatabaseJdbc> factory) {
		this.type = type;
		this.urlPrefix = urlPrefix;
		this.driverClassName = driverClassName;
		this.factory = factory;
	}

	public static JdbcTestBackend parse(String name) {
		for (var backend : values())
			if (backend.name().equals(name))
				return backend;
		throw new IllegalStateException("zeze.test.database unsupported type: " + name);
	}

	public static JdbcTestBackend of(Config.DbType type) {
		return parse(type.name());
	}

	public DatabaseJdbc create(Config.DatabaseConf conf) {
		return factory.apply(conf);
	}
}
