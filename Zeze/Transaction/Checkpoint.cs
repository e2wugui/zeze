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

        private static object checkpointLock = new object(); // Checkpoint 仅有一份在运行。先这样吧。

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

        private ManualResetEvent[] readys;

        public void Run()
        {
            lock (checkpointLock)
            {
                readys = new ManualResetEvent[dbs.Count];

                int i = 0;
                foreach (var v in dbs)
                {
                    v.Ready.Reset();
                    readys[i++] = v.Ready;
                }

                i = 0;
                Task[] tasks = new Task[dbs.Count];
                foreach (var db in dbs)
                {
                    tasks[i++] = Task.Run(() => db.Checkpoint(this));
                }
                Task.WaitAll(tasks);
                readys = null;
            }
        }

        internal void WaitAll()
        {
            WaitHandle.WaitAll(readys);
        }
    }
}
