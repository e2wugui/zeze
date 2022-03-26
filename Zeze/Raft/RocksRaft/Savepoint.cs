using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Raft.RocksRaft
{
    sealed class Savepoint
    {
        internal Dictionary<long, Log> Logs { get; } = new Dictionary<long, Log>(); // 保存所有的log
        //private readonly Dictionary<long, Log> Newly = new Dictionary<long, Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

        public void PutLog(Log log)
        {
            Logs[log.LogKey] = log;
            //newly[log.LogKey] = log;
        }

        public Log GetLog(long logKey)
        {
            return Logs.TryGetValue(logKey, out var log) ? log : null;
        }

        public Savepoint BeginSavepoint()
        {
            var sp = new Savepoint();
            foreach (var e in Logs)
            {
                sp.Logs[e.Key] = e.Value.BeginSavepoint();
            }
            return sp;
        }

        internal readonly List<Action> CommitActions = new();
        internal readonly List<Action> RollbackActions = new();

        public void EndSavepoint(Savepoint other, bool isCommit)
        {
            if (isCommit)
            {
                foreach (var e in other.Logs)
                {
                    e.Value.EndSavepoint(this);
                }
                CommitActions.AddRange(other.CommitActions);
            }
            else
            {
                CommitActions.AddRange(other.RollbackActions);
                RollbackActions.AddRange(other.RollbackActions);
            }
        }

        public void Rollback()
        {
            // 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
            /*
            foreach (var e in newly)
            {
                e.Value.Rollback();
            }
            */
        }
    }
}
