package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMySql;
import junit.framework.TestCase;
import org.junit.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestDatabaseMySql extends TestCase {

	public static boolean checkDriverClassExist(String driverClassName) {
		try {
			Class.forName(driverClassName);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static String getPersonalUrl() throws UnknownHostException {
		return null;
		/*
		var hostName = InetAddress.getLocalHost().getHostName();
		System.out.println("hostName=" + hostName);
		switch (hostName) {
		case "DESKTOP-DVFC8AI": // lichenghua's computer 1 & 2
		case "DESKTOP-L17H5QD": // lichenghua's computer 2
			return "jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
		case "DESKTOP-VVU42V2":
			return null;
		default:
			return "jdbc:mysql://localhost:3306/mysql?user=root&password=123&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
		}
		*/
	}

	public final void test1() throws Exception {
		if (!checkDriverClassExist("com.mysql.cj.jdbc.Driver")) {
			return;
		}
		String url = getPersonalUrl();
		if (url == null)
			return;
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MySql);
		databaseConf.setDatabaseUrl(url);
		databaseConf.setName("mysql");
		databaseConf.setDruidConf(new Config.DruidConf());

		DatabaseMySql sqlserver = new DatabaseMySql(databaseConf);
		Database.Table table = sqlserver.openTable("test_1");
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
		Assert.assertEquals(0, table.walk(TestDatabaseMySql::PrintRecord));
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
		Assert.assertEquals(2, table.walk(TestDatabaseMySql::PrintRecord));
	}

	public static boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
