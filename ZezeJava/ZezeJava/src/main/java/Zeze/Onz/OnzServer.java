package Zeze.Onz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
import org.jspecify.annotations.Nullable;

/**
 * Onz 分布式事务协调者服务：管理网络通信、决策持久化、两阶段提交/Saga 补偿与后台 redo 恢复，支持多实例独立或交叉部署。
 */
public class OnzServer extends AbstractOnz {
	private static final Logger logger = LogManager.getLogger(OnzServer.class);

	private final OnzAgent onzAgent;
	private final boolean sharedServiceManager;
	private final ConcurrentHashMap<String, AbstractAgent> zezes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Connector> instances = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, OnzTransactionStub<?, ?>> remoteStubs = new ConcurrentHashMap<>();
	private final OnzServerService service;

	private final RocksDatabase database;
	private final RocksDatabase.Table commitPoint;
	private final RocksDatabase.Table commitIndex;
	// 决策与 saga 投递意图必须先于远端效果 fsync，否则断电后丢失唯一重做/补偿记录。
	private WriteOptions writeOptions = RocksDatabase.getSyncWriteOptions();
	// 周期守护：redoTimer 在 worker 池运行，stop 时有界等待在途轮次结束。
	private final DaemonTimer redoDaemon = new DaemonTimer("OnzServer.redoTimer", 60_000, this::redoTimer);
	private final AbstractAgent myServiceManager;
	private final AutoKey onzTidAutoKey;

	// 生命周期：stop 幂等且为终态，之后拒绝新工作且不可再次 start。
	private volatile boolean stopped;
	// getZezeInstance 的“选择→创建→登记”按集群名加锁串行化。
	private final ConcurrentHashMap<String, ReentrantLock> nameLocks = new ConcurrentHashMap<>();
	// 保护 RocksDB 访问与 database.close() 互斥：仅覆盖毫秒级库读写/快照收集，不持锁等待网络 RPC。
	private final ReentrantLock dbLock = new ReentrantLock();

	public long nextOnzTid() {
		return onzTidAutoKey.next();
	}

	/** SM 代理启动并等待就绪（Raft 首次选主可能失败一次，重试一次）。 */
	private static void startAgentAndWaitReady(AbstractAgent agent) throws Exception {
		agent.start();
		try {
			agent.waitReady();
		} catch (Exception ignored) {
			agent.waitReady();
		}
	}

	/** 构造或启动失败回滚时的尽力释放，失败仅记日志。 */
	private static void rollbackClose(String what, java.io.@Nullable Closeable resource) {
		if (resource == null)
			return;
		try {
			resource.close();
		} catch (Throwable e) {
			logger.error("rollback close {}", what, e);
		}
	}

	/**
	 * 供调用方明确选择持久化与性能取舍；关闭 sync 或 WAL 会绕过断电恢复保证。
	 * 选项生命周期由调用方管理，使用期间不得关闭；本服务不会关闭传入选项。
	 */
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
		var createdServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (createdServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		myServiceManager = createdServiceManager;
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
				if (zezeNameAndConfig[0].isBlank())
					throw new IllegalArgumentException("empty zeze name. zezes=" + zezeConfigs);
				if (this.zezes.containsKey(zezeNameAndConfig[0]))
					throw new RuntimeException("duplicate zeze=" + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
				var zezeConfig = Config.load(zezeNameAndConfig[1]);
				var serviceManager = Application.createServiceManager(zezeConfig, "OnzServerServiceManager");
				if (serviceManager == null)
					throw new RuntimeException("serviceManager not found for " + zezeNameAndConfig[0] + " zezes=" + zezeConfigs);
				// 先登记再启动，确保后续步骤失败时 catch 块能回收该实例。
				this.zezes.put(zezeNameAndConfig[0], serviceManager);
				startAgentAndWaitReady(serviceManager);
				serviceManager.subscribeService(new BSubscribeInfo(Onz.eServiceName));
			}
			this.sharedServiceManager = false;

			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造失败时逆序回收已获取资源，避免线程、端口或 RocksDB 目录锁泄漏。
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
			// 启动半途失败时调用 stop 回收资源并进入终态。
			try {
				stop();
			} catch (Throwable e) {
				logger.error("rollback start", e);
			}
			throw ex;
		}
	}

	// ePreparing 最小 redo 年龄：覆盖未登记窗口，避免误向正常进行中的事务补发不可逆 Rollback。
	private static final long RedoPreparingMinAgeMs = 120_000;

	// 在途超龄 ePreparing 告警去重集合，perform 结束时回收。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> hangWarnedTids = ConcurrentHashMap.newKeySet();

	// 超龄 eSagaNotFound 分诊预算，与参与方默认 saga 上下文 TTL 对齐。
	private static final long SagaNotFoundAgedBudgetMs = Onz.eDefaultSagaContextTimeoutMs;

	// 超龄 NotFound 及成功步骤上下文消失嫌疑告警去重集合，记录收敛或人工清算时回收。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> agedNotFoundWarnedTids = ConcurrentHashMap.newKeySet();

	// 未知 commitIndex state 告警去重集合，仅由 settleStuckRecord 人工清算回收。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> unknownStateWarnedTids = ConcurrentHashMap.newKeySet();

	// redo 收到非 0 非 NotFound 错误码的告警去重集合，记录收敛或清算时回收。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> redoResultWarnedTids = ConcurrentHashMap.newKeySet();

	// redo 整体异常持续超龄的告警与可清算分诊阈值（远大于在途窗口与 redo 周期）。
	private static final long RedoFailAgedBudgetMs = Onz.eDefaultSagaContextTimeoutMs;

	// 超龄 redo 整体失败告警去重集合，记录收敛或人工清算时回收。
	private final ConcurrentHashMap.KeySetView<Long, Boolean> redoFailWarnedTids = ConcurrentHashMap.newKeySet();

	private void redoTimer() {
		if (stopped)
			return;
		// 两段式 redo：锁内仅收集快照，网络重发在锁外执行，删除前再回锁复核，避免阻塞业务写库点。
		var records = collectRedoCandidates();
		var n = records.size();
		if (n == 0) {
			redoResumeKey = null;
			return;
		}
		// 轮转续扫：预算耗尽后下轮从上次游标后继续；必须按无符号字节序与 RocksDB 键序一致。
		var start = 0;
		if (redoResumeKey != null)
			while (start < n && java.util.Arrays.compareUnsigned(records.get(start).key, redoResumeKey) <= 0)
				start++;
		var deadline = System.currentTimeMillis() + RedoRoundNetworkBudgetMs;
		byte[] lastAttempted = null;
		var completed = true;
		for (var k = 0; k < n; k++) {
			if (stopped)
				break;
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

	/**
	 * 在 dbLock 内迭代 commitIndex、过滤状态/年龄/在途登记并解码 commitPoint，返回键序候选快照。
	 * 毒条目、未知 state 与点表解码失败均单条隔离，不阻塞后续未决记录的恢复。
	 */
	private ArrayList<RedoRecord> collectRedoCandidates() {
		var records = new ArrayList<RedoRecord>();
		dbLock.lock();
		try {
			if (stopped)
				return records;
			try (var it = commitIndex.iterator()) {
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					var key = it.key();
					var value = it.value();
					// 单条隔离索引解码失败的毒条目，避免阻断后续记录遍历。
					int state;
					long tid;
					long stamp;
					try {
						var bb = ByteBuffer.Wrap(value);
						state = bb.ReadUInt();
						tid = ByteBuffer.ToLongBE(key, 0);
						// 新格式含 8B BE 写入时戳；无时戳的旧格式记录按 0 处理（立即 redo）。
						stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
					} catch (Throwable ex) {
						logger.error("onz redo: commitIndex毒条目解码失败跳过（key={}，tid={}），留库人工排查",
								java.util.Arrays.toString(key),
								key.length >= 8 ? ByteBuffer.ToLongBE(key, 0) : null, ex);
						continue;
					}
					// 仅跳过在途登记中的 ePreparing（防止误向活跃事务发不可逆 Rollback）；eCommitting 重发幂等，无需跳过。
					boolean commitDecision;
					switch (state) {
					case eCommitting:
						commitDecision = true;
						break;
					case ePreparing:
						var age = System.currentTimeMillis() - stamp;
						if (onzAgent.hasTransaction(tid)) {
							if (age >= 2 * RedoPreparingMinAgeMs && hangWarnedTids.add(tid))
								logger.warn("onz redo: tid={} 登记中ePreparing年龄{}ms，疑似挂死 perform，redo 封锁中", tid, age);
							continue;
						}
						if (age < RedoPreparingMinAgeMs)
							continue; // 尚未超过最小 redo 年龄窗口，留待下轮
						commitDecision = false;
						break;
					default:
						// 未知 state 不盲目补发决策，按 tid 去重告警后留库人工排查。
						if (unknownStateWarnedTids.add(tid))
							logger.error("onz redo: commitIndex条目state={} 未知（tid={}），跳过不redo，条目留库人工排查",
									state, tid);
						continue;
					}
					// 点表读取与解码放在锁内完成，避免锁外重发阶段访问 database。
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

	// redo 等待 saga 参与方 FuncSagaEnd 的超时（覆盖参与方 businessLock 等待，且小于 redo 周期）。
	private static final int RedoSagaEndTimeoutMs = 30_000;

	// 单轮 redo 锁外网络总预算，约束在 DaemonTimer 任务看门狗（120s）以内，超预算部分留待下轮轮转。
	private static final long RedoRoundNetworkBudgetMs = 60_000;

	// 上轮预算耗尽时最后尝试的键（仅在串行的 redo 轮线程内读写，null 表示下轮从头开始）。
	private byte @Nullable [] redoResumeKey;

	/** redo 候选的锁内快照：携带键、原索引值（供删前复核）、决策方向、时戳与参与方列表。 */
	private record RedoRecord(byte[] key, long tid, byte[] indexValue, boolean commitDecision, long stamp,
			BSavedCommits.Data savedCommits) {
	}

	// 按参与方类型幂等重发决策（procedure 发 Commit/Rollback，saga 发 FuncSagaEnd）。
	private void redoRecord(RedoRecord rec) {
		var tid = rec.tid;
		var zezeOnzs = new HashMap<String, Connector>();
		try {
			var deliveries = new ArrayList<RedoDelivery>();
			Exception deliveryFailure = null;
			for (var e : rec.savedCommits.getOnzs()) {
				try {
					var sagaName = OnzTransaction.decodeSagaParticipant(e);
					var zezeName = sagaName != null ? sagaName : e;
					var socket = zezes.containsKey(zezeName)
							? getZezeInstance(zezeName)
							: openRedoConnection(zezeOnzs, zezeName).GetReadySocket();
					deliveries.add(sendRedoDecision(socket, tid, sagaName, rec.commitDecision));
				} catch (Exception ex) {
					if (deliveryFailure == null)
						deliveryFailure = ex;
				}
			}
			var removeOk = deliveryFailure == null;
			for (var delivery : deliveries) {
				try {
					delivery.future.await();
				} catch (Exception ex) {
					removeOk = false;
					if (deliveryFailure == null)
						deliveryFailure = ex;
					continue;
				}
				// 非零结果码也会正常完成 future，仍需检查参与方应答。
				var rpc = delivery.rpc;
				if (rpc.getResultCode() == 0)
					continue;
				// 参与方超时自愈回滚后收到迟到决策返回一次性 eDivergence；重发无意义，记录 error 后按该参与方了结。
				if (IModule.getErrorCode(rpc.getResultCode()) == AbstractOnz.eDivergence) {
					logger.error("onz redo: 参与方应答eDivergence（已超时回滚的参与方收到迟到决策"
									+ "=部分提交分歧/预算违例信号）. tid={}, decision={}, resultCode={}",
							tid, rec.commitDecision ? "eCommitting" : "ePreparing", rpc.getResultCode());
					continue;
				}
				// eSagaNotFound 分诊：超龄或 commit(end) 或已登记丢失嫌疑的均保留记录待对账，其余年轻 rollback 视为良性终态移除。
				if (IModule.getErrorCode(rpc.getResultCode()) == AbstractOnz.eSagaNotFound) {
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
						// commit 决策的年轻 NotFound 保留重试直至超龄终判。
						removeOk = false;
					} else {
						if (agedNotFoundWarnedTids.contains(tid))
							removeOk = false;
						else {
							logger.info("onz redo: saga参与方应答eSagaNotFound，rollback决策年轻记录按良性终态移除"
											+ "（tid={}, age={}ms）", tid, recordAge);
						}
					}
					continue;
				}
				removeOk = false;
				// 按 tid 去重告警，避免确定性错误每轮 redo 刷屏。
				if (redoResultWarnedTids.add(tid))
					logger.error("redo result error, keep record for retry. tid={}, resultCode={}",
							tid, rpc.getResultCode());
			}
			if (deliveryFailure != null)
				logRedoFail(tid, rec.stamp, deliveryFailure);
			if (removeOk)
				removeRedoRecordIfUnchanged(rec);
		} catch (Throwable ex) {
			logRedoFail(tid, rec.stamp, ex);
		} finally {
			for (var zeze : zezeOnzs.values())
				zeze.stop();
		}
	}

	/**
	 * 回锁复核 commitIndex 原值未变后再删除，防止并发终结或外部改库导致的误删；stopped 时静默跳过留待下次启动恢复。
	 */
	private void removeRedoRecordIfUnchanged(RedoRecord rec) throws RocksDBException {
		dbLock.lock();
		try {
			if (stopped)
				return;
			var current = commitIndex.get(rec.key);
			if (current == null) {
				recycleRedoWarnTids(rec.tid);
				return;
			}
			if (!java.util.Arrays.equals(current, rec.indexValue)) {
				logger.warn("onz redo: 快照后决策记录已变化，保留待下轮分诊. tid={}", rec.tid);
				return;
			}
			if (tryRemoveCommitRecord(rec.key))
				recycleRedoWarnTids(rec.tid);
		} finally {
			dbLock.unlock();
		}
	}

	/** 记录收敛后回收 redo 告警去重集合中的 tid（unknownState 条目仅由 settleStuckRecord 回收）。 */
	private void recycleRedoWarnTids(long tid) {
		agedNotFoundWarnedTids.remove(tid);
		redoResultWarnedTids.remove(tid);
		redoFailWarnedTids.remove(tid);
	}

	/** 登记已应答成功步骤在 cancel 时出现 eSagaNotFound 的丢失嫌疑，复用 agedNotFoundWarnedTids 供 redo 保守保留与人工清算。 */
	void noteSagaContextLost(long tid, String zezeName) {
		if (agedNotFoundWarnedTids.add(tid))
			logger.error("onz saga context lost: 已应答成功步骤的上下文在cancel送达前消失"
					+ " (participant process lost in-memory context while writes persisted,"
					+ " or TTL cleanup; compensation lost -- manual reconciliation required)."
					+ " tid={}, zeze={}", tid, zezeName);
	}

	/** 记录 redo 整体异常：超龄后按 tid 去重告警并登记可清算集合，未超龄打印完整堆栈。 */
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

	// redo 投递记录：配对 RPC 与等待 future，便于统一 await 后检查结果码。
	private record RedoDelivery(Rpc<?, ?> rpc, TaskCompletionSource<?> future) {
	}

	private static RedoDelivery sendRedoDecision(AsyncSocket socket, long tid,
			@Nullable String sagaName, boolean commitDecision) {
		if (sagaName != null) {
			var r = new FuncSagaEnd();
			r.Argument.setOnzTid(tid);
			r.Argument.setCancel(!commitDecision);
			return new RedoDelivery(r, r.SendForWait(socket, RedoSagaEndTimeoutMs));
		}
		Rpc<BCommit.Data, EmptyBean.Data> r = commitDecision ? new Commit() : new Rollback();
		r.Argument.setOnzTid(tid);
		return new RedoDelivery(r, r.SendForWait(socket));
	}

	void saveCommitPoint(byte[] tidBytes, BSavedCommits.Data bState, int state) throws RocksDBException {
		bState.setState(state);
		var bb = ByteBuffer.Allocate();
		bState.encode(bb);
		var bbIndex = ByteBuffer.Allocate(13);
		bbIndex.WriteUInt(state);
		bbIndex.WriteLong8BE(System.currentTimeMillis()); // ePreparing年龄判据，见redoTimer
		// 仅写库期间持 dbLock 并复查 stopped，与 stop() 关库互斥，避免向已关闭句柄写入。
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

	/** 步骤投递前持久保存完整参与方意图，失败必须阻止该步骤发送；不覆盖已存在的提交决定。 */
	void saveSagaPreparingForCrashWindow(OnzTransaction<?, ?> txn) {
		var tidBytes = new byte[8];
		ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
		dbLock.lock();
		try {
			if (stopped)
				throw new IllegalStateException("OnzServer stopped: saga step rejected. tid=" + txn.getOnzTid());
			var current = commitIndex.get(tidBytes);
			boolean committing = false;
			if (current != null) {
				try {
					committing = ByteBuffer.Wrap(current).ReadUInt() == eCommitting;
				} catch (Throwable ignored) {
					// 毒值按可覆写处理
				}
			}
			if (committing)
				throw new IllegalStateException("Onz saga step after commit decision. tid=" + txn.getOnzTid());
			saveCommitPoint(tidBytes, txn.buildSavedCommits(), ePreparing);
		} catch (RocksDBException ex) {
			throw Zeze.Util.Task.forceThrow(ex);
		} finally {
			dbLock.unlock();
		}
	}

	void removeCommitRecord(byte[] tidBytes) {
		tryRemoveCommitRecord(tidBytes);
	}

	private boolean tryRemoveCommitRecord(byte[] tidBytes) {
		// 持 dbLock 并复查 stopped；已停机或删库失败时保留记录，交下次启动 redo 恢复。
		dbLock.lock();
		try {
			if (stopped) {
				logger.error("OnzServer stopped: removeCommitRecord rejected, keep record for next-startup redo. tid={}",
						ByteBuffer.ToLongBE(tidBytes, 0));
				return false;
			}
			try {
				// commitIndex 与 commitPoint 同 batch 原子双删，避免残留孤儿点条目。
				try (var batch = database.borrowBatch()) {
					commitIndex.delete(batch, tidBytes);
					commitPoint.delete(batch, tidBytes);
					batch.commit(writeOptions);
				}
				return true;
			} catch (RocksDBException e) {
				logger.error("onz decision cleanup failed; keep record for retry. tid={}",
						ByteBuffer.ToLongBE(tidBytes, 0), e);
				return false;
			}
		} finally {
			dbLock.unlock();
		}
	}

	/**
	 * 人工清算滞留决策记录：运维对账完成后按 tid 删除记录并回收告警去重项。
	 * 仅放行已被协调者报告滞留的 tid（四类告警去重集合之一），删除前尽力输出审计日志。
	 *
	 * @return true 表示记录已关闭（含已不在库的幂等情况），false 表示被拒绝或删除失败
	 */
	public boolean settleStuckRecord(long tid) {
		if (stopped) {
			logger.error("onz settle rejected: OnzServer stopped. tid={}", tid);
			return false;
		}
		if (!(agedNotFoundWarnedTids.contains(tid) || redoResultWarnedTids.contains(tid)
				|| unknownStateWarnedTids.contains(tid) || redoFailWarnedTids.contains(tid))) {
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
			auditRetainedRecord(tid, tidBytes);
			if (!tryRemoveCommitRecord(tidBytes))
				return false;
			agedNotFoundWarnedTids.remove(tid);
			redoResultWarnedTids.remove(tid);
			unknownStateWarnedTids.remove(tid);
			redoFailWarnedTids.remove(tid);
			return true;
		} finally {
			dbLock.unlock();
		}
	}

	/** 删除滞留记录前尽力读取并记录审计信息（tid/state/age/onzs），读取失败不阻断清算。 */
	private void auditRetainedRecord(long tid, byte[] tidBytes) {
		var state = "未知";
		var age = "未知";
		var onzs = "未知";
		try {
			var indexValue = commitIndex.get(tidBytes);
			if (indexValue == null) {
				state = age = onzs = "已不在库";
			} else {
				var bb = ByteBuffer.Wrap(indexValue);
				state = String.valueOf(bb.ReadUInt());
				var stamp = bb.size() >= 8 ? bb.ReadLong8BE() : 0L;
				if (stamp != 0)
					age = (System.currentTimeMillis() - stamp) + "ms";
				var pointValue = commitPoint.get(tidBytes);
				if (pointValue != null) {
					var saved = new BSavedCommits.Data();
					saved.decode(ByteBuffer.Wrap(pointValue));
					onzs = String.valueOf(saved.getOnzs());
				} else {
					onzs = "无点条目";
				}
			}
		} catch (Throwable ex) {
			logger.warn("onz settle audit: 滞留记录审计读取部分失败（尽力携带）. tid={}", tid, ex);
		}
		logger.warn("onz settle audit: 清算滞留决策记录（人工对账完成，删除前留痕）. tid={}, state={}, age={}, onzs={}",
				tid, state, age, onzs);
	}

	private Connector openRedoConnection(HashMap<String, Connector> conns, String ip_port) {
		var conn = conns.computeIfAbsent(ip_port, __ -> {
			var newConn = new Connector(ip_port, false);
			newConn.SetService(onzAgent.getService());
			newConn.start();
			return newConn;
		});
		conn.GetReadySocket();
		return conn;
	}

	/**
	 * 幂等停止 OnzServer（终态，不可再次 start）：不做优雅排空，未决决策记录保留在库中由下次启动 redo 恢复。
	 * 按顺序尽力释放：停 redo 守护 → 停缓存连接器（须先于服务停止以防自动重连）→ 关 SM 代理 → 停服务 → 关库。
	 */
	public void stop() throws Exception {
		if (stopped)
			return;
		stopped = true;

		redoDaemon.stop();
		// 持 dbLock 直至关库完成，与迟到 redo 轮次及在途写库点互斥。
		dbLock.lock();
		try {
			// 先停连接器再停服务，避免服务关 socket 触发连接器持续重连。
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
		var createdServiceManager = Application.createServiceManager(myConfig, "OnzServerMyServiceManager");
		if (createdServiceManager == null)
			throw new RuntimeException("My ServiceManager not found");
		myServiceManager = createdServiceManager;
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
				if (zeze.isBlank())
					throw new IllegalArgumentException("empty zeze name. zezes=" + specialZezeNames);
				// 集群名禁止含 '='，避免与 saga 参与方持久化前缀（"saga="）冲突。
				if (zeze.indexOf('=') >= 0)
					throw new IllegalArgumentException("zeze name contains '='. zeze=" + zeze
							+ " zezes=" + specialZezeNames);
				if (this.zezes.containsKey(zeze))
					throw new RuntimeException("duplicate zeze=" + zeze + " zezes=" + specialZezeNames);
				this.zezes.put(zeze, sharedAgent);
				// 共享 SM 模式下逐集群名订阅，与 getZezeInstance 的按名查询及参与方 setRegisterServiceName 对齐。
				sharedAgent.subscribeService(new BSubscribeInfo(zeze));
			}
			this.sharedServiceManager = true;
			service = new OnzServerService(myConfig);
			onzAgent = new OnzAgent();
			RegisterProtocols(service);
		} catch (Throwable ex) {
			// 构造失败时逆序回收共享 SM、数据库与本地 SM。
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

		// 按集群名串行化连接选择、创建与登记，避免并发冷路径互相 stop 握手中的连接器。
		var nameLock = nameLocks.computeIfAbsent(zezeName, __ -> new ReentrantLock());
		nameLock.lock();
		try {
			// 加锁后复查，避免重复建连。
			connector = instances.get(zezeName);
			if (null != connector) {
				var socket = connector.TryGetReadySocket();
				if (null != socket)
					return socket;
				if (isAdvertised(onzServices, connector.getName()))
					// 目标地址仍在通告中，等待现有连接就绪而不替换。
					return connector.GetReadySocket();
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
				old.stop(); // 停止已失效旧地址的连接器，避免僵尸重连
			if (stopped) {
				// stop() 已在建连窗口内执行，撤销新建连接器以防残留自动重连。
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
	private static boolean isAdvertised(AbstractAgent.SubscribeState onzServices,
										String connectorName) {
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
	 * 回滚前有界等待 pendingAsync（上限 flushTimeout），确保在途异步续作尽量完成登记后再执行 cancelSaga；
	 * 超时或中断仅告警并继续回滚，不挂死 perform。
	 */
	private static void drainPendingAsyncBeforeRollback(OnzTransaction<?, ?> txn, String cause) {
		try {
			if (!txn.waitPendingAsync(txn.getFlushTimeout()))
				logger.warn("onz perform: rollback前排空pendingAsync超时（续作仍在途，其迟到注册的步骤可能失补偿）. tid={}, {}",
						txn.getOnzTid(), cause);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("onz perform: rollback前排空pendingAsync被中断（续作仍在途，其迟到注册的步骤可能失补偿）. tid={}, {}",
					txn.getOnzTid(), cause, ex);
		}
	}

	private void savePreparing(OnzTransaction<?, ?> txn, byte[] tidBytes) throws RocksDBException {
		// 与 callSagaAsync 的登记→保存同锁，旧快照不得覆盖已落盘的新参与方意图。
		txn.lock();
		try {
			saveCommitPoint(tidBytes, txn.buildSavedCommits(), ePreparing);
		} finally {
			txn.unlock();
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
		// 重复 tid 登记失败须直接抛出，避免进入 catch/finally 破坏胜者事务的记录与在途登记。
		onzAgent.addTransaction(txn);
		try {
			var rc = txn.perform();
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			// 与后续 eCommitting 分两步落盘：saga 步骤投递前已先落 ePreparing，此处统一落盘覆盖 procedure 参与方。
			savePreparing(txn, tidBytes);
			if (0 == rc) {
				txn.waitPendingAsync();
				// 异步续作完成后重建完整参与方快照再进入 eCommitting 提交。
				var state = txn.buildSavedCommits();
				txn.commit(tidBytes, state);
				txn.waitFlushDone();
				return 0;
			}
			// 失败路径先有界排空异步续作并刷新 ePreparing 快照，确保回滚与 redo 覆盖完整参与方。
			drainPendingAsyncBeforeRollback(txn, "perform rc=" + rc);
			savePreparing(txn, tidBytes);
			// 全量回滚投递了结即删记录（在 finally 摘除在途登记之前，与 redo 无并发冲突）；不确定时留库交 redo。
			if (txn.rollback())
				removeCommitRecord(tidBytes);
			return rc;

		} catch (Throwable ex) {
			if (txn.isCommitDecided()) {
				logger.error("onz perform failed after commit decision. tid={}", txn.getOnzTid(), ex);
				return 0;
			}
			// 异常路径同样排空异步续作并尽力落 ePreparing 供 redo 兜底，失败不阻断本次同步 rollback。
			drainPendingAsyncBeforeRollback(txn, "perform exception");
			var tidBytes = new byte[8];
			ByteBuffer.longBeHandler.set(tidBytes, 0, txn.getOnzTid());
			try {
				savePreparing(txn, tidBytes);
			} catch (Throwable saveEx) {
				logger.error("onz perform: exception path saveCommitPoint(ePreparing) fail, "
						+ "redo兜底不可用，补偿仅剩本次rollback投递. tid={}", txn.getOnzTid(), saveEx);
			}
			if (txn.rollback())
				removeCommitRecord(tidBytes);
			logger.error("", ex);
			return Procedure.Exception;

		} finally {
			onzAgent.removeTransaction(txn);
			// 回收超龄告警去重项，防止慢事务累积。
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

		var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument());
		var txn = stub.createTransaction(r.Argument.getFuncName(), buffer);
		var rc = perform(txn);
		if (0 != rc)
			return rc;

		var bbResult = ByteBuffer.Allocate();
		//noinspection DataFlowIssue
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
