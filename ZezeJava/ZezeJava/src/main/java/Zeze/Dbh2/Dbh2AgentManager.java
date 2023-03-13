package Zeze.Dbh2;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Raft.RaftConfig;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 这个类管理到桶的raft-client-agent。
 * 实际上不能算池子，一个桶目前考虑只建立一个实例，多线程使用时共享同一个实例。
 */
public class Dbh2AgentManager {
	private static final Logger logger = LogManager.getLogger(Dbh2AgentManager.class);
	// 多master支持
	private final ConcurrentHashMap<String, MasterAgent> masterAgent = new ConcurrentHashMap<>();
	// master->database->tableBuckets
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, MasterTable.Data>>>
			buckets = new ConcurrentHashMap<>();
	// agent 不同 master 也装在一起。
	private final ConcurrentHashMap<String, Dbh2Agent> agents = new ConcurrentHashMap<>();

	private static final Dbh2AgentManager instance = new Dbh2AgentManager();

	public static Dbh2AgentManager getInstance() {
		return instance;
	}

	public Dbh2AgentManager() {
	}

	public MasterAgent openDatabase(
			String masterName,
			String databaseName) {
		var master = masterAgent.computeIfAbsent(masterName, (_masterName) -> {
			var config = new Config();
			var serviceConf = new ServiceConf();
			var ipPort = masterName.split(":");
			config.getServiceConfMap().put(MasterAgent.eServiceName, serviceConf);
			serviceConf.tryGetOrAddConnector(ipPort[0], Integer.parseInt(ipPort[1]), true, null);
			var m = new MasterAgent(config);
			m.startAndWaitConnectionReady();
			return m;
		});
		master.createDatabase(databaseName);
		return master;
	}

	public boolean createTable(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		var out = new OutObject<MasterTable.Data>();
		var isNew = masterAgent.createTable(databaseName, tableName, out);
		putBuckets(out.value, masterName, databaseName, tableName);
		return isNew;
	}

	// todo Database.Table中缓存MasterTableDaTa，减少map查找。
	//  难点是信息发生了变更需要刷新Table中缓存的数据。
	public Dbh2Agent open(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Binary key) {
		var master = buckets.computeIfAbsent(masterName, (mName) -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, (dbName) -> new ConcurrentHashMap<>());
		var table = database.computeIfAbsent(tableName, (tbName) -> masterAgent.getBuckets(databaseName, tableName));
		var bucket = table.locate(key);
		return open(bucket.getRaftConfig());
	}

	public Dbh2Agent open(String raft) {
		return agents.computeIfAbsent(raft, (_raft) -> {
			try {
				var raftConfig = RaftConfig.loadFromString(raft);
				return new Dbh2Agent(raftConfig);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public synchronized void reload(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		putBuckets(masterAgent.getBuckets(databaseName, tableName), masterName, databaseName, tableName);
	}

	public synchronized void putBuckets(
			MasterTable.Data buckets,
			String masterName,
			String databaseName,
			String tableName) {
		var master = this.buckets.computeIfAbsent(masterName, (mName) -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, (dbName) -> new ConcurrentHashMap<>());
		var table = database.get(tableName);
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
					logger.error("", e);
				}
			}
		}
		database.put(tableName, buckets);
	}
}
