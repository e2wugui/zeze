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

	@Test
	public void rocksDbSelectionCreatesIndependentDirectoriesPerDatabase() {
		var config = config();
		config.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
		var other = new Config.DatabaseConf();
		other.setName("other");
		config.getDatabaseConfMap().put("other", other);
		assertSame(config, TestDatabaseConfig.apply(config, "RocksDb", null));
		var urls = new java.util.HashSet<String>();
		for (var db : config.getDatabaseConfMap().values()) {
			assertEquals(Config.DbType.RocksDb, db.getDatabaseType());
			assertTrue(db.getDatabaseUrl().contains("test-rocksdb"), "目录集中在build/test-rocksdb下");
			assertTrue(urls.add(db.getDatabaseUrl()), "每个数据库必须独立目录（目录锁不互斥、状态不共享）");
		}
		assertNull(config.getDatabaseConfMap().get("shared").getDruidConf(), "RocksDb不动连接池配置");
		assertEquals("", config.getGlobalCacheManagerHostNameOrAddress(), "RocksDb连Global配置一起摘（createDatabase硬闸）");
		assertFalse(config.hasGlobal());
		// 已选后端不重复切换（对齐JDBC路径的空集早退语义）。
		assertSame(config, TestDatabaseConfig.apply(config, "unsupported", null));
	}

	@Test
	public void rocksDbSelectionRejectsUrl() {
		var config = config();
		assertThrows(IllegalStateException.class,
				() -> TestDatabaseConfig.apply(config, "RocksDb", "jdbc:mysql://localhost/test"));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
		assertEquals("", config.getDatabaseConfMap().get("shared").getDatabaseUrl());
	}

	@Test
	public void mongoDbSelectionAppliesUrlToEverySwitchableDatabase() {
		var config = config();
		config.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
		var other = new Config.DatabaseConf();
		other.setName("other");
		config.getDatabaseConfMap().put("other", other);
		var url = "mongodb://127.0.0.1:27017/?replicaSet=rs0";
		assertSame(config, TestDatabaseConfig.apply(config, "MongoDb", url));
		for (var db : config.getDatabaseConfMap().values()) {
			assertEquals(Config.DbType.MongoDb, db.getDatabaseType());
			assertEquals(url, db.getDatabaseUrl());
			assertNull(db.getDruidConf(), "MongoDb不动连接池配置");
		}
		// databaseName不被重定向触碰：fixture预置的保持预置值，未预置的保持DatabaseConf默认。
		assertEquals("test_database", config.getDatabaseConfMap().get("shared").getDatabaseName());
		assertEquals("zeze_mongodb", config.getDatabaseConfMap().get("other").getDatabaseName());
		assertEquals("127.0.0.1", config.getGlobalCacheManagerHostNameOrAddress(),
				"MongoDb与GCM无互斥硬闸，Global保持（区别于RocksDb分支）");
		// 已选后端不重复切换（对齐JDBC路径的空集早退语义）。
		assertSame(config, TestDatabaseConfig.apply(config, "unsupported", null));
	}

	@Test
	public void mongoDbSelectionRequiresUrlPrefix() {
		for (var badUrl : new String[]{null, "", "jdbc:mysql://localhost/test"}) {
			var config = config();
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, "MongoDb", badUrl));
			assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
			assertEquals("", config.getDatabaseConfMap().get("shared").getDatabaseUrl());
		}
	}

	@Test
	public void dbh2SelectionAppliesUrlFromZezeXml() {
		var config = config();
		config.setGlobalCacheManagerHostNameOrAddress("127.0.0.1");
		var other = new Config.DatabaseConf();
		other.setName("other");
		config.getDatabaseConfMap().put("other", other);
		assertSame(config, TestDatabaseConfig.apply(config, "Dbh2", null));
		for (var db : config.getDatabaseConfMap().values()) {
			assertEquals(Config.DbType.Dbh2, db.getDatabaseType());
			// 漂移守卫：连接串唯一来源是ZezeJavaTest/zeze.xml，改xml须同步本断言。
			assertEquals("dbh2://127.0.0.1:10999/dbh2_unittest", db.getDatabaseUrl());
			assertNull(db.getDruidConf(), "Dbh2不动连接池配置");
		}
		// 远程提交模式：协调端点为环境内CommitServer，app无本地CommitService。
		assertFalse(config.isDbh2LocalCommit());
		var customize = config.getCustomizes().get("Dbh2Config");
		assertNotNull(customize);
		assertEquals(TestDatabaseConfig.COMMIT_SERVER_ADDRESS, customize.getAttribute("CommitServerAddress"));
		// databaseName不被重定向触碰：fixture预置的保持预置值，未预置的保持DatabaseConf默认。
		assertEquals("test_database", config.getDatabaseConfMap().get("shared").getDatabaseName());
		assertEquals("127.0.0.1", config.getGlobalCacheManagerHostNameOrAddress(),
				"Dbh2与GCM无互斥硬闸，Global保持（区别于RocksDb分支）");
		// 已选后端不重复切换（对齐JDBC路径的空集早退语义）。
		assertSame(config, TestDatabaseConfig.apply(config, "unsupported", null));
	}

	@Test
	public void dbh2CommitServerAddressMatchesZezeXmlAcceptor() throws Exception {
		// 三处同源漂移守卫：harness注入地址 == listener端口常量 == zeze.xml的Zeze.Dbh2.Commit Acceptor。
		var text = java.nio.file.Files.readString(java.nio.file.Path.of("zeze.xml"));
		var m = java.util.regex.Pattern.compile(
				"<ServiceConf Name=\"Zeze\\.Dbh2\\.Commit\">\\s*<Acceptor Ip=\"([^\"]*)\" Port=\"(\\d+)\"\\s*/>")
				.matcher(text);
		assertTrue(m.find(), "zeze.xml须有Zeze.Dbh2.Commit的Acceptor（环境CommitServer绑定与app连接的事实来源）");
		assertEquals(m.group(1) + ":" + m.group(2), TestDatabaseConfig.COMMIT_SERVER_ADDRESS);
		assertEquals(7788, TestEnvLauncherListener.DBH2_COMMIT_SERVER_PORT);
	}

	@Test
	public void dbh2SelectionRejectsUrlAndDisabledServiceManager() {
		var config = config();
		assertThrows(IllegalStateException.class,
				() -> TestDatabaseConfig.apply(config, "Dbh2", "dbh2://127.0.0.1:1/other"));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
		assertEquals("", config.getDatabaseConfMap().get("shared").getDatabaseUrl());
		config.setServiceManager("disable");
		assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.apply(config, "Dbh2", null));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
	}

	@Test
	public void dbh2UrlParserExtractsFromCommentAndFailsCleanly() throws Exception {
		var dir = java.nio.file.Files.createTempDirectory("dbh2conf");
		try {
			var empty = java.nio.file.Files.writeString(dir.resolve("empty.xml"), "<zeze/>");
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.dbh2UrlFromZezeXml(empty));
			// 条目在注释块内亦有效（按原文匹配）：zeze.xml现状即此形态。
			var commented = java.nio.file.Files.writeString(dir.resolve("commented.xml"),
					"<!--\n<DatabaseConf Name=\"\" DatabaseType=\"Dbh2\""
							+ " DatabaseUrl=\"dbh2://127.0.0.1:10999/dbh2_unittest\"/>\n-->");
			assertEquals("dbh2://127.0.0.1:10999/dbh2_unittest", TestDatabaseConfig.dbh2UrlFromZezeXml(commented));
			// 非Dbh2条目不被误选。
			var memoryOnly = java.nio.file.Files.writeString(dir.resolve("memory.xml"),
					"<DatabaseConf Name=\"\" DatabaseType=\"Memory\" DatabaseUrl=\"\"/>");
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.dbh2UrlFromZezeXml(memoryOnly));
			// 前缀错误立即失败。
			var badPrefix = java.nio.file.Files.writeString(dir.resolve("bad.xml"),
					"<DatabaseConf DatabaseType=\"Dbh2\" DatabaseUrl=\"http://127.0.0.1:10999/x\"/>");
			assertThrows(IllegalStateException.class, () -> TestDatabaseConfig.dbh2UrlFromZezeXml(badPrefix));
		} finally {
			try (var walk = java.nio.file.Files.walk(dir)) {
				walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
	}

	@Test
	public void keepOriginalExemptsRedirect() {
		var config = config();
		assertSame(config, TestDatabaseConfig.keepOriginal(config));
		assertSame(config, TestDatabaseConfig.apply(config, "MySql", "jdbc:mysql://localhost/test"));
		assertSame(config, TestDatabaseConfig.apply(config, "RocksDb", null));
		assertEquals(Config.DbType.Memory, config.getDatabaseConfMap().get("shared").getDatabaseType());
		assertEquals("", config.getDatabaseConfMap().get("shared").getDatabaseUrl());
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
