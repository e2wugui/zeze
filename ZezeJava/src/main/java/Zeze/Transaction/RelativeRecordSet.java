package Zeze.Transaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Util.KV;

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

	private void Merge(KV<Record, RelativeRecordSet> rrrs) {
		final var rrs = rrrs.getValue();

		if (rrs == this) // 这个方法仅用于合并其他rrs，自己是孤立记录时外面特别处理。
			throw new RuntimeException("Merge Self! " + rrs);

		if (getRecordSet() == null) {
			setRecordSet(new HashSet<>());
		}

		if (rrs.getRecordSet() == null) {
			// 合并孤立记录
			final var r = rrrs.getKey();
			// 必须在这里创建，因为使用的地方有自己合并自己。【慢慢调整】
			getRecordSet().add(r);
			if (r.getRelativeRecordSet() != this) { // 自己：不需要更新MergeTo和引用。
				r.getRelativeRecordSet().setMergeTo(this);
				// 在原孤立集合中添加当前记录的引用。
				// 并发访问时，可以从这里重新得到新的集合的引用。
				// 孤立集合初始化时没有包含自己。
				// 【注意】这个不需要了，_lock_and_check_ 里面直接从MergeTo得到新关联集合。
				//r.getRelativeRecordSet().setRecordSet(new HashSet<>());
				//r.getRelativeRecordSet().getRecordSet().add(r);
				// setup new ref
				r.setRelativeRecordSet(this);
			}
			return; // 孤立记录，后面单独合并。
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

	private final ReentrantLock mutex = new ReentrantLock();

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
		// 原来就是想少创建一个对象，没有记住Record，这个用来处理孤立记录。看来这个KV对象省不掉。【慢慢优化】
		var RelativeRecordSets = new TreeMap<Long, KV<Record, RelativeRecordSet>>();
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
			// 读写都需要收集。
			if (!RelativeRecordSets.containsKey(ar.OriginRecord.getRelativeRecordSet().Id)) {
				// 关联集合稳定（访问的记录都在一个关联集合中）的时候。查询存在，少创建一个对象。
				RelativeRecordSets.put(ar.OriginRecord.getRelativeRecordSet().Id,
						KV.Create(ar.OriginRecord, ar.OriginRecord.getRelativeRecordSet()));
			}
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

		var LockedRelativeRecordSets = new ArrayList<KV<Record, RelativeRecordSet>>();
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
				var mergedSet = _merge_(LockedRelativeRecordSets);
				commit.run(); // 必须在锁获得并且合并完集合以后才提交修改。
				if (needFlushNow) {
					procedure.getZeze().getCheckpoint().Flush(mergedSet);
					mergedSet.Delete();
					//logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
				}
				else {
					// 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
					RelativeRecordSetMap.put(mergedSet, mergedSet);
				}
			}
			// else
			// 本次事务没有访问任何数据。
		}
		finally {
			for (var relative : LockedRelativeRecordSets) {
				relative.getValue().UnLock();
			}
		}
	}

	private static RelativeRecordSet _merge_(
			ArrayList<KV<Record, RelativeRecordSet>> LockedRelativeRecordSets) {
		// find largest
		var largest = LockedRelativeRecordSets.get(0);
		for (int index = 1; index < LockedRelativeRecordSets.size(); ++index) {
			var r = LockedRelativeRecordSets.get(index);
			var cur = largest.getValue().getRecordSet() == null ? 1 : largest.getValue().getRecordSet().size();
			if (r.getValue().getRecordSet() != null && r.getValue().getRecordSet().size() > cur) {
				largest = r;
			}
		}

		// merge all other set to largest
		for (var r : LockedRelativeRecordSets) {
			if (r == largest) {
				if (largest.getValue().RecordSet == null) {
					// 当前目标是孤立记录时，需要把自己的记录添加进去。此时不需要修改MergeTo。
					largest.getValue().RecordSet = new HashSet<>();
					largest.getValue().RecordSet.add(largest.getKey());
				}
				continue;
			}
			largest.getValue().Merge(r);
		}
		return largest.getValue();
	}

	private static void _lock_(ArrayList<KV<Record, RelativeRecordSet>> LockedRelativeRecordSets,
							   TreeMap<Long, KV<Record, RelativeRecordSet>> RelativeRecordSets) {

		while (true) {
			var GotoLabelLockRelativeRecordSets = false;
			int index = 0;
			int n = LockedRelativeRecordSets.size();
			for (var rrrs : RelativeRecordSets.values()) {
				if (index >= n) {
					if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, rrrs)) {
						continue;
					}
					GotoLabelLockRelativeRecordSets = true;
					break;
				}
				var curset = LockedRelativeRecordSets.get(index);
				int c = Long.compare(curset.getValue().Id, rrrs.getValue().Id);
				if (c == 0) {
					++index;
					continue;
				}
				if (c < 0) {
					// 释放掉不需要的锁（已经被Delete了，Has Flush）。
					int unlockEndIndex = index;
					for (; unlockEndIndex < n
							&& LockedRelativeRecordSets.get(unlockEndIndex).getValue().Id < rrrs.getValue().Id;
							++unlockEndIndex) {

						LockedRelativeRecordSets.get(unlockEndIndex).getValue().UnLock();
					}
					LockedRelativeRecordSets.subList(index, unlockEndIndex).clear();
					n = LockedRelativeRecordSets.size();
					continue;
				}
				// RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
				// 从当前位置释放锁，再次尝试。
				for (int i = index; i < n; ++i) {
					LockedRelativeRecordSets.get(i).getValue().UnLock();
				}
				LockedRelativeRecordSets.subList(index, n).clear();
				n = LockedRelativeRecordSets.size();
			}
			if (!GotoLabelLockRelativeRecordSets)
				break; // success
		}
	}

	private static boolean _lock_and_check_(ArrayList<KV<Record, RelativeRecordSet>> locked,
											TreeMap<Long, KV<Record, RelativeRecordSet>> all,
											KV<Record, RelativeRecordSet> rrrs) {
		final var rrs = rrrs.getValue();
		rrs.Lock();
		var mergeTo = rrs.getMergeTo();
		if (mergeTo != null) {
			rrs.UnLock();

			if (mergeTo == Deleted) {
				// 拿到被删除的关联集合，此时处于其关联集合处于重新设置过程中。
				// 需要重新读取。由于并发访问可能会循环多次。
				rrrs.setValue(rrrs.getKey().getRelativeRecordSet()); // volatile
				return false;
			}

			if (mergeTo == rrs) {
				throw new RuntimeException("Impossible!");
			}
			// 这个和上面Deleted相比会快一点，MergeTo是锁内获得的，肯定是新的。
			// 当然，由于并发访问，也可能会循环多次。
			rrrs.setValue(mergeTo);
			/*
			for (var r : rrs.getRecordSet()) {
				var tmp = r.getRelativeRecordSet(); // concurrent
				all.put(tmp.Id, tmp);
			}
			// TODO XXX 原来为什么写成，所有记录重新获取一次，应该都存在于合并后MergeTo里面了。
			*/
			return false;
		}
		locked.add(rrrs);
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
		while (true) {
			if (_FlushWhenReduce(r.getRelativeRecordSet(), checkpoint, after)) {
				break;
			}
		}
	}

	private static boolean _FlushWhenReduce(RelativeRecordSet rrs, Checkpoint checkpoint, Runnable after) {
		rrs.Lock();
		try {
			if (rrs.getMergeTo() == null) {
				if (rrs.getRecordSet() != null) { // 孤立记录不用保存，肯定没有修改。
					checkpoint.Flush(rrs);
					rrs.Delete();
				}
				after.run();
				return true;
			}

			// 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
			// 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
			// 或者被 Checkpoint Flush。
			// 此时可以认为直接成功了吧？
			// 或者不判断这个，总是由上面的步骤中处理。
			if (rrs.getMergeTo() == RelativeRecordSet.Deleted) {
				// has flush
				after.run();
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