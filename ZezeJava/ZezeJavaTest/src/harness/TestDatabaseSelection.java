package harness;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabasePostgreSQL;
import Zeze.Transaction.DatabaseSqlServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** 可带 -PzezeTestDatabase 运行：普通生产入口必须忽略测试属性，不连接所选 JDBC 后端。 */
public class TestDatabaseSelection {
	private static Config config() {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		config.getDatabaseConfMap().put("", new Config.DatabaseConf());
		return config;
	}

	@Test
	public void plainApplicationUsesConfiguredMemoryBackend() throws Exception {
		var app = new Application("TestDatabaseSelection", config());
		try {
			assertInstanceOf(DatabaseMemory.class, app.getDatabase(""));
		} finally {
			app.stop();
		}
	}

	@Test
	public void preparedApplicationUsesSelectedBackend() throws Exception {
		var app = new Application("TestDatabaseSelection", TestDatabaseConfig.apply(config()));
		try {
			var backend = System.getProperty("zeze.test.database", "Memory");
			var expectedClass = switch (backend) {
				case "MySql" -> DatabaseMySql.class;
				case "PostgreSQL" -> DatabasePostgreSQL.class;
				case "SqlServer" -> DatabaseSqlServer.class;
				default -> DatabaseMemory.class;
			};
			assertInstanceOf(expectedClass, app.getDatabase(""));
		} finally {
			app.stop();
		}
	}
}
