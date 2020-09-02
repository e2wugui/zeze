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

        public ReaderWriterLockSlim FlushReadWriteLock { get; }

        private static object checkpointLock = new object(); // 限制 Checkpoint 仅有一份在运行。先这样吧。

        public Checkpoint()
        {
            FlushReadWriteLock = new ReaderWriterLockSlim();
        }

        public Checkpoint(IEnumerable<Database> dbs)
        {
            FlushReadWriteLock = new ReaderWriterLockSlim();
            Add(dbs);
        }

        private Checkpoint(Checkpoint other)
        {
            FlushReadWriteLock = other.FlushReadWriteLock;
            Add(other.dbs);
        }

        public Checkpoint Duplicate()
        {
            return new Checkpoint(this);
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
            FlushReadWriteLock.EnterWriteLock();
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
                FlushReadWriteLock.ExitWriteLock();
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

        internal void WaitRun()
        {
            Thread.Sleep(10);
            lock (checkpointLock)
            {                
            }
        }

        internal void Run()
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
            FlushReadWriteLock.EnterReadLock();
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
                FlushReadWriteLock.ExitReadLock();
            }
        }

        internal void WaitAll()
        {
            WaitHandle.WaitAll(readys);
        }
    }
}
