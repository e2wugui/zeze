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
import Zeze.Util.TaskSpec;
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
	// 死桶重定向agent的闲置回收（dbh2-06）：putBuckets只回收"离开主表"的raft，经splitHistory链式
	// 重定向打开的死桶agent不在任何主表缓存里，既有路径永不回收（连接器+每实例1s resend任务+
	// pending表单调泄漏）。弱生命周期：openBucket刷新活跃时刻；周期扫描回收"不在任何已知主表
	// 缓存&&闲置超阈值"的agent。误杀防护见reclaimIdleAgents。
	private final ConcurrentHashMap<String, Long> agentActiveTimes = new ConcurrentHashMap<>();
	private static final long IdleAgentReclaimTimeoutMs = 10 * 60_000L; // >>rpcTimeout(默认60s)：正常在飞请求不可能跨越阈值窗口
	private static final long IdleAgentReclaimPeriodMs = 60_000L;
	private volatile Future<?> idleReclaimTask;

	private final ProxyAgent proxyAgent;
	private final Config config;
	private final Dbh2Config dbh2Config = new Dbh2Config();
	private Commit commit;
	private CommitAgent commitAgent;
	private volatile Future<?> refreshMasterTableTask; // 任务线程会置null（见startRefreshMasterTable），需要可见性
	// stop后置位且不可复位（本类无重启路径）。后台刷新任务体整体持本管理器锁与stop串行：
	// stopped的检查-执行与stop的清理互斥，锁外检查会让stop在检查后清理前重建MasterAgent并回填buckets。
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
						// 整体持锁与stop串行：stopped检查到openMasterAgent/reload之间不允许插入stop。
						lock();
						try {
							if (stopped)
								return;
							try {
								reload(openMasterAgent(masterName), masterName, databaseName, tableName);
							} catch (Exception e) {
								// 刷新失败可容忍：下次PrepareBatch拒绝会重新触发本刷新，自愈。
								logger.warn("refresh master table fail. master={} database={} table={}",
										masterName, databaseName, tableName, e);
							}
						} finally {
							// 失败也必须复位：置null不能只在reload成功路径执行：reload抛异常
							//（getBuckets对master短暂不可达即抛）时任务体异常完结，下面的复位若被跳过，
							// startRefreshMasterTable的门槛if(null!=refreshMasterTableTask)对一个早已
							// 完结的Future永久成立，之后所有拒绝触发的刷新成为no-op直到进程重启——
							// 路由缓存陈旧（每笔多付一轮refused→redirect）且死桶agent不再回收。
							// 置null与startRefreshMasterTable的检查-调度同锁原子，避免与重新调度交错。
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
		if (config.isDbh2LocalCommit()) {
			if (null == commit) {
				commit = new Commit(this, config);
			}
		} else {
			// fail-fast：远程提交模式必须显式配置Dbh2Config的CommitServerAddress（单实例），
			// 构造期报配置错误，不等第一次commit才失败。
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

	public KV<String, Integer> commitServiceAcceptor() {
		// KV.key构造后不变：累积首个acceptor后create，不再setKey。
		var ip = new String[1];
		var port = new int[1];
		commit.getService().getConfig().forEachAcceptor2((a) -> {
			ip[0] = a.getIp();
			port[0] = a.getPort();
			return false;
		});
		if (ip[0] == null || port[0] == 0)
			throw new RuntimeException("Commit Query Acceptor Not Set.");
		return KV.create(ip[0], port[0]);
	}

	public void commitBreakAfterPrepareForDebugOnly(BPrepareBatches.Data batches) {
		if (config.isDbh2LocalCommit()) {
			var query = commitServiceAcceptor();
			var state = CommitRocks.buildTransactionState(batches);
			commit.getRocks().prepare(query.getKey(), query.getValue(), state, batches, null);
		}
	}

	private KV<String, Integer> choiceCommitServer() {
		// 配置直连：Dbh2Config的CommitServerAddress，单实例；构造期已fail-fast校验过配置存在。
		return KV.create(dbh2Config.getCommitServerHost(), dbh2Config.getCommitServerPort());
	}

	public void commit(BPrepareBatches.Data batches) {
		if (config.isDbh2LocalCommit()) {
			var query = commitServiceAcceptor();
			commit.getRocks().commit(query.getKey(), query.getValue(), batches);
			return;
		}
		var query = choiceCommitServer();
		commitAgent.commit(query.getKey(), query.getValue(), batches, dbh2Config.getRpcTimeout());
	}

	// Dbh2Agent 嵌入服务器需要初始化；
	// CommitServer 独立服务器需要初始化；
	public void start() throws Exception {
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
			stopped = true;
			var task = refreshMasterTableTask;
			if (null != task)
				task.cancel(false); // 未启动的直接作废；已启动的在途轮由任务体锁内的stopped检查兜底
			refreshMasterTableTask = null;
			ShutdownHook.remove(this);
			// 逐项独立捕获+收尾重抛首个异常（对齐Dbh2Manager.stop逐桶close形态）：任一环节
			// 失败不得中断其余清理——未stop的MasterAgent保留连接器与重连，未close的Dbh2Agent
			// 保留1s周期resend任务、连接器与pending rpc表（嵌入式/测试反复create-stop累积泄漏）。
			// 首个异常在全部清理完成后重抛，保留stop失败的调用方可见性。
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
					reclaim.cancel(false); // 在途轮由任务体锁内的stopped检查兜底
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
				// 兜底清账（Error级逃逸也必须执行）：masterAgent/agents/agentActiveTimes残留
				// 已停条目，buckets清空路由缓存——stop后masterAgent已关，locateBucket不得
				// 命中陈旧缓存免rpc。
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
		// stopped门禁（对齐openBucket）：stop清理后本类不可重启，此后新建的MasterAgent（连接器+
		// 重连）无人回收必泄漏。构造与连接等待仍在锁外（对齐locateBucket自立的约束：阻塞的连接
		// 等待不能持管理器锁，会阻塞同锁的其他操作，最长约READY_TIMEOUT），故构造前先查一次，
		// putIfAbsent前在锁内复查：构造期间发生stop则回收自建agent并失败。
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
				newAgent.stop(); // 构造期间已stop：回收自建agent（连接器+重连），不得泄漏
				throw new IllegalStateException("Dbh2AgentManager stopped.");
			}
			var old = masterAgent.putIfAbsent(masterName, newAgent);
			if (null != old) {
				newAgent.stop(); // 竞态败者：已启动的连接器必须回收，不得泄漏
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
		var table = database.get(tableName);
		if (null == table) {
			// getBuckets是阻塞RPC，不能放进computeIfAbsent的映射函数（持bin锁会阻塞同bin其他键的访问）；重复RPC幂等无害。
			table = masterAgent.getBuckets(databaseName, tableName);
			var old = database.putIfAbsent(tableName, table);
			if (null != old)
				table = old;
		}
		return table.locate(key).getRaftConfig();
	}

	public Iterator<BBucketMeta.Data> locateBucketIterator(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName,
			Binary key, boolean desc) {
		var master = buckets.computeIfAbsent(masterName, __ -> new ConcurrentHashMap<>());
		var database = master.computeIfAbsent(databaseName, __ -> new ConcurrentHashMap<>());
		var table = database.get(tableName);
		if (null == table) {
			// 同locateBucket：阻塞RPC不能放进computeIfAbsent的映射函数。
			table = masterAgent.getBuckets(databaseName, tableName);
			var old = database.putIfAbsent(tableName, table);
			if (null != old)
				table = old;
		}
		// 从key所在桶（含）开始迭代：asc向后（keyFirst递增），desc向前（keyFirst递减）。
		// desc且key为空表示从表尾开始：直接全表降序。
		if (desc)
			return key.size() == 0
					? table.getBuckets().descendingMap().values().iterator()
					: table.getBuckets().headMap(key, true).descendingMap().values().iterator();
		return table.tailMap(key).values().iterator();
	}

	public Dbh2Agent openBucket(String raftString) {
		Dbh2Agent agent;
		// 整体持管理器锁（与stop互斥，dbh2-04）：stop已清理agents并停止回收任务，此后新建的
		// agent（每秒resend任务+连接器）无人回收，必须拒绝。
		lock();
		try {
			if (stopped)
				throw new IllegalStateException("Dbh2AgentManager stopped.");
			// 与回收路径（reclaimIdleAgents/putBuckets/stop均持本锁先remove后close，dbh2-02）
			// 原子互斥：不持锁时computeIfAbsent返回与并发回收交错会把已close的agent交给调用方，
			// 其上sendForWait因resendTask已取消且await无超时兜底而永久悬挂。Dbh2Agent构造无
			// 阻塞IO，持锁创建代价可忽略。
			agent = agents.computeIfAbsent(raftString, _raft -> {
				logger.info("openBucket: new Dbh2Agent: {}", raftString);
				try {
					return new Dbh2Agent(raftString, proxyAgent);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			// 闲置回收的活跃刷新（与回收同锁原子，不留孤儿条目）：每次使用（含复用命中）都更新。
			// 本管理器全部使用方（walkPage、CommitRocks的prepare/commit/undo/redirect、Database.find）
			// 均按次经openBucket取agent、无跨长时间持有agent引用的路径——"最近用过"的agent必然未超闲置阈值。
			agentActiveTimes.put(raftString, System.currentTimeMillis());
		} finally {
			unlock();
		}
		startIdleReclaim();
		return agent;
	}

	// 周期回收惰性启动：无agent即无扫描任务（首个openBucket启动）；stop取消。
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

	// 误杀防护（三重）：
	//  1) 主表成员永不回收——主表缓存内全部raft的并集视为在役，其存续由putBuckets的旧raft回收
	//     路径负责（与迁移联动，语义不变）；
	//  2) 在用保护——openBucket刷新活跃时刻，阈值(10min)内使用过的agent不回收；且无跨长时间
	//     持有agent的调用方（见openBucket注释），"闲置超10min"即"确无使用"；
	//  3) 在飞兜底——即使极端交错下回收了仍有pending的agent：Dbh2Agent.close→Agent.stop对
	//     pending rpc以RpcTimeoutException终局触发，await方得到异常而非悬挂，事务按既有失败
	//     路径重试并经openBucket重开（同raft串重建agent），最坏代价是一笔在飞请求失败重试。
	// 本地表陈旧时重定向目标（仍是活桶）可能暂不在主表缓存：闲置10min后被回收属正确弱生命周期
	// （下次使用重建，仅多一次建连）；链式迁移的已死中间桶则被永久回收，不再泄漏。
	private void reclaimIdleAgents() {
		lock();
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
						agent.close(); // Agent.stop取消resendTask、触发pending终局、回收连接器（只读核对过）
					} catch (Exception ex) {
						logger.error("reclaim idle agent close fail: " + raft, ex);
					}
				}
			}
		} finally {
			unlock();
		}
	}

	public void reload(
			MasterAgent masterAgent, String masterName,
			String databaseName, String tableName) {
		lock();
		try {
			var masterTable = masterAgent.getBuckets(databaseName, tableName);
			logger.info("reload ... {}", masterTable, new RuntimeException());
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
	private static final class FetchResult {
		final boolean refused;
		final boolean bucketEnd;
		final int count;
		final Binary lastKey;

		FetchResult(boolean refused, boolean bucketEnd, int count, Binary lastKey) {
			this.refused = refused;
			this.bucketEnd = bucketEnd;
			this.count = count;
			this.lastKey = lastKey;
		}
	}

	private static final FetchResult REFUSED = new FetchResult(true, false, 0, null);

	@FunctionalInterface
	private interface PageFetcher {
		FetchResult fetch(Dbh2Agent agent, Binary exclusiveKey, int limit) throws Exception;
	}

	// 抓取一页（可跨桶）：从exclusiveKey起按方向交付最多proposeLimit条（交付由fetcher执行），
	// 返回下一页游标；null表示全表走完。
	private ByteBuffer walkPage(MasterAgent masterAgent, String masterName, String databaseName, String tableName,
								Binary exclusiveKey, int proposeLimit, boolean desc, byte @Nullable [] prefix,
								PageFetcher fetch) throws Exception {
		var bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey, desc);
		if (!bucketIt.hasNext())
			return null;
		var bucket = bucketIt.next();
		var limit = proposeLimit;
		// refused重定向上限256次（bf8923edc自2放宽；Dbh2Table.find仍为2的先例形态）：master侧表
		// 长期陈旧时reload不收敛，无上限会永久自旋占线程与rpc配额。计数只累计连续refused，
		// fetch成功即清零——长遍历中途多次真实分桶各自获得新预算，不受累计误伤。
		var refusedCount = 0;
		while (true) {
			var exclusiveForBucket = bucketExclusive(exclusiveKey, bucket, desc);
			if (exclusiveForBucket == null) {
				// 桶整体已在游标走过的范围：跳过。
				if (!bucketIt.hasNext())
					return null; // no more bucket
				bucket = bucketIt.next();
				continue;
			}
			var result = fetch.fetch(openBucket(bucket.getRaftConfig()), exclusiveForBucket, limit);
			if (result.refused) {
				if (++refusedCount > 255)
					throw new RuntimeException("walkPage bucket refused too many redirect: master="
							+ masterName + " database=" + databaseName + " table=" + tableName
							+ " refusedCount=" + refusedCount);
				// 分桶但是本地信息没有更新会出现这种情况，此时重新装载桶的信息，再次定位。
				reload(masterAgent, masterName, databaseName, tableName);
				bucketIt = locateBucketIterator(masterAgent, masterName, databaseName, tableName, exclusiveKey, desc);
				if (bucketIt.hasNext()) {
					bucket = bucketIt.next();
					continue; // refused and redirect success.
				}
				return null; // no more bucket
			}
			refusedCount = 0;
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
				return null; // no more bucket
			bucket = bucketIt.next();
		}
	}

	public long walk(MasterAgent masterAgent,
					 String masterName, String databaseName, String tableName,
					 TableWalkHandleRaw callback,
					 boolean desc,
					 byte @Nullable [] prefix) throws Exception {
		var total = new long[1];
		var exclusiveKey = Binary.Empty;
		while (true) {
			var cursor = walkPage(masterAgent, masterName, databaseName, tableName, exclusiveKey, 5000, desc, prefix,
					(agent, exclusive, limit) -> {
						var r = agent.walk(exclusive, limit, desc, prefix);
						if (r.getResultCode() != 0)
							throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
						if (r.Result.isBucketRefuse())
							return REFUSED;
						Binary lastKey = null;
						for (var keyValue : r.Result.getKeyValues()) {
							callback.handle(keyValue.getKey().bytesUnsafe(), keyValue.getValue().bytesUnsafe());
							lastKey = keyValue.getKey();
						}
						total[0] += r.Result.getKeyValues().size();
						return new FetchResult(false, r.Result.isBucketEnd(), r.Result.getKeyValues().size(), lastKey);
					});
			if (cursor == null)
				return total[0];
			exclusiveKey = new Binary(cursor);
		}
	}

	public ByteBuffer walk(MasterAgent masterAgent,
						   String masterName, String databaseName, String tableName,
						   ByteBuffer exclusiveStartKey, int proposeLimit,
						   TableWalkHandleRaw callback,
						   boolean desc,
						   byte @Nullable [] prefix) throws Exception {
		return walkPage(masterAgent, masterName, databaseName, tableName,
				exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty, proposeLimit, desc, prefix,
				(agent, exclusive, limit) -> {
					var r = agent.walk(exclusive, limit, desc, prefix);
					if (r.getResultCode() != 0)
						throw new RuntimeException("walk result=" + IModule.getErrorCode(r.getResultCode()));
					if (r.Result.isBucketRefuse())
						return REFUSED;
					Binary lastKey = null;
					for (var keyValue : r.Result.getKeyValues()) {
						callback.handle(keyValue.getKey().copyIf(), keyValue.getValue().copyIf());
						lastKey = keyValue.getKey();
					}
					return new FetchResult(false, r.Result.isBucketEnd(), r.Result.getKeyValues().size(), lastKey);
				});
	}

	public long walkKey(MasterAgent masterAgent,
						String masterName, String databaseName, String tableName,
						TableWalkKeyRaw callback,
						boolean desc,
						byte @Nullable [] prefix) throws Exception {
		var total = new long[1];
		var exclusiveKey = Binary.Empty;
		while (true) {
			var cursor = walkPage(masterAgent, masterName, databaseName, tableName, exclusiveKey, 5000, desc, prefix,
					(agent, exclusive, limit) -> {
						var r = agent.walkKey(exclusive, limit, desc, prefix);
						if (r.getResultCode() != 0)
							throw new RuntimeException("walkKey result=" + IModule.getErrorCode(r.getResultCode()));
						if (r.Result.isBucketRefuse())
							return REFUSED;
						Binary lastKey = null;
						for (var key : r.Result.getKeys()) {
							callback.handle(key.bytesUnsafe());
							lastKey = key;
						}
						total[0] += r.Result.getKeys().size();
						return new FetchResult(false, r.Result.isBucketEnd(), r.Result.getKeys().size(), lastKey);
					});
			if (cursor == null)
				return total[0];
			exclusiveKey = new Binary(cursor);
		}
	}

	public ByteBuffer walkKey(MasterAgent masterAgent,
							  String masterName, String databaseName, String tableName,
							  ByteBuffer exclusiveStartKey, int proposeLimit,
							  TableWalkKeyRaw callback,
							  boolean desc,
							  byte @Nullable [] prefix) throws Exception {
		return walkPage(masterAgent, masterName, databaseName, tableName,
				exclusiveStartKey != null ? new Binary(exclusiveStartKey) : Binary.Empty, proposeLimit, desc, prefix,
				(agent, exclusive, limit) -> {
					var r = agent.walkKey(exclusive, limit, desc, prefix);
					if (r.getResultCode() != 0)
						throw new RuntimeException("walkKey result=" + IModule.getErrorCode(r.getResultCode()));
					if (r.Result.isBucketRefuse())
						return REFUSED;
					Binary lastKey = null;
					for (var key : r.Result.getKeys()) {
						callback.handle(key.copyIf());
						lastKey = key;
					}
					return new FetchResult(false, r.Result.isBucketEnd(), r.Result.getKeys().size(), lastKey);
				});
	}
}
