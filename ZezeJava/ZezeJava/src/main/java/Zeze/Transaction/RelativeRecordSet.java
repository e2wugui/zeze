package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.History.History;
import Zeze.Onz.OnzProcedure;
import Zeze.Services.GlobalCacheManagerConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * see zeze/README.md －＞ 18) 事务提交模式
 * 一个事务内访问的记录的集合。如果事务没有没提交，需要合并集合。
 */
public final class RelativeRecordSet extends ReentrantLock {
	private static final AtomicLong idGenerator = new AtomicLong(1);
	private static final RelativeRecordSet deleted = new RelativeRecordSet();

	private final long id = idGenerator.getAndIncrement();
	// 采用链表，可以O(1)处理Merge，但是由于Merge的时候需要更新Record所属的关联集合，
	// 所以避免不了遍历，那就使用HashSet，遍历吧。
	// 可做的小优化：把Count小的关联集合Merge到大的里面。
	private @Nullable HashSet<Record> recordSet;
	private volatile @Nullable RelativeRecordSet mergeTo; // 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
	private @Nullable Set<OnzProcedure> onzProcedures;
	private volatile @Nullable History history;

	RelativeRecordSet() {
		super(true);
	}

	public @Nullable History getHistory() {
		return history;
	}

	void addLogChanges(@NotNull BLogChanges.Data logChanges) {
		var h = history;
		if (h == null)
			history = new History(logChanges);
		else
			h.addLogChanges(logChanges); // 这是锁内的，可以不考虑这个警告。怎么去除？
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

	private void merge(@NotNull Record r) {
		//if (r.getRelativeRecordSet().RecordSet != null)
		//    return; // 这里仅合并孤立记录。外面检查。

		if (recordSet == null)
			recordSet = new HashSet<>();
		recordSet.add(r);
		if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
			r.getRelativeRecordSet().mergeTo = this;
			r.setRelativeRecordSet(this);
		}
	}

	private void merge(@NotNull RelativeRecordSet rrs) {
		if (rrs == this) // 这个方法仅用于合并其他rrs
			throw new IllegalStateException("Merge Self! " + rrs);

		//noinspection NonAtomicOperationOnVolatileField
		history = History.merge(history, rrs.history);

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
			// Flush完成以后，清除关联集合，
			recordSet.forEach(r -> r.setRelativeRecordSet(new RelativeRecordSet()));
			mergeTo = deleted;
		}
	}

	boolean tryLockWhenIdle() {
		return !hasQueuedThreads() && tryLock();
	}

	static void tryUpdateAndCheckpoint(@NotNull Transaction trans, @NotNull Procedure procedure,
									   @NotNull Runnable commit, @Nullable OnzProcedure onzProcedure,
									   @NotNull Callable<BLogChanges.Data> collectChanges) throws Exception {

		switch (procedure.getZeze().getConfig().getCheckpointMode()) {
		case Immediately:
			commit.run();
			var logChanges = collectChanges.call();
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint != null)
				checkpoint.flush(trans, onzProcedure, logChanges != null ? new History(logChanges) : null);
			// 这种模式下 RelativeRecordSet 都是空的。
			return; // done

//		case Period:
//			if (onzProcedure != null)
//				throw new RuntimeException("Onz Procedure Not Supported On Period Mode.");
//			commit.run();
//			collectChanges.call(); // skip result, 这个模式不支持History.
//			return; // done

		default:
			break;
		}

		// CheckpointMode.Table
		boolean needFlushNow = onzProcedure != null; // 此参数存在即表示Onz.eFlushImmediately。
		boolean allCheckpointWhenCommit = true;

		var all = new TreeMap<Long, RelativeRecordSet>();
		var transAccessRecords = new HashSet<Record>();
		boolean allRead = true;
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.dirty)
				allRead = false;

			var record = ar.atomicTupleRecord.record;
			if (record.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.dirty) {
					needFlushNow = true;
				}
			} else {
				allCheckpointWhenCommit = false;
			}
			// 读写都需要收集。
			transAccessRecords.add(record);
			var volatileRrs = record.getRelativeRecordSet();
			all.putIfAbsent(volatileRrs.id, volatileRrs);
		}

		if (allCheckpointWhenCommit) {
			// && procedure.Zeze.Config.CheckpointMode != CheckpointMode.Period
			// CheckpointMode.Period上面已经处理了，此时不会是它。
			// 【优化】，事务内访问的所有记录都是Immediately的，马上提交，不需要更新关联记录集合。
			commit.run();
			var logChanges = collectChanges.call();
			var checkpoint = procedure.getZeze().getCheckpoint();
			if (checkpoint != null)
				checkpoint.flush(trans, onzProcedure, logChanges != null ? new History(logChanges) : null);
			// 这种情况下 RelativeRecordSet 都是空的。
			//logger.Debug($"allCheckpointWhenCommit AccessedCount={trans.AccessedRecords.Count}");
			return;
		}

		var locked = new ArrayList<RelativeRecordSet>();
		try {
			_lock_(locked, all, transAccessRecords);
			if (!locked.isEmpty()) {
				var mergedSet = _merge_(locked, trans, allRead);
				commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
				var logChanges = collectChanges.call();
				mergedSet.addOnzProcedures(onzProcedure);
				if (logChanges != null)
					mergedSet.addLogChanges(logChanges); // History存在并且开启，则加入rrs。

				if (needFlushNow) {
					var checkpoint = procedure.getZeze().getCheckpoint();
					if (checkpoint != null)
						checkpoint.flush(mergedSet);
					mergedSet.delete();
					//logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
				} else if (mergedSet.recordSet != null) {
					// mergedSet 合并结果是孤立的，不需要Flush。
					// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
					var checkpoint = procedure.getZeze().getCheckpoint();
					if (checkpoint != null) {
						checkpoint.relativeRecordSetMap.add(mergedSet);
					}
				}
			}
			// else
			// 本次事务没有访问任何数据。
		} finally {
			locked.forEach(ReentrantLock::unlock);
		}
	}

	private static void verify(@NotNull TreeMap<String, ArrayList<Object>> group,
							   @NotNull TreeMap<String, ArrayList<Object>> result) {
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
	private static void verify(@NotNull ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked,
							   @NotNull TreeMap<String, ArrayList<Object>> groupTrans,
							   @NotNull RelativeRecordSet result) {
		var groupResult = new TreeMap<String, ArrayList<Object>>();
		if (result.recordSet != null) {
			for (var r : result.recordSet)
				groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
		}
		for (var locked : groupLocked)
			verify(locked, groupResult);
		verify(groupTrans, groupResult);
		if (!groupResult.isEmpty()) {
			groupResult.clear(); // reuse this var
			if (result.recordSet != null) {
				for (var r : result.recordSet)
					groupResult.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
			}
			Checkpoint.logger.info("locked.size={} trans.size={}\nlocked:{}\ntrans:{}\nresult:{}",
					groupLocked.size(), groupTrans.size(), groupLocked, groupTrans, groupResult);
		}
	}

	@SuppressWarnings("unused")
	private static void build(@NotNull Transaction trans, @NotNull TreeMap<String, ArrayList<Object>> groupTrans) {
		for (var ar : trans.getAccessedRecords().values()) {
			groupTrans.computeIfAbsent(ar.atomicTupleRecord.record.getTable().getName(), __ -> new ArrayList<>())
					.add(ar.atomicTupleRecord.record.getObjectKey());
		}
	}

	@SuppressWarnings("unused")
	private static void build(@NotNull ArrayList<RelativeRecordSet> locked,
							  @NotNull ArrayList<TreeMap<String, ArrayList<Object>>> groupLocked) {
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

	private static @NotNull RelativeRecordSet _merge_(@NotNull ArrayList<RelativeRecordSet> locked,
													  @NotNull Transaction trans, boolean allRead) {
		// find largest
		var largest = locked.get(0);
		for (int index = 1; index < locked.size(); ++index) {
			var r = locked.get(index);
			var cur = largest.recordSet == null ? 0 : largest.recordSet.size();
			if (r.recordSet != null && r.recordSet.size() > cur) {
				largest = r;
			}
		}

		/*
		var groupLocked = new ArrayList<TreeMap<String, ArrayList<Object>>>();
		var groupTrans = new TreeMap<String, ArrayList<Object>>();
		build(locked, groupLocked);
		build(trans, groupTrans);
		*/

		// merge all other set to largest
		for (var r : locked) {
			if (r != largest) // skip self
				largest.merge(r);
		}

		// 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
		if (largest.recordSet != null || !allRead) {
			// merge 孤立记录。
			for (var ar : trans.getAccessedRecords().values()) {
				var record = ar.atomicTupleRecord.record;
				var rrs = record.getRelativeRecordSet();
				if (rrs.recordSet == null || rrs == largest /* is self. ugly */)
					largest.merge(record); // 合并孤立记录。这里包含largest是孤立记录的情况。
			}
		}
		//verify(groupLocked, groupTrans, largest);
		return largest;
	}

	private static void _lock_(@NotNull ArrayList<RelativeRecordSet> locked,
							   @NotNull TreeMap<Long, RelativeRecordSet> all,
							   @NotNull HashSet<Record> transAccessRecords) {
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
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					while (unlockEndIndex < n && locked.get(unlockEndIndex).id < rrs.id)
						locked.get(unlockEndIndex++).unlock();
					locked.subList(index, unlockEndIndex).clear();
					n = locked.size();
					// 重新从当前 rrs 继续锁。
					continue;
				}
				// RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
				// 从当前位置释放锁，再次尝试。
				for (int i = index; i < n; i++)
					locked.get(i).unlock();
				locked.subList(index, n).clear();
				n = locked.size();
				// 重新从当前 rrs 继续锁。
			}
			if (!GotoLabelLockRelativeRecordSets)
				break; // success
		}
	}

	private static boolean _lock_and_check_(@NotNull ArrayList<RelativeRecordSet> locked,
											@NotNull TreeMap<Long, RelativeRecordSet> all,
											@NotNull RelativeRecordSet rrs,
											@NotNull HashSet<Record> transAccessRecords) {
		rrs.lock();
		var mergeTo = rrs.mergeTo;
		if (mergeTo != null) {
			rrs.unlock();
			all.remove(rrs.id); // remove merged or deleted rrs
			if (mergeTo == deleted) {
				// flush 后进入这个状态。此时表示旧的关联集合的checkpoint点已经完成。
				// 但仍然需要重新获得当前事务中访问的记录的rrs。
				// 进入 deleted 以后，rrs.recordSet 不再发生变化。只读，锁外使用。
				//Checkpoint.logger.info("deleted rrs=" + rrs.id);
				for (var r : transAccessRecords) {
					//noinspection DataFlowIssue
					if (rrs.recordSet.contains(r)) {
						var volatileTmp = r.getRelativeRecordSet();
						all.putIfAbsent(volatileTmp.id, volatileTmp);
						//if (all.putIfAbsent(volatileTmp.id, volatileTmp) == null)
						//	Checkpoint.logger.info("deleted rrs=" + rrs.id + " get rrs=" + volatileTmp.id);
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

	/*
	private static void FlushAndDelete(Checkpoint checkpoint, RelativeRecordSet rrs) {
		rrs.Lock();
		try {
			if (rrs.MergeTo == null) {
				checkpoint.Flush(rrs);
				rrs.Delete();
			}
			checkpoint.RelativeRecordSetMap.remove(rrs);
		} finally {
			rrs.UnLock();
		}
	}
	*/

	static class FlushSet {
		private final @NotNull Checkpoint checkpoint;
		private final TreeMap<Long, RelativeRecordSet> sortedRrs = new TreeMap<>();
		private int sumHint;

		public FlushSet(@NotNull Checkpoint cp) {
			checkpoint = cp;
		}

		private boolean add(@NotNull RelativeRecordSet rrs) {
			if (sortedRrs.putIfAbsent(rrs.id, rrs) != null)
				throw new IllegalStateException("duplicate rrs");
			if (null != rrs.recordSet) {
				// 这里没有加锁，得到的recordSet.size可能会变，但这里仅作为一个控制一次提交的量，不加锁是可以的。
				sumHint += rrs.recordSet.size();
			}
			var flushLimit = checkpoint.getZeze().getConfig().getCheckpointModeTableFlushSetCount();
			return sortedRrs.size() >= flushLimit || sumHint >= 10000;
		}

		private int size() {
			return sortedRrs.size();
		}

		private void flush() {
			var timeBegin = System.nanoTime();
			var n = sortedRrs.size();
			var locks = new ArrayList<RelativeRecordSet>(n);
			try {
				var nr = 0;
				if (checkpoint.zeze.getConfig().isHistory()) {
					for (var rrs : sortedRrs.values()) {
						rrs.lock();
						locks.add(rrs);
						//noinspection DataFlowIssue
						nr += rrs.recordSet.size();
					}
				} else {
					for (var rrs : sortedRrs.values()) {
						rrs.lock();
						locks.add(rrs);
						//noinspection DataFlowIssue
						nr += rrs.recordSet.size();
					}
				}
				var rs = new ArrayList<Record>(nr);
				var onzProcedures = new HashSet<OnzProcedure>();
				History history = null;
				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo != null)
						continue; // merged or deleted
					rs.addAll(rrs.recordSet);
					history = History.merge(history, rrs.getHistory());
					//onzProcedures.addAll(rrs.getOnzProcedures());
				}
				/*
				var debug = new java.util.HashMap<String, ArrayList<Object>>();
				for (var r : rs)
					debug.computeIfAbsent(r.getTable().getName(), __ -> new ArrayList<>()).add(r.getObjectKey());
				Checkpoint.logger.info(debug.toString() + sortedRrs.keySet());
				*/

				checkpoint.flush(rs, onzProcedures, history);
				for (var r : rs)
					r.setDirty(false);
				for (var rrs : sortedRrs.values()) {
					if (rrs.mergeTo == null)
						rrs.delete(); // normal rrs: not merged and not deleted.
					checkpoint.relativeRecordSetMap.remove(rrs);
				}
				sortedRrs.clear();
				sumHint = 0;

				// verify
				var verifyAction = DatabaseRocksDb.verifyAction;
				if (verifyAction != null)
					verifyAction.run();
			} finally {
				locks.forEach(RelativeRecordSet::unlock);
				Checkpoint.logger.trace("flush: {} rrs, {} ns", n, System.nanoTime() - timeBegin);
			}
		}
	}

	static void flush(@NotNull Checkpoint checkpoint, @NotNull RelativeRecordSet rrs) {

		if (rrs.mergeTo == null) {
			// 多线程，未保护访问变量，可以不是很准确。
			var history = rrs.getHistory();
			if (history != null)
				history.encodeN(); // 锁外尝试编码。
		}

		rrs.lock();
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

	static void flushWhenCheckpoint(@NotNull Checkpoint checkpoint) {
		// 根据选项执行不同的flush模式。
		switch (checkpoint.zeze.getConfig().getCheckpointFlushMode()) {
		case SingleThread:
			for (var rrs : checkpoint.relativeRecordSetMap)
				flush(checkpoint, rrs);
			break;

		case MultiThread:
			checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> flush(checkpoint, rrs));
			break;

		case SingleThreadMerge: {
			var flushSet = new FlushSet(checkpoint);
			for (var rrs : checkpoint.relativeRecordSetMap) {
				if (flushSet.add(rrs))
					flushSet.flush();
			}
			if (flushSet.size() > 0)
				flushSet.flush();
		}
		break;

		case MultiThreadMerge: {
			//Checkpoint.logger.info("Global.Releaser rrs={}", checkpoint.relativeRecordSetMap.size());
			var flushSetMap = new ConcurrentHashMap<Thread, FlushSet>();
			checkpoint.relativeRecordSetMap.keySet().parallelStream().forEach(rrs -> {
				var fs = parallelFlushSet(checkpoint, flushSetMap);
				if (fs.add(rrs))
					fs.flush();
			});
			for (var fs : flushSetMap.values()) {
				if (fs.size() > 0)
					fs.flush();
			}
		}
		break;
		}
	}

	private static @NotNull FlushSet parallelFlushSet(@NotNull Checkpoint checkpoint,
													  @NotNull ConcurrentHashMap<Thread, FlushSet> map) {
		return map.computeIfAbsent(Thread.currentThread(), __ -> new FlushSet(checkpoint));
	}

	static void flushWhenReduce(@NotNull Record r, @NotNull Checkpoint checkpoint) {
		var rrs = r.getRelativeRecordSet();
		while (rrs != null) {
			r.enterFairLock(); // 用来保护State的查看。
			try {
				if (r.getState() == GlobalCacheManagerConst.StateRemoved)
					return;
			} finally {
				r.exitFairLock();
			}
			rrs = flushWhenReduce(rrs, checkpoint);
		}
	}

	private static @Nullable RelativeRecordSet flushWhenReduce(@NotNull RelativeRecordSet rrs,
															   @NotNull Checkpoint checkpoint) {
		rrs.lock();
		try {
			var mergeTo = rrs.mergeTo;
			if (mergeTo == null) {
				if (rrs.recordSet != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.flush(rrs);
					rrs.delete();
				}
				return null;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			// 此时可以认为直接成功了吧？
			// 或者不判断这个，总是由上面的步骤中处理。
			if (mergeTo == RelativeRecordSet.deleted)
				return null; // has flush

			return mergeTo; // 返回这个能更快得到新集合的引用。
		} finally {
			rrs.unlock();
		}
	}

	@Override
	public @NotNull String toString() {
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

	public static @NotNull String relativeRecordSetMapToString(@NotNull Checkpoint checkpoint) {
		return checkpoint.relativeRecordSetMap.toString();
	}
}
