package Zeze.Dbh2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Raft.RaftConfig;
import Zeze.Util.KV;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

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

	private Config config;
	private Commit commit;
	private CommitAgent commitAgent;

	private static final Dbh2AgentManager instance = new Dbh2AgentManager();

	public static Dbh2AgentManager getInstance() {
		return instance;
	}

	public Dbh2AgentManager() {
	}

	public boolean saveCommitPoint(String host, int port, HashMap<Dbh2Agent, Database.BatchWithTid> batches) throws RocksDBException {
		if (config.isDbh2LocalCommit()) {
			commit.getRocks().saveCommitPoint(batches);
			return true;
		}
		// choice CommitServer outside.
		commitAgent.commit(host, port, batches);
		return false;
	}

	// Dbh2Agent 嵌入服务器需要初始化；
	// CommitServer 独立服务器需要初始化；
	public synchronized void start(Config config) throws Exception {
		this.config = config;

		// ugly
		if (config.isDbh2LocalCommit()) {
			if (null == commit) {
				commit = new Commit(this, config);
				commit.start();
			}
		} else {
			if (null == commitAgent) {
				commitAgent = new CommitAgent();
				commitAgent.startAndWaitConnectionReady();
			}
		}
	}

	public KV<String, Integer> choiceCommitServer() {
		// ugly
		if (config.isDbh2LocalCommit()) {
			var out = new KV<String, Integer>();
			commit.getService().getConfig().forEachAcceptor2((a) -> {
				out.setKey(a.getIp());
				out.setValue(a.getPort());
				return false;
			});
			return out;
		}

		// todo 选择独立 CommitServer。
		return null;
	}

	public synchronized void stop() throws Exception {
		for (var ma : masterAgent.values())
			ma.stop();
		masterAgent.clear();
		for (var da : agents.values())
			da.close();
		agents.clear();

		if (null != commit) {
			commit.stop();
			commit = null;
		}
		if (null != commitAgent) {
			commitAgent.stop();
			commitAgent = null;
		}
	}

	public MasterAgent openDatabase(
			String masterName,
			String databaseName) {
		var master = masterAgent.computeIfAbsent(masterName, _masterName -> {
			var config = new Config();
			var serviceConf = new ServiceConf();
			var ipPort = _masterName.split(":");
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

	public void dumpAgents() {
		System.out.println("dump agents ...");
		for (var e : agents.keySet())
			System.out.println(e);
	}

	// todo Database.Table中缓存MasterTableDaTa，减少map查找。
	//  难点是信息发生了变更需要刷新Table中缓存的数据。
	public Dbh2Agent start(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Binary key) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = database.computeIfAbsent(tableName, tbName -> masterAgent.getBuckets(databaseName, tbName));
		var bucket = table.locate(key);
		var raftConf = RaftConfig.loadFromString(bucket.getRaftConfig());
		return start(raftConf);
	}

	public Dbh2Agent startWithSortedNames(String raftNames) {
		var raftConf = RaftConfig.loadFromSortedNames(raftNames);
		return start(raftConf);
	}

	public Dbh2Agent start(RaftConfig raftConf) {
		return agents.computeIfAbsent(raftConf.getSortedNames(), _raft -> {
			try {
				return new Dbh2Agent(raftConf);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public synchronized void reload(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		//System.out.println("reload ..." + tableName);
		putBuckets(masterAgent.getBuckets(databaseName, tableName), masterName, databaseName, tableName);
	}

	public synchronized void putBuckets(
			MasterTable.Data buckets,
			String masterName,
			String databaseName,
			String tableName) {
		var master = this.buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
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
			var raftConf = RaftConfig.loadFromString(raft);
			var agent = agents.remove(raftConf.getSortedNames());
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
