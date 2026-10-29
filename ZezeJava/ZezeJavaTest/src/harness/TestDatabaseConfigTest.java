package harness;

import java.util.List;
import java.util.stream.Stream;
import Zeze.Config;
import Zeze.Transaction.DatabaseJdbc;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabasePostgreSQL;
import Zeze.Transaction.DatabaseSqlServer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDatabaseConfigTest {
	private record Backend(Config.DbType type, String url, String driver, Class<? extends DatabaseJdbc> adapter) {
		String name() {
			return type.name();
		}
	}

	// 预期值独立于JdbcTestBackend，防止配置与断言共同依赖一个错误映射。
	private static final List<Backend> BACKENDS = List.of(
			new Backend(Config.DbType.MySql, "jdbc:mysql://localhost/test", "com.mysql.cj.jdbc.Driver", DatabaseMySql.class),
			new Backend(Config.DbType.PostgreSQL, "jdbc:postgresql://localhost/test", "org.postgresql.Driver", DatabasePostgreSQL.class),
			new Backend(Config.DbType.SqlServer, "jdbc:sqlserver://localhost;databaseName=test",
					"com.microsoft.sqlserver.jdbc.SQLServerDriver", DatabaseSqlServer.class));

	private static Stream<Backend> backends() {
		return BACKENDS.stream();
	}

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
	@MethodSource("backends")
	void selectsJdbcBackendBeforeApplicationConstruction(Backend backend) {
		var config = config();
		var database = config.getDatabaseConfMap().get("shared");
		var other = new Config.DatabaseConf();
		other.setName("other");
		other.setDatabaseType(Config.DbType.Memory); // 显式参与选择，不再按 setter 调用历史区别对待。
		config.getDatabaseConfMap().put("other", other);
		assertSame(config, TestDatabaseConfig.apply(config, backend.name(), backend.url()));
		for (var db : config.getDatabaseConfMap().values()) {
			assertEquals(backend.type(), db.getDatabaseType());
			assertEquals(backend.url(), db.getDatabaseUrl());
			var druid = db.getDruidConf();
			assertNotNull(druid);
			assertEquals(backend.driver(), druid.driverClassName);
			assertEquals(0, druid.initialSize);
			assertEquals(0, druid.minIdle);
			assertEquals(4, druid.maxActive);
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

	@ParameterizedTest
	@MethodSource("backends")
	void rejectsMissingUrlWithoutChangingConfiguration(Backend backend) {
		for (var url : new @Nullable String[]{null, "", " "}) {
			var config = config();
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, backend.name(), url));
			assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
		}
	}

	@Test
	void unselectedJdbcTestsHaveNoConfiguration() {
		for (var backend : BACKENDS)
			assertNull(TestDatabaseConfig.jdbcTestConf(backend.type(), null, null));
	}

	@ParameterizedTest
	@MethodSource("backends")
	void jdbcTestsSelectExactlyOneBackend(Backend backend) {
		for (var expected : BACKENDS) {
			var conf = TestDatabaseConfig.jdbcTestConf(expected.type(), backend.name(), backend.url());
			if (expected.type() == backend.type()) {
				assertNotNull(conf);
				assertEquals(expected.type(), conf.getDatabaseType());
				assertEquals(backend.url(), conf.getDatabaseUrl());
			} else
				assertNull(conf);
		}
	}

	@Test
	void explicitInvalidJdbcSelectionFailsEvenForAnotherBackend() {
		assertThrows(IllegalStateException.class,
				() -> TestDatabaseConfig.jdbcTestConf(Config.DbType.MySql, "SqlServer", null));
		assertThrows(IllegalStateException.class,
				() -> TestDatabaseConfig.jdbcTestConf(Config.DbType.MySql, "unknown", "jdbc:unknown:test"));
		assertThrows(IllegalStateException.class,
				() -> TestDatabaseConfig.jdbcTestConf(Config.DbType.MySql, null, "jdbc:mysql://localhost/test"));
	}

	@ParameterizedTest
	@CsvSource({"MySql,jdbc:postgresql://localhost/test", "PostgreSQL,jdbc:sqlserver://localhost",
			"SqlServer,jdbc:mysql://localhost/test"})
	void mismatchedUrlFailsWithoutChangingConfiguration(String backend, String url) {
		var config = config();
		assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, backend, url));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
	}

	@ParameterizedTest
	@MethodSource("backends")
	void selectedDriverAndAdapterAreAvailable(Backend backend) throws Exception {
		assertNotNull(Class.forName(backend.driver()));
		var conf = TestDatabaseConfig.jdbcConf(backend.name(), backend.url());
		conf.setDisableOperates(true);
		var database = JdbcTestBackend.of(backend.type()).create(conf);
		try {
			assertInstanceOf(backend.adapter(), database);
		} finally {
			database.close();
		}
	}
}
