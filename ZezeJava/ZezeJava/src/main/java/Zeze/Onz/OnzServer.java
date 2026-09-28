package Zeze.Onz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Builtin.Onz.BCommit;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Builtin.Onz.Checkpoint;
import Zeze.Builtin.Onz.Commit;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Builtin.Onz.FuncSagaEnd;
import Zeze.Builtin.Onz.Rollback;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Rpc;
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

	// 生命周期：stop后拒绝新工作；stop幂等；stop后不可再start（终态）。
	private volatile boolean stopped;
	// getZezeInstance的"选择→创建→登记"按名原子化。
	private final ConcurrentHashMap<String, ReentrantLock> nameLocks = new ConcurrentHashMap<>();
	// redo轮次与database.close()互斥：stop()超预算逃逸的轮次仍可能在库上，直接关库会与
	// 遍历/写入commitPoint竞态。轮次内的网络等待只发生在有未决事务时（常态为空）。
	// perform的写库点（saveCommitPoint/removeCommitRecord）同入dbLock域：
	// 锁内只含毫秒级写库操作，不含perform业务窗口（那会把长业务与redo串行化）。
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
				// 先登记再启动/订阅：start/waitReady/订阅任一步失败时，回滚
				// 循环都要能找到这个可能已占用网络线程的实例——put在start之后会漏掉
				// "start成功但waitReady二次失败"窗口。
				this.zezes.put(zezeNameAndConfig[0], serviceManager);
				startAgentAndWaitReady(serviceManager);
				serviceManager.subscribeService(new BSubscribeInfo(Onz.eServiceName));
			}
			this.sharedServiceManager = false;

			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造的全有或全无：逆序释放已获取资源——半途失败时stop()不可达
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
			redoDaemon.start();
		} catch (Throwable ex) {
			// start的全有或全无：半途失败按停机路径回收已启动资源。
			// stop幂等且best-effort；此后对象为终态（stopped），与构造失败不逃逸同口径。
			try {
				stop();
			} catch (Throwable e) {
				logger.error("rollback start", e);
			}
			throw ex;
		}
	}

	// ePreparing最小redo年龄：perform从saveCommitPoint(ePreparing)到txn.commit
	// 覆盖为eCommitting之间存在waitPendingAsync等待窗口（业务异步放大，时长不定），
	// redoTimer的iterator快照会捕获窗口内的ePreparing——不看年龄直接Rollback命中进行中
	// 事务：参与方回滚后对迟到Commit假应答成功（readyProcedures.remove为null直接
	// SendResult(0)），协调者perform返回0，静默部分提交。真残留（协调者崩溃重启）在
	// 超过年龄后的下一轮redo恢复，仅多时延。120s=2×redo周期，量级覆盖默认
	// flushTimeout(10s)量级的业务等待放大。
	private static final long RedoPreparingMinAgeMs = 120_000;

	// redo封锁告警去重：登记中的ePreparing年龄超过2×RedoPreparingMinAgeMs
	// 仍存活时按tid只warn一次，防每轮redo刷屏。perform结束（finally）即回收tid，
	// 集合有界于挂死perform数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> hangWarnedTids = ConcurrentHashMap.newKeySet();

	// 超龄NotFound分诊预算：对齐参与方sagaContextTimeoutMs默认值——协调者
	// 无从得知各参与方的实际TTL配置，按默认预算分诊（联动契约推导见Onz.sagaContextTimeoutMs）。
	// rollback决策记录年龄≥本预算的eSagaNotFound按恶性升格error并保留记录。
	private static final long SagaNotFoundAgedBudgetMs = Onz.eDefaultSagaContextTimeoutMs;

	// 超龄NotFound告警去重：分诊保留的决策记录每轮redo重发都会再次
	// NotFound，按tid只error一次防刷屏（对齐hangWarnedTids形态）。决策记录收敛
	// 删除时回收tid，集合有界于在库的超龄未决决策数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> agedNotFoundWarnedTids = ConcurrentHashMap.newKeySet();

	// 未知state告警去重：commitIndex值可解但state∉{ePreparing,eCommitting}的
	// 条目（损坏但未截断的垃圾值/未来版本前向写入降级运行）不会被redo收敛删除，
	// 只静默跳过则零信号（同函数hang/超龄NotFound/毒值均有告警，唯此支没有）。按tid
	// 只error一次（对齐agedNotFoundWarnedTids形态）。
	// tid不回收，集合有界于在库的未知state条目数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> unknownStateWarnedTids = ConcurrentHashMap.newKeySet();

	// redo结果错误告警去重：非0非eSagaNotFound应答保留的决策记录每轮redo
	// 重发、确定性补偿失败每轮再现——按tid只error一次防刷屏（对齐agedNotFound
	// WarnedTids形态）。记录收敛删除时回收tid，集合有界于在库的失败重试决策数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> redoResultWarnedTids = ConcurrentHashMap.newKeySet();

	// redo整体失败分诊预算：redo参与者循环内任何异常（按名解析
	// unknown zeze/subscribe not found/no advertised service——集群除名、无通告；
	// openRedoConnection的GetReadySocket满时超时——旧格式死地址；点表
	// requireNonNull/decode——缺失/错配/损坏毒值；futures await超时——参与方僵死）
	// 全部落入redo唯一catch，走不到任何告警集合的add点：对settle守卫不可见，
	// 确定性滞留（地址漂移后每轮建连失败、除名集群按名解析恒抛）每60s重放一条带栈
	// error（每tid每天1440条）且永不可清算——settle拒绝文案指引"等下一轮redo≤60s
	// 重新分诊"，该指引对此类永不兑现（其redo路径永远到不了集合add点）。记录年龄
	// （redo既有stamp入参，零新状态）≥本预算的本轮异常才登记第四集合并error一次。
	// 年龄即持续失败时长的下界证据（无需失败计数器）：redoTimer每轮尝试所有在库
	// 记录，非异常轮的结局只有三种——记录删除（removeOk）/入agedNotFound/入
	// redoResult，unknownState在到达redo前已分流——因此"年龄≥门槛且本轮异常"
	// ⇒自写入起每轮均未收敛。取值对齐SagaNotFoundAgedBudgetMs（1h）：语义独立
	// （参与方TTL预算 vs 持续失败分诊），数值同量级；下限约束=远超在飞窗口
	// （eCommitting的commit/flush有界等待为秒级，await失败走fatal后perform正常
	// 返回交redo接管；ePreparing在飞由hasTransaction skip先于redo分流）与redo
	// 周期（60s，门槛内已累积几十轮失败证据），守卫不会放行进行中事务；上限只
	// 影响"可清算延迟"不影响正确性。
	private static final long RedoFailAgedBudgetMs = Onz.eDefaultSagaContextTimeoutMs;

	// redo整体失败告警去重：超龄分诊登记的redo失败滞留每轮redo
	// 重放同型异常——按tid只error一次防刷屏（对齐redoResultWarnedTids形态，成因
	// 片段只记首见形态是dedup既定代价）。瞬态失败自愈（参与方恢复→某轮全0→
	// removeOk）与人工清算（settleStuckRecord）时回收tid，集合有界于在库的异常
	// 滞留决策数。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> redoFailWarnedTids = ConcurrentHashMap.newKeySet();

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
					// 索引侧毒条目单条隔离：索引自身的值空/截断（ReadUInt抛）或key短于
					// 8字节（ToLongBE越界抛）时，异常若直接冲出循环体会中止整个commitIndex遍历
					// ——周期路径被DaemonTimer.runBody吞掉后下一轮从头再撞同一条，排序在后的
					// 未决决策redo永久停滞（redo内层隔离只覆盖点表侧的requireNonNull/decode，
					// 索引侧需在本层隔离）。单条处理整体包try，毒条目记error（带key/tid）后跳过
					// 留库人工排查，不阻塞其后记录的收敛（对齐ApplyHelper逐记录隔离形态；与
					// redo内层隔离构成两层防御）。毒条目留库期间每轮
					// redo都会再撞到并再记一条（对齐内层"redo fail"形态——真损坏必被持续看见，
					// 条目被人工清除/修复后即静默）。
					try {
						var bb = ByteBuffer.Wrap(value);
						var state = bb.ReadUInt();
						var tid = ByteBuffer.ToLongBE(key, 0);
						// 新格式值=state(varint)+写入时戳(8B BE)；旧格式（仅state，
						// 升级遗留的未决决策）读不到时戳视为年龄无穷——立即redo。
						var stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
						// 先读状态再判登记，skip仅作用于登记中的ePreparing。
						// ePreparing的redo是Rollback，回滚不可逆：登记窗口从addTransaction覆盖到
						// finally removeTransaction，其中saveCommitPoint(ePreparing)→无界
						// waitPendingAsync是年龄闸挡不住的进行中窗口，误发Rollback回滚存活参与方后
						// 对迟到Commit假应答成功（readyProcedures.remove为null直接SendResult(0)），
						// perform静默全量回滚却返回0——必须skip。真残留只能源于进程崩溃（登记表
						// 与perform同进程同生共死：perform异常结束必经finally摘除登记，进程存活则
						// 登记必在），崩溃重启后onzAgent为空，skip天然放行；存活perform的finally
						// 摘除登记后下轮redo可见。年龄闸只作用于未登记的ePreparing
						// （区分崩溃残留与刚落盘的窗口条目），登记中的条目与年龄无关。
						// eCommitting不做skip：其redo是幂等Commit重发（参与方已ready，重复Commit
						// 亦应答成功；且redo经getZezeInstance现查新地址），登记中执行也安全——它
						// 是perform的Commit散发通道socket僵死时的收敛通道，skip会把补发推迟到
						// perform的finally之后，多等一个perform生命周期。
						switch (state) {
						case eCommitting:
							redo(key, true, stamp);
							break;
						case ePreparing:
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
								redo(key, false, stamp);
							// else：进行中窗口，等超过年龄后的下一轮
							break;
						default:
							// 未知state：值可解但state∉{ePreparing,eCommitting}——损坏但
							// 未截断的垃圾值（截断/空值走外层的catch），或未来版本新增
							// state常量写入后降级运行。语义未知不盲目redo（补发Commit/Rollback都可能
							// 制造协调者与参与方分歧），只静默跳过则永不清算且零信号
							// （同函数hang/超龄NotFound/毒值均有告警，唯此支没有）。按tid只error一次
							// （集合见unknownStateWarnedTids）暴露后跳过，留库人工排查。
							if (unknownStateWarnedTids.add(tid))
								logger.error("onz redo: commitIndex条目state={} 未知（tid={}），跳过不redo，条目留库人工排查",
										state, tid);
							break;
						}
					} catch (Throwable ex) {
						// 单条隔离的兜底跳过：key/tid尽力携带——key短于8字节时tid本就
						// 解不出（这正是被隔离的异常形态之一），用原始key字节定位。
						logger.error("onz redo: commitIndex毒条目解码失败跳过（key={}，tid={}），留库人工排查",
								java.util.Arrays.toString(key),
								key.length >= 8 ? ByteBuffer.ToLongBE(key, 0) : null, ex);
					}
				}
			}
		} finally {
			dbLock.unlock();
		}
	}

	// redo对saga参与方FuncSagaEnd的等待超时：参与方处理FuncSagaEnd与在途业务互斥
	//（businessLock），应答可能慢于rpc默认超时（慢业务场景）；本轮超时保留记录，
	// 每轮redo重试收敛，取小于redo周期(60s)的量级。
	private static final int RedoSagaEndTimeoutMs = 30_000;

	// redo按决策与参与方类型分流：procedure参与方发Commit/Rollback；
	// saga参与方发FuncSagaEnd——commit决策补发endSaga未完成的结束(cancel=false)，
	// rollback决策补偿已提交的步骤(cancel=true)，参与方幂等。
	// stamp=commitIndex写入时戳，供超龄NotFound分诊。
	private void redo(byte[] key, boolean commitDecision, long stamp) throws RocksDBException {
		var tid = ByteBuffer.ToLongBE(key, 0);
		var zezeOnzs = new HashMap<String, Connector>();
		try {
			// 毒记录隔离：索引有条目而点表无（错配/遗留）或值损坏（截断）时
			// requireNonNull/decode抛运行时异常，在try内捕获记error——本条留库人工排查，
			// 不得中止迭代：排序在其后的未决决策redo是它们唯一的收敛通道。
			var value = Objects.requireNonNull(commitPoint.get(key));
			var state = new BSavedCommits.Data();
			state.decode(ByteBuffer.Wrap(value));

			var futures = new ArrayList<TaskCompletionSource<?>>();
			var rpcs = new ArrayList<Rpc<?, ?>>();
			for (var e : state.getOnzs()) {
				// saga参与方带前缀持久化，其余为procedure参与方（含旧版本ip_port格式）。
				var sagaName = OnzTransaction.decodeSagaParticipant(e);
				var zezeName = sagaName != null ? sagaName : e;
				AsyncSocket socket;
				if (zezes.containsKey(zezeName)) {
					// 参与方按集群名持久化：现查当前地址——地址漂移后redo
					// 不对死地址重试（连接器由instances缓存管理生命周期）。
					socket = getZezeInstance(zezeName);
				} else {
					// 旧版本持久化的ip_port（升级窗口遗留的未决决策）：按地址建连兜底。
					socket = openRedoConnection(zezeOnzs, zezeName).GetReadySocket();
				}
				rpcs.add(sendRedoDecision(socket, tid, sagaName, commitDecision, futures));
			}
			for (var e : futures)
				e.await();
			// await只在异常完成（超时/发送失败，上面catch保留记录）时抛出；应答非0码是
			// 正常完成（Rpc直接setResult），必须显式检查（对齐commit()）：参与方补偿失败
			// 已按契约保留上下文等重发（Onz.ProcessFuncSagaEndRequest放回sagas），此处删
			// 记录等于掐断唯一自动重试通道——已提交步骤永久未补偿。保留记录等下一轮幂等收敛。
			var removeOk = true;
			for (var rpc : rpcs) {
				if (rpc.getResultCode() == 0)
					continue;
				// eSagaNotFound：上下文已清理（业务失败自清理/参与方TTL回收/已处理过的
				// 重复发送），无补偿对象，可忽略（线上为moduleId组合值，解码后比较）。
				if (IModule.getErrorCode(rpc.getResultCode()) == AbstractOnz.eSagaNotFound) {
					// 超龄分诊：rollback决策（cancel=true路径）的NotFound有两种不可
					// 区分成因——良性（业务失败自清理/已补偿的重复发送）与恶性（参与方上下文
					// 已被TTL清理，补偿永久丢失）。记录年龄超参与方TTL预算（SagaNotFoundAged
					// BudgetMs）的升格为error（按tid去重防每轮刷屏）并保留决策记录（人工对账
					// 需要记录在场）；年龄内与commit决策（cancel=false的end无数据效应）维持静默。
					var recordAge = System.currentTimeMillis() - stamp;
					if (!commitDecision && recordAge >= SagaNotFoundAgedBudgetMs) {
						removeOk = false;
						if (agedNotFoundWarnedTids.add(tid))
							logger.error("onz redo: rollback决策的saga参与方应答eSagaNotFound且决策记录超龄"
											+ "（tid={}, age={}ms）：补偿可能已因参与方TTL清理而丢失，需人工对账（决策记录保留在库）",
									tid, recordAge);
					}
					continue;
				}
				removeOk = false;
				// 周期重试防刷屏：非0非eSagaNotFound应答保留决策记录等重试，
				// 确定性补偿失败每轮redo重发重失败——按tid只error一次（对齐agedNotFound
				// WarnedTids形态，集合见redoResultWarnedTids）：首条error已含tid与
				// resultCode，后续每轮重发无新信息，纯日志洪水（每tid每天1440条）还会
				// 稀释同文件真一次性错误的可见性。记录收敛删除时回收tid。
				if (redoResultWarnedTids.add(tid))
					logger.error("redo result error, keep record for retry. tid={}, resultCode={}",
							tid, rpc.getResultCode());
			}
			if (removeOk) {
				removeCommitRecord(key);
				agedNotFoundWarnedTids.remove(tid); // 记录收敛后回收告警去重项
				redoResultWarnedTids.remove(tid); // 同上：结果错误告警一并回收
				redoFailWarnedTids.remove(tid); // 同上：瞬态失败自愈（参与方恢复→本轮
				// 全0）后tid离开第四集合，集合有界于在库的异常滞留决策数。
			}
		} catch (Throwable ex) {
			// timer will redo
			// 年龄门槛分诊：参与者循环/点表读取的任一异常未达门槛时只落本处
			// "redo fail"全量日志——走不到任何告警集合add点，确定性滞留（死地址/除名
			// 集群/毒点表）对settle守卫不可见且每60s刷一条带栈error。记录年龄≥
			// RedoFailAgedBudgetMs的本轮异常转去重形态：按tid登记第四告警集合并
			// error一次（成因片段=异常类名+消息——诊断"先修参与方还是改库"的依据，
			// 首见形态即可，门槛内的年轻阶段有全量带栈日志），登记即可被
			// settleStuckRecord清算；已登记的后续轮次静默（重放无新信息）。未达门槛
			// 记全量日志：瞬态/年轻失败完全可见，且行数有界（门槛/60s轮后转去重形态）。
			// 旧格式记录（stamp=0）年龄视为无穷（对齐redoTimer读时戳口径），首轮异常即分诊。
			var recordAge = System.currentTimeMillis() - stamp;
			if (recordAge >= RedoFailAgedBudgetMs) {
				if (redoFailWarnedTids.add(tid))
					logger.error("redo fail aged, keep record for settle. tid={}, age={}ms, cause={}: {}",
							tid, recordAge, ex.getClass().getName(), ex.getMessage());
			} else {
				logger.error("redo fail. tid={}", tid, ex);
			}
		} finally {
			for (var zeze : zezeOnzs.values())
				zeze.stop();
		}
	}

	// redo的发送分流（决策语义见redo）：saga参与方发FuncSagaEnd（cancel取反决策，
	// 等待超时见RedoSagaEndTimeoutMs），procedure参与方发Commit/Rollback。
	// future入列供统一await，返回rpc供结果码检查。
	private static Rpc<?, ?> sendRedoDecision(AsyncSocket socket, long tid,
			String sagaName, boolean commitDecision, List<TaskCompletionSource<?>> futures) {
		if (sagaName != null) {
			var r = new FuncSagaEnd();
			r.Argument.setOnzTid(tid);
			r.Argument.setCancel(!commitDecision);
			futures.add(r.SendForWait(socket, RedoSagaEndTimeoutMs));
			return r;
		}
		Rpc<BCommit.Data, EmptyBean.Data> r = commitDecision ? new Commit() : new Rollback();
		r.Argument.setOnzTid(tid);
		futures.add(r.SendForWait(socket));
		return r;
	}

	void saveCommitPoint(byte[] tidBytes, BSavedCommits.Data bState, int state) throws RocksDBException {
		bState.setState(state);
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(13);
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis()); // ePreparing年龄判据，见redoTimer
		// perform写库点纳入dbLock域+锁内stopped双检（对齐redoTimer头部/
		// settleStuckRecord的锁内双检形态）：本方法与commit()内的调用都跑在业务线程、若不
		// 持dbLock也不复查stopped——perform的业务长窗口（txn.perform()时长无上界）期间
		// stop()可以走完整个关库链，此后写点对已释放的列族/db句柄做native写，正是
		// RocksDatabase.close契约声明的use-after-free直接崩溃。互斥只需覆盖写库本身（毫秒级，
		// 不含业务窗口——那会把长业务与redo轮串行化）：与stop()的database.close()（同在
		// dbLock域内）互斥后，写要么先于close完成、要么在锁内看到stopped拒写。拒写抛
		// RuntimeException（对齐getZezeInstance的"stopped"拒绝形态）：perform/commit的既有
		// catch→rollback链把停机时在飞事务转为显式失败（stop javadoc"在途事务可能失败"——
		// 失败而非崩溃）；此刻ePreparing/eCommitting均未落盘，回滚是正确的2pc决策，参与方由
		// ready等待超时自愈与重启redo兜底。
		dbLock.lock();
		try {
			if (stopped)
				throw new RuntimeException("OnzServer stopped: saveCommitPoint rejected. tid="
						+ ByteBuffer.ToLongBE(tidBytes, 0));
			try (var batch = database.borrowBatch()) {
				commitPoint.put(batch, tidBytes, tidBytes.length, bb.Bytes, bb.WriteIndex);
				commitIndex.put(batch, tidBytes, tidBytes.length, bbIndex.Bytes, bbIndex.WriteIndex);
				batch.commit(writeOptions);
			}
		} finally {
			dbLock.unlock();
		}
	}

	void removeCommitRecord(byte[] tidBytes) {
		// 同saveCommitPoint纳入dbLock+锁内stopped双检：commit()两处调用
		//（失败分支/成功路径）都跑在业务线程。redo()/settleStuckRecord的既有调用点本就在
		// dbLock域内（ReentrantLock可重入，行为不变）。stopped时拒删不是错误：与下方
		// RocksDBException分支同语义——记录留库，由下次进程启动的redo恢复（stop javadoc
		// 既定承诺）；此刻库已关或即将关，不再触碰。commit()成功路径因拒删留下的eCommitting
		// 残留由重启redo的幂等Commit重发收敛，参与方已按Commit提交，重发应答成功即清理。
		dbLock.lock();
		try {
			if (stopped) {
				logger.error("OnzServer stopped: removeCommitRecord rejected, keep record for next-startup redo. tid={}",
						ByteBuffer.ToLongBE(tidBytes, 0));
				return;
			}
			try {
				// 两表同key生命周期：索引删则点删，同一batch原子落地。
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
		} finally {
			dbLock.unlock();
		}
	}

	/**
	 * 滞留决策记录的人工清算："保留 + 曝光"（超龄NotFound / 未知state / 确定性补偿失败 /
	 * 超龄redo整体失败）之后的可达终点——对账完成后（如参与方带外补了数据），运维按 tid
	 * 显式关闭滞留记录。暴露惯例对齐 Onz.cleanupTimeoutSagas：嵌入方从自己的运维面调用
	 * （全仓无独立OnzServer部署形态），方法体即未来任何远程形态的 handler 体；测试可直接调用。
	 * <p>
	 * 守卫（下限）：只放行协调者自己已报告为滞留的 tid——agedNotFoundWarnedTids ∪
	 * redoResultWarnedTids ∪ unknownStateWarnedTids ∪ redoFailWarnedTids。清算的正确性
	 * 依赖"操作者已完成对账"这一协调者不可验证的外部事实，设计的本质不是验证它，而是
	 * 把该断言约束在最小爆破半径内：集合成员资格是协调者自己"redo 收敛不动此记录"的
	 * 证据，进行中事务的 tid 不可达——误删进行中事务唯一收敛通道（redo）的操作因此
	 * 不可达。不在集合的分型如实指引（redo失败类不适用"下一轮≤60s重新分诊"的指引——
	 * 它要等记录年龄超 RedoFailAgedBudgetMs 后的下一轮 redo才分诊，进行中/未分诊才走
	 * "下一轮≤60s重诊"路径）：进行中 / 未分诊（进程
	 * 重启后集合清空，等下一轮 redo 重新分诊）/ redo失败未达龄（等超龄后的下一轮），
	 * 拒绝并 error。
	 * <p>
	 * 删除：dbLock 域内 + stopped 双检（对齐 redoTimer 头部，与 stop 的关库互斥）；
	 * 删前读 commitIndex/commitPoint 原值记审计日志（被放弃的补偿对象的最后留痕——
	 * 记录内容删除后不可再读）；复用 removeCommitRecord 单 batch 原子双删（两表同
	 * key 生命周期）；四个告警去重集合同步回收（回收点对齐 redo 的 removeOk 分支；
	 * unknownState 条目按设计永不 redo 收敛，本方法是该集合唯一回收通道，汇流闭合
	 * 同形滞留四类）。
	 *
	 * @return true=记录已关闭（删除，或守卫通过后已不在库的幂等no-op）；false=拒绝
	 * （tid未被报告滞留/服务器已停止），库与集合均不动。
	 */
	public boolean settleStuckRecord(long tid) {
		// stopped前置检查对齐redoTimer头部：终态服务器先拒绝，不进守卫（getZezeInstance/
		// perform对stopped的拒绝同口径）。锁内双检覆盖检查与加锁之间的stop窗口。
		if (stopped) {
			logger.error("onz settle rejected: OnzServer stopped. tid={}", tid);
			return false;
		}
		if (!(agedNotFoundWarnedTids.contains(tid) || redoResultWarnedTids.contains(tid)
				|| unknownStateWarnedTids.contains(tid) || redoFailWarnedTids.contains(tid))) {
			// 分型如实指引：redo失败类不适用"下一轮redo≤60s重新分诊"——异常轮走不到任何
			// 集合add点，分诊要等记录年龄超RedoFailAgedBudgetMs后的下一轮，该指引对这类永不兑现。
			logger.error("onz settle rejected: tid={} 不是协调者已报告滞留的tid（进行中/未分诊/redo失败未达龄："
							+ "未分诊（含进程重启后集合清空）等下一轮redo重新分诊；redo失败类等记录年龄超"
							+ RedoFailAgedBudgetMs + "ms后的下一轮redo分诊），记录不动", tid);
			return false;
		}
		var tidBytes = new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, tid);
		dbLock.lock();
		try {
			if (stopped) {
				logger.error("onz settle rejected: OnzServer stopped. tid={}", tid);
				return false;
			}
			// 守卫通过到加锁之间，某轮redo可能已收敛该记录并回收tid（removeOk分支的删除+
			// 回收都在dbLock域内原子完成）：此时锁内读不到原值，审计记"已不在库"，删除与
			// 集合remove均为幂等no-op——无害路径，不在锁内复查守卫（四集合的分诊add与回收
			// remove都只发生在dbLock域内，锁内复查只会迟到地看到"已被redo收敛"，与no-op等价）。
			auditRetainedRecord(tid, tidBytes);
			removeCommitRecord(tidBytes);
			agedNotFoundWarnedTids.remove(tid); // 记录关闭后回收告警去重项（对齐removeOk分支）
			redoResultWarnedTids.remove(tid); // 同上
			unknownStateWarnedTids.remove(tid); // 同上：本方法是该集合唯一的tid回收点
			redoFailWarnedTids.remove(tid); // 同上：与removeOk双回收点，人工清算后
			// tid离开第四集合（redo失败类的分诊登记在redo的catch内，回收点对齐既有两处）。
			return true;
		} finally {
			dbLock.unlock();
		}
	}

	/**
	 * 删除前的滞留记录审计留痕：tid/原state/年龄/参与方清单——记录两表条目
	 * 删除后内容即不可再读，日志是被放弃的补偿对象的最后痕迹。审计尽力而为：守卫已确认
	 * 滞留，个别字段读不到（已不在库/无点条目/毒值）不得阻断清算——删除本身的正确性
	 * 不依赖审计，只依赖守卫 + removeCommitRecord 的原子性。
	 */
	private void auditRetainedRecord(long tid, byte[] tidBytes) {
		var state = "未知";
		var age = "未知";
		var onzs = "未知";
		try {
			var indexValue = commitIndex.get(tidBytes);
			if (indexValue == null) {
				// 守卫通过但索引已不在：某轮redo刚收敛（见settleStuckRecord锁内注释）或外部
				// 改库。照常走删除（幂等no-op），审计如实记录。
				state = age = onzs = "已不在库";
			} else {
				var bb = ByteBuffer.Wrap(indexValue);
				state = String.valueOf(bb.ReadUInt());
				// 旧格式（仅state无时戳）读不到时戳与redoTimer同口径：年龄未知，不臆造。
				var stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
				if (stamp != 0)
					age = (System.currentTimeMillis() - stamp) + "ms";
				var pointValue = commitPoint.get(tidBytes);
				if (pointValue != null) {
					var saved = new BSavedCommits.Data();
					saved.decode(ByteBuffer.Wrap(pointValue));
					onzs = String.valueOf(saved.getOnzs());
				} else {
					// 未知state条目可无点条目（索引直注）；索引在而点不在的
					// 错配条目同样如实记录。
					onzs = "无点条目";
				}
			}
		} catch (Throwable ex) {
			// 毒值（时戳后截断/点值损坏等）：审计携带可读部分，异常补一条定位，不阻断删除。
			logger.warn("onz settle audit: 滞留记录审计读取部分失败（尽力携带）. tid={}", tid, ex);
		}
		// warn对齐cleanupTimeoutSagas的"放弃补偿对象"日志级别：比info可保证默认可见，
		// 比error少一分"系统自检发现异常"的语义——这是人工决策的执行留痕。
		logger.warn("onz settle audit: 清算滞留决策记录（人工对账完成，删除前留痕）. tid={}, state={}, age={}, onzs={}",
				tid, state, age, onzs);
	}

	private Connector openRedoConnection(HashMap<String, Connector> conns, String ip_port) {
		var conn = conns.computeIfAbsent(ip_port, __ -> {
			var newConn = new Connector(ip_port, false);
			newConn.SetService(onzAgent.getService()); // 未SetService即start()快速失败（Connector契约）；对齐getZezeInstance的绑定形态
			newConn.start();
			return newConn;
		});
		conn.GetReadySocket();
		return conn;
	}

	/**
	 * 停止OnzServer（幂等，可重入）。语义：不做优雅排空——在途事务可能失败，
	 * 未完成的补发记录（commitIndex）留在库中由下次进程启动的redo恢复；终态，不可再start。
	 * 在飞perform的写库点（saveCommitPoint/removeCommitRecord）在dbLock内复查stopped后
	 * 拒写——停机时在飞事务显式失败（perform/commit的catch→rollback链），
	 * 不再对已释放句柄做native写（RocksDatabase.close契约的use-after-free）。
	 * 停机为best-effort：任一步失败仅记error并继续——半途上抛会让幂等守卫把停机
	 * 永久卡在半途（库/代理无法补关），失败步骤由日志定位人工处理。
	 * 顺序：拒绝新工作 → 停定时器 → 停缓存connector（必须先于服务停止：
	 * 服务关socket会触发connector自动重连，停止后仍无限重连）→ close各SM代理
	 * （按identity去重，共享配置是同一实例）→ 停服务 → 最后关库。
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
				} catch (Throwable e) {
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
				} catch (Throwable e) {
					logger.error("close ServiceManager agent", e);
				}
			}
			try {
				myServiceManager.close();
			} catch (Throwable e) {
				logger.error("close myServiceManager", e);
			}

			try {
				onzAgent.stop();
			} catch (Throwable e) {
				logger.error("stop onzAgent", e);
			}
			try {
				service.stop();
			} catch (Throwable e) {
				logger.error("stop service", e);
			}

			try {
				database.close();
			} catch (Throwable e) {
				logger.error("close database", e);
			}
		} finally {
			dbLock.unlock();
		}
	}

	/**
	 * 所有zeze集群共享同一个ServiceManager实例时，使用这个构造函数。
	 * 共享配置时，每个zeze集群需要额外的唯一名配置，并且把它拼接到ServiceManager的注册参数中：
	 * 参与方侧在各集群 {@link Zeze.Onz.Onz#start()} 之前用 {@link Zeze.Onz.Onz#setRegisterServiceName(String)}
	 * 配置同名注册（缺省仍为"Onz"，非共享模式零变化）；本协调者对每个名字逐名订阅，
	 * getZezeInstance按名查询解析地址（订阅键=查询键）。
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
				// 订阅键=查询键：getZezeInstance在shared模式下按集群名查
				// subscribeStates（Agent以订阅请求里的服务名为键建表），共享agent上若只订阅
				// 固定名"Onz"而按别名查询——别名键永不存在，共享模式下每次地址解析恒抛
				// "subscribe not found"，所有Onz事务/redo/commit路径100%失败。
				// 逐名订阅与getZezeInstance的按名查询对齐；各名对应的参与方以同名注册进
				// 共享SM（Onz.setRegisterServiceName，构造器javadoc的配对说明），两头对名后该
				// 模式才真正可用。
				sharedAgent.subscribeService(new BSubscribeInfo(zeze));
			}
			this.sharedServiceManager = true;
			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造的全有或全无：逆序释放——共享SM（zezes各值同一实例，只关一次）
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

		// "选择→创建→登记"按名原子化：无同步时并发冷路径互相stop对方的connector
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
			// 这里和下面的txn.commit分成两步saveCommitPoint，
			// 中间没有做太多额外的事情，但为了明确两个事务状态，仍然分开。原因如下：
			// 参考Dbh2的两步：由于Dbh2一开始就知道所有的服务器，所以可以一开始就保存一次ePreparing，
			// 而这上面的perform是边执行边产生服务器地址，无法一开始保存事务状态。
			// 最严格的做法是每产生一个服务器地址，就写一次ePreparing（包含所有的服务器地址）。
			// 此处处理为：等待perform完成。
			if (0 == rc) {
				txn.waitPendingAsync();
				// pendingAsync窗口内注册的参与方不进上面的ePreparing快照：窗口后
				// 重建快照再传给commit——txn.commit内saveCommitPoint(eCommitting)持久化的
				// 参与方列表完整，崩溃/commitFail后redo补发不缺迟到参与方（迟到者等不到
				// Commit会ready超时自愈回滚，与已报成功的协调者分歧）。上面的ePreparing快照
				// 保持不动：窗口内落盘可观察是redo年龄闸与在途登记skip的依赖，
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
			// 慢而最终完成的perform也会触发过封锁告警（登记中+超龄即入集合），完成即回收，
			// 集合真正有界于挂死数——否则按tid无界累积。
			hangWarnedTids.remove(txn.getOnzTid());
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
		// 复用嵌入zeze的Onz组件协议来处理OnzServer的远程调用。
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
