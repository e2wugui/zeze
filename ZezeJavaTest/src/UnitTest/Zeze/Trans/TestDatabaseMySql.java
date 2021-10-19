package UnitTest.Zeze.Trans;

import Zeze.Config.DatabaseConf;
import Zeze.Config.DbType;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMySql;
import junit.framework.TestCase;

public class TestDatabaseMySql extends TestCase{
	
	public final void test1() {
		String url = "server=localhost;database=devtest;uid=dev;pwd=devtest12345";
		DatabaseConf databaseConf = new DatabaseConf();
		databaseConf.setDatabaseType(DbType.MySql);
		databaseConf.setDatabaseUrl(url);
		databaseConf.setName("mysql");
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
		System.out.println(String.format("key=%1$s value=%2$s", ikey, ivalue));
		return true;
	}
}
