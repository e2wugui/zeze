package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import Zeze.Config;
import Zeze.Util.Task;
import com.alibaba.druid.pool.DruidDataSource;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * OperatesMySql构造器曾无条件DROP+CREATE三个存储过程——DDL在构造器执行、
 * 先于启动锁（atomicOpenDatabase），多实例共享库时每次启动都打开"过程缺失"窗口，
 * 干扰正在调用过程的在跑实例。修复后按版本戳门控：三个过程体SQL文本的SHA-256存
 * _ZezeDataWithVersion_专用行，戳一致跳过全部过程DDL，仅过程体变更时重建一次；
 * 运维dropOperatesProcedures清戳，下次启动重建。
 * <p>
 * 真实生产类+真实Druid池，仅物理JDBC为记录型stub驱动（判例：审核复现AuditMysqlWalkPool
 * 的代理形态）：记录全部SQL并模拟_ZezeDataWithVersion_戳行的存取。
 */
@Fast
public class TestMysqlOperatesProceduresStamp {

	/** 模拟库：戳行存储 + SQL流水。 */
	private static final class StubDatabase {
		final ConcurrentHashMap<String, byte[]> dataWithVersion = new ConcurrentHashMap<>();
		final List<String> sqlLog = new ArrayList<>();

		/** 按语句前缀计数：CREATE PROCEDURE的过程体文本包含DELETE等字样，contains会误计。 */
		long count(String prefix) {
			return sqlLog.stream().filter(s -> s.startsWith(prefix)).count();
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

	private static String keyOf(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	// setBytes参数按参数序暂存（同线程set后立即execute，Druid maxActive=1下串行）；
	// 戳行SELECT/REPLACE/DELETE均以参数1为行key，REPLACE参数2为戳值。
	private static final ThreadLocal<byte[][]> bindArgs = ThreadLocal.withInitial(() -> new byte[3][]);

	private static ResultSet resultSet(StubDatabase db, String sql) {
		var args = bindArgs.get();
		var stamp = sql.contains("FROM _ZezeDataWithVersion_") && args[0] != null
				? db.dataWithVersion.get(keyOf(args[0])) : null;
		var tableExistsProbe = sql.contains("FROM information_schema.tables"); // drop路径的表存在探测：恒存在
		var seen = new int[]{0};
		return (ResultSet)Proxy.newProxyInstance(TestMysqlOperatesProceduresStamp.class.getClassLoader(),
				new Class<?>[]{ResultSet.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "next" -> ++seen[0] == 1 && (stamp != null || tableExistsProbe);
						case "getBytes" -> stamp;
						case "getString" -> "1";
						case "getLong" -> 0L;
						case "getInt" -> 0;
						case "wasNull" -> false;
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static Object preparedStatement(StubDatabase db, String sql) {
		var args = bindArgs.get();
		return Proxy.newProxyInstance(TestMysqlOperatesProceduresStamp.class.getClassLoader(),
				new Class<?>[]{PreparedStatement.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "setBytes" -> {
							args[(Integer)a[0] - 1] = (byte[])a[1];
							yield null;
						}
						case "setString" -> {
							args[(Integer)a[0] - 1] = ((String)a[1]).getBytes(StandardCharsets.UTF_8);
							yield null;
						}
						case "executeQuery" -> {
							db.sqlLog.add(sql);
							yield resultSet(db, sql);
						}
						case "executeUpdate" -> {
							db.sqlLog.add(sql);
							if (sql.startsWith("REPLACE INTO _ZezeDataWithVersion_") && args[0] != null)
								db.dataWithVersion.put(keyOf(args[0]), args[1]);
							else if (sql.startsWith("DELETE FROM _ZezeDataWithVersion_") && args[0] != null)
								db.dataWithVersion.remove(keyOf(args[0]));
							yield 0;
						}
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static Object statement(StubDatabase db) {
		return Proxy.newProxyInstance(TestMysqlOperatesProceduresStamp.class.getClassLoader(),
				new Class<?>[]{Statement.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "execute" -> {
							db.sqlLog.add((String)a[0]);
							yield false;
						}
						case "executeQuery" -> {
							db.sqlLog.add((String)a[0]);
							yield resultSet(db, (String)a[0]);
						}
						case "executeUpdate" -> {
							db.sqlLog.add((String)a[0]);
							yield 0;
						}
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static Connection connection(StubDatabase db) {
		return (Connection)Proxy.newProxyInstance(TestMysqlOperatesProceduresStamp.class.getClassLoader(),
				new Class<?>[]{Connection.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "getAutoCommit", "isValid" -> true;
						case "isClosed" -> false; // 必须false：Druid建连后validateConnection据此判定，true=无限重建连接
						case "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED;
						case "getHoldability" -> ResultSet.CLOSE_CURSORS_AT_COMMIT;
						case "prepareStatement" -> preparedStatement(db, (String)a[0]);
						case "createStatement" -> statement(db); // 返回null会被Druid包装成半初始化对象，close时NPE
						case "getMetaData" -> Proxy.newProxyInstance(
								TestMysqlOperatesProceduresStamp.class.getClassLoader(),
								new Class<?>[]{DatabaseMetaData.class},
								(p2, m2, a2) -> m2.getName().equals("getDatabaseProductName")
										? "MySQL" : defaultValue(m2.getReturnType()));
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static final class StubDriver implements Driver {
		private final StubDatabase db;

		StubDriver(StubDatabase db) {
			this.db = db;
		}

		@Override
		public Connection connect(String url, Properties info) {
			return connection(db);
		}

		@Override
		public boolean acceptsURL(String url) {
			return true;
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

	/** 反射构造OperatesMySql（私有内部类），走真实构造器的DDL流程。 */
	private static void runOperatesCtor(DatabaseMySql db) throws Exception {
		Class<?> operatesClass = null;
		for (var c : DatabaseMySql.class.getDeclaredClasses())
			if (c.getSimpleName().equals("OperatesMySql"))
				operatesClass = c;
		Assertions.assertNotNull(operatesClass);
		var ctor = operatesClass.getDeclaredConstructor(DatabaseMySql.class);
		ctor.setAccessible(true);
		ctor.newInstance(db);
	}

	@Test
	public void testStampGatesProcedureDdl() throws Exception {
		Task.tryInitThreadPool();
		var stub = new StubDatabase();
		var conf = new Config.DatabaseConf();
		conf.setDisableOperates(true); // 构造期不跑DDL，换装stub驱动后再显式触发
		conf.setDatabaseUrl("jdbc:mysql://stamp-test.invalid/dummy");
		conf.setDruidConf(new Config.DruidConf());
		conf.getDruidConf().initialSize = 0;
		conf.getDruidConf().minIdle = 0;
		conf.getDruidConf().maxActive = 1;
		var db = new DatabaseMySql(null, conf);
		try {
			var field = DatabaseJdbc.class.getDeclaredField("dataSource");
			field.setAccessible(true);
			var pool = (DruidDataSource)field.get(db);
			pool.setDriver(new StubDriver(stub));
			pool.setTestOnBorrow(false);
			pool.setTestWhileIdle(false);
			pool.setPoolPreparedStatements(false);

			// 首轮（无戳）：建表x2 + DROPx3 + CREATE PROCEDUREx3 + 写戳
			runOperatesCtor(db);
			Assertions.assertEquals(1, stub.count("CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_"));
			Assertions.assertEquals(1, stub.count("CREATE TABLE IF NOT EXISTS _ZezeInstances_"));
			Assertions.assertEquals(3, stub.count("DROP PROCEDURE IF EXISTS"), "无戳时必须DROP三个过程");
			Assertions.assertEquals(3, stub.count("CREATE PROCEDURE"), "无戳时必须CREATE三个过程");
			Assertions.assertEquals(1, stub.count("REPLACE INTO _ZezeDataWithVersion_"), "DDL后必须写入版本戳");
			var stampKey = keyOf(DatabaseMySql.keyOfOperatesProceduresStamp);
			var stamp = stub.dataWithVersion.get(stampKey);
			Assertions.assertNotNull(stamp, "戳行必须落库");
			Assertions.assertEquals(64, stamp.length, "戳为SHA-256 hex（64字符），实际=" + keyOf(stamp));

			// 次轮（戳一致）：跳过全部过程DDL——常态启动零过程DDL是本修复的核心
			runOperatesCtor(db);
			Assertions.assertEquals(3, stub.count("DROP PROCEDURE IF EXISTS"), "戳一致时不得再DROP");
			Assertions.assertEquals(3, stub.count("CREATE PROCEDURE"), "戳一致时不得再CREATE");
			Assertions.assertEquals(1, stub.count("REPLACE INTO _ZezeDataWithVersion_"), "戳一致时不得重写戳");

			// 运维drop：清过程且清戳（留戳会让下次启动按戳跳过重建、过程持续缺失）
			db.dropOperatesProcedures();
			Assertions.assertEquals(6, stub.count("DROP PROCEDURE IF EXISTS"));
			// DELETE累计=2：首轮构造器drop一次 + 本次运维drop一次
			Assertions.assertEquals(2, stub.count("DELETE FROM _ZezeDataWithVersion_"), "drop必须清戳");
			Assertions.assertNull(stub.dataWithVersion.get(stampKey), "drop后戳行必须删除");

			// 第三轮（戳已清）：重建
			runOperatesCtor(db);
			Assertions.assertEquals(6, stub.count("CREATE PROCEDURE"), "drop清戳后必须重建三个过程");
			Assertions.assertEquals(3, stub.count("DELETE FROM _ZezeDataWithVersion_"),
					"重建轮drop同样清戳（幂等）");
			Assertions.assertNotNull(stub.dataWithVersion.get(stampKey), "重建后重新写戳");
		} finally {
			db.close();
		}
	}
}
