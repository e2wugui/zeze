package Zeze.Transaction;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * 1. TableX 的 8 个 walk* 入口统一校验事务外调用，覆盖不继承 AbstractKVTable 的关系映射表。
 * 2. TableMysqlRelational.walkDatabase / walkDatabaseKey 走流式抓取（walkStreamed），
 *    TablePostgreSQLRelational.walkDatabase / walkDatabaseKey 走游标分批（walkCursor），避免全表缓冲 OOM。
 */
@Fast
public class TestRelationalTableWalkGuardsAndStreaming {
	private static final class WalkBean extends Bean {
		long val;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(val);
		}

		@Override
		public void decode(IByteBuffer bb) {
			val = bb.ReadLong();
		}

		@Override
		public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws java.sql.SQLException {
			val = rs.getLong("val");
		}

		@Override
		public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
			st.appendLong("val", val);
		}

		@Override
		public WalkBean copy() {
			var c = new WalkBean();
			c.val = val;
			return c;
		}
	}

	private static final class WalkRelTable extends TableX<Long, WalkBean> {
		WalkRelTable(int id, String name) {
			super(id, name);
		}

		@Override
		public boolean isRelationalMapping() {
			return true;
		}

		@Override
		public Class<Long> getKeyClass() {
			return Long.class;
		}

		@Override
		public Class<WalkBean> getValueClass() {
			return WalkBean.class;
		}

		@Override
		public Long decodeKey(ByteBuffer bb) {
			return bb.ReadLong();
		}

		@Override
		public ByteBuffer encodeKey(Long key) {
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong(key);
			return bb;
		}

		@Override
		public Long decodeKeyResultSet(ResultSet rs) throws java.sql.SQLException {
			return rs.getLong("__key");
		}

		@Override
		public void encodeKeySQLStatement(SQLStatement st, Long key) {
			st.appendLong("__key", key);
		}

		@Override
		public WalkBean newValue() {
			return new WalkBean();
		}
	}

	/** 直接实现 Database.Table（不继承 AbstractKVTable），验证门面守卫在 TableX 生效。 */
	private static final class StubRelationalDatabase extends Database implements DatabaseRelationalMapping {
		final AtomicInteger walkCalls = new AtomicInteger();

		StubRelationalDatabase(Config.DatabaseConf conf) {
			super(null, conf);
		}

		@Override
		public Map<String, String> getSqlTypeMap() {
			return Map.of("long", "BIGINT");
		}

		@Override
		public Database.Table openTable(String name, int id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Database.Table openRelationalTable(String name) {
			return (Database.Table)Proxy.newProxyInstance(Database.Table.class.getClassLoader(),
					new Class<?>[]{Database.Table.class}, (proxy, method, args) -> switch (method.getName()) {
						case "getDatabase" -> this;
						case "close" -> null;
						case "isNew" -> true;
						case "walk", "walkDesc", "walkKey", "walkKeyDesc" -> {
							walkCalls.incrementAndGet();
							yield method.getReturnType() == long.class ? 0L : null;
						}
						default -> throw new UnsupportedOperationException(method.getName());
					});
		}

		@Override
		public Database.Transaction beginTransaction() {
			return new Database.Transaction() {
				@Override
				public void commit() {
				}

				@Override
				public void rollback() {
				}

				@Override
				public void close() {
				}
			};
		}
	}

	private static Schemas.RelationalTable buildRelationalMeta(DatabaseRelationalMapping mapping, String tableName) {
		var schemas = new Schemas();
		var bean = new Schemas.Bean("WalkBean", false);
		var v = new Schemas.Variable();
		v.id = 1;
		v.name = "val";
		v.typeName = "long";
		bean.getVariables().put(v.id, v);
		schemas.addBean(bean);
		var st = new Schemas.Table(tableName, "long", "WalkBean");
		schemas.addTable(st);
		schemas.compile();
		return Schemas.newRelationalTable(mapping, st, null);
	}

	private static void setRelationalTable(TableX<?, ?> table, Schemas.RelationalTable meta) throws Exception {
		var f = TableX.class.getDeclaredField("relationalTable");
		f.setAccessible(true);
		f.set(table, meta);
	}

	@Test
	public void testAllEightRelationalWalkEntryPointsRejectActiveTransaction() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_RELATIONAL_TABLE_WALK_GUARDS);
		conf.setCheckpointMode(CheckpointMode.Table);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("rel_walk_guards_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", dbConf);

		var app = new Application("TestRelationalTableWalkGuardsAndStreaming", conf);
		var table = new WalkRelTable(9919101, "UnitTest_WalkRelGuard");
		var relConf = new Config.DatabaseConf();
		relConf.setName("rel");
		relConf.setDatabaseUrl("rel_walk_stub_" + conf.getServerId());
		var relDb = new StubRelationalDatabase(relConf);

		app.start();
		try {
			table.setDatabase(relDb);
			setRelationalTable(table, buildRelationalMeta(relDb, table.getName()));
			var storageField = TableX.class.getDeclaredField("storage");
			storageField.setAccessible(true);
			storageField.set(table, new Storage<>(table, relDb, table.getName()));

			List<Executable> allEight = List.of(
					() -> table.walk((k, v) -> true),
					() -> table.walkDesc((k, v) -> true),
					() -> table.walkKey(k -> true),
					() -> table.walkKeyDesc(k -> true),
					() -> table.walk(null, 10, (k, v) -> true),
					() -> table.walkDesc(null, 10, (k, v) -> true),
					() -> table.walkKey(null, 10, k -> true),
					() -> table.walkKeyDesc(null, 10, k -> true)
			);

			var rc = app.newProcedure(() -> {
				for (var call : allEight) {
					var ex = Assertions.assertThrows(IllegalStateException.class, call);
					Assertions.assertEquals("must be called without transaction", ex.getMessage());
				}
				return Procedure.Success;
			}, "walkInsideTxn").call();
			Assertions.assertEquals(Procedure.Success, rc);
			Assertions.assertEquals(0, relDb.walkCalls.get(), "事务内调用必须在 TableX 门面被拦截，不得到达底层关系表");

			// 事务外调用正常放行
			table.walk((k, v) -> true);
			table.walkKey(null, 10, k -> true);
			Assertions.assertEquals(2, relDb.walkCalls.get(), "事务外调用必须正常委托到底层关系表");
		} finally {
			app.stop();
		}
	}

	// -------------------------------------------------------------------------
	// Stub JDBC 驱动：验证 MySQL walkStreamed 与 PostgreSQL walkCursor
	// -------------------------------------------------------------------------

	private static final ConcurrentHashMap<String, JdbcRecorder> RECORDERS = new ConcurrentHashMap<>();

	private static final class JdbcRecorder {
		final String productName;
		final List<String> sqlLog = Collections.synchronizedList(new ArrayList<>());
		final List<Integer> fetchSizes = Collections.synchronizedList(new ArrayList<>());
		final List<Boolean> autoCommitLog = Collections.synchronizedList(new ArrayList<>());
		final AtomicInteger commitCount = new AtomicInteger();

		JdbcRecorder(String productName) {
			this.productName = productName;
		}
	}

	private static @Nullable Object defaultValue(Class<?> type) {
		if (!type.isPrimitive() || type == void.class)
			return null;
		if (type == boolean.class)
			return false;
		if (type == byte.class)
			return (byte)0;
		if (type == short.class)
			return (short)0;
		if (type == int.class)
			return 0;
		if (type == long.class)
			return 0L;
		if (type == float.class)
			return 0f;
		if (type == double.class)
			return 0d;
		return (char)0;
	}

	private static ResultSet rowsResultSet(String sql) {
		final boolean isConnId = sql.contains("CONNECTION_ID()");
		final int[] cursor = {0};
		final int maxRows = isConnId ? 1 : 3;
		return (ResultSet)Proxy.newProxyInstance(
				TestRelationalTableWalkGuardsAndStreaming.class.getClassLoader(),
				new Class<?>[]{ResultSet.class}, (p, m, a) -> switch (m.getName()) {
					case "next" -> ++cursor[0] <= maxRows;
					case "getLong" -> {
						if (isConnId)
							yield 42L;
						if (a[0] instanceof String col && "val".equals(col))
							yield cursor[0] * 10L;
						yield (long)cursor[0];
					}
					case "wasNull" -> false;
					default -> defaultValue(m.getReturnType());
				});
	}

	private static PreparedStatement preparedStatement(JdbcRecorder rec, String sql) {
		return (PreparedStatement)Proxy.newProxyInstance(
				TestRelationalTableWalkGuardsAndStreaming.class.getClassLoader(),
				new Class<?>[]{PreparedStatement.class}, (p, m, a) -> switch (m.getName()) {
					case "setFetchSize" -> {
						rec.fetchSizes.add((Integer)a[0]);
						yield null;
					}
					case "executeQuery" -> {
						rec.sqlLog.add(sql);
						yield rowsResultSet(sql);
					}
					case "executeUpdate" -> {
						rec.sqlLog.add(sql);
						yield 0;
					}
					default -> defaultValue(m.getReturnType());
				});
	}

	private static Statement statement(JdbcRecorder rec) {
		return (Statement)Proxy.newProxyInstance(
				TestRelationalTableWalkGuardsAndStreaming.class.getClassLoader(),
				new Class<?>[]{Statement.class}, (p, m, a) -> switch (m.getName()) {
					case "execute" -> {
						rec.sqlLog.add((String)a[0]);
						yield false;
					}
					case "executeQuery" -> {
						var sql = (String)a[0];
						rec.sqlLog.add(sql);
						yield rowsResultSet(sql);
					}
					default -> defaultValue(m.getReturnType());
				});
	}

	private static Connection connection(JdbcRecorder rec) {
		final boolean[] autoCommit = {true};
		return (Connection)Proxy.newProxyInstance(
				TestRelationalTableWalkGuardsAndStreaming.class.getClassLoader(),
				new Class<?>[]{Connection.class}, (p, m, a) -> switch (m.getName()) {
					case "getAutoCommit" -> autoCommit[0];
					case "setAutoCommit" -> {
						autoCommit[0] = (Boolean)a[0];
						rec.autoCommitLog.add(autoCommit[0]);
						yield null;
					}
					case "commit" -> {
						rec.commitCount.incrementAndGet();
						yield null;
					}
					case "isValid" -> true;
					case "isClosed" -> false;
					case "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED;
					case "getHoldability" -> ResultSet.CLOSE_CURSORS_AT_COMMIT;
					case "prepareStatement" -> preparedStatement(rec, (String)a[0]);
					case "createStatement" -> statement(rec);
					case "getMetaData" -> Proxy.newProxyInstance(
							TestRelationalTableWalkGuardsAndStreaming.class.getClassLoader(),
							new Class<?>[]{DatabaseMetaData.class},
							(p2, m2, a2) -> "getDatabaseProductName".equals(m2.getName())
									? rec.productName : defaultValue(m2.getReturnType()));
					default -> defaultValue(m.getReturnType());
				});
	}

	public static final class RecordingJdbcDriver implements Driver {
		@Override
		public Connection connect(String url, Properties info) {
			var rec = RECORDERS.get(url);
			if (rec == null)
				throw new IllegalStateException("no recorder for url=" + url);
			return connection(rec);
		}

		@Override
		public boolean acceptsURL(String url) {
			return RECORDERS.containsKey(url);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
			return new DriverPropertyInfo[0];
		}

		@Override
		public int getMajorVersion() {
			return 1;
		}

		@Override
		public int getMinorVersion() {
			return 0;
		}

		@Override
		public boolean jdbcCompliant() {
			return false;
		}

		@Override
		public Logger getParentLogger() {
			return Logger.getGlobal();
		}
	}

	@Test
	public void testMysqlRelationalWalkDatabaseUsesStreamingAndKillsOnEarlyStop() throws Exception {
		Task.tryInitThreadPool();
		var url = "jdbc:mysql://rel-walk-test.invalid/mysql_streaming_walk";
		var rec = new JdbcRecorder("MySQL");
		RECORDERS.put(url, rec);
		var conf = new Config.DatabaseConf();
		conf.setDisableOperates(true);
		conf.setDatabaseUrl(url);
		conf.setDruidConf(new Config.DruidConf());
		conf.getDruidConf().initialSize = 0;
		conf.getDruidConf().minIdle = 0;
		conf.getDruidConf().maxActive = 1;
		conf.getDruidConf().driverClassName = RecordingJdbcDriver.class.getName();

		var db = new DatabaseMySql(null, conf);
		try {
			var table = new WalkRelTable(9919102, "rel_mysql_walk");
			setRelationalTable(table, buildRelationalMeta(db, table.getName()));
			db.addTable(table);
			var relDbTable = db.openRelationalTable(table.getName());

			// 1. walkDatabase 提前返回 false：必须启用 Integer.MIN_VALUE 流式并发送 KILL QUERY
			var seen = new ArrayList<Long>();
			var count = relDbTable.walkDatabase(table, (k, v) -> {
				seen.add(k);
				Assertions.assertEquals(k * 10L, v.val);
				return false;
			});
			Assertions.assertEquals(1L, count);
			Assertions.assertEquals(List.of(1L), seen);
			Assertions.assertTrue(rec.fetchSizes.contains(Integer.MIN_VALUE),
					"MySQL 关系表 walkDatabase 必须走 walkStreamed(Integer.MIN_VALUE)");
			Assertions.assertTrue(rec.sqlLog.contains("SET SESSION net_write_timeout=600"));
			Assertions.assertTrue(rec.sqlLog.contains("KILL QUERY 42"),
					"流式提前中断必须发送 KILL QUERY 中止服务端推送");
			Assertions.assertTrue(rec.sqlLog.contains("SET SESSION net_write_timeout=DEFAULT"));

			// 2. walkDatabaseKey 全量走完：同样走 walkStreamed
			rec.fetchSizes.clear();
			rec.sqlLog.clear();
			var keys = new ArrayList<Long>();
			var keyCount = relDbTable.walkDatabaseKey(table, k -> {
				keys.add(k);
				return true;
			});
			Assertions.assertEquals(3L, keyCount);
			Assertions.assertEquals(List.of(1L, 2L, 3L), keys);
			Assertions.assertTrue(rec.fetchSizes.contains(Integer.MIN_VALUE),
					"MySQL 关系表 walkDatabaseKey 必须走 walkStreamed(Integer.MIN_VALUE)");
		} finally {
			db.close();
			RECORDERS.remove(url);
		}
	}

	@Test
	public void testPostgresqlRelationalWalkDatabaseUsesCursorBatching() throws Exception {
		Task.tryInitThreadPool();
		var url = "jdbc:postgresql://rel-walk-test.invalid/pg_cursor_walk";
		var rec = new JdbcRecorder("PostgreSQL");
		RECORDERS.put(url, rec);
		var conf = new Config.DatabaseConf();
		conf.setDisableOperates(true);
		conf.setDatabaseUrl(url);
		conf.setDruidConf(new Config.DruidConf());
		conf.getDruidConf().initialSize = 0;
		conf.getDruidConf().minIdle = 0;
		conf.getDruidConf().maxActive = 1;
		conf.getDruidConf().driverClassName = RecordingJdbcDriver.class.getName();

		var db = new DatabasePostgreSQL(null, conf);
		try {
			var table = new WalkRelTable(9919103, "rel_pg_walk");
			setRelationalTable(table, buildRelationalMeta(db, table.getName()));
			db.addTable(table);
			var relDbTable = db.openRelationalTable(table.getName());

			// 1. walkDatabase 提前返回 false：必须走 walkCursor（autoCommit=false + fetchSize=1000 + commit）
			var seen = new ArrayList<Long>();
			var count = relDbTable.walkDatabase(table, (k, v) -> {
				seen.add(k);
				Assertions.assertEquals(k * 10L, v.val);
				return false;
			});
			Assertions.assertEquals(1L, count);
			Assertions.assertEquals(List.of(1L), seen);
			Assertions.assertTrue(rec.fetchSizes.contains(1000),
					"PostgreSQL 关系表 walkDatabase 必须走 walkCursor(fetchSize=1000)");
			Assertions.assertTrue(rec.sqlLog.contains("SET LOCAL idle_in_transaction_session_timeout=0"));
			Assertions.assertTrue(rec.autoCommitLog.contains(false));
			Assertions.assertTrue(rec.commitCount.get() >= 1);

			// 2. walkDatabaseKey 全量走完：同样走 walkCursor
			rec.fetchSizes.clear();
			rec.sqlLog.clear();
			var keys = new ArrayList<Long>();
			var keyCount = relDbTable.walkDatabaseKey(table, k -> {
				keys.add(k);
				return true;
			});
			Assertions.assertEquals(3L, keyCount);
			Assertions.assertEquals(List.of(1L, 2L, 3L), keys);
			Assertions.assertTrue(rec.fetchSizes.contains(1000),
					"PostgreSQL 关系表 walkDatabaseKey 必须走 walkCursor(fetchSize=1000)");
		} finally {
			db.close();
			RECORDERS.remove(url);
		}
	}
}
