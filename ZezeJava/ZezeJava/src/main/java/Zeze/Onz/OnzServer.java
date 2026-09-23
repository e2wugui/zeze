package Zeze.Onz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Data;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.DaemonTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 开发onz服务器基础
 * <p>
 * 包装网络和onz协议，
 * 允许多个server实例，
 * 不同的server实例功能可以交叉也可以完全不同，
 */
public class OnzServer extends AbstractOnz {
	private static final @NotNull Logger logger = LogManager.getLogger(OnzServer.class);

	private final OnzAgent onzAgent;
	private final boolean sharedServiceManager;
	private final ConcurrentHashMap<String, AbstractAgent> zezes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> instances = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, OnzTransactionStub<?, ?>> remoteStubs = new ConcurrentHashMap<>();
	private final OnzServerService service;

	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	// 周期守护：redoTimer(迭代+RPC等待)进worker池不占调度线程；stop有界等待在飞一轮
	private final DaemonTimer redoDaemon = new DaemonTimer("OnzServer.redoTimer", 60_000, this::redoTimer);
	private final AbstractAgent myServiceManager;
	private final AutoKey onzTidAutoKey;

	// 生命周期（FND3-54）：stop后拒绝新工作；stop幂等；stop后不可再start（终态）。
	private volatile boolean stopped;
	// getZezeInstance的"选择→创建→登记"按名原子化（FND3-53）。
	private final ConcurrentHashMap<String, ReentrantLock> nameLocks = new ConcurrentHashMap<>();
	// redo轮次与database.close()互斥：stop()超预算逃逸的轮次仍可能在库上，直接关库会与
	// 遍历/写入commitPoint竞态。轮次内的网络等待只发生在有未决事务时（常态为空）。
	private final ReentrantLock dbLock = new ReentrantLock();

	public long nextOnzTid() {
		return onzTidAutoKey.next();
	}

	/** SM代理启动并等待就绪：raft版第一次等待由于选择leader原因肯定会失败一次。 */
	private static void startAgentAndWaitReady(AbstractAgent agent) throws Exception {
		agent.start();
		try {
			agent.waitReady();
		} catch (Exception ignored) {
			agent.waitReady();
		}
	}

	/** 构造/start失败回滚时的best-effort释放（对齐stop的分步容错口径），失败仅记日志。 */
	private static void rollbackClose(String what, java.io.Closeable resource) {
		if (resource == null)
			return;
		try {
			resource.close();
		} catch (Throwable e) {
			logger.error("rollback close {}", what, e);
		}
	}

	public void setWriteOptions(WriteOptions writeOptions) {
		this.writeOptions = writeOptions;
	}

	public WriteOptions getWriteOptions() {
		return writeOptions;
	}

	/**
	 * 每个zeze集群使用独立的ServiceManager实例时，使用这个方法构造OnzServer。
	 * 建议按这种方式配置，便于解耦。
	 * 此时zezes编码如下：
	 * zeze1=zeze1.xml;zeze2=zeze2.xml;...
	 * zeze1,zeze2是OnzServer自己对每个zeze集群的命名，以后用于Onz分布式事务的调用。需要唯一。
	 * zeze1.xml,zeze2.xml是不同zeze集群的配置文件path。
	 */
	public OnzServer(String zezeConfigs, Config myConfig) throws Exception {
		myServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (myServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		RocksDatabase db = null;
		try {
			startAgentAndWaitReady(myServiceManager);
			onzTidAutoKey = myServiceManager.getAutoKey("OnzServerTidAutoKey");

			db = new RocksDatabase("CommitOnzServer" + myConfig.getServerId());
			database = db;
			commitPoint = db.getOrAddTable("CommitPoint");
			commitIndex = db.getOrAddTable("CommitIndex");

			var zezesArray = zezeConfigs.split(";");
			for (var zeze : zezesArray) {
				var zezeNameAndConfig = zeze.split("=");
				if (zezeNameAndConfig.length != 2)
					throw new RuntimeException("error zezes=" + zezeConfigs);
				if (this.zezes.containsKey(zezeNameAndConfig[0]))
					throw new RuntimeException("duplicate zeze=" + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
				var zezeConfig = Config.load(zezeNameAndConfig[1]);
				var serviceManager = Application.createServiceManager(zezeConfig, "OnzServerServiceManager");
				if (serviceManager == null)
					throw new RuntimeException("serviceManager not found for " + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
				// 先登记再启动/订阅（FND4-89/FND5-48）：start/waitReady/订阅任一步失败时，回滚
				// 循环都要能找到这个可能已占用网络线程的实例——put在start之后会漏掉
				// "start成功但waitReady二次失败"窗口（FND4-89红测注入的null在start之前，恰好绕开）。
				this.zezes.put(zezeNameAndConfig[0], serviceManager);
				startAgentAndWaitReady(serviceManager);
				serviceManager.subscribeService(new BSubscribeInfo(Onz.eServiceName));
			}
			this.sharedServiceManager = false;

			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造的全有或全无（FND4-89）：逆序释放已获取资源——半途失败时stop()不可达
			// （对象未构造完成），不回收会泄漏网络线程、端口与RocksDB目录锁，阻碍同进程重试。
			// 各zeze的SM为独立实例（重名在启动前被拒），逐个close。
			for (var agent : zezes.values())
				rollbackClose("zeze agent", agent);
			rollbackClose("database", db);
			rollbackClose("myServiceManager", myServiceManager);
			throw ex;
		}
	}

	public void start() throws Exception {
		try {
			service.start();
			onzAgent.start();

			try {
				redoTimer();
			} catch (Exception ex) {
				logger.error("first try.", ex);
			}
			// 1 minute?
			redoDaemon.start();
		} catch (Throwable ex) {
			// start的全有或全无（FND4-89）：半途失败按停机路径回收已启动资源。
			// stop幂等且best-effort；此后对象为终态（stopped），与构造失败不逃逸同口径。
			try {
				stop();
			} catch (Throwable e) {
				logger.error("rollback start", e);
			}
			throw ex;
		}
	}

	// ePreparing最小redo年龄（FND5-44）：perform从saveCommitPoint(ePreparing)到txn.commit
	// 覆盖为eCommitting之间存在waitPendingAsync等待窗口（业务异步放大，时长不定），
	// redoTimer的iterator快照会捕获窗口内的ePreparing——不看年龄直接Rollback命中进行中
	// 事务：参与方回滚后对迟到Commit假应答成功（readyProcedures.remove为null直接
	// SendResult(0)），协调者perform返回0，静默部分提交。真残留（协调者崩溃重启）在
	// 超过年龄后的下一轮redo恢复，仅多时延。120s=2×redo周期，量级覆盖默认
	// flushTimeout(10s)量级的业务等待放大。
	private static final long RedoPreparingMinAgeMs = 120_000;

	// redo封锁告警去重（FND6-36可观测性）：登记中的ePreparing年龄超过2×RedoPreparingMinAgeMs
	// 仍存活时按tid只warn一次，防每轮redo刷屏。tid来自AutoKey单调递增，perform正常结束后
	// 不复用，集合有界于挂死perform数，无需清理。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> hangWarnedTids = ConcurrentHashMap.newKeySet();

	private void redoTimer() throws RocksDBException {
		if (stopped)
			return;
		dbLock.lock();
		try {
			if (stopped)
				return;
			try (var it = commitIndex.iterator()) {
				for (it.seekToFirst(); it.isValid(); it.next()) {
					var key = it.key();
					var value = it.value();
					var bb = ByteBuffer.Wrap(value);
					var state = bb.ReadUInt();
					var tid = ByteBuffer.ToLongBE(key, 0);
					// FND6-36（skip收窄）：先读状态再判登记，skip仅作用于登记中的ePreparing。
					// ePreparing的redo是Rollback，回滚不可逆：登记窗口从addTransaction覆盖到
					// finally removeTransaction，其中saveCommitPoint(ePreparing)→无界
					// waitPendingAsync是年龄闸挡不住的进行中窗口，误发Rollback回滚存活参与方后
					// 对迟到Commit假应答成功（readyProcedures.remove为null直接SendResult(0)），
					// perform静默全量回滚却返回0——必须skip。真残留只能源于进程崩溃（登记表
					// 与perform同进程同生共死：perform异常结束必经finally摘除登记，进程存活则
					// 登记必在），崩溃重启后onzAgent为空，skip天然放行；存活perform的finally
					// 摘除登记后下轮redo可见。年龄闸（FND5-44）只作用于未登记的ePreparing
					// （区分崩溃残留与刚落盘的窗口条目），登记中的条目与年龄无关。
					// eCommitting不做skip：其redo是幂等Commit重发（参与方已ready，重复Commit
					// 亦应答成功；且redo经getZezeInstance现查新地址），登记中执行也安全——它
					// 是perform的Commit散发通道socket僵死时的收敛通道，skip会把补发推迟到
					// perform的finally之后，多等一个perform生命周期。
					switch (state) {
					case eCommitting:
						redo(key, true);
						break;
					case ePreparing:
						// FND5-44：新格式值=state(varint)+写入时戳(8B BE)；旧格式（仅state，
						// 升级遗留的未决决策）读不到时戳视为年龄无穷——行为与修复前一致（立即redo）。
						var stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
						var age = System.currentTimeMillis() - stamp;
						if (onzAgent.hasTransaction(tid)) {
							// 可观测性：登记中却远超年龄窗口（2×RedoPreparingMinAgeMs）仍停在
							// ePreparing，基本是perform因业务bug永挂——登记项与commitIndex条目
							// 将永久泄漏且redo被其封锁，按tid只warn一次暴露（集合见hangWarnedTids）。
							if (age >= 2 * RedoPreparingMinAgeMs && hangWarnedTids.add(tid))
								logger.warn("onz redo: tid={} 登记中ePreparing年龄{}ms，疑似挂死 perform，redo 封锁中", tid, age);
							continue;
						}
						if (age >= RedoPreparingMinAgeMs)
							redo(key, false);
						// else：进行中窗口，等超过年龄后的下一轮
						break;
					}
				}
			}
		} finally {
			dbLock.unlock();
		}
	}

	private static TaskCompletionSource<EmptyBean.Data> commit(AsyncSocket socket, long tid) {
		var r = new Commit();
		r.Argument.setOnzTid(tid);
		return r.SendForWait(socket);
	}

	private static TaskCompletionSource<EmptyBean.Data> rollback(AsyncSocket socket, long tid) {
		var r = new Rollback();
		r.Argument.setOnzTid(tid);
		return r.SendForWait(socket);
	}

	// redo对saga参与方FuncSagaEnd的等待超时：参与方处理FuncSagaEnd与在途业务互斥
	//（businessLock），应答可能慢于rpc默认超时（慢业务场景）；本轮超时保留记录，
	// 每轮redo重试收敛，取小于redo周期(60s)的量级。
	private static final int RedoSagaEndTimeoutMs = 30_000;

	/** redo路径对saga参与方的FuncSagaEnd：cancel=决策为rollback（补偿）。 */
	private static TaskCompletionSource<Zeze.Builtin.Onz.BFuncSagaEndResult.Data> sagaEnd(
			AsyncSocket socket, long tid, boolean cancel) {
		var r = new FuncSagaEnd();
		r.Argument.setOnzTid(tid);
		r.Argument.setCancel(cancel);
		return r.SendForWait(socket, RedoSagaEndTimeoutMs);
	}

	// redo按决策与参与方类型分流（OH1-F1）：procedure参与方发Commit/Rollback；
	// saga参与方发FuncSagaEnd——commit决策补发endSaga未完成的结束(cancel=false)，
	// rollback决策补偿已提交的步骤(cancel=true)，参与方幂等。
	private void redo(byte[] key, boolean commitDecision) throws RocksDBException {

		var value = Objects.requireNonNull(commitPoint.get(key));
		var state = new BSavedCommits.Data();
		state.decode(ByteBuffer.Wrap(value));

		var zezeOnzs = new HashMap<String, Connector>();
		var tid = ByteBuffer.ToLongBE(key, 0);
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			for (var e : state.getOnzs()) {
				// saga参与方带前缀持久化（OH1-F1），其余为procedure参与方（含旧版本ip_port格式）。
				var sagaName = OnzTransaction.decodeSagaParticipant(e);
				var zezeName = sagaName != null ? sagaName : e;
				AsyncSocket socket;
				if (zezes.containsKey(zezeName)) {
					// 参与方按集群名持久化（FND4-90起）：现查当前地址——地址漂移后redo
					// 不再对死地址重试（连接器由instances缓存管理生命周期）。
					socket = getZezeInstance(zezeName);
				} else {
					// 旧版本持久化的ip_port（升级窗口遗留的未决决策）：按地址建连兜底。
					socket = openRedoConnection(zezeOnzs, zezeName).GetReadySocket();
				}
				if (sagaName != null)
					futures.add(sagaEnd(socket, tid, !commitDecision));
				else
					futures.add(commitDecision ? commit(socket, tid) : rollback(socket, tid));
			}
			for (var e : futures)
				e.await();
			removeCommitRecord(key);
		} catch (Throwable ex) {
			// timer will redo
			logger.error("", ex);
		} finally {
			for (var zeze : zezeOnzs.values())
				zeze.stop();
		}
	}

	void saveCommitPoint(byte[] tidBytes, BSavedCommits.Data bState, int state) throws RocksDBException {
		bState.setState(state);
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(13);
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis()); // FND5-44：ePreparing年龄判据，见redoTimer
		try (var batch = database.borrowBatch()) {
			// putIfAbsent ？？？ 报错！
			commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
			commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
			batch.commit(writeOptions);
		}
	}

	void removeCommitRecord(byte[] tidBytes) {
		try {
			// 两表同key生命周期（FND4-88）：索引删则点删，同一batch原子落地。
			// commitPoint只在redo（遍历commitIndex时requireNonNull读取）被消费，
			// 孤儿点条目永不被读还占磁盘——磁盘随事务数单调增长。
			try (var batch = database.borrowBatch()) {
				commitIndex.delete(batch, tidBytes);
				commitPoint.delete(batch, tidBytes);
				batch.commit(writeOptions);
			}
		} catch (RocksDBException e) {
			// 这个错误仅仅记录日志，所有没有删除的index，以后重启和Timer会尝试重做。
			logger.error("", e);
		}
	}

	private static Connector openRedoConnection(HashMap<String, Connector> conns, String ip_port) {
		var conn = conns.computeIfAbsent(ip_port, __ -> {
			var newConn = new Connector(ip_port, false);
			newConn.start();
			return newConn;
		});
		conn.GetReadySocket();
		return conn;
	}

	/**
	 * 停止OnzServer（幂等，可重入）。语义：不做优雅排空——在途事务可能失败，
	 * 未完成的补发记录（commitIndex）留在库中由下次进程启动的redo恢复；终态，不可再start。
	 * 停机为best-effort：任一步失败仅记error并继续——半途上抛会让幂等守卫把停机
	 * 永久卡在半途（库/代理无法补关），失败步骤由日志定位人工处理。
	 * 顺序：拒绝新工作 → 停定时器 → 停缓存connector（必须先于服务停止：
	 * 服务关socket会触发connector自动重连，停止后仍无限重连）→ close各SM代理
	 * （按identity去重，共享配置是同一实例）→ 停服务 → 最后关库（FND3-54）。
	 */
	public void stop() throws Exception {
		if (stopped)
			return; // 幂等
		stopped = true;

		redoDaemon.stop(); // 有界等待在飞一轮（预算=timeoutMs+5s）

		// redo轮次在dbLock内遍历/写入库；持有dbLock直到关库完成，
		// 与超预算逃逸/迟到的轮次互斥（迟到轮次在锁内检查stopped返回）。
		dbLock.lock();
		try {
			// 停缓存connector并清表：它们挂在onzAgent的服务上，必须在其停止前显式停掉
			// 自动重连，否则服务关socket反而触发无限重连（1s起、上限8s）。
			for (var connector : instances.values()) {
				try {
					connector.stop();
				} catch (Throwable e) { // logger.error
					logger.error("stop connector {}", connector.getName(), e);
				}
			}
			instances.clear();

			// close各zeze的SM代理（Agent.close停client/tid128/线程；raft版停loginFuture与raftClient）。
			var closedAgents = Collections.newSetFromMap(new IdentityHashMap<AbstractAgent, Boolean>());
			for (var agent : zezes.values()) {
				if (!closedAgents.add(agent))
					continue;
				try {
					agent.close();
				} catch (Throwable e) { // logger.error
					logger.error("close ServiceManager agent", e);
				}
			}
			try {
				myServiceManager.close();
			} catch (Throwable e) { // logger.error
				logger.error("close myServiceManager", e);
			}

			try {
				onzAgent.stop();
			} catch (Throwable e) { // logger.error
				logger.error("stop onzAgent", e);
			}
			try {
				service.stop();
			} catch (Throwable e) { // logger.error
				logger.error("stop service", e);
			}

			try {
				database.close();
			} catch (Throwable e) { // logger.error
				logger.error("close database", e);
			}
		} finally {
			dbLock.unlock();
		}
	}

	/**
	 * 所有zeze集群共享同一个ServiceManager实例时，使用这个构造函数。
	 * 共享配置时，每个zeze集群需要额外的唯一名配置，并且把它拼接到ServiceManager的注册参数中。
	 *
	 * @param sharedZezeConfig 共享的ServiceManager配置
	 * @param specialZezeNames 共享配置时，已经配置成不同的zeze集群的唯一名字的列表，OnzServer不再自定义命名。
	 */
	public OnzServer(String sharedZezeConfig, String specialZezeNames, Config myConfig) throws Exception {
		myServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (myServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		AbstractAgent sharedAgent = null;
		RocksDatabase db = null;
		try {
			startAgentAndWaitReady(myServiceManager);
			onzTidAutoKey = myServiceManager.getAutoKey("OnzServerTidAutoKey");

			db = new RocksDatabase("CommitOnzServer" + myConfig.getServerId());
			database = db;
			commitPoint = db.getOrAddTable("CommitPoint");
			commitIndex = db.getOrAddTable("CommitIndex");

			var config = Config.load(sharedZezeConfig);
			sharedAgent = Application.createServiceManager(config, "OnzServerServiceManager");
			if (sharedAgent == null)
				throw new RuntimeException("create ServiceManager fail. " + sharedZezeConfig);
			startAgentAndWaitReady(sharedAgent);
			var zezeArray = specialZezeNames.split(";");
			for (var zeze : zezeArray) {
				if (this.zezes.containsKey(zeze))
					throw new RuntimeException("duplicate zeze=" + zeze + " zezes=" + specialZezeNames);
				this.zezes.put(zeze, sharedAgent);
			}
			this.sharedServiceManager = true;
			sharedAgent.subscribeService(new BSubscribeInfo(Onz.eServiceName));
			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造的全有或全无（FND4-89）：逆序释放——共享SM（zezes各值同一实例，只关一次）
			// → 库 → myServiceManager。
			rollbackClose("shared agent", sharedAgent);
			rollbackClose("database", db);
			rollbackClose("myServiceManager", myServiceManager);
			throw ex;
		}
	}

	public OnzAgent getOnzAgent() {
		return onzAgent;
	}

	public AsyncSocket getZezeInstance(String zezeName) {
		if (stopped)
			throw new RuntimeException("OnzServer stopped");

		// find connected
		var connector = instances.get(zezeName);
		if (null != connector) {
			var socket = connector.TryGetReadySocket();
			if (null != socket)
				return socket;
		}

		// find from serviceManager
		var zeze = zezes.get(zezeName);
		if (null == zeze)
			throw new RuntimeException("unknown zeze=" + zezeName);

		var onzSmName = sharedServiceManager ? zezeName : Onz.eServiceName;
		var onzServices = zeze.getSubscribeStates().get(onzSmName);
		if (null == onzServices)
			throw new RuntimeException("serviceManager subscribe not found. " + zezeName);

		// "选择→创建→登记"按名原子化（FND3-53）：无同步时并发冷路径互相stop对方的connector
		// （GetReadySocket等待者收到异常，事务假性失败），重连窗口每个新请求都杀死上一个
		// 正在握手的尝试（churn，连接永远建立不起来）。
		var nameLock = nameLocks.computeIfAbsent(zezeName, __ -> new ReentrantLock());
		nameLock.lock();
		try {
			// double-check：并发者可能刚刚创建并连上。
			connector = instances.get(zezeName);
			if (null != connector) {
				var socket = connector.TryGetReadySocket();
				if (null != socket)
					return socket;
				if (isAdvertised(onzServices, connector.getName()))
					// 目标未变：连接/重连进行中，等待就绪。此时替换会stop正在握手的连接，
					// 杀死并发等待者并重置重连退避——只有目标真的变化才允许替换。
					return connector.GetReadySocket();
				// 目标已变（SM不再通告当前地址）：走到下面替换。
			}

			if (stopped)
				throw new RuntimeException("OnzServer stopped");

			var serviceInfos = onzServices.getServiceInfos(0);
			if (serviceInfos == null)
				throw new RuntimeException("create connector fail. " + zezeName);
			var identities = serviceInfos.getSortedIdentities();
			if (identities.isEmpty())
				throw new RuntimeException("no advertised service. " + zezeName);
			var onzService = identities.getFirst();
			connector = new Connector(onzService.getPassiveIp(), onzService.getPassivePort());
			connector.SetService(onzAgent.getService());
			connector.start();
			var old = instances.put(zezeName, connector);
			if (old != null && old != connector)
				old.stop(); // 旧地址不再被通告：停止其僵尸重连；等待者得到的是"目标已失效"的真实失败。
			if (stopped) {
				// stop()在本方法的创建窗口完成（已清空instances）：撤销刚创建的连接器，
				// 不留自动重连的僵尸。stopped是stop()的第一步，晚于清空落地的put必然可见它。
				instances.remove(zezeName, connector);
				connector.stop();
				throw new RuntimeException("OnzServer stopped");
			}
			return connector.GetReadySocket();
		} finally {
			nameLock.unlock();
		}
	}

	/** connector目标（ip_port）是否仍在SM的通告名单内。 */
	private static boolean isAdvertised(@NotNull AbstractAgent.SubscribeState onzServices,
										@NotNull String connectorName) {
		var serviceInfos = onzServices.getServiceInfos(0);
		if (serviceInfos == null)
			return false;
		for (var identity : serviceInfos.getSortedIdentities())
			if ((identity.getPassiveIp() + "_" + identity.getPassivePort()).equals(connectorName))
				return true;
		return false;
	}

	/**
	 * 独立进程运行OnzServer时需要注册。
	 * 嵌入时不用注册。
	 */
	public <A extends Data, R extends Data> void register(Class<OnzTransaction<?, ?>> txnClass,
														  Class<A> argumentClass, Class<R> resultClass) {
		if (null != remoteStubs.putIfAbsent(txnClass.getName(),
				new OnzTransactionStub<>(this, argumentClass, resultClass)))
			throw new RuntimeException("duplicate OnzTransaction Name=" + txnClass.getName());
	}

	/**
	 * Class.forName＆set ; 主动创建并且控制txn的初始化。
	 * 由于嵌入时，本地是知道className的，可以直接new出来，
	 * 以获得更大灵活度。
	 */
	public static <A extends Data, R extends Data> OnzTransaction<A, R> createTransaction(
			String name, OnzServer onzServer, A argument, R result) throws Exception {

		@SuppressWarnings("unchecked")
		var cls = (Class<OnzTransaction<A, R>>)Class.forName(name);
		var txn = cls.getConstructor((Class<?>[])null).newInstance((Object[])null);
		txn.setOnzServer(onzServer);
		txn.setArgument(argument);
		txn.setResult(result);
		return txn;
	}

	/**
	 * 执行onz分布式事务。
	 * <p>
	 * 1. 自行决定txn的创建和初始化。
	 * 2. 可以不通过A,R结构传递参数和结果，完全自定义实现。
	 * 3. 设置其他onz事务的控制参数。如flushMode,flushTimeout等。
	 */
	public long perform(OnzTransaction<?, ?> txn) {
		if (stopped) {
			logger.error("perform on stopped OnzServer");
			return Procedure.Exception; // 尚未开始执行，无需rollback
		}
		try {
			onzAgent.addTransaction(txn);
			var rc = txn.perform();
			var state = txn.buildSavedCommits();
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			saveCommitPoint(tidBytes, state, ePreparing);
			// 这里和下面的txn.Commit分成两步saveCommitPoint，
			// 实际上这中间没有做太多额外的事情，可以考虑合并成异步，
			// 但为了明确两个事务状态，仍然分开。原因如下：
			// 参考Dbh2的两步：由于Dbh2一开始就知道所有的服务器，所以可以一开始就保存一次ePreparing，
			// 而这上面的perform是便执行边产生服务器地址，无法一开始保存事务状态。
			// 最严格的做法是每产生一个服务器地址，就写一次ePreparing（包含所有的服务器地址）。
			// 现在先简单处理为：等待perform完成。
			if (0 == rc) {
				txn.waitPendingAsync();
				// pendingAsync窗口内注册的参与方不进上面的ePreparing快照（OH1-F4）：窗口后
				// 重建快照再传给commit——txn.commit内saveCommitPoint(eCommitting)持久化的
				// 参与方列表完整，崩溃/commitFail后redo补发不缺迟到参与方（迟到者等不到
				// Commit会ready超时自愈回滚，与已报成功的协调者分歧）。592行的ePreparing快照
				// 保持不动：窗口内落盘可观察是FND5-44/FND6-36回归契约与redo所有权门控的依赖，
				// 其内容不全无害（崩溃走redo(rollback)整体回滚一致）。
				state = txn.buildSavedCommits();
				txn.commit(tidBytes, state);
				txn.waitFlushDone();
				return 0;
			}
			txn.rollback();
			return rc;

		} catch (Throwable ex) {
			txn.rollback();
			logger.error("", ex);
			return Procedure.Exception;

		} finally {
			onzAgent.removeTransaction(txn);
		}
	}

	@Override
	protected long ProcessCheckpointRequest(Checkpoint r) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessCommitRequest(Commit r) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessFuncProcedureRequest(FuncProcedure r) throws Exception {
		// 这个本来是嵌入zeze的组件Onz的处理协议，直接拿来作为OnzServer的远程调用，够用，还超了一点。
		var stub = remoteStubs.get(r.Argument.getFuncName());
		if (stub == null)
			return errorCode(eProcedureNotFound);

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
		var txn = stub.createTransaction(r.Argument.getFuncName(), buffer);
		var rc = perform(txn);
		if (0 != rc)
			return rc;

		var bbResult = ByteBuffer.Allocate();
		txn.getResult().encode(bbResult);
		r.Result.setFuncResult(new Binary(bbResult));
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessFuncSagaRequest(FuncSaga r) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessFuncSagaEndRequest(FuncSagaEnd r) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessRollbackRequest(Rollback r) {
		throw new UnsupportedOperationException();
	}
}
