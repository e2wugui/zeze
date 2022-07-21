package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Services.GlobalCacheManagerServer;

/**
 * see zeze/README.md -> 18) 事务提交模式
 * 一个事务内访问的记录的集合。如果事务没有没提交，需要合并集合。
 */
public final class RelativeRecordSet {
	private static final AtomicLong IdGenerator = new AtomicLong(1);
	private static final RelativeRecordSet Deleted = new RelativeRecordSet();

	private final ReentrantLock mutex = new ReentrantLock(true);
	private final long Id = IdGenerator.getAndIncrement();
	// 采用链表，可以O(1)处理Merge，但是由于Merge的时候需要更新Record所属的关联集合，
	// 所以避免不了遍历，那就使用HashSet，遍历吧。
	// 可做的小优化：把Count小的关联集合Merge到大的里面。
	private HashSet<Record> RecordSet;
	private volatile RelativeRecordSet MergeTo; // 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。

	public HashSet<Record> getRecordSet() {
		return RecordSet;
	}

	public RelativeRecordSet getMergeTo() {
		return MergeTo;
	}

	private void Merge(Record r) {
		//if (r.getRelativeRecordSet().RecordSet != null)
		//    return; // 这里仅合并孤立记录。外面检查。

		if (RecordSet == null) {
			RecordSet = new HashSet<>();
		}
		RecordSet.add(r);
		if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
			r.getRelativeRecordSet().MergeTo = this;
			r.setRelativeRecordSet(this);
		}
	}

	private void Merge(RelativeRecordSet rrs) {
		if (rrs == this) // 这个方法仅用于合并其他rrs
			throw new IllegalStateException("Merge Self! " + rrs);

		if (rrs.RecordSet == null) {
			return; // 孤立记录，后面单独合并。
		}

		if (RecordSet == null) {
			RecordSet = new HashSet<>();
		}

		for (var r : rrs.RecordSet) {
			RecordSet.add(r);
			r.setRelativeRecordSet(this);
		}

		rrs.MergeTo = this;
	}

	public void Delete() {
		if (RecordSet != null) { // 孤立记录不需要更新。
			// Flush完成以后，清除关联集合，
			RecordSet.forEach(r -> r.setRelativeRecordSet(new RelativeRecordSet()));
			MergeTo = Deleted;
		}
	}

	public void Lock() {
		mutex.lock();
	}

	public boolean TryLockWhenIdle() {
		if (mutex.hasQueuedThreads())
			return false;
		return mutex.tryLock();
	}

	// 必须且仅调用一次。
	public void UnLock() {
		mutex.unlock();
	}

	public static void TryUpdateAndCheckpoint(Transaction trans, Procedure procedure, Runnable commit) {
		switch (procedure.getZeze().getConfig().getCheckpointMode()) {
		case Immediately:
			commit.run();
			procedure.getZeze().getCheckpoint().Flush(trans);
			// 这种模式下 RelativeRecordSet 都是空的。
			return;

		case Period:
			commit.run();
			// 这种模式下 RelativeRecordSet 都是空的。
			return;

		default:
			break;
		}

		// CheckpointMode.Table
		boolean needFlushNow = false;
		boolean allCheckpointWhenCommit = true;

		var RelativeRecordSets = new TreeMap<Long, RelativeRecordSet>();
		var transAccessRecords = new HashSet<Record>();
		boolean allRead = true;
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.Dirty)
				allRead = false;

			var record = ar.AtomicTupleRecord.Record;
			if (record.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.Dirty) {
					needFlushNow = true;
				}
			} else {
				allCheckpointWhenCommit = false;
			}
			// 读写都需要收集。
			transAccessRecords.add(record);
			var volatileRrs = record.getRelativeRecordSet();
			RelativeRecordSets.put(volatileRrs.Id, volatileRrs);
		}

		if (allCheckpointWhenCommit) {
			// && procedure.Zeze.Config.CheckpointMode != CheckpointMode.Period
			// CheckpointMode.Period上面已经处理了，此时不会是它。
			// 【优化】，事务内访问的所有记录都是Immediately的，马上提交，不需要更新关联记录集合。
			commit.run();
			procedure.getZeze().getCheckpoint().Flush(trans);
			// 这种情况下 RelativeRecordSet 都是空的。
			//logger.Debug($"allCheckpointWhenCommit AccessedCount={trans.AccessedRecords.Count}");
			return;
		}

		var LockedRelativeRecordSets = new ArrayList<RelativeRecordSet>();
		try {
			_lock_(LockedRelativeRecordSets, RelativeRecordSets, transAccessRecords);
			if (!LockedRelativeRecordSets.isEmpty()) {
				/*
				// 锁住以后重新检查是否可以不用合并，直接提交。
				//【这个算优化吗？】效果应该不明显，而且正确性还要仔细分析，先不实现了。
				var allCheckpointWhenCommit2 = true;
				foreach (var ar in trans.AccessedRecords.Values)
				{
				    // CheckpointWhenCommit Dirty Isolated NeedMerge
				    // false                false false    Yes
				    // false                false true     No
				    // false                true  false    Yes
				    // false                true  true     No
				    // true                 false false!   No !马上提交的记录不会有关联集合
				    // true                 false true     No
				    // true                 true  false!   No !马上提交的记录不会有关联集合
				    // true                 true  true     No
				    if (false == ar.OriginRecord.Table.TableConf.CheckpointWhenCommit
				        && ar.OriginRecord.RelativeRecordSet.RecordSet != null)
				    {
				        allCheckpointWhenCommit2 = false;
				        break;
				    }
				}
				if (allCheckpointWhenCommit2)
				{
				    commit();
				    procedure.Zeze.Checkpoint.Flush(trans);
				    // 这种情况下 RelativeRecordSet 都是空的。
				    //logger.Debug($"allCheckpointWhenCommit2 AccessedCount={trans.AccessedRecords.Count}");
				    return;
				}
				*/
				var mergedSet = _merge_(LockedRelativeRecordSets, trans, allRead);
				commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
				if (needFlushNow) {
					procedure.getZeze().getCheckpoint().Flush(mergedSet);
					mergedSet.Delete();
					//logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
				} else if (mergedSet.RecordSet != null) {
					// mergedSet 合并结果是孤立的，不需要Flush。
					// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
					procedure.getZeze().getCheckpoint().RelativeRecordSetMap.add(mergedSet);
				}
			}
			// else
			// 本次事务没有访问任何数据。
		} finally {
			LockedRelativeRecordSets.forEach(RelativeRecordSet::UnLock);
		}
	}

	private static RelativeRecordSet _merge_(
			ArrayList<RelativeRecordSet> LockedRelativeRecordSets, Transaction trans, boolean allRead) {
		// find largest
		var largest = LockedRelativeRecordSets.get(0);
		for (int index = 1; index < LockedRelativeRecordSets.size(); ++index) {
			var r = LockedRelativeRecordSets.get(index);
			var cur = largest.RecordSet == null ? 1 : largest.RecordSet.size();
			if (r.RecordSet != null && r.RecordSet.size() > cur) {
				largest = r;
			}
		}

		// merge all other set to largest
		for (var r : LockedRelativeRecordSets) {
			if (r != largest) // skip self
				largest.Merge(r);
		}

		// 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
		if (largest.RecordSet != null || !allRead) {
			// merge 孤立记录。
			for (var ar : trans.getAccessedRecords().values()) {
				var record = ar.AtomicTupleRecord.Record;
				var rrs = record.getRelativeRecordSet();
				if (rrs.RecordSet == null || rrs == largest /* is self. ugly */)
					largest.Merge(record); // 合并孤立记录。这里包含largest是孤立记录的情况。
			}
		}

		return largest;
	}

	private static void _lock_(ArrayList<RelativeRecordSet> LockedRelativeRecordSets,
							   TreeMap<Long, RelativeRecordSet> RelativeRecordSets,
							   HashSet<Record> transAccessRecords) {
		while (true) {
			var GotoLabelLockRelativeRecordSets = false;
			int index = 0;
			int n = LockedRelativeRecordSets.size();
			final var itRrs = RelativeRecordSets.values().iterator();
			var rrs = itRrs.hasNext() ? itRrs.next() : null;
			while (null != rrs) {
				if (index >= n) {
					if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, rrs, transAccessRecords)) {
						rrs = itRrs.hasNext() ? itRrs.next() : null;
						continue;
					}
					GotoLabelLockRelativeRecordSets = true;
					break;
				}
				var curSet = LockedRelativeRecordSets.get(index);
				int c = Long.compare(curSet.Id, rrs.Id);
				if (c == 0) {
					++index;
					rrs = itRrs.hasNext() ? itRrs.next() : null;
					continue;
				}
				if (c < 0) {
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					for (; unlockEndIndex < n && LockedRelativeRecordSets.get(unlockEndIndex).Id < rrs.Id;
						 ++unlockEndIndex) {
						LockedRelativeRecordSets.get(unlockEndIndex).UnLock();
					}
					LockedRelativeRecordSets.subList(index, unlockEndIndex).clear();
					n = LockedRelativeRecordSets.size();
					// 重新从当前 rrs 继续锁。
					continue;
				}
				// RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
				// 从当前位置释放锁，再次尝试。
				for (int i = index; i < n; ++i) {
					LockedRelativeRecordSets.get(i).UnLock();
				}
				LockedRelativeRecordSets.subList(index, n).clear();
				n = LockedRelativeRecordSets.size();
				// 重新从当前 rrs 继续锁。
			}
			if (!GotoLabelLockRelativeRecordSets)
				break; // success
		}
	}

	private static boolean _lock_and_check_(ArrayList<RelativeRecordSet> locked,
											TreeMap<Long, RelativeRecordSet> all,
											RelativeRecordSet rrs,
											HashSet<Record> transAccessRecords) {
		rrs.Lock();
		var mergeTo = rrs.MergeTo;
		if (mergeTo != null) {
			rrs.UnLock();
			all.remove(rrs.Id); // remove merged or deleted rrs
			if (mergeTo == Deleted) {
				// flush 后进入这个状态。此时表示旧的关联集合的checkpoint点已经完成。
				// 但仍然需要重新获得当前事务中访问的记录的rrs。
				for (var r : rrs.RecordSet) {
					// Deleted 的 rrs 不会再发生变化，在锁外处理 RecordSet。
					if (transAccessRecords.contains(r)) {
						var volatileTmp = r.getRelativeRecordSet();
						all.put(volatileTmp.Id, volatileTmp);
					}
				}
				return false;
			}
			all.put(mergeTo.Id, mergeTo);
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

	static class FlushSet implements Iterable<Record> {
		private final Checkpoint Checkpoint;
		private final TreeMap<Long, RelativeRecordSet> SortedRrs = new TreeMap<>();

		public FlushSet(Checkpoint cp) {
			Checkpoint = cp;
		}

		public int add(RelativeRecordSet rrs) {
			if (null != SortedRrs.putIfAbsent(rrs.Id, rrs))
				throw new RuntimeException("duplicate rrs");
			return SortedRrs.size();
		}

		public int size() {
			return SortedRrs.size();
		}

		@Override
		public Iterator<Record> iterator() {
			return new Iterator<>() {
				final Iterator<RelativeRecordSet> it = SortedRrs.values().iterator();
				Iterator<Record> rrs;

				@Override
				public boolean hasNext() {
					if (null != rrs && rrs.hasNext())
						return true;
					while (it.hasNext()) {
						var n = it.next();
						if (n.MergeTo == null) {
							// normal rrs
							rrs = n.getRecordSet().iterator();
							if (rrs.hasNext())
								return true;
							// continue when rrs is empty
						}
						// continue: Merged Or Deleted
					}
					// nothing
					return false;
				}

				@Override
				public Record next() {
					return rrs.next();
				}
			};
		}

		public void flush() {
			var locks = new ArrayList<RelativeRecordSet>(SortedRrs.size());
			try {
				for (var rrs : SortedRrs.values()) {
					rrs.Lock();
					locks.add(rrs);
				}

				Checkpoint.Flush(this);
				for (var rrs : SortedRrs.values()) {
					if (rrs.MergeTo == null)
						rrs.Delete(); // normal rrs: not merged and not deleted.
					Checkpoint.RelativeRecordSetMap.remove(rrs);
				}
				SortedRrs.clear();
			} finally {
				locks.forEach(RelativeRecordSet::UnLock);
			}
		}
	}

	public static void FlushWhenCheckpoint(Checkpoint checkpoint, ExecutorService pool) {
		var flushSet = new FlushSet(checkpoint);
		var flushLimit = checkpoint.getZeze().getConfig().getCheckpointModeTableFlushSetCount();
		if (pool == null) {
			for (var rrs : checkpoint.RelativeRecordSetMap) {
				if (flushSet.add(rrs) >= flushLimit)
					flushSet.flush();
			}
			if (flushSet.size() > 0)
				flushSet.flush();
		} else {
			// concurrent flush
			for (var rrs : checkpoint.RelativeRecordSetMap) {
				if (flushSet.add(rrs) >= flushLimit) {
					pool.execute(flushSet::flush);
					flushSet = new FlushSet(checkpoint);
				}
			}
			if (flushSet.size() > 0) {
				pool.execute(flushSet::flush);
			}
		}
	}

	public static void FlushWhenReduce(Record r, Checkpoint checkpoint) {
		var rrs = r.getRelativeRecordSet();
		while (rrs != null) {
			r.EnterFairLock(); // 用来保护State的查看。
			try {
				if (r.getState() == GlobalCacheManagerServer.StateRemoved) {
					return;
				}
			} finally {
				r.ExitFairLock();
			}

			rrs = _FlushWhenReduce(rrs, checkpoint);
		}
	}

	private static RelativeRecordSet _FlushWhenReduce(RelativeRecordSet rrs, Checkpoint checkpoint) {
		rrs.Lock();
		try {
			if (rrs.MergeTo == null) {
				if (rrs.RecordSet != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.Flush(rrs);
					rrs.Delete();
				}
				return null;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			// 此时可以认为直接成功了吧？
			// 或者不判断这个，总是由上面的步骤中处理。
			if (rrs.MergeTo == RelativeRecordSet.Deleted) {
				// has flush
				return null;
			}

			return rrs.MergeTo; // 返回这个能更快得到新集合的引用。
		} finally {
			rrs.UnLock();
		}
	}

	@Override
	public String toString() {
		Lock();
		try {
			if (MergeTo != null) {
				return "[MergeTo-" + MergeTo.Id + "]";
			}

			if (RecordSet == null) {
				return Id + "-[Isolated]";
			}
			return Id + "-" + RecordSet;
		} finally {
			UnLock();
		}
	}

	public static String RelativeRecordSetMapToString(Checkpoint checkpoint) {
		return checkpoint.RelativeRecordSetMap.toString();
	}
}
