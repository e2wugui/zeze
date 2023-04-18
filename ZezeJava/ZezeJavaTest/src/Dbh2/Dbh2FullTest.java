package Dbh2;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Dbh2Manager;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import org.junit.Assert;
import org.junit.Test;
import Zeze.Transaction.Database.AbstractKVTable;
import org.rocksdb.RocksDBException;

// 测试整体结构(Dbh2Manager,Master,Agent)
public class Dbh2FullTest {

	private Database newDatabase(String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:30000/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, databaseConf);
	}

	private Future<?> startBench(int keyStart, int keyEnd, Database database, AbstractKVTable table, ByteBuffer value) {
		return Task.runUnsafe(() -> {
			for (int i = keyStart; i < keyEnd; ++i) {
				try (var trans = database.beginTransaction()) {
					var key = ByteBuffer.Allocate();
					key.WriteInt(i);
					table.replace(trans, key, value);
					trans.commit();
				}
			}
		}, "");
	}

	@Test
	public void testBench() throws Exception {
		Task.tryInitThreadPool(null, null, null);
		Zeze.Net.Selectors.getInstance().add(7);

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		master.start();
		var managers = new ArrayList<Dbh2Manager>();
		for (int i = 0; i < 3; ++i)
			managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze.xml"));
		for (var manager : managers)
			manager.start();

		Thread.sleep(2000); // leader 重启apply可能时间较长，给它5秒。

		var database = newDatabase("dbh2TestDb");
		var tables = new ArrayList<AbstractKVTable>();
		for (int i = 0; i < 4; ++i)
			tables.add((Database.AbstractKVTable)database.openTable("table" + i));

		var value = ByteBuffer.Wrap(new byte[] { 1, 2, 3, 4 });

		Dbh2AgentManager.getInstance().start(Config.load());
		try {
			Thread.sleep(3000); // leader 重启apply可能时间较长，给它5秒。

			var count = 1_0000;
			var threads = 8;
			var futures = new ArrayList<Future<?>>();
			var b = new Zeze.Util.Benchmark();
			for (var t = 0; t < threads; ++t) {
				var keyStart = t * count;
				var keyEnd = keyStart + count;
				futures.add(startBench(keyStart, keyEnd, database, tables.get(t % tables.size()), value));
			}
			Thread.sleep(2000);
			Dbh2AgentManager.getInstance().dumpAgents();
			for (var future : futures)
				future.get();
			b.report("Bench Dbh2 Full Transaction", count * threads);
		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			database.close();
			Dbh2AgentManager.getInstance().stop();
		}
	}

	@Test
	public void testCommitServerQueryVerify() throws Exception {
		Task.tryInitThreadPool(null, null, null);

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		Database database = null;
		Dbh2AgentManager.getInstance().start(Config.load());
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze.xml"));
			for (var manager : managers)
				manager.start();

			Thread.sleep(2000); // leader 重启apply可能时间较长，给它5秒。
			database = newDatabase("dbh2TestDb");
			var table1 = (Database.AbstractKVTable)database.openTable("table1");
			Thread.sleep(2000); // leader 重启apply可能时间较长，给它5秒。
			var key = ByteBuffer.Wrap(new byte[] {});
			var key1 = ByteBuffer.Wrap(new byte[] { 1 });
			var value = ByteBuffer.Wrap(new byte[] { 1, 2, 3, 4 });
			try (var _trans = database.beginTransaction()) {
				var trans = (Database.Dbh2Transaction)_trans;
				table1.replace(trans, key, value);
				table1.replace(trans, key1, value);
				trans.commitBreakAfterPrepareForDebugOnly();
			}
			Thread.sleep(8000);

		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			if (null != database)
				database.close();
			Dbh2AgentManager.getInstance().stop();
		}
	}

	@Test
	public void testFull() throws Exception {
		Task.tryInitThreadPool(null, null, null);

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		Database database = null;
		Dbh2AgentManager.getInstance().start(Config.load());
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze.xml"));
			for (var manager : managers)
				manager.start();

			Thread.sleep(2000); // leader 重启apply可能时间较长，给它5秒。
			database = newDatabase("dbh2TestDb");
			var table1 = (Database.AbstractKVTable)database.openTable("table1");
			var table2 = (Database.AbstractKVTable)database.openTable("table2");

			var key = ByteBuffer.Wrap(new byte[] {});
			var key1 = ByteBuffer.Wrap(new byte[] { 1 });
			var value = ByteBuffer.Wrap(new byte[] { 1, 2, 3, 4 });

			Thread.sleep(3000); // leader 重启apply可能时间较长，给它5秒。
			try (var trans = database.beginTransaction()) {
				table1.replace(trans, key, value);
				table1.replace(trans, key1, value);
				table2.replace(trans, key, value);
				table2.replace(trans, key1, value);
				trans.commit();
			}
			{
				var valueFindKey = table1.find(key);
				Assert.assertNotNull(valueFindKey);
				Assert.assertEquals(valueFindKey, value);

				var valueFindKey1 = table1.find(key1);
				Assert.assertNotNull(valueFindKey1);
				Assert.assertEquals(valueFindKey1, value);
			}
			{
				var valueFindKey = table2.find(key);
				Assert.assertNotNull(valueFindKey);
				Assert.assertEquals(valueFindKey, value);

				var valueFindKey1 = table2.find(key1);
				Assert.assertNotNull(valueFindKey1);
				Assert.assertEquals(valueFindKey1, value);
			}

		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			if (null != database)
				database.close();
			Dbh2AgentManager.getInstance().stop();
		}
	}
}
