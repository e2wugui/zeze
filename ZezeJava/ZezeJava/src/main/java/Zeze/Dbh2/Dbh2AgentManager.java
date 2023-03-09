package Zeze.Dbh2;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMetaDaTa;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTableDaTa;
import Zeze.Net.Binary;
import Zeze.Raft.RaftConfig;

/**
 * 这个类管理到桶的raft-client-agent。
 * 实际上不能算池子，一个桶目前考虑只建立一个实例，多线程使用时共享同一个实例。
 */
public class Dbh2AgentManager {
	private final MasterAgent masterAgent;
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, MasterTableDaTa>> buckets = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Dbh2Agent> agents = new ConcurrentHashMap<>();

	public Dbh2AgentManager(Config config) {
		masterAgent = new MasterAgent(config);
	}

	public void start() throws Exception {
		masterAgent.start();
	}

	public void stop() throws Exception {
		masterAgent.stop();
	}

	public void createTable(String databaseName, String tableName) {
		masterAgent.createTable(databaseName, tableName);
	}

	public Dbh2Agent locate(String databaseName, String tableName, Binary key) {
		var database = buckets.computeIfAbsent(databaseName, (dbName) -> new ConcurrentHashMap<>());
		var table = database.computeIfAbsent(tableName, (tbName) -> masterAgent.getBuckets(databaseName, tableName));
		var bucket = table.locate(key);
		return open(databaseName, tableName, bucket.getRaftConfig());
	}

	public Dbh2Agent open(String databaseName, String tableName, String raft) {
		return agents.computeIfAbsent(raft, (_raft) -> {
			try {
				var raftConfig = RaftConfig.loadFromString(raft);
				return new Dbh2Agent(databaseName, tableName, raftConfig);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public synchronized void reloadBuckets(String databaseName, String tableName) {
		var database = buckets.computeIfAbsent(databaseName, (dbName) -> new ConcurrentHashMap<>());
		var table = database.get(tableName);
		var buckets = masterAgent.getBuckets(databaseName, tableName);
		if (table == null) {
			database.put(tableName, buckets);
			return;
		}
		var oldRaft = new HashSet<String>();
		for (var bucket : table.buckets())
			oldRaft.add(bucket.getRaftConfig());
		for (var bucket : buckets.buckets())
			oldRaft.remove(bucket.getRaftConfig());
		for (var raft : oldRaft) {
			var agent = agents.remove(raft);
			if (null != agent) {
				try {
					agent.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
