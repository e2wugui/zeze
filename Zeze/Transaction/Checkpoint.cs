using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class Checkpoint
    {
        private HashSet<Database> dbs = new HashSet<Database>();

        private static object checkpointLock = new object(); // 限制 Checkpoint 仅有一份在运行。先这样吧。

        public Checkpoint()
        {

        }

        public Checkpoint(IEnumerable<Database> dbs)
        {
            Add(dbs);
        }

        public Checkpoint Clear()
        {
            dbs.Clear();
            return this;
        }

        public Checkpoint Add(Database db)
        {
            dbs.Add(db);
            return this;
        }

        public Checkpoint Add(IEnumerable<Database> dbs)
        {
            foreach (var db in dbs)
            {
                this.dbs.Add(db);
            }
            return this;
        }

        public Checkpoint Duplicate()
        {
            return new Checkpoint(dbs);
        }

        private ManualResetEvent[] readys;

        private void EncodeN()
        {
            int i = 0;
            Task[] tasks = new Task[dbs.Count];
            foreach (var db in dbs)
            {
                tasks[i++] = Task.Run(db.EncodeN);
            }
            Task.WaitAll(tasks);
        }

        private void Snapshot()
        {
            Transaction.FlushReadWriteLock.EnterWriteLock();
            actionDeny = true;
            try
            {
                int i = 0;
                Task[] tasks = new Task[dbs.Count];
                foreach (var db in dbs)
                {
                    tasks[i++] = Task.Run(db.Snapshot);
                }
                Task.WaitAll(tasks);
            }
            finally
            {
                Transaction.FlushReadWriteLock.ExitWriteLock();
            }
        }

        private void Flush()
        {
            readys = new ManualResetEvent[dbs.Count];

            int i = 0;
            foreach (var v in dbs)
            {
                v.CommitReady.Reset();
                readys[i++] = v.CommitReady;
            }

            i = 0;
            Task[] tasks = new Task[dbs.Count];
            foreach (var db in dbs)
            {
                tasks[i++] = Task.Run(() => db.Flush(this));
            }
            Task.WaitAll(tasks);
            readys = null;
        }

        private void Cleanup()
        {
            int i = 0;
            Task[] tasks = new Task[dbs.Count];
            foreach (var db in dbs)
            {
                tasks[i++] = Task.Run(db.Cleanup);
            }
            Task.WaitAll(tasks);
        }

        public void Run()
        {
            lock (checkpointLock)
            {
                EncodeN();
                Snapshot();
                Flush();
                foreach (Action act in actions)
                {
                    act();
                }
                Cleanup();
            }
        }

        private bool actionDeny = false;
        private List<Action> actions = new List<Action>();

        internal bool TryAddActionAfterCommit(Action act)
        {
            Transaction.FlushReadWriteLock.EnterReadLock();
            try
            {
                if (actionDeny)
                    return false;

                lock (actions)
                {
                    actions.Add(act);
                    return true;
                }
            }
            finally
            {
                Transaction.FlushReadWriteLock.ExitReadLock();
            }
        }

        internal void WaitAll()
        {
            WaitHandle.WaitAll(readys);
        }
    }
}
