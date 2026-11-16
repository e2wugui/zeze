package Zeze.Transaction;

import org.jspecify.annotations.NullMarked;

import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMongoDb;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("CallToPrintStackTrace")
@NullMarked
public class TestDatabaseMongoDb {
	// 固定本机url；服务不在由serverReachable守卫跳过（原个人电脑主机名门已随之退役）
	private static final String DB_URL = "mongodb://127.0.0.1:27017/?replicaSet=rs0";

	// 守卫：mongo 服务不在（如 WSL 未启动）时 assumeTrue 跳过（报 SKIPPED），
	// 不再弹 30s MongoTimeoutException 环境红（soak-20261116-1329 R1 实锤，mongo/pg 未启动）。
	// 兄弟类形态：Tikv/RocketMQ 静态 @Disabled、TestPostgreSqlResolvedRelation 系统属性门；
	// 本类在个人机器上应保持服务在时自动真跑，故选可用性探针。
	private static boolean serverReachable(String url) {
		try {
			var uri = new URI(url);
			var host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
			var port = uri.getPort() != -1 ? uri.getPort() : 27017;
			try (var socket = new Socket()) {
				socket.connect(new InetSocketAddress(host, port), 500);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	public final void test1() throws Exception {
		assumeTrue(serverReachable(DB_URL), "mongodb server not running: " + DB_URL);
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MongoDb);
		databaseConf.setDatabaseUrl(DB_URL);
		databaseConf.setName("mongodb");
		//databaseConf.setDruidConf(new Config.DruidConf());

		var sqlserver = new DatabaseMongoDb(null, databaseConf);
		Database.AbstractKVTable table;
		{
			Database.Table tableTmp = sqlserver.openTable("test_1", Bean.hash32("test_1"));
			if (tableTmp instanceof Database.AbstractKVTable)
				table = (Database.AbstractKVTable)tableTmp;
			else
				return;
		}
		{
			try (var trans = sqlserver.beginTransaction()) {
				{
					ByteBuffer key = ByteBuffer.Allocate();
					key.WriteInt(1);
					table.remove(trans, key);
				}
				{
					ByteBuffer key = ByteBuffer.Allocate();
					key.WriteInt(2);
					table.remove(trans, key);
				}
				trans.commit();
			}
		}
		Assertions.assertEquals(0, table.walk(TestDatabaseMongoDb::PrintRecord));
		{
			try (var trans = sqlserver.beginTransaction()) {
				{
					ByteBuffer key = ByteBuffer.Allocate();
					key.WriteInt(1);
					ByteBuffer value = ByteBuffer.Allocate();
					value.WriteInt(1);
					table.replace(trans, key, value);
				}
				{
					ByteBuffer key = ByteBuffer.Allocate();
					key.WriteInt(2);
					ByteBuffer value = ByteBuffer.Allocate();
					value.WriteInt(2);
					table.replace(trans, key, value);
				}
				trans.commit();
			}
		}
		{
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(1);
			ByteBuffer value = table.find(key);
			Assertions.assertNotNull(value);
			Assertions.assertEquals(1, value.ReadInt());
			Assertions.assertEquals(value.ReadIndex, value.WriteIndex);
		}
		{
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(2);
			ByteBuffer value = table.find(key);
			Assertions.assertNotNull(value);
			Assertions.assertEquals(2, value.ReadInt());
			Assertions.assertEquals(value.ReadIndex, value.WriteIndex);
		}
		Assertions.assertEquals(2, table.walk(TestDatabaseMongoDb::PrintRecord));
		System.out.println(table.getSizeApproximation());
	}

	public static boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
