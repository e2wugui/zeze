package Zeze.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Util.Task;
import com.alibaba.druid.pool.DruidDataSource;

public abstract class DatabaseJdbc extends Database {
	protected final DruidDataSource dataSource;

	public DatabaseJdbc(Application zeze, DatabaseConf conf) {
		super(zeze, conf);

		dataSource = new DruidDataSource();
		var druidConf = conf.getDruidConf();

		DruidDataSource pool = dataSource;// 连接池

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
	public Transaction beginTransaction() {
		return new JdbcTrans();
	}

	public class JdbcTrans implements Transaction {
		Connection connection;

		public JdbcTrans() {
			try {
				connection = dataSource.getConnection();
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				Task.forceThrow(e);
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
