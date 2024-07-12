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
		pool.setMaxPoolPreparedStatementPerConnectionSize(32);

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
		try {
			super.close();
		} catch (Throwable e) { // logger.error
			logger.error("Database.close exception:", e);
		}
		try {
			dataSource.close();
		} catch (Throwable e) { // logger.error
			logger.error("DruidDataSource.close exception:", e);
		}
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		try {
			return new JdbcTrans(dataSource.getConnection());
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		}
	}

	public static class JdbcTrans implements Transaction {
		final @NotNull Connection conn;

		public JdbcTrans(@NotNull Connection conn) throws SQLException {
			conn.setAutoCommit(false);
			this.conn = conn;
		}

		@Override
		public void commit() {
			try {
				conn.commit();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public void rollback() {
			try {
				conn.rollback();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public void close() {
			try {
				conn.close();
			} catch (Throwable e) { // logger.error
				logger.error("JdbcTrans.close exception:", e);
			}
		}
	}
}
