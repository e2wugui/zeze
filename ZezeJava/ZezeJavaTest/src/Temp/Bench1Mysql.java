package Temp;

import java.util.ArrayList;
import java.util.concurrent.Future;
import com.alibaba.druid.pool.DruidDataSource;

public class Bench1Mysql {
	public static void main(String [] args) throws Exception {
		var url = "jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

		var dataSource = new DruidDataSource();
		dataSource.setUrl(url);
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

		try {
			Zeze.Util.Task.tryInitThreadPool(null, null, null);
			var futures = new ArrayList<Future<?>>();
			var bTotal = new Zeze.Util.Benchmark();
			final var count = 1_0000;
			final var threads = 3;
			for (int i = 0; i < threads; ++i) {
				var startKey = i * count;
				var endKey = startKey + count;
				futures.add(Zeze.Util.Task.runUnsafe(() -> {
					var b = new Zeze.Util.Benchmark();
					try (var conn = dataSource.getConnection()) {
						conn.setAutoCommit(true);
						var sql = "replace into bench1 values(?, ?)";
						for (var key = startKey; key < endKey; ++key) {
							try (var pre = conn.prepareStatement(sql)) {
								pre.setLong(1, key);
								pre.setInt(2, key);
								pre.executeUpdate();
							}
						}
					}
					b.report("mysql replace bench", count);
				}, "mysql replace"));
			}
			for (var future : futures)
				future.get();
			bTotal.report("Total mysql replace bench", count * threads);
		} finally {
			dataSource.close();
		}
	}
}
