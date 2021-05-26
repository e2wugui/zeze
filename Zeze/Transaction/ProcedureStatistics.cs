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
        public static ProcedureStatistics Instance { get; } = new ProcedureStatistics();

        public ConcurrentDictionary<string, Statistics> Procedures { get; } = new ConcurrentDictionary<string, Statistics>();

        public Statistics GetOrAdd(string procedureName)
        {
            return Procedures.GetOrAdd(procedureName, (key) => new Statistics());
        }

        public class Statistics
        {
            public ConcurrentDictionary<int, Zeze.Util.AtomicLong> Results { get; } = new ConcurrentDictionary<int, Util.AtomicLong>();

            public Zeze.Util.AtomicLong GetOrAdd(int result)
            {
                return Results.GetOrAdd(result, (key) => new Util.AtomicLong());
            }
        }
    }
}
