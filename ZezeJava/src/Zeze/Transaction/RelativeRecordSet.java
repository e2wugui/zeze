package Zeze.Transaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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
	private long Id;
	public final long getId() {
		return Id;
	}

	// 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
	private RelativeRecordSet MergeTo;
	public final RelativeRecordSet getMergeTo() {
		return MergeTo;
	}
	private void setMergeTo(RelativeRecordSet value) {
		MergeTo = value;
	}

	public final static AtomicLong IdGenerator = new AtomicLong();
	public final static RelativeRecordSet Deleted = new RelativeRecordSet();

	private final static java.util.concurrent.ConcurrentHashMap<RelativeRecordSet, RelativeRecordSet> RelativeRecordSetMap = new java.util.concurrent.ConcurrentHashMap<RelativeRecordSet, RelativeRecordSet>();

	public RelativeRecordSet() {
		Id = IdGenerator.incrementAndGet();
	}

	private void Merge(RelativeRecordSet rrs) {
		// check outside
		//if (rrs == this)
		//    throw new Exception("!!!");

		if (rrs.getRecordSet() == null) {
			return; // 孤立记录，后面单独合并。
		}

		if (getRecordSet() == null) {
			setRecordSet(new HashSet<Record>());
		}

		for (var r : rrs.getRecordSet()) {
			getRecordSet().add(r);
			r.setRelativeRecordSet(this);
		}

		rrs.setMergeTo(this);
	}

	private void Merge(Record r) {
		if (getRecordSet() == null) {
			setRecordSet(new HashSet<Record>());
		}

		getRecordSet().add(r);

		if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
			// check outside
			//if (r.RelativeRecordSet.RecordSet != null)
			//    throw new Exception("Error State: Only Isolated Record Need Merge This Way.");

			r.getRelativeRecordSet().setMergeTo(this);

			// 在原孤立集合中添加当前记录的引用。
			// 并发访问时，可以从这里重新得到新的集合的引用。
			// 孤立集合初始化时没有包含自己。
			r.getRelativeRecordSet().setRecordSet(new HashSet<Record>());
			r.getRelativeRecordSet().getRecordSet().add(r);

			// setup new ref
			r.setRelativeRecordSet(this);
		}
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

	private ReentrantLock mutex = new ReentrantLock();

	public final void Lock() {
		mutex.lock();
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
		for (var ar : trans.getAccessedRecords().values()) {
			if (ar.OriginRecord.getTable().getTableConf().getCheckpointWhenCommit()) {
				// 修改了需要马上提交的记录。
				if (ar.Dirty) {
					needFlushNow = true;
				}
			}
			else {
				allCheckpointWhenCommit = false;
			}
			RelativeRecordSets.put(ar.OriginRecord.getRelativeRecordSet().getId(), ar.OriginRecord.getRelativeRecordSet());
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
			_lock_(LockedRelativeRecordSets, RelativeRecordSets);
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
				var mergedSet = _merge_(LockedRelativeRecordSets, trans);
				commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
				if (needFlushNow) {
					procedure.getZeze().getCheckpoint().Flush(mergedSet);
					mergedSet.Delete();
					//logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
				}
				// else
				// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
			}
			// else
			// 本次事务没有访问任何数据。
		}
		finally {
			for (var relative : LockedRelativeRecordSets) {
				relative.UnLock();
			}
		}
	}

	private static RelativeRecordSet _merge_(ArrayList<RelativeRecordSet> LockedRelativeRecordSets, Transaction trans) {
		// find largest
		RelativeRecordSet largestCountSet = LockedRelativeRecordSets.get(0);
		for (int index = 1; index < LockedRelativeRecordSets.size(); ++index) {
			var r = LockedRelativeRecordSets.get(index);
			var cur = largestCountSet.getRecordSet() == null ? 0 : largestCountSet.getRecordSet().size();
			if (r.getRecordSet() != null && r.getRecordSet().size() > cur) {
				largestCountSet = r;
			}
		}
		// merge all other set to largest
		for (var r : LockedRelativeRecordSets) {
			if (r == largestCountSet) {
				continue; // skip self
			}
			largestCountSet.Merge(r);
		}
		// 合并当前事务中访问的孤立记录。
		for (var ar : trans.getAccessedRecords().values()) {
			// 记录访问 已经存在关联集合 需要额外合并记录
			// 读取     不存在          不需要（仅仅读取孤立记录，不需要加入关联集合）
			// 读取     存在（已有修改） 不需要（存在的集合前面合并了）
			// 修改     不存在（第一次） 【需要】
			// 修改     存在（继续修改） 不需要（存在的集合前面合并了）
			if (ar.Dirty && ar.OriginRecord.getRelativeRecordSet().RecordSet == null) {
				largestCountSet.Merge(ar.OriginRecord);
			}
		}
		return largestCountSet;
	}

	private static void _lock_(ArrayList<RelativeRecordSet> LockedRelativeRecordSets,
			TreeMap<Long, RelativeRecordSet> RelativeRecordSets) {

		while (true) {
			int index = 0;
			int n = LockedRelativeRecordSets.size();
			for (var rrs : RelativeRecordSets.values()) {
				if (index >= n) {
					if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, rrs)) {
						continue;
					}

					break;
				}
				var curset = LockedRelativeRecordSets.get(index);
				int c = Long.compare(curset.getId(), rrs.Id);
				if (c == 0) {
					++index;
					continue;
				}
				if (c < 0) {
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					for (; unlockEndIndex < n
							&& Long.compare(LockedRelativeRecordSets.get(unlockEndIndex).getId(), rrs.Id) < 0;
							++unlockEndIndex) {

						LockedRelativeRecordSets.get(unlockEndIndex).UnLock();
					}
					LockedRelativeRecordSets.subList(index, unlockEndIndex).clear();
					n = LockedRelativeRecordSets.size();
					continue;
				}
				// RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
				// 从当前位置释放锁，再次尝试。
				for (int i = index; i < n; ++i) {
					LockedRelativeRecordSets.get(i).UnLock();
				}
				LockedRelativeRecordSets.subList(index, n).clear();
				n = LockedRelativeRecordSets.size();
			}
		}
	}

	private static boolean _lock_and_check_(ArrayList<RelativeRecordSet> locked, TreeMap<Long, RelativeRecordSet> all, RelativeRecordSet rrs) {
		rrs.Lock();
		if (rrs.getMergeTo() != null) {
			rrs.UnLock();

			if (rrs.getMergeTo() == rrs) {
				throw new RuntimeException("Impossible!");
			}

			all.remove(rrs.getId());

			// 重新读取记录的关联集合的引用（并发）。
			for (var r : rrs.getRecordSet()) {
				var tmp = r.getRelativeRecordSet(); // concurrent
				all.put(tmp.Id, tmp);
			}
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

	public static void FlushWhenReduce(Record r, Checkpoint checkpoint, tangible.Action0Param after) {
		while (true) {
			if (_FlushWhenReduce(r.getRelativeRecordSet(), checkpoint, after)) {
				break;
			}
		}
	}

	private static boolean _FlushWhenReduce(RelativeRecordSet rrs, Checkpoint checkpoint, tangible.Action0Param after) {
		rrs.Lock();
		try {
			if (rrs.getMergeTo() == null) {
				if (rrs.getRecordSet() != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.Flush(rrs);
					rrs.Delete();
				}
				after.invoke();
				return true;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			// 此时可以认为直接成功了吧？
			// 或者不判断这个，总是由上面的步骤中处理。
			if (rrs.getMergeTo() == RelativeRecordSet.Deleted) {
				// has flush
				after.invoke();
				return true;
			}
			// */

			// return rrs.MergeTo; // 返回这个能更快得到新集合的引用。
			return false;
		}
		finally {
			rrs.UnLock();
		}
	}
}