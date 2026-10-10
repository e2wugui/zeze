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
	// redo轮次与database.close()互斥：redo的两段式（见redoTimer）只在快照收集与删除
	// 复核两段持dbLock（毫秒级，不含网络等待——滞留记录每条最多30s+的重发等待在锁外，
	// 不再反压perform的决策写点，onz-04）；stop()超预算逃逸的轮次在两段的锁内stopped
	// 双检下不再触碰库，RocksDatabase的close-safe契约（迟到库操作抛IllegalStateException
	// 而非JNI崩溃）是资源层兜底。
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
				// 空名集群（"=config"）拒绝：注册后以空名路由/订阅，业务每笔调用以晦涩的
				// "subscribe not found"收场，错误配置须在构造期暴露（对齐length!=2的严格度）。
				if (zezeNameAndConfig[0].isBlank())
					throw new IllegalArgumentException("empty zeze name. zezes=" + zezeConfigs);
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
		if (stopped)
			throw new IllegalStateException("OnzServer stopped: start rejected");
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
	// 集合同时承载"已应答成功步骤的上下文消失"嫌疑登记（cancel阶段的
	// noteSagaContextLost，年轻即入集）——复用其按tid去重的error、settle守卫与收敛回收
	// 链；redoRecord对rollback决策年轻NotFound据成员资格保守保留（缺席非终态）。
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
	// 全部落入redo失败分诊（锁内快照收集的点表读取与锁外重发段，见logRedoFail），
	// 走不到任何告警集合的add点：对settle守卫不可见，
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

	private void redoTimer() {
		if (stopped)
			return;
		// 两段式（onz-04）：原先整轮（迭代+每条记录的重发网络等待，滞留记录每条最多
		// 30s+5s）持dbLock——perform的saveCommitPoint(ePreparing/eCommitting)同锁排队
		// 等完整轮次，参与方ready等待（flushTimeout预算）超时自愈回滚后协调者仍按提交
		// 推进=无崩溃的暴露型分歧。锁内只收集快照（毫秒级，含点表读取），重发网络与
		// 结果判定出锁，删除决策回锁内复核（快照→锁外期间记录可能被并发终结：perform
		// 的commit成功路径/settleStuckRecord）。dbLock仍覆盖database生命周期互斥。
		var records = collectRedoCandidates();
		var n = records.size();
		if (n == 0) {
			redoResumeKey = null;
			return;
		}
		// 轮转起点（onz-05）：从上一轮预算耗尽处之后继续——滞留头部记录（每条吃满单条
		// 网络上限）若每轮都从头开始，排序在后的记录每轮都轮不到（收敛系统性停滞）；
		// 预算内走完全部候选则游标清空（下轮从头）。
		// 比较必须用无符号字节序（compareUnsigned）：records是RocksDB迭代器的bytewise
		//（无符号）key序，Arrays.compare为有符号——首个差异字节跨0x80边界（如tid…7F/…80）
		// 时两序不一致，有符号比较会把无符号序更大的记录判为更小，扫描越过它绕回头部：
		// 预算耗尽的轮次下该记录重新陷入onz-05要根治的系统性饥饿（回归守卫见
		// TestOnzRedoRotationCursor）。
		var start = 0;
		if (redoResumeKey != null)
			while (start < n && java.util.Arrays.compareUnsigned(records.get(start).key, redoResumeKey) <= 0)
				start++;
		var deadline = System.currentTimeMillis() + RedoRoundNetworkBudgetMs;
		byte[] lastAttempted = null;
		var completed = true;
		for (var k = 0; k < n; k++) {
			if (stopped)
				break; // 出锁段完成当前记录后不再取下一条；终态下游标无意义（stop后无下一轮）
			if (System.currentTimeMillis() >= deadline) {
				completed = false;
				break;
			}
			var rec = records.get((start + k) % n);
			redoRecord(rec);
			lastAttempted = rec.key;
		}
		redoResumeKey = completed ? null : lastAttempted;
	}

	/** redo候选的锁内快照收集（onz-04两段式的第一段）：dbLock内完成commitIndex迭代、
	 * 状态/年龄/在途登记分诊与点表读取解码（毫秒级）——锁外重发段不得触碰database
	 * （关库互斥以dbLock+锁内stopped双检实现）。返回key序候选列表；毒条目/未知state/
	 * 点表失败均单条隔离（不中止收集，排序在其后的未决决策redo是它们唯一的收敛通道）。 */
	private ArrayList<RedoRecord> collectRedoCandidates() {
		var records = new ArrayList<RedoRecord>();
		dbLock.lock();
		try {
			if (stopped)
				return records; // 锁内stopped双检（对齐settleStuckRecord形态）
			try (var it = commitIndex.iterator()) {
				for (it.seekToFirst(); it.isValid(); it.next()) {
					var key = it.key();
					var value = it.value();
					// 索引侧毒条目单条隔离：索引自身的值空/截断（ReadUInt抛）或key短于
					// 8字节（ToLongBE越界抛）时，异常若直接冲出循环体会中止整个commitIndex遍历
					// ——周期路径被DaemonTimer.runBody吞掉后下一轮从头再撞同一条，排序在后的
					// 未决决策redo永久停滞（点表侧的requireNonNull/decode在下方同层隔离）。
					// 单条处理整体包try，毒条目记error（带key/tid）后跳过留库人工排查，不阻塞
					// 其后记录的收敛（对齐ApplyHelper逐记录隔离形态）。毒条目留库期间每轮
					// redo都会再撞到并再记一条（对齐"redo fail"形态——真损坏必被持续看见，
					// 条目被人工清除/修复后即静默）。
					int state;
					long tid;
					long stamp;
					try {
						var bb = ByteBuffer.Wrap(value);
						state = bb.ReadUInt();
						tid = ByteBuffer.ToLongBE(key, 0);
						// 新格式值=state(varint)+写入时戳(8B BE)；旧格式（仅state，
						// 升级遗留的未决决策）读不到时戳视为年龄无穷——立即redo。
						stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
					} catch (Throwable ex) {
						// 单条隔离的兜底跳过：key/tid尽力携带——key短于8字节时tid本就
						// 解不出（这正是被隔离的异常形态之一），用原始key字节定位。
						logger.error("onz redo: commitIndex毒条目解码失败跳过（key={}，tid={}），留库人工排查",
								java.util.Arrays.toString(key),
								key.length >= 8 ? ByteBuffer.ToLongBE(key, 0) : null, ex);
						continue;
					}
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
					boolean commitDecision;
					switch (state) {
					case eCommitting:
						commitDecision = true;
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
						if (age < RedoPreparingMinAgeMs)
							continue; // 进行中窗口，等超过年龄后的下一轮
						commitDecision = false;
						break;
					default:
						// 未知state：值可解但state∉{ePreparing,eCommitting}——损坏但
						// 未截断的垃圾值（截断/空值走上方的索引解码catch），或未来版本新增
						// state常量写入后降级运行。语义未知不盲目redo（补发Commit/Rollback都可能
						// 制造协调者与参与方分歧），只静默跳过则永不清算且零信号
						// （同函数hang/超龄NotFound/毒值均有告警，唯此支没有）。按tid只error一次
						// （集合见unknownStateWarnedTids）暴露后跳过，留库人工排查。
						if (unknownStateWarnedTids.add(tid))
							logger.error("onz redo: commitIndex条目state={} 未知（tid={}），跳过不redo，条目留库人工排查",
									state, tid);
						continue;
					}
					// 点表读取+解码（原redo()的锁内段，挪入快照收集——锁外段不得触库）：
					// 索引有条目而点表无（错配/遗留）或值损坏（截断）时requireNonNull/decode抛，
					// 失败分类对齐redo整体失败分诊（logRedoFail）：本条留库人工排查。
					try {
						var saved = new BSavedCommits.Data();
						saved.decode(ByteBuffer.Wrap(Objects.requireNonNull(commitPoint.get(key))));
						records.add(new RedoRecord(key, tid, value, commitDecision, stamp, saved));
					} catch (Throwable ex) {
						logRedoFail(tid, stamp, ex);
					}
				}
			}
		} finally {
			dbLock.unlock();
		}
		return records;
	}

	// redo对saga参与方FuncSagaEnd的等待超时：参与方处理FuncSagaEnd与在途业务互斥
	//（businessLock），应答可能慢于rpc默认超时（慢业务场景）；本轮超时保留记录，
	// 每轮redo重试收敛，取小于redo周期(60s)的量级。
	private static final int RedoSagaEndTimeoutMs = 30_000;

	// redo轮锁外重发段的总网络预算（onz-05）：预算耗尽即本轮到此为止，剩余记录留下轮
	//（60s周期后，配合redoResumeKey轮转）。取值约束：预算+单条记录网络上限（发送段每
	// 参与方GetReadySocket最多5s串行+等待段FuncSagaEnd上限RedoSagaEndTimeoutMs，少量
	// 参与方约35s）≈95s，须显著小于DaemonTimer每轮任务看门狗（2参构造timeoutMs取
	// Task.defaultTimeout=120s）——看门狗中断不再落在锁外重发的await内；参与方数极端
	// 放大（单条>55s）时中断落在单条await上：该记录当轮作废保留重试（既有安全语义，
	// 无库状态破坏）。预算=周期使被挡记录的推迟有界于一个周期。
	private static final long RedoRoundNetworkBudgetMs = 60_000;

	// redo轮转游标：上一轮预算耗尽时最后尝试过的key（null=下轮从头开始）。
	// 仅redo轮线程读写（无锁）：DaemonTimer链式排期（下一轮在上一轮收尾的
	// finishRound内才排）保证轮次串行；start()的首轮同步调用先于redoDaemon.start()。
	private byte[] redoResumeKey;

	/** redo候选的锁内快照（onz-04两段式）：collectRedoCandidates在dbLock内构建，
	 * 锁外重发段使用——锁外不得触碰database。
	 * key=tid（8B BE）；indexValue=收集时的commitIndex原值（state(varint)+写入时戳(8B BE)），
	 * 作删除前复核判据（快照→锁外→回锁期间记录可能被并发终结）；commitDecision=
	 * true(eCommitting)/false(ePreparing，补发Rollback/cancel)；stamp=写入时戳（超龄
	 * NotFound/redo失败分诊判据）；savedCommits=参与方列表（saga参与方带前缀编码）。 */
	private record RedoRecord(byte[] key, long tid, byte[] indexValue, boolean commitDecision, long stamp,
			BSavedCommits.Data savedCommits) {
	}

	// redo按决策与参与方类型分流：procedure参与方发Commit/Rollback；
	// saga参与方发FuncSagaEnd——commit决策补发endSaga未完成的结束(cancel=false)，
	// rollback决策补偿已提交的步骤(cancel=true)，参与方幂等（重发无害是两段式的
	// 正确性前提：重复Commit命中ready条目或已提交的幂等路径应答0；重复FuncSagaEnd
	// 对已清理上下文应答eSagaNotFound可辨识忽略）。
	// stamp取自快照（commitIndex写入时戳），供超龄NotFound分诊。
	private void redoRecord(RedoRecord rec) {
		var tid = rec.tid;
		var zezeOnzs = new HashMap<String, Connector>();
		try {
			var futures = new ArrayList<TaskCompletionSource<?>>();
			var rpcs = new ArrayList<Rpc<?, ?>>();
			for (var e : rec.savedCommits.getOnzs()) {
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
				rpcs.add(sendRedoDecision(socket, tid, sagaName, rec.commitDecision, futures));
			}
			for (var e : futures)
				e.await();
			// await只在异常完成（超时/发送失败，下面catch保留记录）时抛出；应答非0码是
			// 正常完成（Rpc直接setResult），必须显式检查（对齐commit()）：参与方补偿失败
			// 已按契约保留上下文等重发（Onz.ProcessFuncSagaEndRequest放回sagas），此处删
			// 记录等于掐断唯一自动重试通道——已提交步骤永久未补偿。保留记录等下一轮幂等收敛。
			var removeOk = true;
			for (var rpc : rpcs) {
				if (rpc.getResultCode() == 0)
					continue;
				// onz-01（FND25裁定）：参与方决策等待已超时自愈回滚，迟到的Commit/Rollback命中
				// 哨兵的一次性分歧应答。Commit分歧=部分提交（redo重发无法翻正已回滚的参与方）；
				// Rollback与本地一致=已收敛（预算违例信号已由协调者侧error暴露，OnzTransaction.
				// rollback/commit特判）。两种都按该参与方了结（removeOk不动）：重发只会命中null
				// 幂等路径应答0（哨兵一次性消耗，见Onz.ProcessCommitRequest），保留记录空转一轮
				// 毫无收益——否则该应答会落入下方"redo result error"分支制造假失败噪声并推迟收敛。
				if (IModule.getErrorCode(rpc.getResultCode()) == AbstractOnz.eDivergence) {
					logger.error("onz redo: 参与方应答eDivergence（已超时回滚的参与方收到迟到决策"
									+ "=部分提交分歧/预算违例信号）. tid={}, decision={}, resultCode={}",
							tid, rec.commitDecision ? "eCommitting" : "ePreparing", rpc.getResultCode());
					continue;
				}
				// eSagaNotFound：上下文已清理（业务失败自清理/参与方TTL回收/已处理过的
				// 重复发送），无补偿对象，可忽略（线上为moduleId组合值，解码后比较）。
				if (IModule.getErrorCode(rpc.getResultCode()) == AbstractOnz.eSagaNotFound) {
					// 超龄分诊：NotFound的成因不可区分——rollback决策（cancel=true）为良性
					// （业务失败自清理/已补偿的重复发送）或恶性（上下文被TTL清理，补偿永久丢失）；
					// commit决策（cancel=false的end，onz-05）为良性（end已应用的重复/参与方落库
					// 完成后宕机、上下文TTL清理——写安全）或恶性（参与方在发结果后本地提交前宕机
					// =丢写而协调者按成功收场）。记录年龄超参与方TTL预算（SagaNotFoundAged
					// BudgetMs）的升格error（按tid去重防每轮刷屏）并保留决策记录（人工对账
					// 需要记录在场）；年龄内的rollback NotFound维持静默移除。
					var recordAge = System.currentTimeMillis() - rec.stamp;
					if (recordAge >= SagaNotFoundAgedBudgetMs) {
						removeOk = false;
						if (agedNotFoundWarnedTids.add(tid))
							logger.error("onz redo: saga参与方应答eSagaNotFound且决策记录超龄"
											+ "（tid={}, age={}ms, decision={}）：{}，需人工对账（决策记录保留在库）",
									tid, recordAge, rec.commitDecision ? "eCommitting" : "ePreparing",
									rec.commitDecision
											? "end未送达而上下文已消失——参与方可能在发结果后、本地提交前宕机（丢写嫌疑，ONZ-F25-05）"
											: "补偿可能已因参与方TTL清理而丢失");
					} else if (rec.commitDecision) {
						// commit决策（end补发）的年轻NotFound保留记录重试——成功步骤的
						// 上下文在end送达前不应消失（end是唯一正常清理者；业务失败自清理不可能：
						// end只在全部步骤成功后发送；TTL清理需空闲超1h，与年轻记录矛盾），
						// 消失即丢写嫌疑。保留至超龄由上面的分诊终判（重发幂等：上下文在则end成功
						// 应答0并收敛，不在则NotFound，均无副作用）。rollback决策的年轻NotFound
						// 除已被cancel阶段登记嫌疑的tid外按良性终态移除（见下方else分支）。
						removeOk = false;
					} else {
						// rollback决策的年轻NotFound：无补偿对象的良性终态，移除收敛——除非该tid
						// 已被cancel阶段登记"已应答成功步骤的上下文消失"嫌疑（noteSagaContextLost）：
						// 该形态的缺席非终态（上下文存在过），保守保留交redo幂等重发与人工清算
						// （上下文在则补偿成功收敛即删，不在则NotFound直至settle）。
						if (agedNotFoundWarnedTids.contains(tid))
							removeOk = false;
						else {
							// info留痕带tid，补偿链的收敛删除点不至于事后对账零线索。
							logger.info("onz redo: saga参与方应答eSagaNotFound，rollback决策年轻记录按良性终态移除"
											+ "（tid={}, age={}ms）", tid, recordAge);
						}
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
			if (removeOk)
				removeRedoRecordIfUnchanged(rec);
		} catch (Throwable ex) {
			// timer will redo：锁外段任一异常（按名解析unknown zeze/subscribe not found、
			// GetReadySocket满时超时、futures await超时/中断、复核读失败）保留记录，
			// 分诊见logRedoFail。
			logRedoFail(tid, rec.stamp, ex);
		} finally {
			for (var zeze : zezeOnzs.values())
				zeze.stop();
		}
	}

	/** 删除决策回锁内复核（onz-04）：快照收集→锁外重发期间记录可能被并发终结——
	 * perform线程commit成功路径的removeCommitRecord（redo不skip登记中的eCommitting，
	 * 与perform自己的Commit散发并发是既有语义）或人工settleStuckRecord。复核
	 * commitIndex原值仍在且未变才删除：本轮"全部应答0/良性NotFound"的结论只终结
	 * 快照时见到的那条记录。值已不在=并发方已终结，幂等no-op（告警去重集合同步
	 * 回收，集合有界于在库滞留数）。值已变=进程内不可达的防御分支：同key的写入只有
	 * perform的两步saveCommitPoint且tid不复用（AutoKey单调），ePreparing候选的perform
	 * 已死（快照时未登记，见collectRedoCandidates的登记判据）、eCommitting候选的perform
	 * 亦不再改写（commit只调用一次）——防外进程改库，保留+warn待下轮按新值分诊。
	 * stopped时静默跳过：记录留库由下次进程启动的redo恢复（stop javadoc承诺），
	 * 不走removeCommitRecord的拒删error——停机窗口内每条记录一条error是纯噪声。 */
	private void removeRedoRecordIfUnchanged(RedoRecord rec) throws RocksDBException {
		dbLock.lock();
		try {
			if (stopped)
				return;
			var current = commitIndex.get(rec.key);
			if (current == null) {
				recycleRedoWarnTids(rec.tid); // 并发方已终结：集合回收对齐"有界于在库滞留数"
				return;
			}
			if (!java.util.Arrays.equals(current, rec.indexValue)) {
				logger.warn("onz redo: 快照后决策记录已变化，保留待下轮分诊. tid={}", rec.tid);
				return;
			}
			removeCommitRecord(rec.key); // dbLock可重入
			recycleRedoWarnTids(rec.tid);
		} finally {
			dbLock.unlock();
		}
	}

	/** 记录收敛（本轮复核删除，或并发方已终结）后回收告警去重项（原redo() removeOk
	 * 分支的回收点；unknownState条目不经redo收敛，仍由settleStuckRecord唯一回收）。 */
	private void recycleRedoWarnTids(long tid) {
		agedNotFoundWarnedTids.remove(tid); // 记录收敛后回收告警去重项
		redoResultWarnedTids.remove(tid); // 同上：结果错误告警一并回收
		redoFailWarnedTids.remove(tid); // 同上：瞬态失败自愈（参与方恢复→本轮全0）后
		// tid离开第四集合，集合有界于在库的异常滞留决策数。
	}

	/** cancelSaga对"已应答成功步骤"的eSagaNotFound登记：协调者已收到该步骤的成功应答，
	 * 上下文存在过，其缺席非终态（成功步骤上下文的唯一正常清理者是补偿自身；业务失败
	 * 自清理只适用于失败步骤；成因=参与方丢失内存上下文或TTL超龄清理）——补偿丢失
	 * 嫌疑。登记复用 agedNotFoundWarnedTids（按tid去重的error+settle守卫+收敛回收链）：
	 * redoRecord对rollback决策的年轻NotFound据此保守保留（重发幂等：上下文在则补偿成功
	 * 收敛即删，不在则NotFound直至人工清算）。协调者自身重启丢失登记是残余（嫌疑无法
	 * 随决策记录持久化）。 */
	void noteSagaContextLost(long tid, String zezeName) {
		if (agedNotFoundWarnedTids.add(tid))
			logger.error("onz saga context lost: 已应答成功步骤的上下文在cancel送达前消失"
					+ " (participant process lost in-memory context while writes persisted,"
					+ " or TTL cleanup; compensation lost -- manual reconciliation required)."
					+ " tid={}, zeze={}", tid, zezeName);
	}

	/** redo整体失败分诊（原redo()唯一catch的逻辑；点表读取移入快照收集后，
	 * 快照收集与锁外重发两处共用）：记录年龄≥RedoFailAgedBudgetMs（分诊依据见该
	 * 常量）的本轮异常按tid登记redoFailWarnedTids并error一次（成因片段=异常类名+
	 * 消息，首见形态即可）；未达门槛记全量带栈日志（瞬态/年轻失败完全可见，行数
	 * 有界：门槛/60s轮后转去重形态）。旧格式记录（stamp=0）年龄视为无穷（对齐
	 * 快照收集读时戳口径），首轮异常即分诊。 */
	private void logRedoFail(long tid, long stamp, Throwable ex) {
		var recordAge = System.currentTimeMillis() - stamp;
		if (recordAge >= RedoFailAgedBudgetMs) {
			if (redoFailWarnedTids.add(tid))
				logger.error("redo fail aged, keep record for settle. tid={}, age={}ms, cause={}: {}",
						tid, recordAge, ex.getClass().getName(), ex.getMessage());
		} else {
			logger.error("redo fail. tid={}", tid, ex);
		}
	}

	// redo的发送分流（决策语义见redoRecord）：saga参与方发FuncSagaEnd（cancel取反决策，
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

	/**
	 * saga步骤注册后的崩溃窗口先落（onz-01，调用方OnzTransaction.callSagaAsync）：saga参与方
	 * "发结果即本地提交"（OnzSaga.sendReadyAndWait），而perform的首条决策记录严格后置于
	 * txn.perform()返回——业务窗口（时长无上界）内进程硬崩溃时commitIndex无该tid记录，redo
	 * 迭代不到，已提交步骤的补偿永久丢失且协调者侧零痕迹（参与方仅TTL清理warn）。每个saga步骤
	 * 注册后即落/更新一次ePreparing快照（含当时全部已注册参与方），崩溃后redo按本记录对已见
	 * 步骤补发FuncSagaEnd(cancel)（参与方上下文在TTL预算内仍在，见Onz.sagaContextTimeoutMs
	 * 契约）；成功/失败路径的既有saveCommitPoint(ePreparing)以完整快照覆写，正常收尾行为不变。
	 * 时戳随每次先落刷新：在途事务由登记skip保护（与年龄无关），年龄闸只对崩溃后残留自最后一步
	 * 起算，语义不变。
	 * <p>
	 * 不覆盖既有eCommitting（dbLock内读判）：续作越过pendingAsync契约迟到注册（嵌入方提前清旗
	 * 的误用形态）不得把已持久化的commit决策翻转为rollback——那会让redo对调用方已按成功收场的
	 * 事务补发cancel，制造真分歧；正常路径不可达该分支（waitPendingAsync先于commit清空续作）。
	 * 毒值（解码失败）按可覆写处理，覆写即修复。
	 * <p>
	 * best-effort：落库失败（stopped拒绝/RocksDB错误）只记error不外抛——外抛会中断业务的步骤
	 * 发送链，改变perform既有语义；此刻崩溃窗口的补偿不可得，活进程仍靠主路径落库与rollback
	 * 投递兜底（对齐exception路径saveCommitPoint失败的既定口径）。
	 */
	void saveSagaPreparingForCrashWindow(OnzTransaction<?, ?> txn) {
		var tidBytes = new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
		dbLock.lock();
		try {
			if (stopped)
				return; // 停机窗口的先落不可得：主路径saveCommitPoint同样被拒，catch→rollback链兜底，不记日志防停机噪声
			var current = commitIndex.get(tidBytes);
			if (current != null) {
				try {
					if (ByteBuffer.Wrap(current).ReadUInt() == eCommitting) {
						logger.warn("onz saga step: 迟到注册的步骤命中已持久化的eCommitting，跳过先落"
										+ "（续作越过pendingAsync契约，该步骤的写入无补偿通道）. tid={}", txn.getOnzTid());
						return;
					}
				} catch (Throwable ignored) {
					// 毒值按可覆写处理：不因毒值放弃先落
				}
			}
			saveCommitPoint(tidBytes, txn.buildSavedCommits(), ePreparing); // dbLock可重入
		} catch (Throwable ex) {
			logger.error("onz saga step: saveCommitPoint(ePreparing) 先落失败，perform业务窗口的崩溃补偿不可用. tid={}",
					txn.getOnzTid(), ex);
		} finally {
			dbLock.unlock();
		}
	}

	void removeCommitRecord(byte[] tidBytes) {
		// 同saveCommitPoint纳入dbLock+锁内stopped双检：commit()两处调用
		//（失败分支/成功路径）都跑在业务线程。redoRecord经removeRedoRecordIfUnchanged的
		// 调用点与settleStuckRecord本就在dbLock域内（ReentrantLock可重入，行为不变）。
		// stopped时拒删不是错误：与下方
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

		redoDaemon.stop(); // 有界等待在飞一轮（预算=timeoutMs+5s；轮长受RedoRoundNetworkBudgetMs约束，常态不逃逸）

		// redo轮的两段式只在快照收集/删除复核两段持dbLock（重发网络在锁外）；持有dbLock
		// 直到关库完成，与迟到轮次的锁内段互斥（锁内stopped双检拒绝库访问）；锁外重发段
		// 在停机后触网络失败（getZezeInstance拒stopped/连接已停）走redoRecord的catch
		// 保留记录，方向安全。
		dbLock.lock();
		try {
			// 停缓存connector并清表：它们挂在onzAgent的服务上，必须在其停止前显式停掉
			// 自动重连，否则服务关socket反而触发无限重连（1s起、上限8s）。
			for (var connector : instances.values()) {
				try {
					var so = connector.getSocket();
					if (null != so)
						so.closeGracefully();
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
				// 空段（""/"a;;b"）拒绝：空名注册+subscribeService("")后按空名解析恒失败，
				// 错误配置须在构造期暴露（对齐duplicate检查的严格度）。
				if (zeze.isBlank())
					throw new IllegalArgumentException("empty zeze name. zezes=" + specialZezeNames);
				// 别名含'='拒绝（onz-03）：持久化编解码依赖"集群名不含'='"的不变式——
				// 非共享构造器按zeze.split("=")解析，'='左段天然无'='；共享构造器的别名
				// 无此约束，含'='的别名（如"saga=foo"）以procedure参与方裸名持久化后，
				// redo的decodeSagaParticipant按"saga="前缀误解码为saga参与方，Commit决策
				// 被误路由成FuncSagaEnd发给不存在的集群——决策永不送达且记录永不收敛
				//（见OnzTransaction.SagaParticipantPrefix）。对齐空名/duplicate的构造期暴露。
				if (zeze.indexOf('=') >= 0)
					throw new IllegalArgumentException("zeze name contains '='. zeze=" + zeze
							+ " zezes=" + specialZezeNames);
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
	 * 失败/异常路径rollback前的pendingAsync有界排空（onz-02）：与成功路径commit前的
	 * waitPendingAsync()对称——rollback内cancelSaga只遍历当下已注册的saga步骤（弱一致
	 * 快照），排空确保在途续作（callSagaAsync回调线程，setPendingAsync存在的原因）先注册
	 * 完再遍历，迟到注册的步骤尽收补偿。
	 * <p>
	 * 清旗依赖推演（不等死决策的依据）：续作等待的future（callSagaAsync/callProcedureAsync
	 * 返回值）由参与方应答/rpc超时/发送失败三者之一终结（OnzAgent回调三分类+Send false即完成，
	 * 无永pending形态），且参与方先发业务结果后等决策（procedure）或发结果即提交（saga）
	 * ——future的完成不依赖协调者的Commit/Rollback/FuncSagaEnd，排空在rollback之前不会与
	 * 续作互等死锁。永不清旗的可能只来自嵌入方（忘清旗/续作阻塞在业务资源/超长链），
	 * 故有界（上限=flushTimeout，单步在途预算，见waitPendingAsync(long)）：超时/中断warn
	 * 暴露"迟到注册可能失补偿"后继续rollback——宁可少等不可挂死perform。
	 */
	private static void drainPendingAsyncBeforeRollback(OnzTransaction<?, ?> txn, String cause) {
		try {
			if (!txn.waitPendingAsync(txn.getFlushTimeout()))
				logger.warn("onz perform: rollback前排空pendingAsync超时（续作仍在途，其迟到注册的步骤可能失补偿）. tid={}, {}",
						txn.getOnzTid(), cause);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt(); // 恢复中断标志：调用方线程的中断语义不因排空丢失
			logger.warn("onz perform: rollback前排空pendingAsync被中断（续作仍在途，其迟到注册的步骤可能失补偿）. tid={}, {}",
					txn.getOnzTid(), cause, ex);
		}
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
		// 登记失败（同实例并发perform/跨实例tid复用的嵌入方误用）原样上抛、不得进入主体（onz-02）：
		// 败者若走下去，catch路径会以同tid落ePreparing（空快照）覆写胜者的决策记录、rollback()
		// 对空参与方恒allDelivered后removeCommitRecord删掉胜者记录，finally的removeTransaction
		// 更会摘掉胜者的在途登记——redo随即按"登记不在+超龄"对存活事务补发Rollback，误杀活事务。
		// tid的在途登记唯一属于addTransaction的胜者；正常路径AutoKey的tid不重复，此形态必为误用，
		// 响亮失败优于静默破坏。
		onzAgent.addTransaction(txn);
		try {
			var rc = txn.perform();
			var state = txn.buildSavedCommits();
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			saveCommitPoint(tidBytes, state, ePreparing);
			// 这里和下面的txn.commit分成两步saveCommitPoint，
			// 中间没有做太多额外的事情，但为了明确两个事务状态，仍然分开。原因如下：
			// 参考Dbh2的两步：由于Dbh2一开始就知道所有的服务器，所以可以一开始就保存一次ePreparing，
			// 而这上面的perform是边执行边产生服务器地址，无法一开始保存事务状态。
			// 最严格的做法是每产生一个服务器地址，就写一次ePreparing（包含所有的服务器地址）——
			// saga参与方已按此实现（callSagaAsync注册后即先落，见saveSagaPreparingForCrashWindow，
			// onz-01：saga发结果即本地提交，perform业务窗口崩溃时该记录是已提交步骤唯一的补偿通道）；
			// procedure参与方无需先落（ready超时自愈回滚，结局一致），此处仍等待perform完成后统一落。
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
			// 失败路径对称排空（onz-02）：成功路径在commit前waitPendingAsync等齐异步续作，
			// 失败路径原先不排空直接rollback——续作在cancelSaga两轮弱一致遍历之后/rollback
			// 之后注册的步骤：FuncSaga照常发出、参与方发结果即本地提交（OnzSaga.
			// sendReadyAndWait），cancelSaga已跑完不再补偿、上面的ePreparing快照早于该注册
			// redo补发也不含——已提交步骤永久失补偿（静默部分提交）。排空后cancelSaga的
			// 活表遍历尽收迟到注册（有界等待，见drainPendingAsyncBeforeRollback）。
			drainPendingAsyncBeforeRollback(txn, "perform rc=" + rc);
			// 快照重建+重存（对齐上面成功路径waitPendingAsync后重建快照的语义）：排空窗口内
			// 注册的迟到参与方更新进ePreparing记录——cancelSaga投递失败（发送失败/超时，此时
			// rollback()返回false，见下）或rollback进行中进程崩溃时，redo是唯一补发通道，记录
			// 必须覆盖完整参与方列表。状态仍为ePreparing（决策不变）；时戳刷新至多把该记录的
			// redo年龄闸推迟一个排空窗口，无害。
			state = txn.buildSavedCommits();
			saveCommitPoint(tidBytes, state, ePreparing);
			// 全量投递了结即删记录（F3，对齐commit()失败分支if(rollback())removeCommitRecord
			// 的先例与redo的removeOk收敛语义）：补偿已确认送达，保留只会让redo先等
			// RedoPreparingMinAgeMs年龄闸、再对全部参与方幂等空转重发一轮（参与方收重复决策）；
			// 投递不确定（发送失败/超时/致命应答）时保留ePreparing交redo补发收敛。
			// 删点先于finally摘登记：redo对登记中的ePreparing恒skip（collectRedoCandidates），
			// 本删除与redo无并发窗口；崩溃落在save与remove之间则记录留库，redo幂等补发，安全。
			if (txn.rollback())
				removeCommitRecord(tidBytes);
			return rc;

		} catch (Throwable ex) {
			// 同rc!=0（onz-02同根）：异常路径的rollback同样先有界排空——txn.perform()抛出时
			// 续作可能在途，且本路径可能尚无ePreparing记录（saveCommitPoint未到达或自身失败），
			// cancelSaga的活表遍历是迟到注册唯一的补偿机会，排空让遍历尽量收全。
			drainPendingAsyncBeforeRollback(txn, "perform exception");
			// 快照重建+落ePreparing（F1，对齐上方rc!=0路径的既有形态）：此前异常路径无
			// 任何决策记录，cancelSaga投递失败时redo看不到、参与方上下文滞留至TTL被无补偿移除
			// ——已提交步骤永久失补偿且无对账通道。此处落记录使redo成为补发兜底。
			// best-effort：落库失败（stopped拒绝/RocksDB错误）只记日志，不得吞掉/替换正在传播
			// 的原始异常，也不得跳过下面的rollback（参与方此刻就靠它同步补偿）。
			// commit()内部失败抛出前已自行rollback（幂等可重复），此处重存至多造成一次
			// 记录重建+重投递的空转，无正确性影响；不会覆盖存活的eCommitting（commit()仅在
			// saveCommitPoint(eCommitting)失败即未持久化时才抛出）。
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			try {
				saveCommitPoint(tidBytes, txn.buildSavedCommits(), ePreparing);
			} catch (Throwable saveEx) {
				logger.error("onz perform: exception path saveCommitPoint(ePreparing) fail, "
						+ "redo兜底不可用，补偿仅剩本次rollback投递. tid={}", txn.getOnzTid(), saveEx);
			}
			// 消费allDelivered（F3，同rc!=0路径与commit()失败分支）：全量投递了结即删记录，
			// 投递不确定时保留交redo补发收敛；无记录时删除为幂等no-op。
			if (txn.rollback())
				removeCommitRecord(tidBytes);
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
