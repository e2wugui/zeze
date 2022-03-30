package Zeze.Transaction;

import java.sql.SQLException;
import Zeze.Config.DatabaseConf;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class DatabaseJdbc extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseMySql.class);

	protected final BasicDataSource dataSource;

	public DatabaseJdbc(DatabaseConf conf) {
		super(conf);

		dataSource = new BasicDataSource();
		var dbcpConf = conf.getDbcpConf();

		BasicDataSource pool = this.dataSource;// 连接池

		// must present
		pool.setUrl(conf.getDatabaseUrl());
		pool.setDriverClassName(dbcpConf.DriverClassName); // setup in Zeze.Config.DatabaseConf

		// always on
		pool.setPoolPreparedStatements(true);

		// options
		if (dbcpConf.UserName != null)
			pool.setUsername(dbcpConf.UserName);
		if (dbcpConf.Password != null)
			pool.setPassword(dbcpConf.Password);
		if (dbcpConf.InitialSize != null)
			pool.setInitialSize(dbcpConf.InitialSize); // 初始的连接数；
		if (dbcpConf.MaxTotal != null)
			pool.setMaxTotal(dbcpConf.MaxTotal);
		if (dbcpConf.MaxIdle != null)
			pool.setMaxIdle(dbcpConf.MaxIdle);
		if (dbcpConf.MinIdle != null)
			pool.setMinIdle(dbcpConf.MinIdle);
		if (dbcpConf.MaxWaitMillis != null)
			pool.setMaxWaitMillis(dbcpConf.MaxWaitMillis);
	}

	@Override
	public void Close() {
		super.Close();
		try {
			dataSource.close();
		} catch (SQLException skip) {
			logger.error(skip);
		}
	}

	@Override
	public Transaction BeginTransaction() {
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
		public void Commit() {
			try {
				Connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void Rollback() {
			try {
				Connection.rollback();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
