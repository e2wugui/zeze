package Temp;

import java.sql.SQLException;
import com.alibaba.druid.pool.DruidDataSource;

public class MysqlTransactionBehaviorWhenHaltProgram {
	public static void main(String []args) throws SQLException, InterruptedException {
		String url = null;
		String cmd = "prepare";
		for (var i = 0; i < args.length; ++i) {
			var arg = args[i];
			if (arg.equals("-url"))
				url = args[++i];
			else if (arg.equals("-cmd"))
				cmd = args[++i];
		}

		var dataSource = new DruidDataSource();
		dataSource.setUrl(url);
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		try {
			switch (cmd) {
			case "prepare":
				prepare(dataSource);
				break;

			case "halt":
				halt(dataSource);
				break;

			case "verify":
				verify(dataSource);
				break;
			}
		} finally {
			dataSource.close();
		}
	}

	static void verify(DruidDataSource dataSource) throws SQLException {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(true);
			var sql = "select id from halt_test";
			try (var cmd = connection.prepareStatement(sql)) {
				try (var rs = cmd.executeQuery()) {
					while (rs.next()) {
						var id = rs.getInt(1);
						System.out.println(id);
					}
				}
			}
		}
	}

	static void halt(DruidDataSource dataSource) throws SQLException, InterruptedException {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);

			var sql = "insert into halt_test values(1)";
			try (var cmd = connection.prepareStatement(sql)) {
				cmd.executeUpdate();
			}
			Thread.sleep(1000); // 基本确保mysql收到请求
			// 没有commit，也没有rollback，halt。
			Runtime.getRuntime().halt(0);
		}
	}

	static void prepare(DruidDataSource dataSource) throws SQLException {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);

			String create = "CREATE TABLE IF NOT EXISTS halt_test (id integer) NOT NULL PRIMARY KEY)";
			try (var cmd = connection.prepareStatement(create)) {
				cmd.executeUpdate();
			}
			String clear = "delete from halt_test";
			try (var cmd = connection.prepareStatement(clear)) {
				cmd.executeUpdate();
			}
			connection.commit();
		}
	}
}
