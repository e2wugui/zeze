package Zeze.Tikv;

import Zeze.*;

public class Test {
	public static void RunScan(String url) {
		System.out.println("RunScan");

		// for keyprefix
		var tikvDb = new DatabaseTikv(url);
		Zeze.Gen.Table tempVar = tikvDb.OpenTable("_testtable_");
		var table = tempVar instanceof DatabaseTikv.TableTikv ? (DatabaseTikv.TableTikv)tempVar : null;

		// prepare data
		var key = Zeze.Serialize.ByteBuffer.Allocate(64);
		key.WriteString("key");
		var value = Zeze.Serialize.ByteBuffer.Allocate(64);
		value.WriteString("value");
		var trans = tikvDb.BeginTransaction();
		tikvDb.Flush(trans);
		trans.Commit();
		var outvalue = table.Find(key);
		System.out.println("Scan Find1 " + outvalue);

		// connect an begin transaction
		table.Walk((key, value) -> {
					System.out.println(String.format("Scan Callback: %1$s=>%2$s", BitConverter.toString(key), BitConverter.toString(value)));
					return true;
		});
	}

	public static void RunWrap(String url) {
		System.out.println("RunWrap");

		var tikvDb = new DatabaseTikv(url);
		var table = tikvDb.OpenTable("_testtable_");
		var key = Zeze.Serialize.ByteBuffer.Allocate(64);
		key.WriteString("key");
		var value = Zeze.Serialize.ByteBuffer.Allocate(64);
		//value.WriteString("value");

		var outvalue = table.Find(key);
		System.out.println("Find1 " + outvalue);
		var trans = tikvDb.BeginTransaction();
		tikvDb.Flush(trans);
		trans.Commit();

		outvalue = table.Find(key);
		System.out.println("Find2 " + outvalue);
		trans = tikvDb.BeginTransaction();
		tikvDb.Flush(trans);
		trans.Commit();

		outvalue = table.Find(key);
		System.out.println("Find3 " + outvalue);
	}

	public static void RunBasic(String url) {
		System.out.println("RunBasic");

		var clientId = Tikv.Driver.NewClient(url);
		try {
			var txnId = Tikv.Driver.Begin(clientId);
			try {
				var key = Zeze.Serialize.ByteBuffer.Allocate(64);
				key.WriteString("key");
				var outvalue = Tikv.Driver.Get(txnId, key);
				System.out.println("1 " + outvalue);
				var value = Zeze.Serialize.ByteBuffer.Allocate(64);
				value.WriteString("value");
				Tikv.Driver.Put(txnId, key, value);
				outvalue = Tikv.Driver.Get(txnId, key);
				System.out.println("2 " + outvalue);
				Tikv.Driver.Delete(txnId, key);
				outvalue = Tikv.Driver.Get(txnId, key);
				System.out.println("3 " + outvalue);
				Tikv.Driver.Commit(txnId);
			}
			catch (RuntimeException e) {
				Tikv.Driver.Rollback(txnId);
			}
		}
		finally {
			Tikv.Driver.CloseClient(clientId);
		}
	}

	public static void Run(String url) {
		var ptr = Marshal.AllocHGlobal(0);
		if (System.IntPtr.opEquals(IntPtr.Zero, ptr)) {
			System.out.println("++++++++++++");
		}
		Marshal.FreeHGlobal(ptr);
		RunBasic(url);
		RunWrap(url);
		RunScan(url);
	}
}