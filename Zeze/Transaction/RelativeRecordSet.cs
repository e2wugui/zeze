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

        // 不为null表示发生了变化，其中 == Deleted 表示被删除（已经Flush了）。
        public RelativeRecordSet MergeTo { get; private set; }

        public readonly static RelativeRecordSet Deleted = new RelativeRecordSet();

        private readonly static ConcurrentDictionary<RelativeRecordSet, RelativeRecordSet> RelativeRecordSetMap
            = new ConcurrentDictionary<RelativeRecordSet, RelativeRecordSet>();

        private void Merge(RelativeRecordSet rrs)
        {
            if (rrs.RecordSet == null)
                return;

            if (RecordSet == null)
                RecordSet = new HashSet<Record>();

            foreach (var r in rrs.RecordSet)
            {
                RecordSet.Add(r);
                r.RelativeRecordSet = this;
            }
            rrs.MergeTo = this;
            rrs.RecordSet = null;
        }

        private void Merge(Record r)
        {
            RecordSet.Add(r);
            r.RelativeRecordSet.MergeTo = this;
            r.RelativeRecordSet = this;
        }

        internal void Delete()
        {
            if (null != RecordSet)
            {
                // Flush完成以后，清除关联集合，
                foreach (var r in RecordSet)
                {
                    // 不重置所有的记录的状态，保留已经删除的状态也能工作。
                    // 这样就意味着本来已经孤立的记录会在旧的关联集合上加锁，
                    // 然后发现已经被删除，再次进行后续处理。
                    // 也就是说等到下一次事务的加锁时，进行一次处理。
                    // 重置能提高并发。并且更加清晰。
                    r.RelativeRecordSet = new RelativeRecordSet();
                }
                RecordSet = null;
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

        private static bool _lock_and_check_(
            List<RelativeRecordSet> locked,
            HashSet<RelativeRecordSet> rrs,
            RelativeRecordSet r)
        {
            r.Lock();
            if (r.MergeTo != null)
            {
                if (r.MergeTo == r)
                    throw new Exception("???"); // TODO 需要确认

                r.UnLock();
                rrs.Remove(r);
                rrs.Add(r.MergeTo);
                return false;
            }
            locked.Add(r);
            return true;
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
            var RelativeRecordSets = new HashSet<RelativeRecordSet>();
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
                RelativeRecordSets.Add(ar.OriginRecord.RelativeRecordSet);
            }

            if (allCheckpointWhenCommit)
            {
                // && procedure.Zeze.Config.CheckpointMode != CheckpointMode.Period
                // CheckpointMode.Period上面已经处理了，此时不会是它。
                // 【优化】，事务内访问的所有记录都是Immediately的，马上提交，不需要更新关联记录集合。
                commit();
                procedure.Zeze.Checkpoint.Flush(trans);
                // 这种情况下 RelativeRecordSet 都是空的。
                return;
            }

            var LockedRelativeRecordSets = new List<RelativeRecordSet>();
            try
            {
                _lock_(LockedRelativeRecordSets, RelativeRecordSets);
                if (LockedRelativeRecordSets.Count > 0)
                {
                    // Merge
                    // find largest
                    RelativeRecordSet largestCountSet = LockedRelativeRecordSets[0];
                    for (int index = 0; index < LockedRelativeRecordSets.Count; ++index)
                    {
                        var r = LockedRelativeRecordSets[index];
                        var cur = largestCountSet.RecordSet == null ? 0 : largestCountSet.RecordSet.Count;
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
                    // 孤立的记录上面没有特殊处理，在Merge函数中判断rrs.ResetSet==null忽略。
                    // 所以可能需要单独合并。
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
                    commit(); // 必须在锁获得并且合并完集合以后才提交修改。
                    if (needFlushNow)
                    {
                        procedure.Zeze.Checkpoint.Flush(largestCountSet);
                        largestCountSet.Delete();
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

        private static void _lock_(
            List<RelativeRecordSet> LockedRelativeRecordSets,
            HashSet<RelativeRecordSet> RelativeRecordSets)
        {
            while (true)
            {
            LabelLockRelativeRecordSets:
                var array = RelativeRecordSets.ToArray();
                Array.Sort(array);
                int index = 0;
                int n = LockedRelativeRecordSets.Count;
                foreach (var r in array)
                {
                    if (index >= n)
                    {
                        if (_lock_and_check_(LockedRelativeRecordSets, RelativeRecordSets, r))
                            continue;
                        goto LabelLockRelativeRecordSets;
                    }
                    var curset = LockedRelativeRecordSets[index];
                    int c = 0; // curset.CompareTo(r); TODO compare object reference
                    if (c == 0)
                    {
                        ++index;
                        continue;
                    }
                    if (c < 0)
                    {
                        // 这种情况应该也是不可能出现的。
                        // 因为已经锁住的集合不会发生变化，
                        // 也就不会被合并到其他地方，
                        // 不可能变没掉。
                        logger.Error("TryUpdateAndCheckpoint.Lock Impossible");
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
                break;
            }
        }

        internal static void FlushRelativeRecordSets(Checkpoint checkpoint)
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
            RelativeRecordSet rrs, Checkpoint checkpoint, Action after)
        {
            while (rrs != null)
            {
                rrs = _FlushWhenReduce(rrs, checkpoint, after);
            }
        }

        private static RelativeRecordSet _FlushWhenReduce(
            RelativeRecordSet rrs, Checkpoint checkpoint, Action after)
        {
            rrs.Lock();
            try
            {
                if (rrs.MergeTo == null)
                {
                    checkpoint.Flush(rrs);
                    rrs.Delete();
                    after();
                    return null;
                }

                if (rrs.MergeTo == RelativeRecordSet.Deleted)
                {
                    // has flush
                    after();
                    return null;
                }

                return rrs.MergeTo;
            }
            finally
            {
                rrs.UnLock();
            }
        }
    }
}
