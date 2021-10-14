package UnitTest.Zeze.Trans;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestDatabaseMySql
public class TestDatabaseMySql {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test1()
	public final void Test1() {
		String url = "server=localhost;database=devtest;uid=dev;pwd=devtest12345";
		DatabaseMySql sqlserver = new DatabaseMySql(url);
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
		assert 0 == table.Walk(::PrintRecord); {
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
		assert 2 == table.Walk(::PrintRecord);
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public bool PrintRecord(byte[] key, byte[] value)
	public final boolean PrintRecord(byte[] key, byte[] value) {
		int ikey = ByteBuffer.Wrap(key).ReadInt();
		int ivalue = ByteBuffer.Wrap(value).ReadInt();
		System.out.println(String.format("key=%1$s value=%2$s", ikey, ivalue));
		return true;
	}
}
