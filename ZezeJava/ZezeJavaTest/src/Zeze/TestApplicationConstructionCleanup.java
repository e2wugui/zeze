package Zeze;

import java.nio.file.Path;
import Zeze.Transaction.DatabaseRocksDb;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestApplicationConstructionCleanup {
	@TempDir
	Path directory;

	private static Config config() {
		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_APPLICATION_CONSTRUCTION_CLEANUP);
		config.setDefaultTableConf(new Config.TableConf());
		config.setTakeoverMode("invalid-construction-test");
		return config;
	}

	@Test
	void laterComponentFailureReleasesDatabaseDirectory() throws Exception {
		var conf = config();
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.RocksDb);
		dbConf.setDatabaseUrl(directory.resolve("database").toString());
		conf.getDatabaseConfMap().put("", dbConf);
		assertThrows(IllegalStateException.class, () -> new Application("ConstructionCleanup", conf));
		var reopened = new DatabaseRocksDb(null, dbConf, false);
		reopened.close();
	}

	@Test
	void laterComponentFailureClosesEveryDatabaseAndPreservesCause() throws Exception {
		var conf = config();
		for (var name : new String[]{"", "second"}) {
			var database = new Config.DatabaseConf();
			database.setName(name);
			database.setDatabaseType(Config.DbType.RocksDb);
			database.setDatabaseUrl(directory.resolve(name.isEmpty() ? "first" : name).toString());
			conf.getDatabaseConfMap().put(name, database);
		}
		var failure = assertThrows(IllegalStateException.class,
				() -> new Application("ConstructionCleanupOrder", conf));
		assertTrue(failure.getMessage().contains("TakeoverMode"));
		for (var database : conf.getDatabaseConfMap().values()) {
			var reopened = new DatabaseRocksDb(null, database, false);
			reopened.close();
		}
	}
}
