using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text;

namespace Zeze.Transaction
{
    class Savepoint
    {
        private readonly Dictionary<long, Log> logs = new Dictionary<long, Log>(); // 保存所有的log
        //private readonly Dictionary<long, Log> newly = new Dictionary<long, Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

        public void PutLog(Log log)
        {
            logs[log.LogKey] = log;
            //newly[log.LogKey] = log;
        }

        public Log GetLog(long logKey)
        {
            return logs.TryGetValue(logKey, out var log) ? log : null;
        }

        public Savepoint Duplicate()
        {
            Savepoint sp = new Savepoint();
            foreach (var e in logs)
            {
                sp.logs[e.Key] = e.Value;
            }
            return sp;
        }

        public void Merge(Savepoint other)
        {
            foreach (var e in other.logs)
            {
                logs[e.Key] = e.Value;
            }
        }

        public void Commit()
        {
            foreach (var e in logs)
            {
                e.Value.Commit();
            }
        }

        public void Rollback()
        {
            /*
            foreach (var e in newly)
            {
                e.Value.Rollback();
            }
            */
        }
    }
}
