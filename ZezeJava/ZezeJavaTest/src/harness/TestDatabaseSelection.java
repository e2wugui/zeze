package harness;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.DatabaseMongoDb;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabasePostgreSQL;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Transaction.DatabaseSqlServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** 可带 -PzezeTestDatabase 运行：普通生产入口必须忽略测试属性，不连接所选 JDBC 后端。 */
public class TestDatabaseSelection {
	private static Config config() {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		// 独占号段（FastServerIds统一登记）：zeze_cache_<id>与dbh2本地提交目录CommitRocks<id>
		// 与demo.App(serverId=0)分离——后者在IT车道泄漏存活时持有CommitRocks0目录锁。
		config.setServerId(FastServerIds.TEST_DATABASE_SELECTION);
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
		var backend = System.getProperty("zeze.test.database", "Memory");
		var config = config();
		if ("Dbh2".equals(backend)) {
			// Dbh2需要ServiceManager（tidAutoKey分配）：对齐zeze.xml的SM agent配置
			//（127.0.0.1:5001，IT车道由TestEnvLauncherListener自动启动）。
			config.setServiceManager("");
			var smConf = new Zeze.Net.ServiceConf();
			smConf.tryGetOrAddConnector("127.0.0.1", 5001, true, null);
			config.getServiceConfMap().put("Zeze.Services.ServiceManager.Agent", smConf);
		}
		var app = new Application("TestDatabaseSelection", TestDatabaseConfig.apply(config));
		try {
			var expectedClass = switch (backend) {
				case "MySql" -> DatabaseMySql.class;
				case "PostgreSQL" -> DatabasePostgreSQL.class;
				case "SqlServer" -> DatabaseSqlServer.class;
				case "RocksDb" -> DatabaseRocksDb.class;
				case "MongoDb" -> DatabaseMongoDb.class;
				case "Dbh2" -> Zeze.Dbh2.Database.class;
				default -> DatabaseMemory.class;
			};
			assertInstanceOf(expectedClass, app.getDatabase(""));
		} finally {
			app.stop();
		}
	}
}
