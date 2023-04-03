package Dbh2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Test;

// 测试桶(raft)，在同一个进程内构建3个桶，通过Dbh2Agent访问。
public class Dbh2Test {
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	public static class Bucket {

		private final ArrayList<Zeze.Dbh2.Dbh2> raftNodes = new ArrayList<>();
		private final Dbh2Agent agent;

		public Bucket(String raftConfigString) throws Exception {
			var raftConfig = RaftConfig.loadFromString(raftConfigString);
			for (var config : raftConfig.getNodes().values())
				raftNodes.add(start(raftConfigString, config.getName()));
			agent = new Dbh2Agent(raftConfig);
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
		Task.tryInitThreadPool(null, null, null);
		var bucket1 = new Bucket("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="10000"/>
					<node Host="127.0.0.1" Port="10001"/>
					<node Host="127.0.0.1" Port="10002"/>
				</raft>
				""");
		var bucket2 = new Bucket("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="">
					<node Host="127.0.0.1" Port="10003"/>
					<node Host="127.0.0.1" Port="10004"/>
					<node Host="127.0.0.1" Port="10005"/>
				</raft>
				""");
		try {

			final var db = "database";
			final var tb1 = "table1";
			final var tb2 = "table2";

			var meta1 = new BBucketMeta.Data();
			meta1.setDatabaseName(db);
			meta1.setTableName(tb1);
			meta1.setRaftConfig("");
			meta1.setMoving(false);
			meta1.setKeyFirst(Binary.Empty);
			meta1.setKeyLast(Binary.Empty);
			bucket1.agent().setBucketMeta(meta1); // 这个meta必须第一个设置。由于测试的meta一样，第二次运行重复设置是可以的。

			var meta2 = new BBucketMeta.Data();
			meta2.setDatabaseName(db);
			meta2.setTableName(tb2);
			meta2.setRaftConfig("");
			meta2.setMoving(false);
			meta2.setKeyFirst(Binary.Empty);
			meta2.setKeyLast(Binary.Empty);
			bucket2.agent().setBucketMeta(meta2); // 这个meta必须第一个设置。由于测试的meta一样，第二次运行重复设置是可以的。

			var key = new Binary(new byte[] { 1 });
			var value = new Binary(new byte[] { 1 });

			{
				var tid = bucket1.agent().beginTransaction(db, tb1);
				Assert.assertNotNull(tid);
				bucket1.agent().put(db, tb1, tid, key, value);
				bucket1.agent().commitTransaction(tid);
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNotNull(kv.getValue());
				Assert.assertEquals(value, new Binary(kv.getValue().Bytes, kv.getValue().ReadIndex, kv.getValue().size()));
			}
			{
				var tid = bucket1.agent().beginTransaction(db, tb1);
				Assert.assertNotNull(tid);
				bucket1.agent().delete(db, tb1, tid, key);
				bucket1.agent().commitTransaction(tid);
			}
			{
				var kv = bucket1.agent().get(db, tb1, key);
				Assert.assertTrue(kv.getKey());
				Assert.assertNull(kv.getValue());
			}

			// multi-bucket transaction
			{
				var tid1 = bucket1.agent().beginTransaction(db, tb1);
				Assert.assertNotNull(tid1);
				var tid2 = bucket2.agent().beginTransaction(db, tb2);
				Assert.assertNotNull(tid2);

				bucket1.agent().put(db, tb1, tid1, key, value);
				bucket2.agent().put(db, tb2, tid2, key, value);

				bucket1.agent().commitTransaction(tid1);
				bucket2.agent().commitTransaction(tid2);
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
		}
	}

	private static Zeze.Dbh2.Dbh2 start(String config, String raftName) throws Exception {
		var raftConfig = RaftConfig.loadFromString(config);
		return new Zeze.Dbh2.Dbh2(raftName, raftConfig, null, false);
	}
}
