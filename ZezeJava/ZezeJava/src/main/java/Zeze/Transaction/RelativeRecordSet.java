package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.ZezeCounter;
import org.jspecify.annotations.Nullable;
import Zeze.Transaction.IHistory.PendingHistory;
import Zeze.Transaction.IHistory.PreparedCommit;

/**
 * 将事务关联的记录编组，供检查点统一落库。
 */
public final class RelativeRecordSet extends ReentrantLock {
	private static final AtomicLong idGenerator = new AtomicLong(1);
	private static final RelativeRecordSet deleted = new RelativeRecordSet();

	private final long id = idGenerator.getAndIncrement();
	// 合并时须逐条更新 Record 的集合归属，使用 HashSet 并优先向大集合合并。
	private @Nullable HashSet<Record> recordSet;
	private volatile @Nullable RelativeRecordSet mergeTo; // null 表示存活，deleted 表示已落库，其余值指向合并目标。
	private @Nullable Set<OnzProcedure> onzProcedures;
	private volatile @Nullable PendingHistory history;

	// Profiler 记录慢事务的锁等待，ZezeCounter 分别聚合提交与落库路径的等待分布。
	private static final ZezeCounter.LongObserver CommitLockWaitObserver =
		ZezeCounter.instance.getRunTimeObserver("RRS.commitLockWait");
	private static final ZezeCounter.LongObserver FlushLockWaitObserver =
		ZezeCounter.instance.getRunTimeObserver("RRS.flushLockWait");
	// 非公平锁不保证等待上界；整组取锁超时仅告警，不干预执行。
	private static final long FlushLockWaitWarnNanos =
		PropertiesHelper.getLong("zeze.rrsFlushLockWaitWarnMs", 1000) * 1_000_000L;

	/** 关闭 zeze.rrs.readFastPath 可恢复纯读事务的完整编组路径。 */
	private static final boolean ReadFastPath = PropertiesHelper.getBool("zeze.rrs.readFastPath", true);

	RelativeRecordSet() {
		// 非公平锁减少并发提交的排队交接开销；多集合按 id 排序取锁以避免死锁。
		// flusher 可能长期等待，通过等待统计与 zeze.rrsFlushLockWaitWarnMs 告警观测。
		super(false);
	}

	public @Nullable PendingHistory getHistory() {
		return history;
	}

	void addHistory(IHistory.PendingHistory pending) {
		var currentHistory = history;
		if (currentHistory == null)
			history = pending;
		else {
			try {
				currentHistory.mergeFrom(pending);
			} catch (Throwable ex) {
				throw new IllegalStateException("history buffer merge failed", ex);
			}
		}
	}

	@Nullable HashSet<Record> getRecordSet() {
		return recordSet;
	}

	@Nullable RelativeRecordSet getMergeTo() {
		return mergeTo;
	}

	public @Nullable Set<OnzProcedure> getOnzProcedures() {
		return onzProcedures;
	}

	public void addOnzProcedures(@Nullable OnzProcedure onzProcedure) {
		if (onzProcedure != null) {
			if (onzProcedures == null)
				onzProcedures = new HashSet<>();
			onzProcedures.add(onzProcedure);
		}
	}

	private void merge(Record r) {
		if (recordSet == null)
			recordSet = new HashSet<>();
		recordSet.add(r);
		if (r.getRelativeRecordSet() != this) {
			r.getRelativeRecordSet().mergeTo = this;
			r.setRelativeRecordSet(this);
		}
	}

	private void merge(RelativeRecordSet rrs) {
		if (rrs == this)
			throw new IllegalStateException("Merge Self! " + rrs);

		var pending = rrs.history;
		if (pending != null)
			addHistory(pending);

		if (rrs.recordSet == null)
			return; // 孤立记录，后面单独合并。

		if (recordSet == null)
			recordSet = new HashSet<>();

		for (var r : rrs.recordSet) {
			recordSet.add(r);
			r.setRelativeRecordSet(this);
		}

		if (rrs.onzProcedures != null) {
			if (onzProcedures == null)
				onzProcedures = new HashSet<>();
			onzProcedures.addAll(rrs.onzProcedures);
		}

		rrs.mergeTo = this;
	}

	private void delete() {
		if (recordSet != null) { // 孤立记录不需要更新。
			recordSet.forEach(r -> r.setRelativeRecordSet(new RelativeRecordSet()));
			mergeTo = deleted;
		}
	}

	boolean tryLockWhenIdle() {
		return !hasQueuedThreads() && tryLock();
	}

	// 无竞争时直接取锁，仅在等待路径记录耗时。
	private static void lockCommit(RelativeRecordSet rrs) {
		if (rrs.tryLock())
			return;
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try (var ignored = Profiler.begin("RrsWaitLock", rrs.id)) {
			rrs.lock();
		} finally {
			if (timeBegin != 0)
				CommitLockWaitObserver.observe(System.nanoTime() - timeBegin);
		}
	}

	// 返回等待纳秒数，供 FlushSet 累计告警；禁用统计时返回 0，也不触发等待告警。
	private static long lockFlush(RelativeRecordSet rrs) {
		if (rrs.tryLock())
			return 0;
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try (var ignored = Profiler.begin("RrsWaitLock", rrs.id)) {
			rrs.lock();
		}
		var waited = timeBegin != 0 ? System.nanoTime() - timeBegin : 0;
		FlushLockWaitObserver.observe(waited);
		return waited;
	}

	private static @Nullable PreparedCommit prepareHistory(Procedure procedure,
														   @Nullable Changes changes,
														   @Nullable IHistory history) throws IHistory.CommitRejectedException {
		if (history == null || changes == null)
			return null;
		return history.prepareCommit(procedure);
	}

	private static @Nullable PendingHistory collectAndCaptureHistory(Transaction trans,
																	 @Nullable Changes changes,
																	 @Nullable PreparedCommit prepared) {
		if (changes == null)
			return null;
		try {
			trans.collectChanges(changes);
			return prepared != null ? prepared.capture(changes) : null;
		} catch (Throwable ex) {
			// 数据已应用。包成普通异常，不能落入只服务于终局回调断言的 AssertionError 旁路。
			if (prepared != null)
				throw new IllegalStateException("history post-apply capture failed", ex);
			throw ex;
		}
	}

	static void tryUpdateAndCheckpoint(Transaction trans, Procedure procedure,
									   Runnable commit, @Nullable OnzProcedure onzProcedure,
									   @Nullable Changes changes, @Nullable IHistory historyPlugin) throws Exception {
		var checkpoint = procedure.getZeze().getCheckpoint();
		// 停机后所有模式（含空事务）统一拒绝提交。
		if (checkpoint == null)
			throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
		//noinspection SwitchStatementWithTooFewBranches
		switch (procedure.getZeze().getConfig().getCheckpointMode()) {
		case Immediately: {
			// 空事务也须经过 flush，提交使用权覆盖整个同步落库过程。
			if (!checkpoint.tryBeginCommit())
				throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
			try {
				var prepared = prepareHistory(procedure, changes, historyPlugin);
				commit.run();
				IHistory.PendingHistory pending = null;
				try {
					pending = collectAndCaptureHistory(trans, changes, prepared);
					checkpoint.flush(trans, onzProcedure, pending);
				} catch (Throwable ex) {
					// Immediately 模式的 runOnce 不补刷，须在记录锁和提交使用权释放前同步补刷。
					// 历史采集不完整时，即使补刷成功也须重抛并进入 fatal。
					try {
						checkpoint.flush(trans, onzProcedure, pending);
					} catch (Throwable ex2) {
						Checkpoint.logger.fatal("Immediately commit flush fail, salvage flush fail again, accept loss", ex2);
						throw ex;
					}
					if (prepared != null && pending == null)
						throw ex; // 采集失败尚无完整历史，补刷成功也必须进入 fatal。
					Checkpoint.logger.fatal("Immediately commit flush fail, salvage flush success, data saved", ex);
				}
			} finally {
				checkpoint.endCommit();
			}
			return;
		}

		default:
			break;
		}

		boolean needFlushNow = onzProcedure != null; // 此参数存在即表示Onz.eFlushImmediately。
		var transAccessRecords = new HashSet<Record>();
		boolean allRead = true;
		boolean hasDirtyWrite = false;
		boolean hasUnregisteredDirty = false;
		for (var ar : trans.getAccessedRecords().values()) {
			var record = ar.atomicTupleRecord.record();
			if (ar.dirty || record.getDirty())
				allRead = false;
			if (ar.dirty)
				hasDirtyWrite = true;
			// oldTable 迁移可在读路径产生未编组的脏记录，须走完整路径登记落库。
			if (record.getDirty() && record.getRelativeRecordSet().getRecordSet() == null)
				hasUnregisteredDirty = true;

			if (record.getTable().getTableConf().getCheckpointWhenCommit()) {
				if (ar.dirty) {
					needFlushNow = true;
				}
			}
			transAccessRecords.add(record);
		}

		// 纯读稳定性由记录锁保证，脏集由写者编组；无未注册脏记录和 Onz 时可跳过编组。
		if (ReadFastPath && !hasDirtyWrite && !hasUnregisteredDirty && onzProcedure == null) {
			commitWithoutRecordSet(checkpoint, procedure, commit, null);
			return;
		}

		var all = new TreeMap<Long, RelativeRecordSet>();
		for (var record : transAccessRecords) {
			var volatileRrs = record.getRelativeRecordSet();
			all.putIfAbsent(volatileRrs.id, volatileRrs);
		}

		var locked = new ArrayList<RelativeRecordSet>();
		try {
			_lock_(locked, all, transAccessRecords);
			if (!locked.isEmpty()) {
				// 非空集合才需提交使用权；获取不阻塞，停机侧不持 RRS 锁等待，不形成锁序环。
				if (!checkpoint.tryBeginCommit())
					throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
				try {
					var mergedSet = _merge_(locked, trans, allRead);
					var prepared = prepareHistory(procedure, changes, historyPlugin);
					commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
					mergedSet.addOnzProcedures(onzProcedure);
					try {
						var pending = collectAndCaptureHistory(trans, changes, prepared);
						if (pending != null)
							mergedSet.addHistory(pending);

						if (needFlushNow) {
							if (mergedSet.recordSet != null) {
								checkpoint.flush(mergedSet);
							} else if (onzProcedure != null) {
								// 孤立集合不会进入 flush，须单独完成 Onz FlushReady 握手。
								OnzProcedure.sendFlushAndWait(Set.of(onzProcedure));
							}
							mergedSet.delete();
						} else if (mergedSet.recordSet != null) {
							// 无需立即落库，登记后交给周期检查点。
							checkpoint.relativeRecordSetMap.add(mergedSet);
						}
					} catch (Throwable ex) {
						// 修改已应用，重抛前须登记合并后的脏集，供后台或致命退出前的检查点重试。
						if (mergedSet.recordSet != null)
							checkpoint.relativeRecordSetMap.add(mergedSet);
						throw ex;
					}
				} finally {
					checkpoint.endCommit();
				}
			} else {
				// 本次事务没有访问任何数据，也要执行提交，否则 whileCommit 回调会丢失。
				commitWithoutRecordSet(checkpoint, procedure, commit, onzProcedure);
			}
		} finally {
			locked.forEach(ReentrantLock::unlock);
		}
	}

	/** 免关联集合锁的提交仍须取得使用权，不能绕过停机闸门。 */
	private static void commitWithoutRecordSet(Checkpoint checkpoint, Procedure procedure,
											 Runnable commit, @Nullable OnzProcedure onzProcedure) {
		if (!checkpoint.tryBeginCommit())
			throw new Transaction.RejectWhileStopping("commit rejected while stopping: " + procedure.getActionName());
		try {
			commit.run();
			// 空事务也须完成 Onz FlushReady 握手，否则协调者只能等待超时。
			if (onzProcedure != null)
				OnzProcedure.sendFlushAndWait(Set.of(onzProcedure));
		} finally {
			checkpoint.endCommit();
		}
	}

	private static void verify(TreeMap<String, ArrayList<Object>> group,
							   TreeMap<String, ArrayList<Object>> result) {
		for (var g : group.entrySet()) {
			for (var value : g.getValue()) {
				var keys = result.get(g.getKey());
				if (keys != null) {
					keys.remove(value);
					if (keys.isEmpty())
						result.remove(g.getKey());
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static void verify(ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked,
							   TreeMap<String, ArrayList<Object>> groupTrans,
							   RelativeRecordSet result) {
		var groupResult = new TreeMap<String, ArrayList<Object>>();
		if (result.recordSet != null) {
			for (var r : result.recordSet)
				groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
		}
		for (var locked : groupLocked)
			verify(locked, groupResult);
		verify(groupTrans, groupResult);
		if (!groupResult.isEmpty()) {
			groupResult.clear();
			if (result.recordSet != null) {
				for (var r : result.recordSet)
					groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
			}
			Checkpoint.logger.info("locked.size={} trans.size={}\nlocked:{}\ntrans:{}\nresult:{}",
				groupLocked.size(), groupTrans.size(), groupLocked, groupTrans, groupResult);
		}
	}

	@SuppressWarnings("unused")
	private static void build(Transaction trans, TreeMap<String, ArrayList<Object>> groupTrans) {
		for (var ar : trans.getAccessedRecords().values()) {
			groupTrans.computeIfAbsent(ar.atomicTupleRecord.record().getTable().getName(), __ -> new ArrayList<>())
				.add(ar.atomicTupleRecord.record().getObjectKey());
		}
	}

	@SuppressWarnings("unused")
	private static void build(ArrayList<RelativeRecordSet> locked,
							  ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked) {
		for (var rrs : locked) {
			var group = new TreeMap<String, ArrayList<Object>>();
			if (rrs.recordSet != null) {
				for (var r : rrs.recordSet) {
					group.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
				}
			}
			groupLocked.add(group);
		}
	}

	private static RelativeRecordSet _merge_(ArrayList<RelativeRecordSet> locked,
											 Transaction trans, boolean allRead) {
		var largest = locked.getFirst();
		for (int index = 1; index < locked.size(); ++index) {
			var r = locked.get(index);
			var cur = largest.recordSet == null ? 0 : largest.recordSet.size();
			if (r.recordSet != null && r.recordSet.size() > cur) {
				largest = r;
			}
		}

		for (var r : locked) {
			if (r != largest)
				largest.merge(r);
		}

		// 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
		if (largest.recordSet != null || !allRead) {
			for (var ar : trans.getAccessedRecords().values()) {
				var record = ar.atomicTupleRecord.record();
				var rrs = record.getRelativeRecordSet();
				if (rrs.recordSet == null || rrs == largest)
					largest.merge(record); // 合并孤立记录。这里包含largest是孤立记录的情况。
			}
		}
		return largest;
	}

	private static void _lock_(ArrayList<RelativeRecordSet> locked,
							   TreeMap<Long, RelativeRecordSet> all,
							   HashSet<Record> transAccessRecords) {
		while (true) {
			var GotoLabelLockRelativeRecordSets = false;
			int index = 0;
			int n = locked.size();
			final var itRrs = all.values().iterator();
			var rrs = itRrs.hasNext() ? itRrs.next() : null;
			while (rrs != null) {
				if (index >= n) {
					if (_lock_and_check_(locked, all, rrs, transAccessRecords)) {
						rrs = itRrs.hasNext() ? itRrs.next() : null;
						continue;
					}
					GotoLabelLockRelativeRecordSets = true;
					break;
				}
				var curSet = locked.get(index);
				int c = Long.compare(curSet.id, rrs.id);
				if (c == 0) {
					index++;
					rrs = itRrs.hasNext() ? itRrs.next() : null;
					continue;
				}
				if (c < 0) {
					// 释放已落库、不再需要的集合锁。
					int unlockEndIndex = index;
					while (unlockEndIndex < n && locked.get(unlockEndIndex).id < rrs.id)
						locked.get(unlockEndIndex++).unlock();
					locked.subList(index, unlockEndIndex).clear();
					n = locked.size();
					continue;
				}
				// 新集合排在已持锁集合之前，须先释放后续锁以维持 id 锁序。
				for (int i = index; i < n; i++)
					locked.get(i).unlock();
				locked.subList(index, n).clear();
				n = locked.size();
			}
			if (!GotoLabelLockRelativeRecordSets)
				break;
		}
	}

	private static boolean _lock_and_check_(ArrayList<RelativeRecordSet> locked,
											TreeMap<Long, RelativeRecordSet> all,
											RelativeRecordSet rrs,
											HashSet<Record> transAccessRecords) {
		lockCommit(rrs);
		var mergeTo = rrs.mergeTo;
		if (mergeTo != null) {
			rrs.unlock();
			all.remove(rrs.id);
			if (mergeTo == deleted) {
				// 旧集合已落库，须重新获取记录的当前集合；deleted 后 recordSet 不变，可在锁外读取。
				for (var r : transAccessRecords) {
					//noinspection DataFlowIssue
					if (rrs.recordSet.contains(r)) {
						var volatileTmp = r.getRelativeRecordSet();
						all.putIfAbsent(volatileTmp.id, volatileTmp);
					}
				}
				return false;
			}
			all.putIfAbsent(mergeTo.id, mergeTo);
			return false;
		}
		locked.add(rrs);
		return true;
	}

	static class FlushSet {
		private final Checkpoint checkpoint;
		private final TreeMap<Long, RelativeRecordSet> sortedRrs = new TreeMap<>();
		private int sumHint;

		public FlushSet(Checkpoint cp) {
			checkpoint = cp;
		}

		boolean add(RelativeRecordSet rrs) {
			if (sortedRrs.putIfAbsent(rrs.id, rrs) != null)
				throw new IllegalStateException("duplicate rrs");
			if (null != rrs.recordSet) {
				// size 仅用于批量大小估算，允许锁外读取近似值。
				sumHint += rrs.recordSet.size();
			}
			var flushLimit = checkpoint.getZeze().getConfig().getCheckpointModeTableFlushSetCount();
			return sortedRrs.size() >= flushLimit || sumHint >= 10000;
		}

		private int size() {
			return sortedRrs.size();
		}

		void flush() {
			var timeBegin = System.nanoTime();
			var n = sortedRrs.size();
			var locks = new ArrayList<RelativeRecordSet>(n);
			try {
				var nr = 0;
				long lockWaited = 0;
				for (var rrs : sortedRrs.values()) {
					lockWaited += lockFlush(rrs);
					locks.add(rrs);
					//noinspection DataFlowIssue
					nr += rrs.recordSet.size();
				}
				if (lockWaited > FlushLockWaitWarnNanos)
					Checkpoint.logger.warn(
						"FlushSet acquire {} rrs locks waited {}ms: flusher排队超阈值(zeze.rrsFlushLockWaitWarnMs)，"
							+ "查RRS.flushLockWait聚合分布与RrsWaitLock慢事务时间线",
						locks.size(), lockWaited / 1_000_000);
				var rs = new ArrayList<Record>(nr);
				var onzProcedures = new HashSet<OnzProcedure>();
				IHistory.PendingHistory history = null;
				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo != null)
						continue; // merged or deleted
					rs.addAll(rrs.recordSet);
					// 只合并历史快照，保留成员容器；失败后重新分组时，写入和核销范围须与本轮成员一致。
					var member = rrs.getHistory();
					if (member != null) {
						if (history == null)
							history = member.snapshot();
						else
							history.mergeFrom(member);
					}
					// 失败后重新登记的集合可能携带未完成的 Onz 握手，重试时须一并收集。
					// 此处持有成员锁；已合并成员的 Onz 已转移至存活集合，由 HashSet 去重。
					var onz = rrs.getOnzProcedures();
					if (onz != null)
						onzProcedures.addAll(onz);
				}

				checkpoint.flush(rs, onzProcedures, history);

				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo == null)
						rrs.delete();
					checkpoint.relativeRecordSetMap.remove(rrs);
				}
				sortedRrs.clear();
				sumHint = 0;

				var verifyAction = DatabaseRocksDb.verifyAction;
				if (verifyAction != null)
					verifyAction.run();
			} finally {
				locks.forEach(RelativeRecordSet::unlock);
				Checkpoint.logger.trace("flush: {} rrs, {} ns", n, System.nanoTime() - timeBegin);
			}
		}
	}

	static void flush(Checkpoint checkpoint, RelativeRecordSet rrs) {

		// 编码须在持有 RRS 锁时交给 Checkpoint.flush，避免并发合并或核销导致历史条目遗漏。
		lockFlush(rrs);
		try {
			if (rrs.mergeTo == null) {
				checkpoint.flush(rrs);
				rrs.delete();
			}
			checkpoint.relativeRecordSetMap.remove(rrs);
			if (DatabaseRocksDb.verifyAction != null)
				DatabaseRocksDb.verifyAction.run();
		} finally {
			rrs.unlock();
		}
	}

	static void flushWhenCheckpoint(Checkpoint checkpoint) {
		// 各 RRS/FlushSet 失败独立处理，保留脏集供下轮重试，避免单个失败阻断整轮。
		switch (checkpoint.zeze.getConfig().getCheckpointFlushMode()) {
		case SingleThread:
			for (var rrs : checkpoint.relativeRecordSetMap) {
				try {
					flush(checkpoint, rrs);
				} catch (Throwable ex) {
					Checkpoint.logger.error("flushWhenCheckpoint(SingleThread) flush fail, keep for next checkpoint", ex);
				}
			}
			break;

		case MultiThread:
			// 阻塞数据库 I/O 使用专用池，避免占满 commonPool。
			Zeze.Util.Task.runInDbIoPool(() -> checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				try {
					flush(checkpoint, rrs);
				} catch (Throwable ex) {
					// lambda内必须捕获：逃逸会取消parallelStream尚未启动的任务并传播出去。
					Checkpoint.logger.error("flushWhenCheckpoint(MultiThread) flush fail, keep for next checkpoint", ex);
				}
			}));
			break;

		case SingleThreadMerge: {
			var flushSet = new FlushSet(checkpoint);
			for (var rrs : checkpoint.relativeRecordSetMap) {
				if (flushSet.add(rrs)) {
					try {
						flushSet.flush();
					} catch (Throwable ex) {
						Checkpoint.logger.error("flushWhenCheckpoint(SingleThreadMerge) FlushSet flush fail, keep for next checkpoint", ex);
						// 换新批次，避免后续集合反复加入已满的失败批次；失败成员留待下轮重试。
						flushSet = new FlushSet(checkpoint);
					}
				}
			}
			flushTail(flushSet);
		}
		break;

		case MultiThreadMerge: {
			var flushSetMap = new ConcurrentHashMap<Thread, FlushSet>();
			// 使用专用 I/O 池，FlushSet 按工作线程归属。
			Zeze.Util.Task.runInDbIoPool(() -> checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				var fs = parallelFlushSet(checkpoint, flushSetMap);
				if (fs.add(rrs)) {
					try {
						fs.flush();
					} catch (Throwable ex) {
						Checkpoint.logger.error("flushWhenCheckpoint(MultiThreadMerge) FlushSet flush fail, keep for next checkpoint", ex);
						// 为本线程后续集合换新批次，失败成员留待下轮重试。
						flushSetMap.put(Thread.currentThread(), new FlushSet(checkpoint));
					}
				}
			}));
			for (var fs : flushSetMap.values())
				flushTail(fs);
		}
		break;
		}
	}

	private static void flushTail(FlushSet flushSet) {
		if (flushSet.size() <= 0)
			return;
		try {
			flushSet.flush();
		} catch (Throwable ex) {
			Checkpoint.logger.error("flushWhenCheckpoint flushTail fail, keep for next checkpoint", ex);
		}
	}

	private static FlushSet parallelFlushSet(Checkpoint checkpoint,
											 ConcurrentHashMap<Thread, FlushSet> map) {
		return map.computeIfAbsent(Thread.currentThread(), __ -> new FlushSet(checkpoint));
	}

	static void flushWhenReduce(Record r, Checkpoint checkpoint) {
		var rrs = r.getRelativeRecordSet();
		while (rrs != null) {
			r.enterFairLock(); // 用来保护State的查看。
			try {
				if (r.getState() == GlobalCacheManagerConst.StateRemoved)
					return;
			} finally {
				r.exitFairLock();
			}
			rrs = flushWhenReduce(r, rrs, checkpoint);
		}
	}

	private static @Nullable RelativeRecordSet flushWhenReduce(Record r, RelativeRecordSet rrs,
															   Checkpoint checkpoint) {
		lockFlush(rrs);
		try {
			var mergeTo = rrs.mergeTo;
			if (mergeTo == null) {
				if (rrs.recordSet != null) {
					checkpoint.flush(rrs);
					rrs.delete();
				} else if (r.getDirty()) {
					// 防御兜底：孤立记录若意外带脏标记，降级前必须落库清脏，避免 Invalid+dirty 残留。
					checkpoint.flush(Collections.singletonList(r), Set.of(), null);
				}
				return null;
			}

			// Reduce 已持记录锁并降级，不再有后续写入；仍可能被读事务合并或被检查点刷盘。
			if (mergeTo == RelativeRecordSet.deleted)
				return null;

			return mergeTo; // 返回这个能更快得到新集合的引用。
		} finally {
			rrs.unlock();
		}
	}

	@Override
	public String toString() {
		lock();
		try {
			var mergeTo = this.mergeTo;
			if (mergeTo != null)
				return "[MergeTo-" + mergeTo.id + "]";
			if (recordSet == null)
				return id + "-[Isolated]";
			return id + "-" + recordSet;
		} finally {
			unlock();
		}
	}

	public static String relativeRecordSetMapToString(Checkpoint checkpoint) {
		return checkpoint.relativeRecordSetMap.toString();
	}
}
