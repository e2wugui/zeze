using DotNext.Threading;
using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Util;

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

        public readonly static AtomicLong IdGenerator = new();
        public readonly static RelativeRecordSet Deleted = new();

        public RelativeRecordSet()
        {
            Id = IdGenerator.IncrementAndGet();
        }

        private void Merge(Record r)
        {
            //if (r.RelativeRecordSet.RecordSet != null)
            //    return; // 这里仅合并孤立记录。外面检查。

            if (RecordSet == null)
                RecordSet = new HashSet<Record>();

            RecordSet.Add(r);

            if (r.RelativeRecordSet != this) // 自己：不需要更新MergeTo和引用。
            {
                r.RelativeRecordSet.MergeTo = this;
                r.RelativeRecordSet = this;
            }
        }

        private void Merge(RelativeRecordSet rrs)
        {
            if (rrs == this)
                throw new Exception("!!!");

            if (rrs.RecordSet == null)
            {
                // 孤立记录后面单独Merge
                return;
            }

            if (RecordSet == null)
                RecordSet = new HashSet<Record>();

            foreach (var r in rrs.RecordSet)
            {
                RecordSet.Add(r);
                r.RelativeRecordSet = this;
            }

            rrs.MergeTo = this;
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

        private readonly AsyncLock Mutex = AsyncLock.Exclusive();

        internal async Task<IDisposable> LockAsync()
        {
            return await Mutex.AcquireAsync(CancellationToken.None);
        }

        internal IDisposable LockTry()
        {
            var source = new CancellationTokenSource();
            var context = Mutex.AcquireAsync(source.Token);
            if (context.AsTask().Wait(0))
            {
                return context.AsTask().Result;
            }
            source.Cancel();
            try
            {
                context.AsTask().Wait();
                return context.AsTask().Result;
            }
            catch (Exception)
            {
            }
            return null;
        }

        public static async Task TryUpdateAndCheckpoint(
            Transaction trans, Procedure procedure, Action commit)
        {
            switch (procedure.Zeze.Config.CheckpointMode)
            {
                case CheckpointMode.Immediately:
                    commit();
                    await Checkpoint.Flush(trans);
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
            var transAccessRecords = new HashSet<Record>();
            bool allRead = true;
            foreach (var ar in trans.AccessedRecords.Values)
            {
                if (ar.Dirty)
                    allRead = false;

                if (ar.Origin.Table.TableConf.CheckpointWhenCommit)
                {
                    // 修改了需要马上提交的记录。
                    if (ar.Dirty)
                        needFlushNow = true;
                }
                else
                {
                    allCheckpointWhenCommit = false;
                }
                transAccessRecords.Add(ar.Origin);
                var volatilerrs = ar.Origin.RelativeRecordSet;
                RelativeRecordSets[volatilerrs.Id] = volatilerrs;
            }

            if (allCheckpointWhenCommit)
            {
                // && procedure.Zeze.Config.CheckpointMode != CheckpointMode.Period
                // CheckpointMode.Period上面已经处理了，此时不会是它。
                // 【优化】，事务内访问的所有记录都是Immediately的，马上提交，不需要更新关联记录集合。
                commit();
                await Checkpoint.Flush(trans);
                // 这种情况下 RelativeRecordSet 都是空的。
                //logger.Debug($"allCheckpointWhenCommit AccessedCount={trans.AccessedRecords.Count}");
                return;
            }

            var locked = new List<(RelativeRecordSet, IDisposable)>();
            try
            {
                await LockRRS(locked, RelativeRecordSets, transAccessRecords);
                if (locked.Count > 0)
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
                    var mergedSet = Merge(locked, trans, allRead);
                    commit(); // 必须在锁获得并且合并完集合以后才提交修改。
                    if (needFlushNow)
                    {
                        await Checkpoint.Flush(mergedSet);
                        mergedSet.Delete();
                        //logger.Debug($"needFlushNow AccessedCount={trans.AccessedRecords.Count}");
                    }
                    else if (mergedSet.RecordSet != null) // mergedSet 合并结果是孤立的，不需要Flush。
                    {
                        // 本次事务没有包含任何需要马上提交的记录，留给 Period 提交。
                        procedure.Zeze.Checkpoint.RelativeRecordSetMap[mergedSet] = mergedSet;
                    }
                }
                // else
                // 本次事务没有访问任何数据。
            }
            finally
            {
                foreach (var rrs in locked)
                {
                    rrs.Item2.Dispose();
                }
            }
        }

        private static RelativeRecordSet Merge(List<(RelativeRecordSet, IDisposable)> locked, Transaction trans, bool allRead)
        {
            // find largest
            var largest = locked[0];
            for (int index = 1; index < locked.Count; ++index)
            {
                var r = locked[index];
                var cur = largest.Item1.RecordSet == null ? 1 : largest.Item1.RecordSet.Count;
                if (r.Item1.RecordSet != null && r.Item1.RecordSet.Count > cur)
                {
                    largest = r;
                }
            }
            // merge all other set to largest
            foreach (var r in locked)
            {
                if (r == largest)
                    continue;
                largest.Item1.Merge(r.Item1);
            }

            // 所有的记录都是读，并且所有的记录都是孤立的，此时不需要关联起来。
            if (largest.Item1.RecordSet != null || false == allRead)
            {
                // merge 孤立记录。
                foreach (var ar in trans.AccessedRecords.Values)
                {
                    if (ar.Origin.RelativeRecordSet.RecordSet == null
                        || ar.Origin.RelativeRecordSet == largest.Item1 // urgly
                        )
                        largest.Item1.Merge(ar.Origin); // 合并孤立记录。这里包含largest是孤立记录的情况。
                }
            }
            return largest.Item1;
        }

        private static async Task LockRRS(List<(RelativeRecordSet, IDisposable)> locked,
            SortedDictionary<long, RelativeRecordSet> sortedrrs, HashSet<Record> transAccessRecords)
        {
        LabelLockRelativeRecordSets:
            {
                int index = 0;
                int n = locked.Count;
                var itrrs = sortedrrs.Values.GetEnumerator();
                bool hasNext = itrrs.MoveNext();
                while (hasNext)
                {
                    var rrs = itrrs.Current;
                    if (index >= n)
                    {
                        if (await LockAndCheck(locked, sortedrrs, rrs, transAccessRecords))
                        {
                            hasNext = itrrs.MoveNext();
                            continue;
                        }
                        goto LabelLockRelativeRecordSets;
                    }
                    var curset = locked[index];
                    int c = curset.Item1.Id.CompareTo(rrs.Id);
                    if (c == 0)
                    {
                        ++index;
                        hasNext = itrrs.MoveNext();
                        continue;
                    }
                    if (c < 0)
                    {
                        // 释放掉不需要的锁（已经被Delete了，Has Flush）。
                        int unlockEndIndex = index;
                        for (;
                            unlockEndIndex < n
                            && locked[unlockEndIndex].Item1.Id < rrs.Id;
                            ++unlockEndIndex)
                        {
                            locked[unlockEndIndex].Item2.Dispose();
                        }
                        locked.RemoveRange(index, unlockEndIndex - index);
                        n = locked.Count;
                        // 重新从当前 rrs 继续锁。
                        continue;
                    }
                    // RelativeRecordSets发生了变化，并且出现排在当前已经锁住对象前面的集合。
                    // 从当前位置释放锁，再次尝试。
                    for (int i = index; i < n; ++i)
                    {
                        locked[i].Item2.Dispose();
                    }
                    locked.RemoveRange(index, n - index);
                    n = locked.Count;
                    // 重新从当前 rrs 继续锁。
                }
            }
        }

        private static async Task<bool> LockAndCheck(
            List<(RelativeRecordSet, IDisposable)> locked,
            SortedDictionary<long, RelativeRecordSet> all,
            RelativeRecordSet rrs,
            HashSet<Record> transAccessRecords)
        {
            var lockrrs = await rrs.LockAsync();
            var mergeTo = rrs.MergeTo;
            if (rrs.MergeTo != null)
            {
                lockrrs.Dispose();

                all.Remove(rrs.Id); // remove merged or deleted rrs
                if (mergeTo == Deleted)
                {
                    // flush 后进入这个状态。此时表示旧的关联集合的checkpoint点已经完成。
                    // 但仍然需要重新获得当前事务中访问的记录的rrs。
                    foreach (var r in rrs.RecordSet)
                    {
                        // Deleted 的 rrs 不会再发生变化，在锁外处理 RecordSet。
                        if (transAccessRecords.Contains(r))
                        {
                            var volatileTmp = r.RelativeRecordSet;
                            all[volatileTmp.Id] = volatileTmp;
                        }
                    }
                    return false;
                }

                all[mergeTo.Id] = mergeTo;
                return false;
            }
            locked.Add((rrs, lockrrs));
            return true;
        }

        /*
        private static async Task FlushAndDelete(Checkpoint checkpoint, RelativeRecordSet rrs)
        {
            using var lockrrs = await rrs.LockAsync();
            if (rrs.MergeTo != null)
            {
                checkpoint.RelativeRecordSetMap.TryRemove(rrs, out var _);
                return;
            }

            await Checkpoint.Flush(rrs);
            rrs.Delete();
            checkpoint.RelativeRecordSetMap.TryRemove(rrs, out var _);
        }
        */

        class FlushSet : IEnumerable<Record>
        {
            private readonly Checkpoint Checkpoint;
            private readonly SortedDictionary<long, RelativeRecordSet> SortedRrs = new();
            private readonly List<IDisposable> Locks = new();

            public FlushSet(Checkpoint cp)
            {
                Checkpoint = cp;
            }

            public int Add(RelativeRecordSet rrs)
            {
                if (false == SortedRrs.TryAdd(rrs.Id, rrs))
                    throw new Exception("duplicate rrs");
                return SortedRrs.Count;
            }

            public int Count => SortedRrs.Count;

            class Iterator : IEnumerator<Record>
            {
                private readonly IEnumerator<RelativeRecordSet> It;
                private IEnumerator<Record> Rrs;

                public Iterator(FlushSet fs)
                {
                    It = fs.SortedRrs.Values.GetEnumerator();
                }

                public Record Current => Rrs.Current;

                object IEnumerator.Current => Rrs.Current;

                public bool MoveNext()
                {
                    if (null != Rrs && Rrs.MoveNext())
                        return true;
                    while (It.MoveNext())
                    {
                        var n = It.Current;
                        if (n.MergeTo == null)
                        {
                            // normal rrs
                            Rrs = n.RecordSet.GetEnumerator();
                            if (Rrs.MoveNext())
                                return true;
                            // continue when rrs is empty
                        }
                        // continue: Merged Or Deleted
                    }
                    // nothing
                    return false;
                }

                public void Reset()
                {
                    It.Reset();
                    Rrs = null;
                }

                public void Dispose()
                {
                    It.Dispose();
                    Rrs?.Dispose();
                    Rrs = null;
                }
            }

            public IEnumerator<Record> GetEnumerator()
            {
                return new Iterator(this);
            }

            IEnumerator IEnumerable.GetEnumerator()
            {
                return new Iterator(this);
            }

            public async Task FlushAsync()
            {
                try
                {
                    foreach (var rrs in SortedRrs.Values)
                    {
                        Locks.Add(await rrs.LockAsync());
                    }
                    await Checkpoint.Flush(this);
                    foreach (var rrs in SortedRrs.Values)
                    {
                        if (rrs.MergeTo == null)
                            rrs.Delete(); // normal rrs: not merged and not deleted.
                        Checkpoint.RelativeRecordSetMap.TryRemove(rrs, out _);
                    }
                    SortedRrs.Clear();
                }
                finally
                {
                    foreach (var lck in Locks)
                    {
                        lck.Dispose();
                    }
                    Locks.Clear();
                }
            }
        }

        internal static async Task FlushWhenCheckpoint(Checkpoint checkpoint, bool synchronously)
        {
            var flushLimit = checkpoint.Zeze.Config.CheckpointModeTableFlushSetCount;
            var flushSet = new FlushSet(checkpoint);
            if (synchronously || checkpoint.Zeze.Config.CheckpointModeTableFlushConcurrent < 2)
            {
                foreach (var rrs in checkpoint.RelativeRecordSetMap.Keys)
                {
                    if (flushSet.Add(rrs) >= flushLimit)
                        await flushSet.FlushAsync();
                }
                if (flushSet.Count > 0)
                    await flushSet.FlushAsync();
                return;
            }

            // flush async
            foreach (var rrs in checkpoint.RelativeRecordSetMap.Keys)
            {
                if (flushSet.Add(rrs) >= flushLimit)
                {
                    _ = flushSet.FlushAsync();
                    flushSet = new FlushSet(checkpoint);
                }
            }
            if (flushSet.Count > 0)
                _ = flushSet.FlushAsync();
        }

        internal static async Task FlushWhenReduce(Record r)
        {
            var rrs = r.RelativeRecordSet;
            while (rrs != null)
            {
                {
                    using var lockr = await r.Mutex.AcquireAsync(CancellationToken.None);
                    if (r.State == Services.GlobalCacheManagerServer.StateRemoved)
                        return;
                }
                rrs = await FlushWhenReduce(rrs);
            }
        }

        private static async Task<RelativeRecordSet> FlushWhenReduce(RelativeRecordSet rrs)
        {
            using var lockrrs = await rrs.LockAsync();

            if (rrs.MergeTo == null)
            {
                if (rrs.RecordSet != null) // 孤立记录不用保存，肯定没有修改。
                {
                    await Checkpoint.Flush(rrs);
                    rrs.Delete();
                }
                return null;
            }
                
            // 这个方法是在 Reduce 获得记录锁，并降级（设置状态）以后才调用。
            // 已经不会有后续的修改（但可能有读取并且被合并然后又被Flush），
            // 或者被 Checkpoint Flush。
            // 此时可以认为直接成功了吧？
            // 或者不判断这个，总是由上面的步骤中处理。
            if (rrs.MergeTo == RelativeRecordSet.Deleted)
            {
                // has flush
                return null;
            }
            // */
            return rrs.MergeTo; // 返回这个能更快得到新集合的引用。
        }

        public override string ToString()
        {
            var task = LockAsync();
            task.Wait();
            using var lockthis = task.Result;

            if (MergeTo != null)
            {
                return "[MergeTo-" + MergeTo.Id + "]";
            }

            if (null == RecordSet)
            {
                return Id + "-[Isolated]";
            }
            var sb = new StringBuilder();
            sb.Append(Id).Append('-');
            Zeze.Serialize.ByteBuffer.BuildString(sb, RecordSet);
            return sb.ToString();
        }

        public static string RelativeRecordSetMapToString(Checkpoint checkpoint)
        {
            var sb = new StringBuilder();
            Zeze.Serialize.ByteBuffer.BuildString(sb, checkpoint.RelativeRecordSetMap.Keys);
            return sb.ToString();
        }
    }
}
