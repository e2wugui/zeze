using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public class TableStatistics
    {
        // 为了使用的地方可以方便访问，定义成全局的。
        // 这里的tableId也是全局分配的，即时起多个Zeze.Application，也是没问题的。see Table.cs
        public static TableStatistics Instance = new TableStatistics();

        public ConcurrentDictionary<int, Statistics> Tables { get; } = new ConcurrentDictionary<int, Statistics>();

        public Statistics GetOrAdd(int tableId)
        {
            return Tables.GetOrAdd(tableId, (key) => new Statistics());
        }

        public class Statistics
        {
            public Zeze.Util.AtomicLong ReadLockTimes { get; } = new Util.AtomicLong();
            public Zeze.Util.AtomicLong WriteLockTimes { get; } = new Util.AtomicLong();
            public Zeze.Util.AtomicLong StorageFindCount { get; } = new Util.AtomicLong(); // 从数据库中转载的次数。

            // 这两个统计用来观察cache清理的影响，
            public Zeze.Util.AtomicLong TryReadLockTimes { get; } = new Util.AtomicLong();
            public Zeze.Util.AtomicLong TryWriteLockTimes { get; } = new Util.AtomicLong();

            // global acquire 的次数，即时没有开启cache-sync，也会有一点点计数，因为没人抢，所以以后总是成功了。
            public Zeze.Util.AtomicLong GlobalAcquireShare { get; } = new Util.AtomicLong();
            public Zeze.Util.AtomicLong GlobalAcquireModify { get; } = new Util.AtomicLong();
            public Zeze.Util.AtomicLong GlobalAcquireInvalid { get; } = new Util.AtomicLong();

            // 虽然有锁升级存在，但数量很少，忽略掉后，就可以把读写访问加起来当作总的查找次数。
            public long TableFindCount => ReadLockTimes.Get() + WriteLockTimes.Get();
            public double TableCacheHit
            {
                get
                {
                    long total = TableFindCount;
                    return (total - StorageFindCount.Get()) / total;
                }
            }
            public double GlobalAcquireShareHit
            {
                get
                {
                    long total = TableFindCount;
                    return (total - GlobalAcquireShare.Get()) / total;
                }
            }
            public double GlobalAcquireModifyHit
            {
                get
                {
                    long total = TableFindCount;
                    return (total - GlobalAcquireModify.Get()) / total;
                }
            }
        }
    }
}
