package Zeze.Transaction;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Config;

/**
 * walk流式抓取中断清理不得依赖业务池空闲容量。
 * <p>
 * walkStreamed持有业务连接期间回调中断，finally的killQuery修复前从同一Druid池再借一条
 * 连接发KILL QUERY：池被并发walk占满（maxActive=1即一次walk占满）且maxWait默认-1无限
 * 等待时，清理者等第二条连接、被清理的walk连接等清理完成才归还——互等死锁，walk永不
 * 返回。修复：killQuery走独立的带期限控制池（容量2、maxWait=10s，懒初始化），控制通路
 * 失败再废弃物理连接；清理永不依赖业务池空闲。
 * <p>
 * 真实DatabaseMySql.walkStreamed+真实Druid（maxActive=1/maxWait=-1），仅物理JDBC是
 * stub驱动（审核复现同法，无真实MySQL）。修复前红：worker在killQuery借连接处永久阻塞，
 * join超时；修复后绿：KILL走控制池的第二条连接，walk在期限内返回、业务连接归还。
 */
@Fast
public class TestMysqlWalkKillControlPool {

	// 借出的物理连接计数：业务连接+控制连接各至少一条，证明KILL走的是独立连接。
	static final AtomicInteger physicalConnections = new AtomicInteger();

	private static Object defaultValue(Class<?> type) {
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

	private static Object resultSet() {
		int[] seen = {0};
		return Proxy.newProxyInstance(TestMysqlWalkKillControlPool.class.getClassLoader(),
				new Class<?>[]{ResultSet.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "next" -> ++seen[0] == 1;
						case "getLong" -> 42L;
						case "getInt" -> 42;
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static Object statement(Class<?> type) {
		return Proxy.newProxyInstance(TestMysqlWalkKillControlPool.class.getClassLoader(),
				new Class<?>[]{type}, (p, m, a) -> {
					return switch (m.getName()) {
						case "executeQuery", "getResultSet" -> resultSet();
						case "execute" -> false;
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	private static Connection connection() {
		return (Connection)Proxy.newProxyInstance(TestMysqlWalkKillControlPool.class.getClassLoader(),
				new Class<?>[]{Connection.class}, (p, m, a) -> {
					return switch (m.getName()) {
						case "getAutoCommit", "isValid" -> true;
						case "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED;
						case "getHoldability" -> ResultSet.CLOSE_CURSORS_AT_COMMIT;
						case "prepareStatement" -> statement(PreparedStatement.class);
						case "createStatement" -> statement(Statement.class);
						case "getMetaData" -> Proxy.newProxyInstance(
								TestMysqlWalkKillControlPool.class.getClassLoader(),
								new Class<?>[]{DatabaseMetaData.class}, (p2, m2, a2) ->
										m2.getName().equals("getDatabaseProductName") ? "MySQL" : defaultValue(m2.getReturnType()));
						default -> defaultValue(m.getReturnType());
					};
				});
	}

	public static final class StubDriver implements Driver {
		@Override
		public Connection connect(String url, Properties info) {
			physicalConnections.incrementAndGet();
			return connection();
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

	@Test
	public void testKillQueryUsesIndependentControlPoolNotSaturatedBusinessPool() throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDisableOperates(true);
		conf.setDatabaseUrl("jdbc:mysql://test.invalid/dummy");
		conf.setDruidConf(new Config.DruidConf());
		conf.getDruidConf().initialSize = 0;
		conf.getDruidConf().minIdle = 0;
		conf.getDruidConf().maxActive = 1;
		conf.getDruidConf().maxWait = -1L; // 默认：无限等待——饱和即互等死锁的配置前提
		conf.getDruidConf().driverClassName = StubDriver.class.getName();

		var db = new DatabaseMySql(null, conf);
		try {
			var method = DatabaseMySql.class.getDeclaredMethod("walkStreamed", String.class, DatabaseJdbc.JdbcWalkRow.class);
			method.setAccessible(true);

			var callbackReached = new CountDownLatch(1);
			var done = new CountDownLatch(1);
			var failure = new AtomicReference<Throwable>();
			var worker = new Thread(() -> {
				try {
					method.invoke(db, "SELECT id FROM test_data", (DatabaseJdbc.JdbcWalkRow)rs -> {
						callbackReached.countDown();
						return false; // walk契约：回调返回false中断
					});
				} catch (Throwable e) {
					failure.set(e);
				} finally {
					done.countDown();
				}
			}, "mysql-walk-worker");
			worker.setDaemon(true);
			worker.start();
			Assertions.assertTrue(callbackReached.await(5, TimeUnit.SECONDS), "回调必须先到达");

			// 修复前：killQuery在业务池getConnection处无限等待（maxActive=1被walk自己占满，
			// maxWait=-1），join必然超时；修复后：KILL走控制池独立连接，迅速返回。
			Assertions.assertTrue(done.await(15, TimeUnit.SECONDS), "walk必须在期限内返回（清理不得依赖业务池空闲）");
			worker.join(5000);
			Assertions.assertFalse(worker.isAlive());
			Assertions.assertNull(failure.get(), "walk不得因清理失败而异常");

			Assertions.assertTrue(physicalConnections.get() >= 2,
					"KILL必须走独立的控制连接（物理连接数>=2：业务1条+控制>=1条），实际=" + physicalConnections.get());
			Assertions.assertEquals(0, db.dataSource.getActiveCount(), "业务连接必须已归还");
		} finally {
			db.close(); // 同时关闭业务池与懒初始化的控制池
		}
	}
}
