using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    /// <summary>
    /// 在Procedure中统计，由于嵌套存储过程存在，总数会比实际事务数多。
    /// 一般嵌套存储过程很少用，事务数量也可以参考这里的数值，不单独统计。
    /// 另外Transaction在重做时会在这里保存重做次数的统计。通过name和存储过程区分开来。
    /// </summary>
    public class ProcedureStatistics
    {
        static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static ProcedureStatistics Instance { get; } = new ProcedureStatistics();

        public ConcurrentDictionary<string, Statistics> Procedures { get; } = new ConcurrentDictionary<string, Statistics>();

        public Statistics GetOrAdd(string procedureName)
        {
            return Procedures.GetOrAdd(procedureName, (key) => new Statistics());
        }

        public class Statistics
        {
            public ConcurrentDictionary<long, Zeze.Util.AtomicLong> Results { get; }
                = new ConcurrentDictionary<long, Util.AtomicLong>();

            public Zeze.Util.AtomicLong GetOrAdd(long result)
            {
                return Results.GetOrAdd(result, (key) => new Util.AtomicLong());
            }

            public long GetTotalCount()
            {
                long total = 0;
                foreach (var e in Results)
                {
                    total += e.Value.Get();
                }
                return total;
            }

            public void Watch(long reachPerSecond, Action handle)
            {
                var watcher = new Watcher(this, reachPerSecond, handle);
                Util.Scheduler.Instance.Schedule((thisTask) => watcher.Check(this),
                    Watcher.CheckPeriod * 1000, Watcher.CheckPeriod * 1000);
            }

            public class Watcher
            {
                public const int CheckPeriod = 30;

                public long Last { get; private set; }
                public long Reach { get; }
                public Action ReachHandle { get; }

                public Watcher(Statistics stats, long reachPerSecond, Action handle)
                {
                    Last = stats.GetTotalCount();
                    Reach = reachPerSecond;
                    ReachHandle = handle;
                }

                public void Check(Statistics stats)
                {
                    var total = stats.GetTotalCount();
                    if ((total - Last) / CheckPeriod > Reach)
                    {
                        try
                        {
                            ReachHandle();
                        }
                        catch (Exception e)
                        {
                            logger.Error(e, "ProcedureStatistics.Watcher");
                        }
                    }
                    Last = total;
                }
            }
        }
    }
}
