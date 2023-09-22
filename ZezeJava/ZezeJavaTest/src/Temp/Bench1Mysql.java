package Temp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.concurrent.Future;
import com.alibaba.druid.pool.DruidDataSource;

/*
use mysql;
create user 'dev'@'localhost' identified by 'devtest12345';
grant all privileges on devtest.* to 'dev'@'localhost';
create table if not exists bench1(a bigint, b int);
*/
public class Bench1Mysql {
	public static void main(String[] args) throws Exception {
		var url = "jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

		try (var dataSource = new DruidDataSource()) {
			dataSource.setUrl(url);
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
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
		}
	}

	public static void mainTestPolarDb1(String[] args) throws Exception {
		try (var dataSource = new DruidDataSource()) {
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
			dataSource.setUrl("jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				var dropSql = "drop procedure if exists TestProc1;";
				try (var cmd = connection.prepareStatement(dropSql)) {
					cmd.executeUpdate();
				}

				var proc = "create procedure TestProc1(in in_data binary, out out_data binary, out out_hex varchar)\n" +
						"    begin\n" +
						"        set out_data = in_data;\n" +
						"        set out_hex = hex(in_data);\n" +
						"    end;";
				try (var cmd = connection.prepareStatement(proc)) {
					cmd.executeUpdate();
				}

				try (var cmd = connection.prepareCall("call TestProc1(?, ?, ?)")) {
					cmd.setBytes(1, new byte[]{(byte)0xa4}); // data
					cmd.registerOutParameter(2, Types.BINARY);
					cmd.registerOutParameter(3, Types.VARCHAR);
					cmd.executeUpdate();
					var data = cmd.getBytes(2);
					//var data = blob.getBytes(1, (int)blob.length());
					System.out.print("out_data =");
					for (byte b : data)
						System.out.format(" %02X", b & 0xff);
					System.out.println();
					System.out.println("out_hex = " + cmd.getString(3));
				}
			}
		}
		System.out.println("OK!");
	}

	public static void mainTestPolarDb2(String[] args) throws Exception {
		try (var dataSource = new DruidDataSource()) {
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
			dataSource.setUrl("jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				try (var cmd = connection.prepareStatement("drop table if exists TestTable;")) {
					cmd.executeUpdate();
				}

				var createSql = "create table if not exists TestTable(id varbinary(767) not null primary key, data blob not null)";
				try (var cmd = connection.prepareStatement(createSql)) {
					cmd.executeUpdate();
				}

//				try (var cmd = connection.prepareStatement("select hex(data) from TestTable where id='key1'")) {
//					// cmd.setBytes(1, "key1".getBytes(StandardCharsets.UTF_8)); // key
//					try (ResultSet rs = cmd.executeQuery()) {
//						if (rs.next()) {
//							byte[] value = rs.getBytes(1);
//							System.out.println("size = " + value.length);
//							for (byte b : value)
//								System.out.format("%02X %c\n", b & 0xff, (char)b);
//						}
//					}
//				}

				var dropSql = "drop procedure if exists TestProc;";
				try (var cmd = connection.prepareStatement(dropSql)) {
					cmd.executeUpdate();
				}

				var createProc = "create procedure TestProc(in in_data blob)\n" +
						"    begin\n" +
						"        replace into TestTable values('key3', in_data);\n" +
						"    end;";

				try (var cmd = connection.prepareStatement(createProc)) {
					cmd.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}

				var bs = new byte[256];
				for (int i = 0; i < 256; i++)
					bs[i] = (byte)i;

				try (var cmd = connection.prepareCall("call TestProc(?)")) {
					cmd.setBytes(1, bs); // data
					cmd.executeUpdate();
				}

				try (var cmd = connection.prepareStatement("replace into TestTable values('key4', ?)")) {
					cmd.setBytes(1, bs); // data
					cmd.executeUpdate();
				}

				try (var cmd = connection.prepareStatement("select id, data from TestTable")) {
					try (ResultSet rs = cmd.executeQuery()) {
						while (rs.next()) {
							var id = rs.getString(1);
							var data = rs.getBytes(2);
							System.out.print("id=" + id + " data.size=" + data.length);
							for (byte b : data)
								System.out.format(" %02X", b & 0xff);
							System.out.println();
						}
					}
				}
			}
		}
		System.out.println("OK!");
	}
}
