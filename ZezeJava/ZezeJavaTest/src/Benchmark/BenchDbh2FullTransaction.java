package Benchmark;

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
import Zeze.Transaction.Database.AbstractKVTable;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Bench;
import org.junit.jupiter.api.Test;

// Dbh2 全事务吞吐基准（master + 3个Dbh2Manager 多bucket拓扑，与 Dbh2.Dbh2FullTest.testFull 的环境一致）。
// 从 Dbh2FullTest 拆出：integrationTest 只保留功能路径（testFull），负载形态整体归 bench 车道。
@SuppressWarnings("NewClassNamingConvention")
@Bench
public class BenchDbh2FullTransaction {
	private static Database newDatabase(Dbh2AgentManager dbh2AgentManager, @SuppressWarnings("SameParameterValue") String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:11000/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, dbh2AgentManager, databaseConf);
	}

	private static Future<?> startBench(int keyStart, int keyEnd, Database database, AbstractKVTable table, ByteBuffer value) {
		return TaskSpec.ofAction(() -> {
			for (int i = keyStart; i < keyEnd; ++i) {
				try (var trans = database.beginTransaction()) {
					var key = ByteBuffer.Allocate();
					key.WriteInt(i);
					table.replace(trans, key, value);
					trans.commit();
				}
			}
		}).name("").submitNow();
	}

	@Test
	public void testBenchmark() throws Exception {
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

			var count = 3000;
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
}
