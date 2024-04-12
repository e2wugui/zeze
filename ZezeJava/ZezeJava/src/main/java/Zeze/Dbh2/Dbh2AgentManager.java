package Zeze.Dbh2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Raft.ProxyAgent;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.Action2;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * 这个类管理到桶的raft-client-agent。
 * 实际上不能算池子，一个桶目前考虑只建立一个实例，多线程使用时共享同一个实例。
 */
public class Dbh2AgentManager extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(Dbh2AgentManager.class);
	// 多master支持
	private final ConcurrentHashMap<String, MasterAgent> masterAgent = new ConcurrentHashMap<>();
	// master->database->tableBuckets
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, MasterTable.Data>>>
			buckets = new ConcurrentHashMap<>();
	// agent 不同 master 也装在一起。
	private final ConcurrentHashMap<String, Dbh2Agent> agents = new ConcurrentHashMap<>();

	private final ProxyAgent proxyAgent;
	private final Config config;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private Commit commit;
	private CommitAgent commitAgent;
	private Future<?> refreshMasterTableTask;
	private final AbstractAgent serviceManager;
	private final AutoKey tidAutoKey;

	public void startRefreshMasterTable(String masterName, String databaseName, String tableName) {
		lock();
		try {
			if (null != refreshMasterTableTask)
				return;

			refreshMasterTableTask = Task.scheduleUnsafe(200,
					() -> {
						reload(openMasterAgent(masterName), masterName, databaseName, tableName);
						refreshMasterTableTask = null;
					});
		} finally {
			unlock();
		}
	}

	public Dbh2Config getDbh2Config() {
		return dbh2Config;
	}

	public AbstractAgent getServiceManager() {
		return serviceManager;
	}

	public Dbh2AgentManager(AbstractAgent serviceManager, Config config) throws Exception {
		this(serviceManager, config, -1);
	}

	public Dbh2AgentManager(AbstractAgent serviceManager, Config config, int serverId) throws Exception {
		if (null == config)
			config = Config.load();
		this.serviceManager = serviceManager;
		this.tidAutoKey = serviceManager.getAutoKey("Dbh2.AutoKey." + config.getName());

		config.parseCustomize(dbh2Config);
		if (serverId != -1) // 为了测试能指定一个不一样的serverId用来连续运行测试。
			config.setServerId(serverId);
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
		proxyAgent = new ProxyAgent(dbh2Config.getRpcTimeout());
	}

	public long nextTransactionId() {
		return tidAutoKey.next();
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
			var state = CommitRocks.buildTransactionState(batches);
			commit.getRocks().prepare(query.getKey(), query.getValue(), state, batches, null);
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
		commitAgent.commit(query.getKey(), query.getValue(), batches, dbh2Config.getRpcTimeout());
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
		proxyAgent.start();
		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		lock();
		try {
			proxyAgent.stop();
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
		} finally {
			unlock();
		}
	}

	public MasterAgent openMasterAgent(String masterName) {
		return masterAgent.computeIfAbsent(masterName, _masterName -> {
			var config1 = new Config();
			var serviceConf = new ServiceConf();
			var ipPort = _masterName.split("_");
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

	public void createTableAsync(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Action2<Integer, Boolean> callback) {
		logger.info("createTableAsync: db={}, table={}", databaseName, tableName);
		masterAgent.createTableAsync(databaseName, tableName, (rc, isNew, masterTable) -> {
			if (rc == 0) {
				putBuckets(masterTable, masterName, databaseName, tableName);
				for (var bucket : masterTable.buckets())
					openBucket(bucket.getRaftConfig());
			}
			callback.run(rc, isNew);
		});
	}

	public void dumpAgents() {
		System.out.println("dump agents ...");
		for (var e : agents.keySet())
			System.out.println(e);
	}

	// Database.Table中缓存MasterTableDaTa，减少map查找。
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

	public Iterator<BBucketMeta.Data> locateBucketIterator(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Binary key) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = database.computeIfAbsent(tableName, tbName -> masterAgent.getBuckets(databaseName, tbName));
		return table.tailMap(key).values().iterator();
	}

	public Dbh2Agent openBucket(String raftString) {
		return agents.computeIfAbsent(raftString, _raft -> {
			logger.info("openBucket: new Dbh2Agent: {}", raftString);
			try {
				return new Dbh2Agent(raftString, proxyAgent);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void reload(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		lock();
		try {
			var masterTable = masterAgent.getBuckets(databaseName, tableName);
			logger.info("reload ... {}", masterTable);
			putBuckets(masterTable, masterName, databaseName, tableName);
		} finally {
			unlock();
		}
	}

	public void putBuckets(
			MasterTable.Data buckets,
			String masterName,
			String databaseName,
			String tableName) {
		lock();
		try {
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
		} finally {
			unlock();
		}
	}

	public long walk(MasterAgent masterAgent,
					 String masterName, String databaseName, String tableName,
					 TableWalkHandleRaw callback,
					 boolean desc,
					 @Nullable byte[] prefix) {
		var table = buckets.get(masterName).get(databaseName).get(tableName);
		var count = 0L;
		for (var bucketIt = table.buckets().iterator(); bucketIt.hasNext(); /* nothing */) {
			var exclusiveStartKey = Binary.Empty;
			var proposeLimit = 5000;
			var bucket = bucketIt.next();
			while (true) {
				var r = openBucket(bucket.getRaftConfig()).walk(exclusiveStartKey, proposeLimit, desc, prefix);
				// 处理错误：1. 需要处理分桶的拒绝；2. 其他错误抛出异常。
				if (r.getResultCode() != 0)
					throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
				if (r.Result.isBucketRefuse()) {
					// 分桶但是本地信息没有更新会出现这种情况，此时重新装载桶的信息，再次定位。
					reload(masterAgent, masterName, databaseName, tableName);
					bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveStartKey);
					if (bucketIt.hasNext()) {
						bucket = bucketIt.next();
						continue; // refused and redirect success.
					}
					break; // no more bucket
				}
				for (var keyValue : r.Result.getKeyValues()) {
					callback.handle(keyValue.getKey().bytesUnsafe(), keyValue.getValue().bytesUnsafe());
					exclusiveStartKey = keyValue.getKey();
					count++;
				}
				if (r.Result.isBucketEnd() || r.Result.getKeyValues().isEmpty())
					break; // no more record in this bucket
			}
		}
		return count;
	}

	public ByteBuffer walk(MasterAgent masterAgent,
						   String masterName, String databaseName, String tableName,
						   ByteBuffer exclusiveStartKey, int proposeLimit,
						   TableWalkHandleRaw callback,
						   boolean desc,
						   @Nullable byte[] prefix) {
		var exclusiveKey = exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty;
		var bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey);
		if (!bucketIt.hasNext())
			return null;
		var bucket = bucketIt.next();
		while (true) {
			Binary lastKey = null;
			var r = openBucket(bucket.getRaftConfig()).walk(exclusiveKey, proposeLimit, desc, prefix);
			// 处理错误：1. 需要处理分桶的拒绝；2. 其他错误抛出异常。
			if (r.getResultCode() != 0)
				throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
			if (r.Result.isBucketRefuse()) {
				// 分桶但是本地信息没有更新会出现这种情况，此时重新装载桶的信息，再次定位。
				reload(masterAgent, masterName, databaseName, tableName);
				bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey);
				if (bucketIt.hasNext()) {
					bucket = bucketIt.next();
					continue; // refused and redirect success.
				}
				break; // no more bucket
			}
			for (var keyValue : r.Result.getKeyValues()) {
				callback.handle(keyValue.getKey().copyIf(), keyValue.getValue().copyIf());
				lastKey = keyValue.getKey();
			}
			return lastKey != null && !r.Result.isBucketEnd() ? ByteBuffer.Wrap(lastKey) : null;
		}
		return null;
	}

	public long walkKey(MasterAgent masterAgent,
						String masterName, String databaseName, String tableName,
						TableWalkKeyRaw callback,
						boolean desc,
						@Nullable byte[] prefix) {
		var table = buckets.get(masterName).get(databaseName).get(tableName);
		var count = 0L;
		for (var bucketIt = table.buckets().iterator(); bucketIt.hasNext(); /* nothing */) {
			var exclusiveStartKey = Binary.Empty;
			var proposeLimit = 5000;
			var bucket = bucketIt.next();
			while (true) {
				var r = openBucket(bucket.getRaftConfig()).walkKey(exclusiveStartKey, proposeLimit, desc, prefix);
				// 处理错误：1. 需要处理分桶的拒绝；2. 其他错误抛出异常。
				if (r.getResultCode() != 0)
					throw new RuntimeException("walkKey result=" + IModule.getErrorCode(r.getResultCode()));
				if (r.Result.isBucketRefuse()) {
					// 分桶但是本地信息没有更新会出现这种情况，此时重新装载桶的信息，再次定位。
					reload(masterAgent, masterName, databaseName, tableName);
					bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveStartKey);
					if (bucketIt.hasNext()) {
						bucket = bucketIt.next();
						continue; // refused and redirect success.
					}
					break; // no more bucket
				}
				for (var key : r.Result.getKeys()) {
					callback.handle(key.bytesUnsafe());
					exclusiveStartKey = key;
					count++;
				}
				if (r.Result.isBucketEnd() || r.Result.getKeys().isEmpty())
					break; // no more record in this bucket
			}
		}
		return count;
	}

	public ByteBuffer walkKey(MasterAgent masterAgent,
							  String masterName, String databaseName, String tableName,
							  ByteBuffer exclusiveStartKey, int proposeLimit,
							  TableWalkKeyRaw callback,
							  boolean desc,
							  @Nullable byte[] prefix) {
		var exclusiveKey = exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty;
		var bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey);
		if (!bucketIt.hasNext())
			return null;
		var bucket = bucketIt.next();
		Binary lastKey = null;
		while (true) {
			var r = openBucket(bucket.getRaftConfig()).walkKey(exclusiveKey, proposeLimit, desc, prefix);
			// 处理错误：1. 需要处理分桶的拒绝；2. 其他错误抛出异常。
			if (r.getResultCode() != 0)
				throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
			if (r.Result.isBucketRefuse()) {
				// 分桶但是本地信息没有更新会出现这种情况，此时重新装载桶的信息，再次定位。
				reload(masterAgent, masterName, databaseName, tableName);
				bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey);
				if (bucketIt.hasNext()) {
					bucket = bucketIt.next();
					continue; // refused and redirect success.
				}
				break; // no more bucket
			}
			for (var key : r.Result.getKeys()) {
				callback.handle(key.copyIf());
				lastKey = key;
			}
			break;
		}
		return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
	}
}
