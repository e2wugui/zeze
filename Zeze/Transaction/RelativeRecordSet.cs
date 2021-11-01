using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    /// <summary>
    /// see zeze/README.md -> 18) 事务提交模式
    /// 一个事务内访问的记录的集合。如果事务没有没提交，需要合并集合。
    /// </summary>
    internal class RelativeRecordSet
    {
        // 采用链表，可以O(1)处理Merge，但是由于Merge的时候需要更新Record所属的关联集合，
        // 所以避免不了遍历，那就使用HashSet，遍历吧。
        // 可做的小优化：把Count小的关联集合Merge到大的里面。
        public HashSet<Record> RecordSet { get; private set; }
        public long Id { get; }

        // 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
        public RelativeRecordSet MergeTo { get; private set; }

        public readonly static Util.AtomicLong IdGenerator = new Util.AtomicLong();
        public readonly static RelativeRecordSet Deleted = new RelativeRecordSet();

        private readonly static ConcurrentDictionary<RelativeRecordSet, RelativeRecordSet> RelativeRecordSetMap
            = new ConcurrentDictionary<RelativeRecordSet, RelativeRecordSet>();

        public RelativeRecordSet()
        {
            Id = IdGenerator.IncrementAndGet();
        }

        private void Merge(RelativeRecordSet rrs)
        {
            // check outside
            //if (rrs == this)
            //    throw new Exception("!!!");

            if (rrs.RecordSet == null)
                return; // 孤立记录，后面单独合并。

            if (RecordSet == null)
                RecordSet = new HashSet<Record>();

            foreach (var r in rrs.RecordSet)
            {
                RecordSet.Add(r);
                r.RelativeRecordSet = this;
            }

            rrs.MergeTo = this;
        }

        private void Merge(Record r)
        {
            if (RecordSet == null)
                RecordSet = new HashSet<Record>();

            RecordSet.Add(r);

            if (r.RelativeRecordSet != this) // 自己：不需要更新MergeTo和引用。
            {
                // check outside
                //if (r.RelativeRecordSet.RecordSet != null)
                //    throw new Exception("Error State: Only Isolated Record Need Merge This Way.");

                r.RelativeRecordSet.MergeTo = this;

                // 在原孤立集合中添加当前记录的引用。
                // 并发访问时，可以从这里重新得到新的集合的引用。
                // 孤立集合初始化时没有包含自己。
                r.RelativeRecordSet.RecordSet = new HashSet<Record>();
                r.RelativeRecordSet.RecordSet.Add(r);

                // setup new ref
                r.RelativeRecordSet = this;
            }
        }

        internal void Delete()
        {
            if (null != RecordSet) // 孤立记录不需要更新。
            {
                // Flush完成以后，清除关联集合，
                foreach (var r in RecordSet)
                {
                    r.RelativeRecordSet = new RelativeRecordSet();
                }
                MergeTo = Deleted;
            }
        }

        internal void Lock()
        {
            System.Threading.Monitor.Enter(this);
        }

        // 必须且仅调用一次。
        internal void UnLock()
        {
            System.Threading.Monitor.Exit(this);
        }

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static void TryUpdateAndCheckpoint(
            Transaction trans, Procedure procedure, Action commit)
        {
            switch (procedure.Zeze.Config.CheckpointMode)
            {
                case CheckpointMode.Immediately:
                    commit();
                    procedure.Zeze.Checkpoint.Flush(trans);
                    // 这种模式下 RelativeRecordSet 都是空的。
                    return;

                case CheckpointMode.Period:
                    commit();
                    // 这种模式下 RelativeRecordSet 都是空的。
                    return;
            }

            // CheckpointMode.Table
            bool needFlushNow = false;
            bool allCheckpointWhenCommit = true;
            var RelativeRecordSets = new SortedDictionary<long, RelativeRecordSet>();
            foreach (var ar in trans.AccessedRecords.Values)
            {
                if (ar.OriginRecord.Table.TableConf.CheckpointWhenCommit)
                {
                    // 修改了需要马上提交的记录。
                    if (ar.Dirty)
                        needFlushNow = true;
                }
                else
                {
                    allCheckpointWhenCommit = false;
                }
                RelativeRecordSets[ar.OriginRecord.RelativeRecordSet.Id]
                    = ar.OriginRecord.RelativeRecordSet;
            }

            if (allCheckpointWhenCommit)
            {
                // && procedure.Zeze.Config.CheckpointMode != CheckpointMode.Period
                // CheckpointMode.Period上面已经处理了，此时不会是它。
                // 【优化】，事务内访问的所有记录都是Immediately的，马上提交，不需要更新关联记录集合。
                commit();
                procedure.Zeze.Checkpoint.Flush(trans);
                // 这种情况下 RelativeRecordSet 都是空的。
                //logger.Debug($"allCheckpointWhenCommit AccessedCount={trans.AccessedRecords.Count}");
                return;
            }

            var LockedRelativeRecordSets = new List<RelativeRecordSet>();
            try
            {
                _lock_(LockedRelativeRecordSets, RelativeRecordSets);
                if (LockedRelativeRecordSets.Count > 0)
                {
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
                    commit(); // 必须在锁获得并且合并完集合以后才提交修改。
                    if (needFlushNow)
                    {
                        procedure.Zeze.Checkpoint.Flush(mergedSet);
                        mergedSet.Delete();
                        //logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
                    }
                    // else
                    // 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
                }
                // else
                // 本次事务没有访问任何数据。
            }
            finally
            {
                foreach (var relative in LockedRelativeRecordSets)
                {
                    relative.UnLock();
                }
            }
        }

        private static RelativeRecordSet _merge_(
            List<RelativeRecordSet> LockedRelativeRecordSets,
            Transaction trans)
        {
            // find largest
            RelativeRecordSet largestCountSet = LockedRelativeRecordSets[0];
            for (int index = 1; index < LockedRelativeRecordSets.Count; ++index)
            {
                var r = LockedRelativeRecordSets[index];
                var cur = largestCountSet.RecordSet == null
                    ? 0 : largestCountSet.RecordSet.Count;
                if (r.RecordSet != null && r.RecordSet.Count > cur)
                {
                    largestCountSet = r;
                }
            }
            // merge all other set to largest
            foreach (var r in LockedRelativeRecordSets)
            {
                if (r == largestCountSet)
                    continue; // skip self
                largestCountSet.Merge(r);
            }
            // 合并当前事务中访问的孤立记录。
            foreach (var ar in trans.AccessedRecords.Values)
            {
                // 记录访问 已经存在关联集合 需要额外合并记录
                // 读取     不存在          不需要（仅仅读取孤立记录，不需要加入关联集合）
                // 读取     存在（已有修改） 不需要（存在的集合前面合并了）
                // 修改     不存在（第一次） 【需要】
                // 修改     存在（继续修改） 不需要（存在的集合前面合并了）
                if (ar.Dirty && ar.OriginRecord.RelativeRecordSet.RecordSet == null)
                    largestCountSet.Merge(ar.OriginRecord);
            }
            return largestCountSet;
        }

        private static void _lock_(
            List<RelativeRecordSet> LockedRelativeRecordSets,
            SortedDictionary<long, RelativeRecordSet> RelativeRecordSets)
        {
            LabelLockRelativeRecordSets:
            {
                int index = 0;
                int n = LockedRelativeRecordSets.Count;
                foreach (var rrs in RelativeRecordSets.Values)
                {
                    if (index >= n)
                    {
                        if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, rrs))
                            continue;
                        goto LabelLockRelativeRecordSets;
                    }
                    var curset = LockedRelativeRecordSets[index];
                    int c = curset.Id.CompareTo(rrs.Id);
                    if (c == 0)
                    {
                        ++index;
                        continue;
                    }
                    if (c < 0)
                    {
                        // 释放掉不需要的锁（已经被Delete了，Has Flush）。
                        int unlockEndIndex = index;
                        for (;
                            unlockEndIndex < n
                            && LockedRelativeRecordSets[unlockEndIndex].Id.CompareTo(rrs.Id) < 0;
                            ++unlockEndIndex)
                        {
                            LockedRelativeRecordSets[unlockEndIndex].UnLock();
                        }
                        LockedRelativeRecordSets.RemoveRange(index, unlockEndIndex - index);
                        n = LockedRelativeRecordSets.Count;
                        continue;
                    }
                    // RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
                    // 从当前位置释放锁，再次尝试。
                    for (int i = index; i < n; ++i)
                    {
                        LockedRelativeRecordSets[i].UnLock();
                    }
                    LockedRelativeRecordSets.RemoveRange(index, n - index);
                    n = LockedRelativeRecordSets.Count;
                }
            }
        }

        private static bool _lock_and_check_(
            List<RelativeRecordSet> locked,
            SortedDictionary<long, RelativeRecordSet> all,
            RelativeRecordSet rrs)
        {
            rrs.Lock();
            if (rrs.MergeTo != null)
            {
                rrs.UnLock();

                if (rrs.MergeTo == rrs)
                    throw new Exception("Impossible!");

                all.Remove(rrs.Id);

                // 重新读取记录的关联集合的引用（并发）。
                foreach (var r in rrs.RecordSet)
                {
                    var tmp = r.RelativeRecordSet; // concurrent
                    all[tmp.Id] = tmp;
                }
                return false;
            }
            locked.Add(rrs);
            return true;
        }

        internal static void FlushWhenCheckpoint(Checkpoint checkpoint)
        {
            foreach (var rrs in RelativeRecordSetMap.Keys)
            {
                rrs.Lock();
                try
                {
                    if (rrs.MergeTo != null)
                        continue;

                    checkpoint.Flush(rrs);
                    rrs.Delete();
                    RelativeRecordSetMap.TryRemove(rrs, out var _);
                }
                finally
                {
                    rrs.UnLock();
                }
            }
        }

        internal static void FlushWhenReduce(
            Record r, Checkpoint checkpoint, Action after)
        {
            while (true)
            {
                if (_FlushWhenReduce(r.RelativeRecordSet, checkpoint, after))
                    break;
            }
        }

        private static bool _FlushWhenReduce(
            RelativeRecordSet rrs, Checkpoint checkpoint, Action after)
        {
            rrs.Lock();
            try
            {
                if (rrs.MergeTo == null)
                {
                    if (rrs.RecordSet != null) // 孤立记录不用保存，肯定没有修改。
                    {
                        checkpoint.Flush(rrs);
                        rrs.Delete();
                    }
                    after();
                    return true;
                }
                
                // 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
                // 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
                // 或者被 Checkpoint Flush。
                // 此时可以认为直接成功了吧？
                /* 或者不判断这个，总是由上面的步骤中处理。
                if (rrs.MergeTo == RelativeRecordSet.Deleted)
                {
                    // has flush
                    after();
                    return true;
                }
                // */

                // return rrs.MergeTo; // 返回这个能更快得到新集合的引用。
                return false;
            }
            finally
            {
                rrs.UnLock();
            }
        }
    }
}
