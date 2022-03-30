package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMySql;
import junit.framework.TestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class TestDatabaseMySql extends TestCase{

	public static boolean checkDriverClassExist(String driverClassName) {
		try {
			Class.forName(driverClassName);
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

	private String getPersonalUrl() throws UnknownHostException {
		var hostName = InetAddress.getLocalHost().getHostName();
		System.out.println("hostName=" + hostName);
		switch (hostName) {
			case "DESKTOP-DVFC8AI": // lichenghua's computer
				return "jdbc:mysql://localhost/devtest?user=dev&password=devtest12345&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

			default:
				return "jdbc:mysql://localhost:3306/mysql?user=root&password=123&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
		}
	}

	public final void test1() throws UnknownHostException {
		if (!checkDriverClassExist("com.mysql.cj.jdbc.Driver")) {
			return;
		}
		String url = getPersonalUrl();
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MySql);
		databaseConf.setDatabaseUrl(url);
		databaseConf.setName("mysql");
		databaseConf.setDbcpConf(new Config.DbcpConf());

		DatabaseMySql sqlserver = new DatabaseMySql(databaseConf);
		Database.Table table = sqlserver.OpenTable("test_1"); {
			var trans = sqlserver.BeginTransaction(); {
				ByteBuffer key = ByteBuffer.Allocate();
				key.WriteInt(1);
				table.Remove(trans, key);
			} {
				ByteBuffer key = ByteBuffer.Allocate();
				key.WriteInt(2);
				table.Remove(trans, key);
			}
			trans.Commit();
		}
		assert 0 == table.Walk(this::PrintRecord); {
			var trans = sqlserver.BeginTransaction(); {
				ByteBuffer key = ByteBuffer.Allocate();
				key.WriteInt(1);
				ByteBuffer value = ByteBuffer.Allocate();
				value.WriteInt(1);
				table.Replace(trans, key, value);
			} {
				ByteBuffer key = ByteBuffer.Allocate();
				key.WriteInt(2);
				ByteBuffer value = ByteBuffer.Allocate();
				value.WriteInt(2);
				table.Replace(trans, key, value);
			}
			trans.Commit();
		} {
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(1);
			ByteBuffer value = table.Find(key);
			assert value != null;
			assert 1 == value.ReadInt();
			assert value.ReadIndex == value.WriteIndex;
		} {
			ByteBuffer key = ByteBuffer.Allocate();
			key.WriteInt(2);
			ByteBuffer value = table.Find(key);
			assert value != null;
			assert 2 == value.ReadInt();
			assert value.ReadIndex == value.WriteIndex;
		}
		assert 2 == table.Walk(this::PrintRecord);
	}

	public final boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(Zeze.Util.Str.format("key={} value={}", ikey, ivalue));
		return true;
	}
}
