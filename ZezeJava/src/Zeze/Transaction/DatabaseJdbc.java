package Zeze.Transaction;

import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Config.DatabaseConf;

public abstract class DatabaseJdbc extends Database {
	private static final Logger logger = LogManager.getLogger(DatabaseMySql.class);

	protected BasicDataSource dataSource;
	
	public DatabaseJdbc(DatabaseConf conf) {
		super(conf);

		dataSource = new BasicDataSource();
		var dbcpconf = conf.getDbcpConf();

		BasicDataSource pool = this.dataSource;// 连接池

		// must present
		pool.setUrl(conf.getDatabaseUrl());
		pool.setDriverClassName(dbcpconf.DriverClassName); // setup in Zeze.Config.DatabaseConf

		// always on
		pool.setPoolPreparedStatements(true);

		// options
		if (dbcpconf.UserName != null)
			pool.setUsername(dbcpconf.UserName);
		if (dbcpconf.Password !=  null)
			pool.setPassword(dbcpconf.Password);
		if (dbcpconf.InitialSize != null)
			pool.setInitialSize(dbcpconf.InitialSize); // 初始的连接数；
		if (dbcpconf.MaxTotal != null)
			pool.setMaxTotal(dbcpconf.MaxTotal);
		if (dbcpconf.MaxIdle != null)
			pool.setMaxIdle(dbcpconf.MaxIdle);
		if (dbcpconf.MinIdle != null)
			pool.setMinIdle(dbcpconf.MinIdle);
		if (dbcpconf.MaxWaitMillis != null)
			pool.setMaxWaitMillis(dbcpconf.MaxWaitMillis);
	}
	
	@Override
	public void Close() {
		super.Close();
		try {
			dataSource.close();
		} catch (SQLException skip) {
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
