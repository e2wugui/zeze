package Dbh2;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GA-C02回归：serialize模式（默认）下Dbh2Transaction构造器逐key加锁，
 * 中途键冲突抛出时必须释放已获取的锁（close回滚后rethrow）。
 * bug链：未回滚时第一个key的semaphore永久扣减，触及该键的后续事务在prepare阶段
 * 持续"lock timeout"直到GC清理WeakHashSet，窗口不可预测。
 * 测试：T1持{k1,k2}；T2={j1,k2}在k2上冲突失败；T3={j1}必须还能prepare成功
 * （bug时j1已随T2泄漏，T3的构造器在j1上冲突）。
 */
public class TestFnd19GA02PrepareConflictRollbackLocks {
	private static final TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	private static ArrayList<Zeze.Dbh2.Dbh2> startBucket(RocksDatabase database, String raftConfigString, Path tempDir) {
		var nodes = new ArrayList<Zeze.Dbh2.Dbh2>();
		for (var config : RaftConfig.loadFromString(raftConfigString).getNodes().values()) {
			var nodeConfig = raftConfigString.replaceFirst("<raft ",
					"<raft DbHome=\"" + tempDir.resolve(config.getName().replace(':', '_')) + "\" ");
			nodes.add(new Zeze.Dbh2.Dbh2(null, config.getName(), database,
					RaftConfig.loadFromString(nodeConfig), null, false, taskOneByOne));
		}
		return nodes;
	}

	private static void stopBucket(ArrayList<Zeze.Dbh2.Dbh2> nodes, Dbh2Agent agent, RocksDatabase database)
			throws Exception {
		for (var dbh2 : nodes) {
			dbh2.close();
			LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
		}
		agent.close();
		database.close();
	}

	private static Binary get(Dbh2Agent agent, Binary key) {
		var kv = agent.get("database", "table1", key);
		Assertions.assertTrue(kv.getKey());
		return kv.getValue() == null ? null : new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size());
	}

	@Test
	public void testConflictPrepareReleasesAcquiredLocks(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var database = new RocksDatabase(tempDir.resolve("fnd19ga02").toString());
		var raftConfig = """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19140"/>
					<node Host="127.0.0.1" Port="19141"/>
					<node Host="127.0.0.1" Port="19142"/>
				</raft>
				""";
		var nodes = startBucket(database, raftConfig, tempDir);
		var agent = new Dbh2Agent(raftConfig);
		try {
			var meta = new BBucketMeta.Data();
			meta.setDatabaseName("database");
			meta.setTableName("table1");
			meta.setRaftConfig("");
			meta.setKeyFirst(Binary.Empty);
			meta.setKeyLast(Binary.Empty);
			agent.setBucketMeta(meta);

			var k1 = new Binary(new byte[]{1});
			var k2 = new Binary(new byte[]{2});
			var j1 = new Binary(new byte[]{0x10});
			var value = new Binary(new byte[]{7});

			// T1: {k1,k2} prepare成功，锁被持有（2PC进行中，不commit）。
			{
				var batch = new BPrepareBatch.Data("", "database", "table1", null);
				batch.getBatch().getPuts().put(k1, value);
				batch.getBatch().getPuts().put(k2, value);
				batch.getBatch().setTid(1);
				var f = agent.prepareBatch(batch);
				f.await();
				Assertions.assertEquals(0, f.get().getResultCode());
			}

			// T2: {j1,k2}：j1加锁成功后在k2上冲突抛出。构造器回滚（修复）则j1被释放；
			// 回滚前（bug）j1的semaphore被永久扣减。
			{
				var batch = new BPrepareBatch.Data("", "database", "table1", null);
				batch.getBatch().getPuts().put(j1, value);
				batch.getBatch().getPuts().put(k2, value);
				batch.getBatch().setTid(2);
				var f = agent.prepareBatch(batch);
				f.await();
				var rc = f.get().getResultCode();
				Assertions.assertTrue(rc != 0 && rc != Procedure.RaftApplied,
						"conflict prepare must fail (lock timeout)");
			}

			// T3: {j1}：必须还能prepare成功——bug时j1随T2泄漏，这里立刻"lock timeout"。
			{
				var batch = new BPrepareBatch.Data("", "database", "table1", null);
				batch.getBatch().getPuts().put(j1, value);
				batch.getBatch().setTid(3);
				var f = agent.prepareBatch(batch);
				f.await();
				Assertions.assertEquals(0, f.get().getResultCode(),
						"key locked by failed prepare must be reusable immediately");
			}
			agent.commitBatch(3).await();
			Assertions.assertEquals(value, get(agent, j1));

			// T1正常收尾。
			agent.commitBatch(1).await();
			Assertions.assertEquals(value, get(agent, k1));
		} finally {
			stopBucket(nodes, agent, database);
		}
	}
}
