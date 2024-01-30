package Temp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.alibaba.druid.pool.DruidDataSource;

public class MysqlTransactionBehaviorWhenHaltProgram {
	public static void main(String []args) throws SQLException, InterruptedException, ClassNotFoundException {
		String url = "jdbc:mysql://localhost:3306/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
		String cmd = "prepare";
		String driver = "jdbc";
		for (var i = 0; i < args.length; ++i) {
			var arg = args[i];
			if (arg.equals("-url"))
				url = args[++i];
			else if (arg.equals("-cmd"))
				cmd = args[++i];
			else if (arg.equals("-driver"))
				driver = args[++i];
		}

		System.out.println("driver=" + driver);
		switch (driver) {
		case "jdbc":
			jdbc(url, cmd);
			break;
		case "druid":
			druid(url, cmd);
			break;
		}
	}

	static void jdbc(String url, String cmd) throws SQLException, InterruptedException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		try (var connection = DriverManager.getConnection(url)) {
			switch (cmd) {
			case "prepare":
				prepare(connection);
				break;

			case "halt":
				halt(connection);
				break;

			case "verify":
				verify(connection);
				break;
			}
		}
	}

	static void druid(String url, String cmd) throws SQLException, InterruptedException {
		try (var dataSource = new DruidDataSource()) {
			dataSource.setUrl(url);
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
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
		}
	}

	static void verify(DruidDataSource dataSource) throws SQLException {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(true);
			var sql = "select id from halt_test";
			try (var cmd = connection.prepareStatement(sql)) {
				try (var rs = cmd.executeQuery()) {
					System.out.println("select");
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

			for (int i = 0; i < 100; ++i) {
				var sql = "insert into halt_test values(" + i + ")";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
				}
			}
			Thread.sleep(1000); // 基本确保mysql收到请求
			// 没有commit，也没有rollback，halt。
			System.out.println("halt");
			Runtime.getRuntime().halt(0);
		}
	}

	static void prepare(DruidDataSource dataSource) throws SQLException {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);

			String create = "CREATE TABLE IF NOT EXISTS halt_test (id integer)";
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

	static void verify(Connection connection) throws SQLException {
		connection.setAutoCommit(true);
		var sql = "select id from halt_test";
		try (var cmd = connection.prepareStatement(sql)) {
			try (var rs = cmd.executeQuery()) {
				System.out.println("select");
				while (rs.next()) {
					var id = rs.getInt(1);
					System.out.println(id);
				}
			}
		}
	}

	static void halt(Connection connection) throws SQLException, InterruptedException {
		connection.setAutoCommit(false);

		for (int i = 0; i < 100; ++i) {
			var sql = "insert into halt_test values(" + i + ")";
			try (var cmd = connection.prepareStatement(sql)) {
				cmd.executeUpdate();
			}
		}
		Thread.sleep(1000); // 基本确保mysql收到请求
		// 没有commit，也没有rollback，halt。
		System.out.println("halt");
		Runtime.getRuntime().halt(0);
	}

	static void prepare(Connection connection) throws SQLException {
		connection.setAutoCommit(false);

		String create = "CREATE TABLE IF NOT EXISTS halt_test (id integer)";
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
