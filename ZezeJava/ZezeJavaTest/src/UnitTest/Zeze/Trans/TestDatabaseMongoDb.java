package UnitTest.Zeze.Trans;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMongoDb;
import Zeze.Transaction.DatabasePostgreSQL;
import junit.framework.TestCase;
import org.junit.Assert;

@SuppressWarnings("CallToPrintStackTrace")
public class TestDatabaseMongoDb extends TestCase {
	private static String getPersonalUrl() throws UnknownHostException {
		var hostName = InetAddress.getLocalHost().getHostName();
		System.out.println("hostName=" + hostName);
		switch (hostName) {
		case "doudouwang": // lichenghua's computer 2
			return "mongodb://127.0.0.1:27017/?replicaSet=rs0";
		default:
			return null; // 默认不测试postgresql。
		}
	}

	public final void test1() throws Exception {
		String url = getPersonalUrl();
		if (url == null) {
			System.out.println("skip postgres test: not found url");
			return;
		}
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MongoDb);
		databaseConf.setDatabaseUrl(url);
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
		Assert.assertEquals(0, table.walk(TestDatabaseMongoDb::PrintRecord));
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
		Assert.assertEquals(2, table.walk(TestDatabaseMongoDb::PrintRecord));
		System.out.println(table.getSizeApproximation());
	}

	public static boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
