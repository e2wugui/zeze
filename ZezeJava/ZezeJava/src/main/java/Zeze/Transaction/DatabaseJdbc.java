package Zeze.Transaction;

import java.sql.SQLException;
import Zeze.Config.DatabaseConf;
import org.apache.commons.dbcp2.BasicDataSource;

public abstract class DatabaseJdbc extends Database {
	protected final BasicDataSource dataSource;

	public DatabaseJdbc(DatabaseConf conf) {
		super(conf);

		dataSource = new BasicDataSource();
		var dbcpConf = conf.getDbcpConf();

		BasicDataSource pool = dataSource;// 连接池

		// must present
		pool.setUrl(conf.getDatabaseUrl());
		pool.setDriverClassName(dbcpConf.driverClassName); // setup in Zeze.Config.DatabaseConf

		// always on
		pool.setPoolPreparedStatements(true);

		// options
		if (dbcpConf.userName != null)
			pool.setUsername(dbcpConf.userName);
		if (dbcpConf.password != null)
			pool.setPassword(dbcpConf.password);
		if (dbcpConf.initialSize != null)
			pool.setInitialSize(dbcpConf.initialSize); // 初始的连接数；
		if (dbcpConf.maxTotal != null)
			pool.setMaxTotal(dbcpConf.maxTotal);
		if (dbcpConf.maxIdle != null)
			pool.setMaxIdle(dbcpConf.maxIdle);
		if (dbcpConf.minIdle != null)
			pool.setMinIdle(dbcpConf.minIdle);
		if (dbcpConf.maxWaitMillis != null)
			pool.setMaxWaitMillis(dbcpConf.maxWaitMillis);
	}

	@Override
	public void close() {
		super.close();
		try {
			dataSource.close();
		} catch (SQLException skip) {
			logger.error("", skip);
		}
	}

	@Override
	public Transaction beginTransaction() {
		return new JdbcTrans();
	}

	public class JdbcTrans implements Transaction {
		java.sql.Connection Connection;

		public JdbcTrans() {
			try {
				Connection = dataSource.getConnection();
				Connection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void close() {
			try {
				Connection.close();
			} catch (Throwable e) {
				logger.error("JdbcTrans.close", e);
			}
		}

		@Override
		public void commit() {
			try {
				Connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void rollback() {
			try {
				Connection.rollback();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
