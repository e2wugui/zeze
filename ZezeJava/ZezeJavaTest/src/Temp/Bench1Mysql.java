package Temp;

import com.alibaba.druid.pool.DruidDataSource;

public class Bench1Mysql {
	public static void main(String [] args) throws Exception {
		var url = "jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

		var dataSource = new DruidDataSource();
		try {
			dataSource.setUrl(url);
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

			final var count = 1_0000;
			var b = new Zeze.Util.Benchmark();
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				var sql = "replace into bench1 values(?, ?)";
				for (var key = 0; key < count; ++key) {
					try (var pre = conn.prepareStatement(sql)) {
						pre.setLong(1, key);
						pre.setInt(2, key);
						pre.executeUpdate();
					}
				}
			}
			b.report("mysql replace bench", count);
		} finally {
			dataSource.close();
		}
	}
}
