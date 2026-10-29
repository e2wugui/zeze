package Zeze.Transaction;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import Zeze.Config;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import harness.Fast;
import harness.JdbcTestBackend;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@NullMarked
public class TestJdbcConstructionResources {
	private static final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

	private static final class State {
		final SQLException failure = new SQLException("metadata initialization failed", "HY000");
		final AtomicInteger opened = new AtomicInteger();
		final AtomicInteger closed = new AtomicInteger();
	}

	private static @Nullable Object defaultValue(Class<?> type) {
		if (!type.isPrimitive() || type == void.class)
			return null;
		if (type == boolean.class)
			return false;
		if (type == int.class)
			return 0;
		if (type == long.class)
			return 0L;
		throw new AssertionError("unexpected JDBC return type: " + type);
	}

	public static final class StubDriver implements Driver {
		@Override
		public Connection connect(String url, Properties info) {
			var state = states.get(url);
			assertNotNull(state);
			state.opened.incrementAndGet();
			var autoCommit = new AtomicBoolean(true);
			var closed = new AtomicBoolean();
			return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
					(proxy, method, args) -> switch (method.getName()) {
					case "getAutoCommit" -> autoCommit.get();
					case "setAutoCommit" -> { autoCommit.set((boolean)args[0]); yield null; }
					case "isValid" -> true;
					case "isClosed" -> closed.get();
					case "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED;
					case "getHoldability" -> ResultSet.CLOSE_CURSORS_AT_COMMIT;
					case "prepareStatement" -> throw state.failure;
					case "createStatement" -> Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class<?>[]{Statement.class}, (p, m, a) -> defaultValue(m.getReturnType()));
					case "close" -> {
						if (closed.compareAndSet(false, true))
							state.closed.incrementAndGet();
						yield null;
					}
					case "getMetaData" -> Proxy.newProxyInstance(getClass().getClassLoader(),
							new Class<?>[]{DatabaseMetaData.class}, (p, m, a) ->
							m.getName().equals("getDatabaseProductName") ? "MySQL" : defaultValue(m.getReturnType()));
					default -> defaultValue(method.getReturnType());
					});
		}

		@Override public boolean acceptsURL(String url) { return states.containsKey(url); }
		@Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
		@Override public int getMajorVersion() { return 1; }
		@Override public int getMinorVersion() { return 0; }
		@Override public boolean jdbcCompliant() { return false; }
		@Override public Logger getParentLogger() { return Logger.getGlobal(); }
	}

	private static DatabaseJdbc construct(String backend, Config.DatabaseConf conf) {
		return JdbcTestBackend.parse(backend).create(conf);
	}

	private static Config.DatabaseConf conf(String url, boolean disabled) {
		var conf = new Config.DatabaseConf(); // 无 Application；仅真实连接池与隔离的物理 JDBC 桩。
		conf.setDatabaseUrl(url);
		conf.setDisableOperates(disabled);
		var druid = new Config.DruidConf();
		druid.driverClassName = StubDriver.class.getName();
		druid.initialSize = 1;
		druid.minIdle = 0;
		druid.maxActive = 1;
		druid.maxWait = 3000L;
		conf.setDruidConf(druid);
		return conf;
	}

	@Test void mysqlInitializationFailureClosesPool() { initializationFailureClosesPool("MySql"); }
	@Test void postgresqlInitializationFailureClosesPool() { initializationFailureClosesPool("PostgreSQL"); }
	@Test void sqlServerInitializationFailureClosesPool() { initializationFailureClosesPool("SqlServer"); }

	private static void initializationFailureClosesPool(String backend) {
		var url = "jdbc:mysql://construction.invalid/" + UUID.randomUUID();
		var state = new State();
		states.put(url, state);
		try {
			assertSame(state.failure, assertThrows(SQLException.class, () -> construct(backend, conf(url, false))));
			assertTrue(state.opened.get() > 0, "必须已启动真实 Druid 池");
			assertEquals(state.opened.get(), state.closed.get(), "未交付的池必须释放全部物理连接");
			assertFalse(DruidDataSourceStatManager.getDruidDataSourceInstances().stream()
					.anyMatch(ds -> ds.getUrl().equals(url)), "失败的池不得继续登记为存活实例");
		} finally {
			// 原实现回归失败也只清理本用例留下的池，不干扰其他并行测试。
			for (var ds : DruidDataSourceStatManager.getDruidDataSourceInstances())
				if (ds.getUrl().equals(url))
					((DruidDataSource)ds).close();
			states.remove(url);
		}
	}

	@Test
	void disabledOperatesKeepsPoolLazy() {
		for (var backend : new String[]{"MySql", "PostgreSQL", "SqlServer"})
			disabledOperatesKeepsPoolLazy(backend);
	}

	private static void disabledOperatesKeepsPoolLazy(String backend) {
		var url = "jdbc:mysql://construction.invalid/" + UUID.randomUUID();
		var state = new State();
		states.put(url, state);
		try {
			var db = construct(backend, conf(url, true));
			try {
				assertEquals(0, state.opened.get());
				assertFalse(db.dataSource.isClosed());
			} finally {
				db.close();
			}
		} finally {
			states.remove(url);
		}
	}
}
