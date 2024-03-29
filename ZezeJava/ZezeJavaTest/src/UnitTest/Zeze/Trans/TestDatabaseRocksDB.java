package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseRocksDb;
import junit.framework.TestCase;
import org.junit.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestDatabaseRocksDB extends TestCase {

	public final void test1() throws Exception {
		var hostName = InetAddress.getLocalHost().getHostName();
		if (hostName.equals("DESKTOP-48A4UQ1")) // 这台电脑CPU不支持这个测试会触发rocksdbjni使用的BMI2指令集中的BZHI指令
			return;

		DatabaseRocksDb db = getDatabaseRocksDb();
		try {
			var table = (Database.AbstractKVTable)db.openTable("test_1", Bean.hash32("test_1"));
			{
				try (var trans = db.beginTransaction()) {
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
			Assert.assertEquals(0, table.walk(TestDatabaseRocksDB::PrintRecord));
			{
				try (var trans = db.beginTransaction()) {
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
				Assert.assertNotNull(value);
				Assert.assertEquals(1, value.ReadInt());
				Assert.assertEquals(value.ReadIndex, value.WriteIndex);
			}
			{
				ByteBuffer key = ByteBuffer.Allocate();
				key.WriteInt(2);
				ByteBuffer value = table.find(key);
				Assert.assertNotNull(value);
				Assert.assertEquals(2, value.ReadInt());
				Assert.assertEquals(value.ReadIndex, value.WriteIndex);
			}
			Assert.assertEquals(2, table.walk(TestDatabaseRocksDB::PrintRecord));
		} finally {
			db.close();
		}
	}

	/**
	 * 执行test1插入数据后 ,再次启动db查看数据是否依然存在
	 */
	public final void test2() throws UnknownHostException {
		var hostName = InetAddress.getLocalHost().getHostName();
		if (hostName.equals("DESKTOP-48A4UQ1")) // 这台电脑CPU不支持这个测试会触发rocksdbjni使用的BMI2指令集中的BZHI指令
			return;

		DatabaseRocksDb db = getDatabaseRocksDb();
		try {
			var table = (Database.AbstractKVTable)db.openTable("test_1", Bean.hash32("test_1"));
			Assert.assertEquals(2, table.walk(TestDatabaseRocksDB::PrintRecord));
		} finally {
			db.close();
		}
	}

	private static DatabaseRocksDb getDatabaseRocksDb() {
		String dbHome = "dbhome";
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MySql);
		databaseConf.setDatabaseUrl(dbHome);
		databaseConf.setName("RocksDB");
		databaseConf.setDruidConf(new Config.DruidConf());

		return new DatabaseRocksDb(null, databaseConf);
	}

	public static boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
