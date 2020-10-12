using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text;

namespace Zeze.Transaction
{
    sealed class Savepoint
    {
        private readonly Dictionary<long, Log> logs = new Dictionary<long, Log>(); // 保存所有的log
        //private readonly Dictionary<long, Log> newly = new Dictionary<long, Log>(); // 当前Savepoint新加的，用来实现Rollback，先不实现。

        private readonly Dictionary<long, ChangeNote> notes = new Dictionary<long, ChangeNote>(); // 所有Collection的ChangeNote。

        /*
        public void PutChangeNote(long key, ChangeNote note)
        {
            notes[key] = note;
        }
        */

        public ChangeNote GetOrAddChangeNote(long key, Func<ChangeNote> factory)
        {
            if (notes.TryGetValue(key, out var exist))
                return exist;
            ChangeNote newNote = factory();
            notes.Add(key, newNote);
            return newNote;
        }

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

            foreach (var e in other.notes)
            {
                if (this.notes.TryGetValue(e.Key, out var cur))
                    cur.Merge(e.Value);
                else
                    this.notes.Add(e.Key, e.Value);
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
            // 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
            /*
            foreach (var e in newly)
            {
                e.Value.Rollback();
            }
            */
        }

        internal Dictionary<long, Log> Logs => logs;
    }
}
