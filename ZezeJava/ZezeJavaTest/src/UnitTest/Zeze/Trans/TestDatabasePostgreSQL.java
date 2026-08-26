package UnitTest.Zeze.Trans;

import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabasePostgreSQL;
import org.junit.jupiter.api.Assertions;

@SuppressWarnings("CallToPrintStackTrace")
public class TestDatabasePostgreSQL {
	public static boolean checkDriverClassExist(String driverClassName) {
		try {
			Class.forName(driverClassName);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean checkTcpPort(String host, int port) {
		try {
			new Socket(host, port).close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String getPersonalUrl() throws UnknownHostException {
		var hostName = InetAddress.getLocalHost().getHostName();
		System.out.println("hostName=" + hostName);
		if (hostName.equals("doudouwang")) { // lichenghua's computer 2
			return "jdbc:postgresql://localhost:5432/devtest?user=dev&password=devtest12345&useSSL=false";
		}
		return null; // 默认不测试postgresql。
	}

	@Test
	public final void test1() throws Exception {
		if (!checkDriverClassExist("org.postgresql.Driver")) {
			System.out.println("skip postgres test: not found postgres driver");
			return;
		}
		String url = getPersonalUrl();
		if (url == null) {
			System.out.println("skip postgres test: not found url");
			return;
		}
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.PostgreSQL);
		databaseConf.setDatabaseUrl(url);
		databaseConf.setName("postgres");
		databaseConf.setDruidConf(new Config.DruidConf());

		var sqlserver = new DatabasePostgreSQL(null, databaseConf);
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
		Assertions.assertEquals(0, table.walk(TestDatabasePostgreSQL::PrintRecord));
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
		Assertions.assertEquals(2, table.walk(TestDatabasePostgreSQL::PrintRecord));
		System.out.println(table.getSizeApproximation());
	}

	public static boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
