package Zeze.Dbh2;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
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

	private final Config config;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private Commit commit;
	private CommitAgent commitAgent;
	private Future<?> refreshMasterTableTask;

	public synchronized void startRefreshMasterTable(
			String masterName, String databaseName, String tableName) {
		if (null != refreshMasterTableTask)
			return;

		refreshMasterTableTask = Task.scheduleUnsafe(200,
				() -> {
					reload(openMasterAgent(masterName), masterName, databaseName, tableName);
					refreshMasterTableTask = null;
				});
	}

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public Dbh2AgentManager(Config config) throws RocksDBException {
		if (null == config)
			config = new Config().addCustomize(dbh2Config).loadAndParse();
		this.config = config;
		// ugly
		if (config.isDbh2LocalCommit()) {
			if (null == commit) {
				commit = new Commit(this, config);
			}
		} else {
			if (null == commitAgent) {
				commitAgent = new CommitAgent();
			}
		}
	}

	private static final SecureRandom secureRandom = new SecureRandom();

	public static byte[] nextTransactionId() {
		var tid = new byte[20];
		secureRandom.nextBytes(tid);
		return tid;
	}

	public KV<String, Integer> commitServiceAcceptor() {
		var out = new KV<String, Integer>();
		commit.getService().getConfig().forEachAcceptor2((a) -> {
			out.setKey(a.getIp());
			out.setValue(a.getPort());
			return false;
		});
		if (out.getKey() == null || out.getValue() == 0)
			throw new RuntimeException("Commit Query Acceptor Not Set.");
		return out;
	}

	public void commitBreakAfterPrepareForDebugOnly(BPrepareBatches.Data batches) {
		if (config.isDbh2LocalCommit()) {
			var query = commitServiceAcceptor();
			commit.getRocks().prepare(query.getKey(), query.getValue(), batches);
		}
		// 这个仅仅用来调试，不报错了。
		// else throw new RuntimeException("commitBreakAfterPrepareForDebugOnly only work with local commit.");
	}

	@SuppressWarnings("MethodMayBeStatic")
	private KV<String, Integer> choiceCommitServer() {
		throw new UnsupportedOperationException();
	}

	public void commit(BPrepareBatches.Data batches) {
		if (config.isDbh2LocalCommit()) {
			var query = commitServiceAcceptor();
			commit.getRocks().commit(query.getKey(), query.getValue(), batches);
			return; // done;
		}
		var query = choiceCommitServer();
		commitAgent.commit(query.getKey(), query.getValue(), batches);
	}

	// Dbh2Agent 嵌入服务器需要初始化；
	// CommitServer 独立服务器需要初始化；
	public void start() throws Exception {
		// ugly
		if (config.isDbh2LocalCommit()) {
			commit.start();
		} else {
			commitAgent.startAndWaitConnectionReady();
		}
		ShutdownHook.add(this, this::stop);
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

	public MasterAgent openMasterAgent(String masterName) {
		return masterAgent.computeIfAbsent(masterName, _masterName -> {
			var config1 = new Config();
			var serviceConf = new ServiceConf();
			var ipPort = _masterName.split(":");
			config1.getServiceConfMap().put(MasterAgent.eServiceName, serviceConf);
			serviceConf.tryGetOrAddConnector(ipPort[0], Integer.parseInt(ipPort[1]), true, null);
			var m = new MasterAgent(config1);
			m.startAndWaitConnectionReady();
			return m;
		});
	}

	public MasterAgent openDatabase(
			String masterName,
			String databaseName) {
		var master = openMasterAgent(masterName);
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
	public String locateBucket(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Binary key) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = database.computeIfAbsent(tableName, tbName -> masterAgent.getBuckets(databaseName, tbName));
		return table.locate(key).getRaftConfig();
	}

	public Dbh2Agent openBucket(String raftString) {
		return agents.computeIfAbsent(raftString, _raft -> {
			try {
				return new Dbh2Agent(raftString);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public synchronized void reload(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		var masterTable = masterAgent.getBuckets(databaseName, tableName);
		logger.info("reload ... {}", masterTable);
		putBuckets(masterTable, masterName, databaseName, tableName);
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
