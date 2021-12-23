package Zeze.Transaction;

import Zeze.Services.GlobalCacheManagerServer;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

/** 
 see zeze/README.md -> 18) 事务提交模式
 一个事务内访问的记录的集合。如果事务没有没提交，需要合并集合。
*/
public class RelativeRecordSet {
	// 采用链表，可以O(1)处理Merge，但是由于Merge的时候需要更新Record所属的关联集合，
	// 所以避免不了遍历，那就使用HashSet，遍历吧。
	// 可做的小优化：把Count小的关联集合Merge到大的里面。
	private HashSet<Record> RecordSet;
	public final HashSet<Record> getRecordSet() {
		return RecordSet;
	}
	private void setRecordSet(HashSet<Record> value) {
		RecordSet = value;
	}
	private final long Id;
	public final long getId() {
		return Id;
	}

	// 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
	private volatile RelativeRecordSet MergeTo;
	public final RelativeRecordSet getMergeTo() {
		return MergeTo;
	}
	private void setMergeTo(RelativeRecordSet value) {
		MergeTo = value;
	}

	public final static AtomicLong IdGenerator = new AtomicLong();
	public final static RelativeRecordSet Deleted = new RelativeRecordSet();

	private final static ConcurrentHashMap<RelativeRecordSet, RelativeRecordSet> RelativeRecordSetMap = new ConcurrentHashMap<>();

	public RelativeRecordSet() {
		Id = IdGenerator.incrementAndGet();
	}

	private void Merge(Record r) {
		//if (r.getRelativeRecordSet().RecordSet != null)
		//    return; // 这里仅合并孤立记录。外面检查。

		if (getRecordSet() == null) {
			setRecordSet(new HashSet<>());
		}
		getRecordSet().add(r);
		if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
			r.getRelativeRecordSet().setMergeTo(this);
			r.setRelativeRecordSet(this);
		}
	}

	private void Merge(RelativeRecordSet rrs) {
		if (rrs == this) // 这个方法仅用于合并其他rrs
			throw new RuntimeException("Merge Self! " + rrs);

		if (rrs.getRecordSet() == null) {
			return; // 孤立记录，后面单独合并。
		}

		if (getRecordSet() == null) {
			setRecordSet(new HashSet<>());
		}

		for (var r : rrs.getRecordSet()) {
			getRecordSet().add(r);
			r.setRelativeRecordSet(this);
		}

		rrs.setMergeTo(this);
	}

	public final void Delete() {
		if (null != getRecordSet()) { // 孤立记录不需要更新。
			// Flush完成以后，清除关联集合，
			for (var r : getRecordSet()) {
				r.setRelativeRecordSet(new RelativeRecordSet());
			}
			setMergeTo(Deleted);
		}
	}

	private final ReentrantLock mutex = new ReentrantLock(true);

	public final void Lock() {
		mutex.lock();
	}

	public final boolean TryLockWhenIdle() {
		if (mutex.hasQueuedThreads())
			return false;
		return mutex.tryLock();
	}

	// 必须且仅调用一次。
	public final void UnLock() {
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
		HashSet<Record> transAccessRecords = new HashSet<>();
		boolean allRead = true;
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.Dirty)
				allRead = false;

			if (ar.OriginRecord.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.Dirty) {
					needFlushNow = true;
				}
			}
			else {
				allCheckpointWhenCommit = false;
			}
			// 读写都需要收集。
			transAccessRecords.add(ar.OriginRecord);
			final var volatilerrs = ar.OriginRecord.getRelativeRecordSet();
			RelativeRecordSets.put(volatilerrs.Id, volatilerrs);
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
				}
				else if (mergedSet.getRecordSet() != null) {
					// mergedSet 合并结果是孤立的，不需要Flush。
					// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
					RelativeRecordSetMap.put(mergedSet, mergedSet);
				}
			}
			// else
			// 本次事务没有访问任何数据。
		}
		finally {
			for (var rrs : LockedRelativeRecordSets) {
				rrs.UnLock();
			}
		}
	}

	private static RelativeRecordSet _merge_(
			ArrayList<RelativeRecordSet> LockedRelativeRecordSets, Transaction trans, boolean allRead) {
		// find largest
		var largest = LockedRelativeRecordSets.get(0);
		for (int index = 1; index < LockedRelativeRecordSets.size(); ++index) {
			var r = LockedRelativeRecordSets.get(index);
			var cur = largest.getRecordSet() == null ? 1 : largest.getRecordSet().size();
			if (r.getRecordSet() != null && r.getRecordSet().size() > cur) {
				largest = r;
			}
		}

		// merge all other set to largest
		for (var r : LockedRelativeRecordSets) {
			if (r == largest) {
				continue; // skip self
			}
			largest.Merge(r);
		}

		// 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
		if (largest.getRecordSet() != null || !allRead) {
			// merge 孤立记录。
			for (var ar : trans.getAccessedRecords().values()) {
				if (ar.OriginRecord.getRelativeRecordSet().RecordSet == null
						|| ar.OriginRecord.getRelativeRecordSet() == largest /* is self. urgly */) {
					largest.Merge(ar.OriginRecord); // 合并孤立记录。这里包含largest是孤立记录的情况。
				}
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
			final var itrrs = RelativeRecordSets.values().iterator();
			var rrs = itrrs.hasNext() ? itrrs.next() : null;
			while (null != rrs) {
				if (index >= n) {
					if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, rrs, transAccessRecords)) {
						rrs = itrrs.hasNext() ? itrrs.next() : null;
						continue;
					}
					GotoLabelLockRelativeRecordSets = true;
					break;
				}
				var curset = LockedRelativeRecordSets.get(index);
				int c = Long.compare(curset.Id, rrs.Id);
				if (c == 0) {
					++index;
					rrs = itrrs.hasNext() ? itrrs.next() : null;
					continue;
				}
				if (c < 0) {
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					for (; unlockEndIndex < n
							&& LockedRelativeRecordSets.get(unlockEndIndex).Id < rrs.Id;
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
		var mergeTo = rrs.getMergeTo();
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

	public static void FlushWhenCheckpoint(Checkpoint checkpoint) {
		for (var rrs : RelativeRecordSetMap.keySet()) {
			rrs.Lock();
			try {
				if (rrs.MergeTo != null) {
					RelativeRecordSetMap.remove(rrs);
					continue;
				}

				checkpoint.Flush(rrs);
				rrs.Delete();
				RelativeRecordSetMap.remove(rrs);
			}
			finally {
				rrs.UnLock();
			}
		}
	}

	public static void FlushWhenReduce(Record r, Checkpoint checkpoint, Runnable after) {
		var rrs = r.getRelativeRecordSet();
		while (rrs != null) {
			r.EnterFairLock(); // 用来保护State的查看。
			try {
				if (r.getState() == GlobalCacheManagerServer.StateRemoved) {
					after.run();
					return;
				}
			} finally {
				r.ExitFairLock();
			}

			rrs = _FlushWhenReduce(rrs, checkpoint, after);
		}
	}

	private static RelativeRecordSet _FlushWhenReduce(RelativeRecordSet rrs, Checkpoint checkpoint, Runnable after) {
		rrs.Lock();
		try {
			if (rrs.getMergeTo() == null) {
				if (rrs.getRecordSet() != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.Flush(rrs);
					rrs.Delete();
				}
				after.run();
				return null;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			// 此时可以认为直接成功了吧？
			// 或者不判断这个，总是由上面的步骤中处理。
			if (rrs.getMergeTo() == RelativeRecordSet.Deleted) {
				// has flush
				after.run();
				return null;
			}

			return rrs.MergeTo; // 返回这个能更快得到新集合的引用。
		}
		finally {
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

			if (null == RecordSet) {
				return Id + "-[Isolated]";
			}
			var sb = new StringBuilder();
			sb.append(Id).append("-");
			sb.append(RecordSet);
			return sb.toString();
		} finally {
			UnLock();
		}
	}

	public static String RelativeRecordSetMapToString() {
		return RelativeRecordSetMap.keySet().toString();
	}
}