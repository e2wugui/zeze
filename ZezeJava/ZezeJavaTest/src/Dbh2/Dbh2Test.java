package Dbh2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import Zeze.Application;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Test;

// 测试桶(raft)，在同一个进程内构建3个桶，通过Dbh2Agent访问。
public class Dbh2Test {
	static {
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private final static TaskOneByOneByKey taskOneByOne = new TaskOneByOneByKey();

	public static class Bucket {
		private final ArrayList<Zeze.Dbh2.Dbh2> raftNodes = new ArrayList<>();
		private final Dbh2Agent agent;

		public Bucket(String raftConfigString) throws Exception {
			var raftConfig = RaftConfig.loadFromString(raftConfigString);
			for (var config : raftConfig.getNodes().values())
				raftNodes.add(start(raftConfigString, config.getName()));
			agent = new Dbh2Agent(raftConfigString);
		}

		public Dbh2Agent agent() {
			return agent;
		}

		public void close() {
			for (var dbh2 : raftNodes) {
				try {
					dbh2.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				// 这个测试不持久化，为了不影响其他测试（可能使用相同的配置），运行结束删除持久化的目录。
				LogSequence.deleteDirectory(new File(dbh2.getRaft().getRaftConfig().getDbHome()));
			}
			try {
				agent.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void testDbh2() throws Exception {
		Task.tryInitThreadPool();
		var bucket1 = new Bucket("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19000"/>
					<node Host="127.0.0.1" Port="19001"/>
					<node Host="127.0.0.1" Port="19002"/>
				</raft>
				""");
		var bucket2 = new Bucket("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="19003"/>
					<node Host="127.0.0.1" Port="19004"/>
					<node Host="127.0.0.1" Port="19005"/>
				</raft>
				""");
		var serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();
		Application.renameAndDeleteDirectory(new File("CommitRocks"));
		var dbh2AgentManager = new Dbh2AgentManager(serviceManager, null, 101);
		try {
			final var db = "database";
			final var tb1 = "table1";
			final var tb2 = "table2";

			var meta1 = new BBucketMeta.Data();
			meta1.setDatabaseName(db);
			meta1.setTableName(tb1);
			meta1.setRaftConfig("");
			meta1.setKeyFirst(Binary.Empty);
			meta1.setKeyLast(Binary.Empty);
			bucket1.agent().setBucketMeta(meta1); // 这个meta必须第一个设置。由于测试的meta一样，第二次运行重复设置是可以的。

			var meta2 = new BBucketMeta.Data();
			meta2.setDatabaseName(db);
			meta2.setTableName(tb2);
			meta2.setRaftConfig("");
			meta2.setKeyFirst(Binary.Empty);
			meta2.setKeyLast(Binary.Empty);
			bucket2.agent().setBucketMeta(meta2); // 这个meta必须第一个设置。由于测试的meta一样，第二次运行重复设置是可以的。

			var key = new Binary(new byte[]{1});
			var value = new Binary(new byte[]{1});

			dbh2AgentManager.start();
			{
				var batch = new BPrepareBatch.Data("", db, tb1, null);
				batch.getBatch().getPuts().put(key, value);
				batch.getBatch().setTid(dbh2AgentManager.nextTransactionId());
				bucket1.agent().prepareBatch(batch).await();
				bucket1.agent().commitBatch(batch.getBatch().getTid()).await();
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNotNull(kv.getValue());
				Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));
			}
			{
				var batch = new BPrepareBatch.Data("", db, tb1, null);
				batch.getBatch().getPuts().put(key, Binary.Empty);
				batch.getBatch().setTid(dbh2AgentManager.nextTransactionId());
				bucket1.agent().prepareBatch(batch).await();
				bucket1.agent().undoBatch(batch.getBatch().getTid()).await();
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNotNull(kv.getValue());
				Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));
			}
			{
				var batch = new BPrepareBatch.Data("", db, tb1, null);
				batch.getBatch().getDeletes().add(key);
				batch.getBatch().setTid(dbh2AgentManager.nextTransactionId());
				bucket1.agent().prepareBatch(batch).await();
				bucket1.agent().commitBatch(batch.getBatch().getTid()).await();
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNull(kv.getValue());
			}

			// multi-bucket transaction
			{
				var batch1 = new BPrepareBatch.Data("", db, tb1, null);
				var batch2 = new BPrepareBatch.Data("", db, tb2, null);

				batch1.getBatch().setTid(dbh2AgentManager.nextTransactionId());
				batch2.getBatch().setTid(batch1.getBatch().getTid());

				batch1.getBatch().getPuts().put(key, value);
				batch2.getBatch().getPuts().put(key, value);

				bucket1.agent().prepareBatch(batch1).await();
				bucket2.agent().prepareBatch(batch2).await();

				bucket1.agent().commitBatch(batch1.getBatch().getTid()).await();
				bucket2.agent().commitBatch(batch2.getBatch().getTid()).await();
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNotNull(kv.getValue());
				Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));
			}
			{
				var kv = bucket2.agent().get(db, tb2, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNotNull(kv.getValue());
				Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));
			}
		} finally {
			bucket1.close();
			bucket2.close();
			dbh2AgentManager.stop();
		}
	}

	private static Zeze.Dbh2.Dbh2 start(String config, String raftName) throws Exception {
		var raftConfig = RaftConfig.loadFromString(config);
		return new Zeze.Dbh2.Dbh2(null, raftName, raftConfig, null, false, taskOneByOne);
	}
}
