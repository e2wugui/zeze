package Zeze.Dbh2;

import java.util.Objects;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Commit.BPrepareBatches;
import Zeze.Builtin.Dbh2.Walk;
import Zeze.Builtin.Dbh2.WalkKey;
import Zeze.Config;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Raft.ProxyAgent;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TableWalkHandleRaw;
import Zeze.Transaction.TableWalkKeyRaw;
import Zeze.Util.Action2;
import Zeze.Util.KVNotNull;
import Zeze.Util.OutObject;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

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
	// 记录各桶 agent 最近活跃时间，供周期任务回收不在主表缓存中且超期闲置的死桶重定向 agent
	private final ConcurrentHashMap<String, Long> agentActiveTimes = new ConcurrentHashMap<>();
	private static final long IdleAgentReclaimTimeoutMs = 10 * 60_000L; // 远大于 rpcTimeout
	private static final long IdleAgentReclaimPeriodMs = 60_000L;
	private volatile @Nullable Future<?> idleReclaimTask;

	private final ProxyAgent proxyAgent;
	private final Config config;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private @Nullable Commit commit;
	private @Nullable CommitAgent commitAgent;
	private @Nullable Future<?> refreshMasterTableTask;
	// stop 后置位且不可复位；后台刷新任务体持管理器锁与 stop 互斥
	private volatile boolean stopped;
	private final AbstractAgent serviceManager;
	private final AutoKey tidAutoKey;

	public void startRefreshMasterTable(String masterName, String databaseName, String tableName) {
		lock();
		try {
			if (stopped)
				return;
			if (null != refreshMasterTableTask)
				return;

			refreshMasterTableTask = TaskSpec.ofAction(() -> {
				lock();
				try {
					if (stopped)
						return;
					try {
						reload(openMasterAgent(masterName), masterName, databaseName, tableName);
					} catch (Exception e) {
						logger.warn("refresh master table fail. master={} database={} table={}",
							masterName, databaseName, tableName, e);
					}
				} finally {
					// 无论成败均须在锁内复位，避免异常后永久阻塞后续刷新调度
					refreshMasterTableTask = null;
					unlock();
				}
			}).scheduleNow(200);
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

	public Dbh2AgentManager(AbstractAgent serviceManager, @Nullable Config config) throws Exception {
		this(serviceManager, config, -1);
	}

	public Dbh2AgentManager(AbstractAgent serviceManager, @Nullable Config config, int serverId) throws Exception {
		if (null == config)
			config = Config.load();
		this.serviceManager = serviceManager;
		this.tidAutoKey = serviceManager.getAutoKey("Dbh2.AutoKey." + config.getName());

		config.parseCustomize(dbh2Config);
		if (serverId != -1) // 为了测试能指定一个不一样的serverId用来连续运行测试。
			config.setServerId(serverId);
		this.config = config;
		if (config.isDbh2LocalCommit()) {
			if (null == commit) {
				commit = new Commit(this, config);
			}
		} else {
			if (null == dbh2Config.getCommitServerHost())
				throw new RuntimeException("Dbh2LocalCommit=false but Dbh2Config CommitServerAddress not configured.");
			if (null == commitAgent) {
				commitAgent = new CommitAgent();
			}
		}
		proxyAgent = new ProxyAgent(dbh2Config.getRpcTimeout());
	}

	public long nextTransactionId() {
		return tidAutoKey.next();
	}

	public KVNotNull<String, Integer> commitServiceAcceptor() {
		var ip = new String[1];
		var port = new int[1];
		//noinspection DataFlowIssue
		commit.getService().getConfig().forEachAcceptor2((a) -> {
			ip[0] = a.getIp();
			// Port=0 自动分配时回读活 socket 的实际绑定端口
			var local = a.getSocket() instanceof TcpSocket tcp ? tcp.getLocalInet() : null;
			port[0] = local != null ? local.getPort() : a.getPort();
			return false;
		});
		if (ip[0] == null || port[0] == 0)
			throw new RuntimeException("Commit Query Acceptor Not Set.");
		return KVNotNull.create(ip[0], port[0]);
	}

	private KVNotNull<String, Integer> choiceCommitServer() {
		return KVNotNull.create(Objects.requireNonNull(dbh2Config.getCommitServerHost()), dbh2Config.getCommitServerPort());
	}

	public void commit(BPrepareBatches.Data batches) {
		if (config.isDbh2LocalCommit()) {
			var query = commitServiceAcceptor();
			//noinspection DataFlowIssue
			commit.getRocks().commit(query.getKey(), query.getValue(), batches);
			return;
		}
		var query = choiceCommitServer();
		// 超时须覆盖服务端同步 decide 与 deliver 全流程，避免误判已提交事务失败
		//noinspection DataFlowIssue
		commitAgent.commit(query.getKey(), query.getValue(), batches, (int)dbh2Config.getCommitRpcTimeout());
	}

	// Dbh2Agent 嵌入服务器需要初始化；
	// CommitServer 独立服务器需要初始化；
	public void start() throws Exception {
		if (config.isDbh2LocalCommit()) {
			//noinspection DataFlowIssue
			commit.start();
			logger.info("Dbh2 local commit query endpoint: {}", commitServiceAcceptor());
		} else {
			//noinspection DataFlowIssue
			commitAgent.startAndWaitConnectionReady();
		}
		proxyAgent.start();
		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		lock();
		try {
			stopped = true;
			var task = refreshMasterTableTask;
			if (null != task)
				task.cancel(false);
			refreshMasterTableTask = null;
			ShutdownHook.remove(this);
			// 逐项独立清理并在全部结束后重抛首个异常，避免单步失败导致其余连接或任务泄漏
			Exception first = null;
			try {
				try {
					proxyAgent.stop();
				} catch (Exception e) {
					first = e;
					logger.error("stop proxyAgent fail", e);
				}
				var reclaim = idleReclaimTask;
				if (null != reclaim)
					reclaim.cancel(false);
				idleReclaimTask = null;
				for (var ma : masterAgent.values()) {
					try {
						ma.stop();
					} catch (Exception e) {
						if (null == first)
							first = e;
						logger.error("stop masterAgent fail", e);
					}
				}
				for (var da : agents.values()) {
					try {
						da.close();
					} catch (Exception e) {
						if (null == first)
							first = e;
						logger.error("stop agent fail", e);
					}
				}
				if (null != commit) {
					try {
						commit.stop();
					} catch (Exception e) {
						if (null == first)
							first = e;
						logger.error("stop commit fail", e);
					}
				}
				if (null != commitAgent) {
					try {
						commitAgent.stop();
					} catch (Exception e) {
						if (null == first)
							first = e;
						logger.error("stop commitAgent fail", e);
					}
				}
			} finally {
				masterAgent.clear();
				agents.clear();
				agentActiveTimes.clear();
				buckets.clear();
				commit = null;
				commitAgent = null;
			}
			if (null != first)
				throw first;
		} finally {
			unlock();
		}
	}

	public MasterAgent openMasterAgent(String masterName) {
		var m = masterAgent.get(masterName);
		if (null != m)
			return m;
		// 锁外建连避免持锁阻塞，构造前后各检查一次 stopped 防止 stop 后泄漏新连接
		lock();
		try {
			if (stopped)
				throw new IllegalStateException("Dbh2AgentManager stopped.");
		} finally {
			unlock();
		}
		var config1 = new Config();
		var serviceConf = new ServiceConf();
		var ipPort = masterName.split("_");
		config1.getServiceConfMap().put(MasterAgent.eServiceName, serviceConf);
		serviceConf.tryGetOrAddConnector(ipPort[0], Integer.parseInt(ipPort[1]), true, null);
		var newAgent = new MasterAgent(config1);
		newAgent.startAndWaitConnectionReady();
		lock();
		try {
			if (stopped) {
				newAgent.stop();
				throw new IllegalStateException("Dbh2AgentManager stopped.");
			}
			var old = masterAgent.putIfAbsent(masterName, newAgent);
			if (null != old) {
				newAgent.stop();
				return old;
			}
			return newAgent;
		} finally {
			unlock();
		}
	}

	public MasterAgent openDatabase(
		String masterName,
		String databaseName) {
		var master = openMasterAgent(masterName);
		master.createDatabase(databaseName);
		return master;
	}

	public boolean createTable(
		@Nullable MasterAgent masterAgent, String masterName,
		String databaseName, String tableName) {
		var out = new OutObject<MasterTable.Data>();
		//noinspection DataFlowIssue
		var isNew = masterAgent.createTable(databaseName, tableName, out);
		putBuckets(Objects.requireNonNull(out.value), masterName, databaseName, tableName);
		return isNew;
	}

	public void createTableAsync(
		@Nullable MasterAgent masterAgent, String masterName,
		String databaseName, String tableName,
		Action2<Integer, Boolean> callback) {
		logger.info("createTableAsync: db={}, table={}", databaseName, tableName);
		Objects.requireNonNull(masterAgent).createTableAsync(databaseName, tableName, (rc, isNew, masterTable) -> {
			if (rc == 0) {
				try {
					putBuckets(Objects.requireNonNull(masterTable), masterName, databaseName, tableName);
					for (var bucket : masterTable.buckets())
						openBucket(bucket.getRaftConfig());
				} catch (RuntimeException e) {
					logger.warn("createTableAsync: preparing bucket agents failed. db={} table={}",
							databaseName, tableName, e);
					callback.run(stopped ? (int)Procedure.Closed : (int)Procedure.Exception, false);
					return;
				}
			}
			callback.run(rc, isNew);
		});
	}

	public void dumpAgents() {
		System.out.println("dump agents ...");
		for (var e : agents.keySet())
			System.out.println(e);
	}

	// 按表缓存 MasterTable 快照；建表窗口期返回的空快照不入缓存，待建表完成后再拉取
	private MasterTable.Data getBucketsCached(
		@Nullable MasterAgent masterAgent, ConcurrentHashMap<String, MasterTable.Data> database,
		String databaseName, String tableName) {
		var table = database.get(tableName);
		if (null != table)
			return table;
		// getBuckets 为阻塞 RPC，避免放入 computeIfAbsent 持桶锁
		//noinspection DataFlowIssue
		table = masterAgent.getBuckets(databaseName, tableName);
		if (!table.getBuckets().isEmpty()) {
			var old = database.putIfAbsent(tableName, table);
			if (null != old)
				table = old;
		}
		return table;
	}

	private static RuntimeException tableNotReady(String databaseName, String tableName) {
		return new RuntimeException("locateBucket: table not ready (empty buckets snapshot): "
			+ databaseName + '.' + tableName);
	}

	public String locateBucket(
		@Nullable MasterAgent masterAgent, String masterName,
		String databaseName, String tableName,
		Binary key) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = getBucketsCached(masterAgent, database, databaseName, tableName);
		var bucket = table.locate(key);
		if (null == bucket)
			throw tableNotReady(databaseName, tableName);
		return bucket.getRaftConfig();
	}

	public Iterator<BBucketMeta.Data> locateBucketIterator(
		@Nullable MasterAgent masterAgent, String masterName,
		String databaseName, String tableName,
		Binary key, boolean desc) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = getBucketsCached(masterAgent, database, databaseName, tableName);
		if (table.getBuckets().isEmpty())
			throw tableNotReady(databaseName, tableName);
		// 从 key 所在桶（含）开始迭代：asc 向后，desc 向前（空 key 表示全表降序）
		if (desc)
			return key.size() == 0
				? table.getBuckets().descendingMap().values().iterator()
				: table.getBuckets().headMap(key, true).descendingMap().values().iterator();
		return table.tailMap(key).values().iterator();
	}

	public Dbh2Agent openBucket(String raftString) {
		Dbh2Agent agent;
		// 整体持管理器锁，与 stop 及回收路径互斥，防止返回已关闭的 agent 或 stop 后泄漏新实例
		lock();
		try {
			if (stopped)
				throw new IllegalStateException("Dbh2AgentManager stopped.");
			agent = agents.computeIfAbsent(raftString, _raft -> {
				logger.info("openBucket: new Dbh2Agent: {}", raftString);
				try {
					return new Dbh2Agent(raftString, proxyAgent);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			agentActiveTimes.put(raftString, System.currentTimeMillis());
		} finally {
			unlock();
		}
		startIdleReclaim();
		return agent;
	}

	// 周期回收惰性启动，stop 时取消
	private void startIdleReclaim() {
		if (null != idleReclaimTask)
			return;
		lock();
		try {
			if (null == idleReclaimTask && !stopped)
				idleReclaimTask = TaskSpec.ofAction(this::reclaimIdleAgents)
					.schedulePeriodNow(IdleAgentReclaimPeriodMs, IdleAgentReclaimPeriodMs);
		} finally {
			unlock();
		}
	}

	// 回收不在主表缓存中且闲置超过阈值的死桶重定向 agent（仅跳过在役主表成员与近期活跃实例）
	private void reclaimIdleAgents() {
		// stop 持本锁取消并等待回收任务，任务体使用 tryLock 避免死锁
		if (!tryLock())
			return;
		try {
			if (stopped)
				return;
			var now = System.currentTimeMillis();
			var mainRafts = new HashSet<String>();
			for (var master : buckets.values())
				for (var database : master.values())
					for (var table : database.values())
						for (var bucket : table.buckets())
							mainRafts.add(bucket.getRaftConfig());
			for (var e : agents.entrySet()) {
				var raft = e.getKey();
				if (mainRafts.contains(raft))
					continue;
				var active = agentActiveTimes.get(raft);
				if (null == active || now - active < IdleAgentReclaimTimeoutMs)
					continue;
				var agent = agents.remove(raft);
				if (null != agent) {
					agentActiveTimes.remove(raft);
					logger.info("reclaim idle dead-bucket agent: {} idleMs={}", raft, now - active);
					try {
						agent.close();
					} catch (Exception ex) {
						logger.error("reclaim idle agent close fail: {}", raft, ex);
					}
				}
			}
		} finally {
			unlock();
		}
	}

	public void reload(
		@Nullable MasterAgent masterAgent, String masterName,
		String databaseName, String tableName) {
		lock();
		try {
			//noinspection DataFlowIssue
			var masterTable = masterAgent.getBuckets(databaseName, tableName);
			logger.info("reload ... {}", masterTable, new RuntimeException());
			putBuckets(Objects.requireNonNull(masterTable), masterName, databaseName, tableName);
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
			if (stopped)
				throw new IllegalStateException("Dbh2AgentManager stopped.");
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
					agentActiveTimes.remove(raft);
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

	// 把全局游标换算成针对目标桶的游标：桶外时asc从桶头、desc从桶尾开始；
	// 返回null表示该桶整体已在游标走过的范围内，跳过。
	private static @Nullable Binary bucketExclusive(Binary exclusiveKey, BBucketMeta.Data bucket, boolean desc) {
		if (exclusiveKey.size() == 0)
			return Binary.Empty;
		if (desc) {
			// desc交付严格小于游标的key：游标<=keyFirst时本桶无keyFirst..游标之间的可交付key，整桶跳过。
			if (exclusiveKey.compareTo(bucket.getKeyFirst()) <= 0)
				return null;
			var keyLast = bucket.getKeyLast();
			return keyLast.size() > 0 && exclusiveKey.compareTo(keyLast) >= 0 ? Binary.Empty : exclusiveKey;
		}
		if (exclusiveKey.compareTo(bucket.getKeyFirst()) < 0)
			return Binary.Empty;
		return bucket.getKeyLast().size() > 0 && exclusiveKey.compareTo(bucket.getKeyLast()) >= 0 ? null : exclusiveKey;
	}

	// 一页抓取结果：refused表示分桶拒绝需重定位；count为本页交付条数；lastKey为最后交付key（count>0时有效）。
	private record FetchResult(boolean refused,
							   boolean bucketEnd,
							   boolean stopped,
							   int count,
							   @Nullable Binary lastKey) {
	}

	private static final FetchResult REFUSED = new FetchResult(true, false, false, 0, null);

	private static FetchResult deliverWalkPage(Walk r, TableWalkHandleRaw callback,
											   Function<Binary, byte[]> callbackBytes) throws Exception {
		if (r.Result.isBucketRefuse())
			return REFUSED;
		Binary lastKey = null;
		var count = 0;
		var stopped = false;
		for (var keyValue : r.Result.getKeyValues()) {
			lastKey = keyValue.getKey();
			count++;
			if (!callback.handle(callbackBytes.apply(lastKey), callbackBytes.apply(keyValue.getValue()))) {
				stopped = true;
				break;
			}
		}
		return new FetchResult(false, r.Result.isBucketEnd(), stopped, count, lastKey);
	}

	private static FetchResult deliverWalkPage(WalkKey r, TableWalkKeyRaw callback,
											   Function<Binary, byte[]> callbackBytes) throws Exception {
		if (r.Result.isBucketRefuse())
			return REFUSED;
		Binary lastKey = null;
		var count = 0;
		var stopped = false;
		for (var key : r.Result.getKeys()) {
			lastKey = key;
			count++;
			if (!callback.handle(callbackBytes.apply(key))) {
				stopped = true;
				break;
			}
		}
		return new FetchResult(false, r.Result.isBucketEnd(), stopped, count, lastKey);
	}

	@FunctionalInterface
	private interface PageFetcher {
		FetchResult fetch(Dbh2Agent agent, BBucketMeta.Data bucket, Binary exclusiveKey, int limit) throws Exception;
	}

	// 抓取一页（可跨桶）：从exclusiveKey起按方向交付最多proposeLimit条（交付由fetcher执行），
	// 返回下一页游标；null表示全表走完。
	private @Nullable ByteBuffer walkPage(@Nullable MasterAgent masterAgent, String masterName, String databaseName, String tableName,
										  Binary exclusiveKey, int proposeLimit, boolean desc, byte @Nullable [] prefix,
										  PageFetcher fetch) throws Exception {
		var bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey, desc);
		if (!bucketIt.hasNext())
			return null;
		var bucket = bucketIt.next();
		var limit = proposeLimit;
		// 连续 refused 重定向上限 256 次，fetch 成功即清零，防止表路由长期陈旧导致死循环
		var refusedCount = 0;
		while (true) {
			var exclusiveForBucket = bucketExclusive(exclusiveKey, bucket, desc);
			if (exclusiveForBucket == null) {
				// 桶整体已在游标走过的范围：跳过。
				if (!bucketIt.hasNext())
					return null;
				bucket = bucketIt.next();
				continue;
			}
			var result = fetch.fetch(openBucket(bucket.getRaftConfig()), bucket, exclusiveForBucket, limit);
			if (result.refused) {
				if (++refusedCount > 255)
					throw new RuntimeException("walkPage bucket refused too many redirect: master="
						+ masterName + " database=" + databaseName + " table=" + tableName
						+ " refusedCount=" + refusedCount);
				// 分桶后本地路由过期，重新装载并重定位
				reload(masterAgent, masterName, databaseName, tableName);
				bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey, desc);
				if (bucketIt.hasNext()) {
					bucket = bucketIt.next();
					continue;
				}
				return null;
			}
			refusedCount = 0;
			if (result.stopped)
				return ByteBuffer.Wrap(Objects.requireNonNull(result.lastKey));
			if (result.lastKey != null) {
				exclusiveKey = result.lastKey;
				limit -= result.count;
				if (limit <= 0)
					return ByteBuffer.Wrap(exclusiveKey); // 页满
			}
			if (!result.bucketEnd && result.count > 0) {
				// 非桶尾但未填满页（服务端不会发生，防御）：返回游标，避免死循环。
				return ByteBuffer.Wrap(exclusiveKey);
			}
			// 桶尾或空页：推进到下一个桶继续填页，全部桶走完才算表尾。
			if (!bucketIt.hasNext())
				return null;
			bucket = bucketIt.next();
		}
	}

	public long walk(@Nullable MasterAgent masterAgent,
					 String masterName, String databaseName, String tableName,
					 TableWalkHandleRaw callback,
					 boolean desc,
					 byte @Nullable [] prefix) throws Exception {
		var total = new long[1];
		var stopped = new boolean[1];
		var exclusiveKey = Binary.Empty;
		while (true) {
			var cursor = walkPage(masterAgent, masterName, databaseName, tableName, exclusiveKey, 5000, desc, prefix,
				(agent, bucket, exclusive, limit) -> {
					var r = agent.walk(exclusive, limit, desc, prefix, bucket.getKeyFirst(), bucket.getKeyLast());
					// 重发命中服务端去重存根返回 RaftApplied，结果完整可直接交付
					if (r.getResultCode() != 0 && r.getResultCode() != Zeze.Transaction.Procedure.RaftApplied)
						throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
					var result = deliverWalkPage(r, callback, Binary::bytesUnsafe);
					total[0] += result.count;
					stopped[0] = result.stopped;
					return result;
				});
			if (stopped[0] || cursor == null)
				return total[0];
			exclusiveKey = new Binary(cursor);
		}
	}

	public @Nullable ByteBuffer walk(@Nullable MasterAgent masterAgent,
									 String masterName, String databaseName, String tableName,
									 @Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
									 TableWalkHandleRaw callback,
									 boolean desc,
									 byte @Nullable [] prefix) throws Exception {
		return walkPage(masterAgent, masterName, databaseName, tableName,
			exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty, proposeLimit, desc, prefix,
			(agent, bucket, exclusive, limit) -> {
				var r = agent.walk(exclusive, limit, desc, prefix, bucket.getKeyFirst(), bucket.getKeyLast());
				// 重发命中服务端去重存根返回 RaftApplied，结果完整可直接交付
				if (r.getResultCode() != 0 && r.getResultCode() != Zeze.Transaction.Procedure.RaftApplied)
					throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
				return deliverWalkPage(r, callback, Binary::copyIf);
			});
	}

	public long walkKey(@Nullable MasterAgent masterAgent,
						String masterName, String databaseName, String tableName,
						TableWalkKeyRaw callback,
						boolean desc,
						byte @Nullable [] prefix) throws Exception {
		var total = new long[1];
		var stopped = new boolean[1];
		var exclusiveKey = Binary.Empty;
		while (true) {
			var cursor = walkPage(masterAgent, masterName, databaseName, tableName, exclusiveKey, 5000, desc, prefix,
				(agent, bucket, exclusive, limit) -> {
					var r = agent.walkKey(exclusive, limit, desc, prefix, bucket.getKeyFirst(), bucket.getKeyLast());
					// 重发命中服务端去重存根返回 RaftApplied，结果完整可直接交付
					if (r.getResultCode() != 0 && r.getResultCode() != Zeze.Transaction.Procedure.RaftApplied)
						throw new RuntimeException("walkKey result=" + IModule.getErrorCode(r.getResultCode()));
					var result = deliverWalkPage(r, callback, Binary::bytesUnsafe);
					total[0] += result.count;
					stopped[0] = result.stopped;
					return result;
				});
			if (stopped[0] || cursor == null)
				return total[0];
			exclusiveKey = new Binary(cursor);
		}
	}

	public @Nullable ByteBuffer walkKey(@Nullable MasterAgent masterAgent,
										String masterName, String databaseName, String tableName,
										@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										TableWalkKeyRaw callback,
										boolean desc,
										byte @Nullable [] prefix) throws Exception {
		return walkPage(masterAgent, masterName, databaseName, tableName,
			exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty, proposeLimit, desc, prefix,
			(agent, bucket, exclusive, limit) -> {
				var r = agent.walkKey(exclusive, limit, desc, prefix, bucket.getKeyFirst(), bucket.getKeyLast());
				// 重发命中服务端去重存根返回 RaftApplied，结果完整可直接交付
				if (r.getResultCode() != 0 && r.getResultCode() != Zeze.Transaction.Procedure.RaftApplied)
					throw new RuntimeException("walkKey result=" + IModule.getErrorCode(r.getResultCode()));
				return deliverWalkPage(r, callback, Binary::copyIf);
			});
	}
}
