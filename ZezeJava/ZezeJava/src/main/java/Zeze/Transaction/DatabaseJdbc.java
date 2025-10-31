package Zeze.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
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

		var dc = conf.getDruidConf();
		var ds = dataSource;

		// 数据库连接的URL: jdbc:mysql://localhost:3306/dbname?user=root&password=xxxx&useSSL=false&serverTimezone=UTC
		ds.setUrl(conf.getDatabaseUrl());

		// 数据库驱动类名, null表示根据url前缀自动设置, 默认:null
		ds.setDriverClassName(dc.driverClassName);

		// 连接的用户名, 如果URL有用户名则可以不设置或设置为null, 默认:null
		ds.setUsername(dc.userName);

		// 连接的密码, 如果URL有密码则可以不设置或设置为null, 默认:null
		ds.setPassword(dc.password);

		// 总是开启PreparedStatement缓存, 默认:false
		ds.setPoolPreparedStatements(true);

		// 总是设置,防止错误连接的结果被下一个查询得到, 默认:false
		ds.setKillWhenSocketReadTimeout(true);

		// 初始连接数, 默认:0
		ds.setInitialSize(Objects.requireNonNullElse(dc.initialSize, 4));

		// 最小空闲连接数, 默认:0
		ds.setMinIdle(Objects.requireNonNullElse(dc.minIdle, 4));

		// 最大连接数(MySQL服务器连接数上限默认151), 默认:8
		ds.setMaxActive(Objects.requireNonNullElse(dc.maxActive, 8));

		// 等待可用连接的超时时间, <=0表示没有超时, 默认:-1
		ds.setMaxWait(Objects.requireNonNullElse(dc.maxWait, -1L));

		// 每个连接的PreparedStatement缓存数量上限(MySQL服务器总量上限默认16K), 开启缓存时默认:10
		ds.setMaxOpenPreparedStatements(Objects.requireNonNullElse(dc.maxOpenPreparedStatements, 256));

		// 通常无需配置, 默认:-1
		ds.setPhyMaxUseCount(Objects.requireNonNullElse(dc.phyMaxUseCount, -1));

		// 通常无需配置, 默认:-1
		ds.setPhyTimeoutMillis(Objects.requireNonNullElse(dc.phyTimeoutMillis, -1L));
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
