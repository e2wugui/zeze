package Zeze.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Util.Task;
import com.alibaba.druid.pool.DruidDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DatabaseJdbc extends Database {
	protected final DruidDataSource dataSource = new DruidDataSource();

	public DatabaseJdbc(@Nullable Application zeze, @NotNull DatabaseConf conf) {
		super(zeze, conf);

		var druidConf = conf.getDruidConf();
		var pool = dataSource; // 连接池

		// must present
		pool.setUrl(conf.getDatabaseUrl());
		pool.setDriverClassName(druidConf.driverClassName); // setup in Zeze.Config.DatabaseConf

		// always on
		pool.setPoolPreparedStatements(true);
		pool.setKillWhenSocketReadTimeout(true); // 总是设置，防止错误连接的结果被下一个查询得到。

		// options
		if (druidConf.initialSize != null)
			pool.setInitialSize(druidConf.initialSize); // 初始的连接数；
		if (druidConf.maxActive != null)
			pool.setMaxActive(druidConf.maxActive);
		if (druidConf.minIdle != null)
			pool.setMinIdle(druidConf.minIdle);
		if (druidConf.maxWait != null)
			pool.setMaxWait(druidConf.maxWait);
		if (druidConf.maxOpenPreparedStatements != null)
			pool.setMaxOpenPreparedStatements(druidConf.maxOpenPreparedStatements);
		//if (druidConf.maxIdle != null)
		//	pool.setMaxIdle(druidConf.maxIdle);

		if (druidConf.phyMaxUseCount != null)
			pool.setPhyMaxUseCount(druidConf.phyMaxUseCount);
		if (druidConf.phyTimeoutMillis != null)
			pool.setPhyTimeoutMillis(druidConf.phyTimeoutMillis);

		if (druidConf.userName != null)
			pool.setUsername(druidConf.userName);
		if (druidConf.password != null)
			pool.setPassword(druidConf.password);
	}

	@Override
	public void close() {
		super.close();
		dataSource.close();
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new JdbcTrans(dataSource);
	}

	public static class JdbcTrans implements Transaction {
		final @NotNull Connection connection;

		public JdbcTrans(@NotNull DruidDataSource dataSource) {
			try {
				connection = dataSource.getConnection();
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				Task.forceThrow(e);
				throw new AssertionError(); // never run here
			}
		}

		@Override
		public void close() {
			try {
				connection.close();
			} catch (Throwable e) { // logger.error
				logger.error("JdbcTrans.close", e);
			}
		}

		@Override
		public void commit() {
			try {
				connection.commit();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public void rollback() {
			try {
				connection.rollback();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}
	}
}
