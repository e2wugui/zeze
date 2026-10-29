package harness;

import Zeze.Config;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDatabaseConfigTest {

	private static Config config() {
		var config = new Config();
		var database = new Config.DatabaseConf();
		database.setName("shared");
		database.setDatabaseName("test_database");
		database.setDistTxn(true);
		database.setDisableOperates(true);
		config.getDatabaseConfMap().put("shared", database);
		return config;
	}

	@Test
	public void absentSelectionKeepsMemoryConfiguration() {
		var config = config();
		var database = config.getDatabaseConfMap().get("shared");
		assertSame(config, TestDatabaseConfig.apply(config, null, null));
		assertSame(database, config.getDatabaseConfMap().get("shared"));
		assertEquals(Config.DbType.Memory, database.getDatabaseType());
		assertEquals("", database.getDatabaseUrl());
		assertNull(database.getDruidConf());
	}

	@ParameterizedTest
	@ValueSource(strings = {"MySql", "PostgreSQL"})
	public void selectsJdbcBackendBeforeApplicationConstruction(String backend) {
		var config = config();
		var database = config.getDatabaseConfMap().get("shared");
		var other = new Config.DatabaseConf();
		other.setName("other");
		other.setDatabaseType(Config.DbType.Memory); // 显式参与选择，不再按 setter 调用历史区别对待。
		config.getDatabaseConfMap().put("other", other);
		var mysql = backend.equals("MySql");
		var url = mysql ? "jdbc:mysql://localhost/test" : "jdbc:postgresql://localhost/test";
		assertSame(config, TestDatabaseConfig.apply(config, backend, url));
		for (var db : config.getDatabaseConfMap().values()) {
			assertEquals(Config.DbType.valueOf(backend), db.getDatabaseType());
			assertEquals(url, db.getDatabaseUrl());
			var druid = db.getDruidConf();
			assertNotNull(druid);
			assertEquals(mysql ? "com.mysql.cj.jdbc.Driver" : "org.postgresql.Driver", druid.driverClassName);
			assertEquals(0, druid.initialSize);
			assertEquals(0, druid.minIdle);
			assertEquals(8, druid.maxActive);
			assertEquals(61_000L, druid.minEvictableIdleTimeMillis);
			assertEquals(10_000L, druid.maxWait);
		}
		assertSame(database, config.getDatabaseConfMap().get("shared"));
		assertEquals("shared", database.getName());
		assertEquals("test_database", database.getDatabaseName());
		assertTrue(database.isDistTxn());
		assertTrue(database.isDisableOperates());
		// 重复准备不替换已经选择的后端。
		assertSame(config, TestDatabaseConfig.apply(config, "unsupported", null));
	}

	@Test
	public void keepsIndependentMemoryBucketsAndOtherBackends() {
		var config = config();
		var memory = config.getDatabaseConfMap().get("shared");
		memory.setDatabaseUrl("independent_bucket");
		var rocks = new Config.DatabaseConf();
		rocks.setDatabaseType(Config.DbType.RocksDb);
		rocks.setDatabaseUrl("rocks_home");
		config.getDatabaseConfMap().put("rocks", rocks);
		assertSame(config, TestDatabaseConfig.apply(config, "unsupported", null));
		assertEquals(Config.DbType.Memory, memory.getDatabaseType());
		assertEquals("independent_bucket", memory.getDatabaseUrl());
		assertEquals(Config.DbType.RocksDb, rocks.getDatabaseType());
		assertEquals("rocks_home", rocks.getDatabaseUrl());
	}

	@Test
	public void rejectsUnsupportedBackendWithoutChangingConfiguration() {
		var config = config();
		assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, "Redis", "redis://localhost"));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
	}

	@Test
	public void rejectsMissingUrlWithoutChangingConfiguration() {
		for (var url : new @Nullable String[]{null, "", " "}) {
			var config = config();
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, "MySql", url));
			assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
		}
	}
}
