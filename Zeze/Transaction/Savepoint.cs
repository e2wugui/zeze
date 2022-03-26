using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    sealed class Savepoint
    {
        internal Dictionary<long, Log> Logs { get; } = new Dictionary<long, Log>(); // 保存所有的log
        //private readonly Dictionary<long, Log> Newly = new Dictionary<long, Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

        internal Dictionary<long, ChangeNote> ChangeNotes { get; } = new Dictionary<long, ChangeNote>(); // 所有Collection的ChangeNote。

        /*
        public void PutChangeNote(long key, ChangeNote note)
        {
            notes[key] = note;
        }
        */

        public ChangeNote GetOrAddChangeNote(long key, Func<ChangeNote> factory)
        {
            if (ChangeNotes.TryGetValue(key, out var exist))
                return exist;
            ChangeNote newNote = factory();
            ChangeNotes.Add(key, newNote);
            return newNote;
        }

        public void PutLog(Log log)
        {
            Logs[log.LogKey] = log;
            //newly[log.LogKey] = log;
        }

        public Log GetLog(long logKey)
        {
            return Logs.TryGetValue(logKey, out var log) ? log : null;
        }

        public Savepoint Duplicate()
        {
            var sp = new Savepoint();
            foreach (var e in Logs)
            {
                sp.Logs[e.Key] = e.Value;
            }
            return sp;
        }

        internal readonly List<Action> CommitActions = new();
        internal readonly List<Action> RollbackActions = new();

        public void MergeFrom(Savepoint other, bool isCommit)
        {
            if (isCommit)
            {
                foreach (var e in other.Logs)
                {
                    Logs[e.Key] = e.Value;
                }

                foreach (var e in other.ChangeNotes)
                {
                    if (this.ChangeNotes.TryGetValue(e.Key, out var cur))
                        cur.Merge(e.Value);
                    else
                        this.ChangeNotes.Add(e.Key, e.Value);
                }
                CommitActions.AddRange(other.CommitActions);
            }
            else
            {
                CommitActions.AddRange(other.RollbackActions);
                RollbackActions.AddRange(other.RollbackActions);
            }
        }

        public void Commit()
        {
            foreach (var e in Logs)
            {
                e.Value.Commit();
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
