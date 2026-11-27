package Zeze.Dbh2;

import java.util.Objects;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Raft.Log;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.Raft;
import Zeze.Raft.RaftLog;
import Zeze.Raft.UniqueRequestState;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskSpec;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
import Zeze.Util.RocksDatabase.Table;

/**
 * Dbh2 桶的 Raft 状态机：apply 各类日志到桶存储，管理桶内事务与负载统计。
 */
public class Dbh2StateMachine extends Zeze.Raft.StateMachine {
	private static final Logger logger = LogManager.getLogger(Dbh2StateMachine.class);
	private @Nullable Bucket bucket;
	private @Nullable TidAllocator tidAllocator;
	private WriteOptions writeOptions = RocksDatabase.getDefaultWriteOptions();
	private @Nullable Dbh2AppliedState appliedState;
	private boolean driveSplitSyncAfterApply;
	private boolean triggerNoTransactionAfterApply;
	private boolean publishTidRangeAfterApply;
	private long tidRangeStart;
	private long tidRangeEnd;
	private final ConcurrentHashMap<Long, Dbh2Transaction> transactions = new ConcurrentHashMap<>();

	// 分桶事务同步持久队列（复用 meta 列族：5+BE seq=记录，{6}=序号，{7}=水位），随桶快照与日志复制；
	// 换代递增 generation 以拒绝旧世代迟到 ACK；splitSyncLock 为叶子锁，保障 advance 与 clear 互斥。
	private static final byte SplitSyncRecordPrefix = 5;
	private static final byte[] SplitSyncSeqKey = {6};
	private static final byte[] SplitSyncWatermarkKey = {7};
	private @Nullable Table splitSyncTable;
	private volatile long splitSyncSeq;
	private volatile long splitSyncWatermark;
	private long splitSyncGeneration;
	private final ReentrantLock splitSyncLock = new ReentrantLock();

	private static byte[] splitSyncRecordKey(long seq) {
		var bb = ByteBuffer.Allocate(9); // 1 前缀 + 8 字节大端 seq，保证键序即投递序
		bb.WriteByte(SplitSyncRecordPrefix);
		bb.WriteLong8BE(seq);
		return bb.Bytes;
	}

	// eCommitting 悬挂告警阈值倍数（10 * bucketMaxTime），每 tid 仅告警一次
	private static final int CommittingHangWarnFactor = 10;
	private final ConcurrentHashMap.KeySetView<Long, Boolean> committingHangWarnedTids = ConcurrentHashMap.newKeySet();

	// 桶侧自主超时 undo 产生的未确认墓碑：释放锁并保留 blob+marker，供迟到 commit 复活或协调者/超窗清扫物理删除
	private static final class UndonePending {
		final Dbh2Transaction txn;
		final long tombstoneNanos;

		UndonePending(Dbh2Transaction txn) {
			this.txn = txn;
			this.tombstoneNanos = System.nanoTime();
		}
	}

	private final ConcurrentHashMap<Long, UndonePending> undonePending = new ConcurrentHashMap<>();

	boolean isTombstonedPending(long tid) {
		return undonePending.containsKey(tid);
	}

	/** 墓碑保留窗口（毫秒）：覆盖 prepare 最迟决策、CommitBatch 在途耗时与安全余量。 */
	long undoResurrectGraceMillis() {
		//noinspection DataFlowIssue
		var conf = dbh2.getDbh2Config();
		return conf.getRpcTimeout() + conf.getPrepareMaxTime() + 30_000L;
	}

	/** 清扫超窗未决墓碑：通过终局 LogUndoBatch 在全副本按日志序一致物理删除并告警。 */
	void expireDueTombstones(long graceMillis) {
		//noinspection DataFlowIssue
		expireDueTombstones(graceMillis, getRaft().getLogSequence().getTerm());
	}

	private void expireDueTombstones(long graceMillis, long expectedTerm) {
		var graceNanos = graceMillis * 1_000_000L;
		for (var e : undonePending.entrySet()) {
			if (System.nanoTime() - e.getValue().tombstoneNanos < graceNanos)
				continue;
			try {
				// 必须经 Raft 日志终局删除，保证与迟到 LogCommitBatch 在各副本按同一日志序收敛
				if (!tryAppendTimerLog(new LogUndoBatch(e.getKey(), true), expectedTerm))
					continue;
				logger.error("undo tombstone expired without coordinator resolution: tid={} (expiry undo"
						+ " appended to raft log; coordinator neither confirmed undo nor delivered commit"
						+ " within {}ms -- divergence possible, manual check)", e.getKey(), graceMillis);
			} catch (Exception ex) {
				logger.error("expire undo tombstone fail, retain for next round: tid={}", e.getKey(), ex);
			}
		}
	}

	private @Nullable Future<?> timer;
	private @Nullable CommitAgent commitAgent;
	private final @Nullable Dbh2 dbh2;
	// 访问由noTransactionLock保护：setupOneShotIfNoTransaction在user-task线程，triggerNoTransactionIf在raft apply线程。
	// 两个线程默认都是虚拟线程,ReentrantLock保证handle将来混入阻塞调用时也不pin载体(JDK21-23)。
	private final ReentrantLock noTransactionLock = new ReentrantLock();
	private @Nullable Runnable noTransactionHandle;

	final AtomicLong counterGet = new AtomicLong();
	private final AtomicLong counterPut = new AtomicLong();
	final AtomicLong sizeGet = new AtomicLong();
	private final AtomicLong sizePut = new AtomicLong();
	private final AtomicLong counterDelete = new AtomicLong();
	private final AtomicLong counterPrepareBatch = new AtomicLong();
	private final AtomicLong counterCommitBatch = new AtomicLong();
	private final AtomicLong counterUndoBatch = new AtomicLong();

	private long lastGet;
	private long lastPut;
	private long lastSizeGet;
	private long lastSizePut;
	private long lastDelete;
	private long lastPrepareBatch;
	private long lastCommitBatch;
	private long lastUndoBatch;
	private long lastReportTime = System.currentTimeMillis();
	private boolean loadSwitch = false;
	// load被loadMonitor定时器线程与setLoadSwitch（raft回调线程）并发调用，last*统计字段需要同步保护。
	private final ReentrantLock loadLock = new ReentrantLock();

	public void setLoadSwitch(boolean value) {
		loadLock.lock();
		try {
			load(); // 修改loadSwitch强制报告一次，达到清理旧的load的目的。loadLock可重入，持锁调用load()安全
			loadSwitch = value;
		} finally {
			loadLock.unlock();
		}
	}

	public double load() {
		loadLock.lock();
		try {
			var now = System.currentTimeMillis();
			// 下限 1ms 防同毫秒并发调用除零或时钟回拨产生异常速率
			var elapse = Math.max((now - lastReportTime) / 1000.0f, 0.001f);
			lastReportTime = now;

			var nowGet = counterGet.get();
			var nowPut = counterPut.get();
			var nowSizeGet = sizeGet.get();
			var nowSizePut = sizePut.get();
			var nowDelete = counterDelete.get();
			var nowPrepareBatch = counterPrepareBatch.get();
			var nowCommitBatch = counterCommitBatch.get();
			var nowUndoBatch = counterUndoBatch.get();

			var diffGet = nowGet - lastGet;
			var diffPut = nowPut - lastPut;
			var diffSizeGet = nowSizeGet - lastSizeGet;
			var diffSizePut = nowSizePut - lastSizePut;
			var diffDelete = nowDelete - lastDelete;
			var diffPrepareBatch = nowPrepareBatch - lastPrepareBatch;
			var diffCommitBatch = nowCommitBatch - lastCommitBatch;
			var diffUndoBatch = nowUndoBatch - lastUndoBatch;

			if (diffGet > 0 || diffPut > 0 || diffDelete > 0 || diffSizeGet > 0 || diffSizePut > 0
					|| diffPrepareBatch > 0 || diffCommitBatch > 0 || diffUndoBatch > 0) {
				lastGet = nowGet;
				lastPut = nowPut;
				lastSizeGet = nowSizeGet;
				lastSizePut = nowSizePut;
				lastDelete = nowDelete;
				lastPrepareBatch = nowPrepareBatch;
				lastCommitBatch = nowCommitBatch;
				lastUndoBatch = nowUndoBatch;

				var avgGet = diffGet / elapse;
				var avgPut = diffPut / elapse;
				var avgDelete = diffDelete / elapse;

				//noinspection StringBufferReplaceableByString
				var sb = new StringBuilder();
				sb.append("load: ");
				sb.append(Dbh2.formatMeta(requireBucket().getBucketMeta()));
				sb.append(" get=").append(avgGet);
				sb.append(" put=").append(avgPut);
				sb.append(" getSize=").append(diffSizeGet / elapse);
				sb.append(" putSize=").append(diffSizePut / elapse);
				sb.append(" delete=").append(avgDelete);
				sb.append(" prepare=").append(diffPrepareBatch / elapse);
				sb.append(" commit=").append(diffCommitBatch / elapse);
				sb.append(" undo=").append(diffUndoBatch / elapse);

				logger.info("{}", sb.toString());

				// 负载，put，delete全算，get算1%。
				return loadSwitch ? (avgPut + avgDelete) + avgGet * 0.01 : 0.0;
				// loadSwitch 没有生效前总是报告负载为0，但是上面的日志还是记录了。
			}
			return 0.0;
		} finally {
			loadLock.unlock();
		}
	}

	public Dbh2StateMachine(@Nullable Dbh2 dbh2) {
		this.dbh2 = dbh2;

		super.addFactory(LogPrepareBatch.TypeId_, LogPrepareBatch::new);
		super.addFactory(LogCommitBatch.TypeId_, LogCommitBatch::new);
		super.addFactory(LogUndoBatch.TypeId_, LogUndoBatch::new);
		super.addFactory(LogSetBucketMeta.TypeId_, LogSetBucketMeta::new);
		super.addFactory(LogAllocateTid.TypeId_, LogAllocateTid::new);

		super.addFactory(LogEndSplit.TypeId_, LogEndSplit::new);
		super.addFactory(LogSetSplittingMeta.TypeId_, LogSetSplittingMeta::new);
		super.addFactory(LogSplitPut.TypeId_, LogSplitPut::new);

		super.addFactory(LogEndMove.TypeId_, LogEndMove::new);
		super.addFactory(LogClearPendingSettle.TypeId_, LogClearPendingSettle::new);
	}

	public void setupOneShotIfNoTransaction(Runnable handle) {
		noTransactionLock.lock();
		try {
			if (transactions.isEmpty())
				handle.run(); // 异步网络调用，持锁执行无阻塞风险
			else
				noTransactionHandle = handle;
		} finally {
			noTransactionLock.unlock();
		}
	}

	public boolean hasNoTransactionHandle() {
		noTransactionLock.lock();
		try {
			return noTransactionHandle != null;
		} finally {
			noTransactionLock.unlock();
		}
	}

	public void clearNoTransactionHandle() {
		noTransactionLock.lock();
		try {
			noTransactionHandle = null;
		} finally {
			noTransactionLock.unlock();
		}
	}

	// 摘除 handle 后转投 raft 串行执行器重新过闸，确保门槛判定与 PrepareBatch 注册处于同一 FIFO 序
	private void triggerNoTransactionIf() {
		if (requireBucket().isApplying()) {
			triggerNoTransactionAfterApply = true;
			return;
		}
		Runnable handle;
		noTransactionLock.lock();
		try {
			if (!transactions.isEmpty() || null == noTransactionHandle)
				return;
			handle = noTransactionHandle;
			noTransactionHandle = null;
		} finally {
			noTransactionLock.unlock();
		}
		//noinspection DataFlowIssue
		Objects.requireNonNull(dbh2.getRaft()).executeUserTask(() -> setupOneShotIfNoTransaction(handle));
	}

	private Bucket requireBucket() {
		return Objects.requireNonNull(bucket, "bucket is not open");
	}

	public @Nullable Bucket getBucket() {
		return bucket;
	}

	public @Nullable TidAllocator getTidAllocator() {
		return tidAllocator;
	}

	public ConcurrentHashMap<Long, Dbh2Transaction> getTransactions() {
		return transactions;
	}

	public void openBucket() {
		if (bucket != null)
			return;
		//noinspection DataFlowIssue
		bucket = new Bucket(getRaft().getRaftConfig());
		requireBucket().setWriteOptions(writeOptions);
		tidAllocator = new TidAllocator();
		// 同步队列依附bucket的meta列族打开，并从落盘状态恢复序号/水位（restore路径同样经此重开）。
		try {
			splitSyncTable = requireBucket().getDb().getOrAddTable("meta");
			appliedState = new Dbh2AppliedState(requireBucket().getDb());
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
		splitSyncSeq = readSplitSyncCounter(SplitSyncSeqKey);
		splitSyncWatermark = readSplitSyncCounter(SplitSyncWatermarkKey);

		if (null == timer) {
			var period = getRaft().getRaftConfig().getAppendEntriesTimeout() + 200;
			var delay = Random.getInstance().nextLong(period);
			timer = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(delay, period);
		}

		if (null == commitAgent)
			commitAgent = new CommitAgent();
	}

	public void setWriteOptions(WriteOptions options) {
		writeOptions = options;
		if (bucket != null)
			requireBucket().setWriteOptions(options);
	}

	@Override
	public boolean hasAtomicAppliedState() {
		return true;
	}

	@Override
	public @Nullable AppliedWatermark readAppliedWatermark() throws RocksDBException {
		//noinspection DataFlowIssue
		return appliedState.readWatermark();
	}

	@Override
	public @Nullable UniqueRequestState getUniqueAppliedStub(IRaftRpc rpc) throws RocksDBException {
		//noinspection DataFlowIssue
		return appliedState.getUniqueAppliedStub(rpc);
	}

	@Override
	public void removeExpiredUniqueStubTables(long expired) throws Exception {
		//noinspection DataFlowIssue
		appliedState.removeExpiredUniqueStubTables(expired);
	}

	@Override
	public void apply(RaftLog holder) throws Exception {
		var heartbeat = isHeartbeatLog(holder);
		// ACK只写投递水位，不得插进队列换代与本批提交之间；poll也在此锁下读已提交队列。
		splitSyncLock.lock();
		try {
			requireBucket().beginApply();
			driveSplitSyncAfterApply = triggerNoTransactionAfterApply = false;
			publishTidRangeAfterApply = false;
			try {
				// leader-ready回调可能发起分桶恢复，须等应用完成事实落盘后触发。
				if (!heartbeat)
					super.apply(holder);
				var batch = requireBucket().getBatch();
				//noinspection DataFlowIssue
				appliedState.addToBatch(holder, batch);
				batch.commit(requireBucket().getWriteOptions());
				if (publishTidRangeAfterApply)
					//noinspection DataFlowIssue
					getTidAllocator().setRange(tidRangeStart, tidRangeEnd);
			} catch (Throwable e) {
				// 内存已改变而批次未完成时不能继续服务或原地重试，沿用dbh2的致命apply语义。
				logger.fatal("dbh2 apply failed at index={}", holder.getIndex(), e);
				//noinspection DataFlowIssue
				getRaft().fatalKill();
				throw Task.forceThrow(e);
			} finally {
				requireBucket().endApply();
			}
		} finally {
			splitSyncLock.unlock();
		}
		if (heartbeat)
			super.apply(holder);
		if (triggerNoTransactionAfterApply)
			triggerNoTransactionIf();
		if (driveSplitSyncAfterApply)
			//noinspection DataFlowIssue
			dbh2.driveSplitSync();
	}

	private long readSplitSyncCounter(byte[] key) {
		try {
			//noinspection DataFlowIssue
			var value = splitSyncTable.get(key);
			return null == value ? 0 : ByteBuffer.Wrap(value).ReadLong8BE();
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
	}

	private void onTimer() {
		var raft = Objects.requireNonNull(getRaft());
		var expectedTerm = raft.getLogSequence().getTerm();
		if (!raft.isLeader())
			return;

		// 按单调钟计龄规避墙钟跳变；逐条 try-catch 防止单次查询失败阻塞后续事务检查
		for (var e : transactions.entrySet()) {
			var tid = e.getKey();
			try {
				var t = e.getValue();
				//noinspection DataFlowIssue
				if (t.elapsedMillis() < dbh2.getDbh2Config().getBucketMaxTime())
					continue;
				//noinspection DataFlowIssue
				var state = commitAgent.query(t.getQueryIp(), t.getQueryPort(), tid, dbh2.getDbh2Config().getRpcTimeout());
				if (Commit.eCommitNotExist == state.getState()
						|| Commit.ePreparing == state.getState()) {
					logger.warn("timeout undo tid={} state={}", tid, state);
					tryAppendTimerLog(new LogUndoBatch(tid), expectedTerm);
				} else if (Commit.eCommitting == state.getState()
						&& t.elapsedMillis() >= dbh2.getDbh2Config().getBucketMaxTime() * CommittingHangWarnFactor
						&& committingHangWarnedTids.add(tid)) {
					// 协调者已进入 eCommitting，桶侧不可擅自回滚，仅告警等待协调者重做收敛
					logger.error("eCommitting transaction hang: tid={} query={}:{} age={}ms; "
									+ "coordinator commit-point exists but redo not arriving",
							tid, t.getQueryIp(), t.getQueryPort(), t.elapsedMillis());
				}
			} catch (Exception ex) {
				logger.warn("onTimer check hanging transaction fail. tid={}", tid, ex);
			}
		}
		expireDueTombstones(undoResurrectGraceMillis(), expectedTerm);
	}

	// restore/reset 持 Raft 锁取消并 join 本 timer：任务体不得反向阻塞该锁。
	// 锁忙保留事务/墓碑，下一轮重试；冻结任期防止旧协调查询越过换主任期。
	// 锁内只异步追加，不等待多数派/apply（等待者同样需要 Raft 锁）。
	private boolean tryAppendTimerLog(Log log, long expectedTerm) throws Exception {
		var raft = Objects.requireNonNull(getRaft());
		if (!raft.tryLock())
			return false;
		try {
			if (!raft.isLeader() || raft.getLogSequence().getTerm() != expectedTerm)
				return false;
			raft.getLogSequence().appendLog(log, null, expectedTerm);
			return true;
		} finally {
			raft.unlock();
		}
	}

	public void setBucketMeta(BBucketMeta.Data argument) {
		try {
			requireBucket().setBucketMeta(argument);
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	public void setSplittingMeta(BBucketMeta.Data argument) {
		try {
			requireBucket().setSplittingMeta(argument);
			// 新分桶世代起点防御清零，日志序上早于后续入队
			clearSplitSyncQueue();
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	// LogClearPendingSettle.apply 入口：仅在身份匹配时清除标志
	public void clearPendingSettle(BBucketMeta.Data to) {
		try {
			requireBucket().clearPendingSettle(to);
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	public void endMove(BBucketMeta.Data to) {
		try (var it = requireBucket().getData().iterator()) {
			// 先置死 meta 再 deleteToEnd，防止并发 Get 读到已删数据而误报“不存在”
			//noinspection DataFlowIssue
			var emptyMeta = requireBucket().getBucketMeta().copy();
			emptyMeta.setKeyFirst(Bucket.DeadBucketMetaBound);
			emptyMeta.setKeyLast(Bucket.DeadBucketMetaBound);
			requireBucket().setBucketMeta(emptyMeta);
			requireBucket().addMoveMetaHistory(to);
			requireBucket().setPendingSettle(null, to);
			requireBucket().deleteSplittingMeta();
			clearSplitSyncQueue();

			it.seekToFirst();
			requireBucket().deleteToEnd(it);
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	public void endSplit(BBucketMeta.Data from, BBucketMeta.Data to) {
		try (var it = requireBucket().getData().iterator()) {
			// 先收窄 meta 再 deleteToEnd，防止并发 Get 读到已删数据而误报“不存在”
			requireBucket().setBucketMeta(from);
			requireBucket().addSplitMetaHistory(from, to);
			requireBucket().setPendingSettle(from, to);
			requireBucket().deleteSplittingMeta();
			clearSplitSyncQueue();

			it.seek(from.getKeyLast().copyIf());
			requireBucket().deleteToEnd(it);
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	/// //////////////////////////////////////////////////////////////////
	// 下面这些方法用于Log.apply，不能失败，失败将停止程序。
	public void allocateTid(long range) {
		try {
			var start = requireBucket().getTid();
			var end = start + range;
			requireBucket().setTid(end);
			if (requireBucket().isApplying()) {
				// volatile区间可绕过Raft锁发号，必须在上界与水位落盘后才发布。
				publishTidRangeAfterApply = true;
				tidRangeStart = start;
				tidRangeEnd = end;
			} else
				//noinspection DataFlowIssue
				getTidAllocator().setRange(start, end);
		} catch (RocksDBException ex) {
			logger.error("", ex);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	private Dbh2Transaction getOrAddTransaction(BBatch.Data batch) {
		return transactions.computeIfAbsent(batch.getTid(),
				_tid -> {
					try {
						//noinspection DataFlowIssue
						return new Dbh2Transaction(dbh2, batch);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
	}

	public void prepareBatch(BBatch.Data batch) {
		try {
			counterPrepareBatch.incrementAndGet();
			var txn = getOrAddTransaction(batch);
			counterPut.addAndGet(txn.getBatch().getPuts().size());
			var totalPutValueSize = 0;
			for (var e : txn.getBatch().getPuts().entrySet()) {
				totalPutValueSize += e.getKey().size();
				totalPutValueSize += e.getValue().size();
			}
			sizePut.addAndGet(totalPutValueSize);
			counterDelete.addAndGet(txn.getBatch().getDeletes().size());
			txn.prepareBatch(requireBucket());
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	public void commitBatch(long tid) {
		try (var txn = transactions.remove(tid)) {
			counterCommitBatch.incrementAndGet();
			committingHangWarnedTids.remove(tid);
			if (null != txn) {
				// 分桶中事务同步入队先于本地落盘，外层 apply 批次原子提交
				enqueueSplitSync(txn.getBatch());
				txn.commitBatch(requireBucket());
			} else {
				var pending = undonePending.remove(tid);
				if (null != pending) {
					// 自主超时 undo 后收到迟到 LogCommitBatch，以提交为终局复活落盘并记 error
					logger.error("commitBatch resurrects tombstoned transaction (undo/commit fence conflict):"
							+ " tid={} (undo applied first but coordinator commit-point exists)", tid);
					enqueueSplitSync(pending.txn.getBatch());
					pending.txn.commitBatch(requireBucket());
				} else
					logger.warn("commitBatch but transaction not found. tid={}"
							+ " (duplicate delivery after commit -- normal idempotent redo, or late"
							+ " commit after expiry finalize; unknown tid repeating warrants manual check)", tid);
			}
			triggerNoTransactionIf();
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	/** Raft 日志 apply 入口（{@link LogUndoBatch}）：fromCoordinator=true 物理删除，false 记未确认墓碑。 */
	public void undoBatch(long tid, boolean fromCoordinator) {
		try (var txn = transactions.remove(tid)) {
			counterUndoBatch.incrementAndGet();
			committingHangWarnedTids.remove(tid);
			if (null != txn) {
				if (fromCoordinator)
					txn.undoBatch(requireBucket());
				else {
					txn.markTombstoned(requireBucket());
					undonePending.put(tid, new UndonePending(txn));
				}
			} else {
				// 重复的自主撤销不视为终局确认，仅协调者确认或超窗清扫才物理删除墓碑
				var pending = fromCoordinator ? undonePending.remove(tid) : undonePending.get(tid);
				if (pending == null)
					logger.warn("undoBatch but transaction not found. tid={}", tid);
				else if (fromCoordinator)
					pending.txn.undoBatch(requireBucket());
			}
			triggerNoTransactionIf();
		} catch (RocksDBException e) {
			logger.error("", e);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}

	// 分桶中事务同步入队（以 splittingMeta!=null 为日志序边界）；delete 编码为 Binary.Empty 墓碑 put
	private void enqueueSplitSync(BBatch.Data batch) throws RocksDBException {
		var splitting = requireBucket().getSplittingMeta();
		if (null == splitting)
			return;
		var puts = new HashMap<Binary, Binary>();
		for (var e : batch.getPuts().entrySet()) {
			if (splitting.getKeyFirst().compareTo(e.getKey()) <= 0)
				puts.put(e.getKey(), e.getValue());
		}
		for (var del : batch.getDeletes()) {
			if (splitting.getKeyFirst().compareTo(del) <= 0)
				puts.put(del, Binary.Empty); // 同批先 put 后 delete 时以墓碑覆盖，与 commitBatch 顺序一致
		}
		if (puts.isEmpty())
			return;
		// 记录与计数器加入同一 apply 批次，提交后才唤醒投递链
		var seq = splitSyncSeq + 1;
		var bb = ByteBuffer.Allocate();
		new BSplitPut.Data(true, puts).encode(bb);
		var key = splitSyncRecordKey(seq);
		requireBucket().put(Objects.requireNonNull(splitSyncTable), key, 0, key.length, bb.Bytes, bb.ReadIndex, bb.WriteIndex);
		splitSyncSeq = seq;
		var seqBb = ByteBuffer.Allocate(8);
		seqBb.WriteLong8BE(seq);
		requireBucket().put(Objects.requireNonNull(splitSyncTable), SplitSyncSeqKey, 0, SplitSyncSeqKey.length, seqBb.Bytes, 0, 8);
		if (requireBucket().isApplying())
			driveSplitSyncAfterApply = true;
		else
			//noinspection DataFlowIssue
			dbh2.driveSplitSync();
	}

	public static final class SplitSyncBatch {
		public final BSplitPut.Data data;
		public final long lastSeq;
		public final long generation;

		SplitSyncBatch(BSplitPut.Data data, long lastSeq, long generation) {
			this.data = data;
			this.lastSeq = lastSeq;
			this.generation = generation;
		}
	}

	// 按 seq 序合并取一批待投递记录（只读不推水位），供 Dbh2.driveSplitSync 单飞调用
	public @Nullable SplitSyncBatch pollSplitSync(int maxCount) {
		splitSyncLock.lock();
		try {
			var generation = splitSyncGeneration;
			var watermark = splitSyncWatermark;
			if (watermark >= splitSyncSeq)
				return null;
			var puts = new HashMap<Binary, Binary>();
			long lastSeq = watermark;
			//noinspection DataFlowIssue
			try (var it = splitSyncTable.iterator()) {
				it.seek(splitSyncRecordKey(watermark + 1));
				for (var count = 0; RocksDatabase.isValidOrThrow(it) && count < maxCount; it.next()) {
					var key = it.key();
					if (key.length != 9 || key[0] != SplitSyncRecordPrefix)
						break;
					var record = new BSplitPut.Data();
					record.decode(ByteBuffer.Wrap(it.value()));
					puts.putAll(record.getPuts());
					lastSeq = ByteBuffer.Wrap(key, 1, 8).ReadLong8BE();
					count++;
				}
			}
			return lastSeq == watermark ? null : new SplitSyncBatch(new BSplitPut.Data(true, puts), lastSeq, generation);
		} finally {
			splitSyncLock.unlock();
		}
	}

	// 投递 ACK 后在锁内校验世代并推进水位，拒绝已换代旧队列的迟到 ACK
	public void advanceSplitSyncWatermark(SplitSyncBatch batch) throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (batch.generation != splitSyncGeneration)
				return;
			advanceSplitSyncWatermark(batch.lastSeq);
		} finally {
			splitSyncLock.unlock();
		}
	}

	// 单调推进并持久化投递水位（换主后副本从各自旧水位幂等重投）
	public void advanceSplitSyncWatermark(long seq) throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (seq <= splitSyncWatermark)
				return;
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong8BE(seq);
			Objects.requireNonNull(splitSyncTable).put(requireBucket().getWriteOptions(), SplitSyncWatermarkKey, 0, SplitSyncWatermarkKey.length, bb.Bytes, 0, 8);
			splitSyncWatermark = seq;
		} finally {
			splitSyncLock.unlock();
		}
	}

	// endSplit 前置门槛：false 表示全部事务同步已送达目标桶
	public boolean hasPendingSplitSync() {
		return splitSyncWatermark < splitSyncSeq;
	}

	// 清空事务同步队列并递增世代号（EndSplit/EndMove 完结或 SetSplittingMeta 新世代起点调用）
	private void clearSplitSyncQueue() throws RocksDBException {
		splitSyncLock.lock();
		try {
			if (splitSyncSeq == 0 && splitSyncWatermark == 0)
				return;
			//noinspection DataFlowIssue
			try (var it = splitSyncTable.iterator()) {
				it.seek(splitSyncRecordKey(1));
				for (; RocksDatabase.isValidOrThrow(it); it.next()) { // 迭代器为快照视图，遍历中delete不影响遍历
					var key = it.key();
					if (key.length != 9 || key[0] != SplitSyncRecordPrefix)
						break;
					requireBucket().delete(Objects.requireNonNull(splitSyncTable), key);
				}
			}
			requireBucket().delete(Objects.requireNonNull(splitSyncTable), SplitSyncSeqKey);
			requireBucket().delete(Objects.requireNonNull(splitSyncTable), SplitSyncWatermarkKey);
			splitSyncSeq = 0;
			splitSyncWatermark = 0;
			splitSyncGeneration++; // 换代：作废一切在途/未决的旧世代投递ACK（seq即将从1重新分配）
		} finally {
			splitSyncLock.unlock();
		}
	}

	/// /////////////////////////////////////////////////////////
	// raft implement
	public @Nullable String getDbHome() {
		//noinspection DataFlowIssue
		return getRaft().getRaftConfig().getDbHome();
	}

	@Override
	public SnapshotResult snapshot(String path) throws Exception {
		long t0 = System.nanoTime();
		SnapshotResult result = new SnapshotResult();
		var cpHome = checkpoint(result);

		// 必须在 finally 中删除临时 checkpoint 目录，防止异常路径泄漏目录
		try {
			long t1 = System.nanoTime();
			var backupDir = Paths.get(Objects.requireNonNull(getDbHome()), "backup").toString();
			var backupFile = new File(backupDir);
			if (!backupFile.isDirectory() && !backupFile.mkdirs())
				logger.error("create backup directory failed: {}", backupDir);
			RocksDatabase.backup(RocksDatabase.DbType.eRocksDb, cpHome, backupDir);

			long t2 = System.nanoTime();
			Zeze.Raft.RocksRaft.Rocks.createZipFromDirectory(backupDir, path);

			long t3 = System.nanoTime();
			//noinspection DataFlowIssue
			getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);

			result.success = true;
			result.checkPointNanoTime = t1 - t0;
			result.backupNanoTime = t2 - t1;
			result.zipNanoTime = t3 - t2;
			result.totalNanoTime = System.nanoTime() - t0;
		} finally {
			LogSequence.deleteDirectory(new File(cpHome));
		}
		return result;
	}

	public void close() throws Exception {
		// 各清理步骤独立捕获异常，保证后续清理继续执行。
		for (var tran : transactions.values()) {
			try {
				tran.close();
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		transactions.clear();
		undonePending.clear();
		committingHangWarnedTids.clear();
		noTransactionLock.lock();
		try {
			noTransactionHandle = null;
		} finally {
			noTransactionLock.unlock();
		}

		splitSyncLock.lock();
		try {
			if (splitSyncSeq != 0 || splitSyncWatermark != 0)
				splitSyncGeneration++; // restore使旧库的在途ACK失效；空队列没有在途批次
			if (bucket != null) {
				writeOptions = requireBucket().getWriteOptions();
				try {
					requireBucket().close();
				} catch (Exception e) {
					logger.error("", e);
				}
				bucket = null;
			}
			appliedState = null;
			splitSyncTable = null;
			splitSyncSeq = splitSyncWatermark = 0;
		} finally {
			splitSyncLock.unlock();
		}

		if (null != timer) {
			try {
				timer.cancel(true);
			} catch (Exception e) {
				logger.error("", e);
			}
			timer = null;
		}

		if (null != commitAgent) {
			try {
				commitAgent.stop();
			} catch (Exception e) {
				logger.error("", e);
			}
			commitAgent = null;
		}
	}

	@Override
	public void reset() {
		//noinspection DataFlowIssue
		getRaft().lock();
		try {
			close();
			var path = Path.of(Objects.requireNonNull(getDbHome()), "statemachine").toAbsolutePath().toFile();
			LogSequence.deletedDirectoryAndCheck(path, 100);
			openBucket();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		} finally {
			getRaft().unlock();
		}
	}

	@Override
	public void recover(@Nullable String snapshotFile) throws Exception {
		try {
			openBucket();
			loadTransactions(); // 水位定位前装载，装载失败不能留下超前的lastApplied
			//noinspection DataFlowIssue
			if (getRaft().getLogSequence().tryRecoverFromWatermark())
				return;
		} catch (RocksDBException | IllegalStateException e) {
			//noinspection DataFlowIssue
			logger.warn("{} local state invalid, recover from snapshot or reset", getRaft().getName(), e);
		}
		super.recover(snapshotFile);
	}

	@Override
	public void loadSnapshot(String path) throws Exception {
		var backupDir = Paths.get(Objects.requireNonNull(getDbHome()), "backup").toString();
		var backupFile = new File(backupDir);
		// 与 RocksRaft/Rocks.loadSnapshot 同款: 无条件以已提交快照为恢复源,
		// 不能按mtime跳过解压, 否则会恢复超前的延时快照而双重应用增量日志.
		LogSequence.deletedDirectoryAndCheck(backupFile, 100);
		Zeze.Raft.RocksRaft.Rocks.extractZipToDirectory(path, backupDir);

		restore(backupDir);
		loadTransactions();
	}

	private void loadTransactions() throws RocksDBException {
		// 两遍分态重建：先收集墓碑 marker 的 tid，再逐 blob 重建为无锁墓碑或持锁存活事务
		var tombstoned = new HashSet<Long>();
		try (var it = requireBucket().getTrans().iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
				var tid = Dbh2Transaction.decodeTombstoneMarkerTid(it.key());
				if (null != tid)
					tombstoned.add(tid);
			}
			it.status();
		}
		try (var it = requireBucket().getTrans().iterator()) {
			for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
				if (null != Dbh2Transaction.decodeTombstoneMarkerTid(it.key()))
					continue;
				var batch = new BBatch.Data();
				batch.decode(ByteBuffer.Wrap(it.value()));
				var tid = batch.getTid();
				if (tombstoned.contains(tid))
					undonePending.put(tid, new UndonePending(new Dbh2Transaction(batch)));
				else {
					try {
						getOrAddTransaction(batch);
					} catch (RuntimeException ex) {
						// 兼容旧版无 marker 的重叠 blob：锁冲突输者降级为墓碑而非中止装载
						logger.error("loadSnapshot live rebuild lock conflict, degrade loser to tombstone."
								+ " tid={} (legacy overlapping blobs without marker; winner stays live)", tid, ex);
						undonePending.put(tid, new UndonePending(new Dbh2Transaction(batch)));
					}
				}
			}
			it.status();
		}
	}

	public String checkpoint(SnapshotResult result) throws RocksDBException {
		var checkpointDir = Paths.get(Objects.requireNonNull(getDbHome()), "checkpoint_" + System.currentTimeMillis()).toString();

		// 快速 checkpoint，持 raft 锁暂停 apply
		Raft raft = Objects.requireNonNull(getRaft());
		raft.lock();
		try {
			var watermark = readAppliedWatermark();
			if (watermark != null) {
				result.lastIncludedIndex = watermark.index();
				result.lastIncludedTerm = watermark.term();
			} else {
				var lastAppliedLog = raft.getLogSequence().lastAppliedLogTermIndex();
				result.lastIncludedIndex = lastAppliedLog.getIndex();
				result.lastIncludedTerm = lastAppliedLog.getTerm();
			}

			try (var cp = requireBucket().getDb().newCheckpoint()) {
				cp.createCheckpoint(checkpointDir);
			}
		} finally {
			raft.unlock();
		}
		return checkpointDir;
	}

	public void restore(String backupDir) throws Exception {
		//noinspection DataFlowIssue
		getRaft().lock();
		try {
			close();
			var dbName = Paths.get(Objects.requireNonNull(getDbHome()), "statemachine").toString();
			RocksDatabase.restore(backupDir, dbName);
			openBucket();
		} finally {
			getRaft().unlock();
		}
	}

	public void applySplitPut(BSplitPut.Data puts) {
		try {
			var table = requireBucket().getData();
			if (puts.isFromTransaction()) {
				// 事务同步流：直接覆盖写入，空值作为墓碑原样落盘以拦截迟到的复制流 putIfAbsent（读路径视空值为不存在）
				for (var e : puts.getPuts().entrySet()) {
					var key = e.getKey();
					var value = e.getValue();

					requireBucket().put(table, key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
				return;
			}

			// 数据复制流：putIfAbsent，墓碑非 null 可自然阻止复活已删键
			for (var e : puts.getPuts().entrySet()) {
				var key = e.getKey();
				var value = e.getValue();

				if (table.get(key.bytesUnsafe(), key.getOffset(), key.size()) == null) {
					requireBucket().put(table, key.bytesUnsafe(), key.getOffset(), key.size(),
							value.bytesUnsafe(), value.getOffset(), value.size());
				}
			}
		} catch (RocksDBException ex) {
			logger.error("", ex);
			//noinspection DataFlowIssue
			getRaft().fatalKill();
		}
	}
}
