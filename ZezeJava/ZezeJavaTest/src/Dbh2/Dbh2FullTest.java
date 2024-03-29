package Dbh2;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Dbh2Manager;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.Task;
import org.junit.Assert;
import org.junit.Test;
import Zeze.Transaction.Database.AbstractKVTable;

// 测试整体结构(Dbh2Manager,Master,Agent)
public class Dbh2FullTest {
	private static Database newDatabase(Dbh2AgentManager dbh2AgentManager, @SuppressWarnings("SameParameterValue") String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:11000/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, dbh2AgentManager, databaseConf);
	}

	private static Future<?> startBench(int keyStart, int keyEnd, Database database, AbstractKVTable table, ByteBuffer value) {
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
		System.setProperty("Dbh2MasterDefaultBucketPortId", "18000");
		Task.tryInitThreadPool();
		Zeze.Net.Selectors.getInstance().add(7);

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();

		var value = ByteBuffer.Wrap(new byte[]{1, 2, 3, 4});
		Database database = null;
		Application.renameAndDeleteDirectory(new File("CommitRocks"));
		var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null, 100);
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze" + i + ".xml"));
			for (var manager : managers)
				manager.start();
			dbh2AgentManager.start();

			database = newDatabase(dbh2AgentManager, "dbh2TestDb");
			var tables = new ArrayList<AbstractKVTable>();
			for (int i = 0; i < 4; ++i) {
				var tableName = "table" + i;
				tables.add((Database.AbstractKVTable)database.openTable(tableName, Bean.hash32(tableName)));
			}
			for (var table : tables)
				table.waitReady();

			var count = 2000;
			var threads = 2;
			var futures = new ArrayList<Future<?>>();
			var b = new Zeze.Util.Benchmark();
			for (var t = 0; t < threads; ++t) {
				var keyStart = t * count;
				var keyEnd = keyStart + count;
				futures.add(startBench(keyStart, keyEnd, database, tables.get(t % tables.size()), value));
			}
			Thread.sleep(1000); // 等待agent都连上，然后dump出来。此时任务在并发执行。
			dbh2AgentManager.dumpAgents();
			for (var future : futures)
				future.get();
			b.report("Bench Dbh2 Full Transaction", count * threads);
		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			if (database != null)
				database.close();
			dbh2AgentManager.stop();
		}
	}

	@Test
	public void testCommitServerQueryVerify() throws Exception {
		System.setProperty("Dbh2MasterDefaultBucketPortId", "18000");

		Task.tryInitThreadPool();

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();

		Database database = null;
		Application.renameAndDeleteDirectory(new File("CommitRocks"));
		var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null, 100);
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze" + i + ".xml"));
			for (var manager : managers)
				manager.start();
			dbh2AgentManager.start();

			database = newDatabase(dbh2AgentManager, "dbh2TestDb");
			var table1 = (Database.AbstractKVTable)database.openTable("table1", Bean.hash32("table1"));
			table1.waitReady();

			var key = ByteBuffer.Wrap(ByteBuffer.Empty);
			var key1 = ByteBuffer.Wrap(new byte[]{1});
			var value = ByteBuffer.Wrap(new byte[]{1, 2, 3, 4});
			try (var _trans = database.beginTransaction()) {
				var trans = (Database.Dbh2Transaction)_trans;
				table1.replace(trans, key, value);
				table1.replace(trans, key1, value);
				trans.commitBreakAfterPrepareForDebugOnly();
			}
			Thread.sleep(13000);
		} finally {
			master.stop();
			for (var manager : managers)
				manager.stop();
			if (null != database)
				database.close();
			dbh2AgentManager.stop();
		}
	}

	@Test
	public void testFull() throws Exception {
		System.setProperty("Dbh2MasterDefaultBucketPortId", "18000");

		Task.tryInitThreadPool();

		var master = new Zeze.Dbh2.Master.Main("zeze.xml");
		var managers = new ArrayList<Dbh2Manager>();
		var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();
		Database database = null;
		Application.renameAndDeleteDirectory(new File("CommitRocks"));
		var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null, 100);
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze" + i + ".xml"));
			for (var manager : managers)
				manager.start();
			dbh2AgentManager.start();

			database = newDatabase(dbh2AgentManager, "dbh2TestDb");
			var table1 = (Database.AbstractKVTable)database.openTable("table1", Bean.hash32("table1"));
			var table2 = (Database.AbstractKVTable)database.openTable("table2", Bean.hash32("table2"));
			table1.waitReady();
			table2.waitReady();

			var key = ByteBuffer.Wrap(ByteBuffer.Empty);
			var key1 = ByteBuffer.Wrap(new byte[]{1});
			var value = ByteBuffer.Wrap(new byte[]{1, 2, 3, 4});

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
			dbh2AgentManager.stop();
		}
	}
}
