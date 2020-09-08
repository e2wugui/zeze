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
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private HashSet<Database> databases = new HashSet<Database>();

        internal ReaderWriterLockSlim FlushReadWriteLock { get; }
        public bool IsRunning { get; private set; }
        public int Period { get; private set; }
        private Task RunningTask;

        public Checkpoint()
        {
            FlushReadWriteLock = new ReaderWriterLockSlim();
        }

        public Checkpoint(IEnumerable<Database> dbs)
        {
            FlushReadWriteLock = new ReaderWriterLockSlim();
            Add(dbs);
        }

        public Checkpoint Add(IEnumerable<Database> databases)
        {
            foreach (var db in databases)
            {
                this.databases.Add(db);
            }
            return this;
        }

        private ManualResetEvent[] readys;

        internal void WaitRun()
        {
            Thread.Sleep(10);
            lock (this)
            {
            }
        }

        internal void Start(int period)
        {
            lock (this)
            {
                if (IsRunning)
                    return;

                IsRunning = true;
                Period = period;
                RunningTask = Task.Run(Run);
            }
        }

        internal void StopAndJoin()
        {
            lock (this)
            {
                IsRunning = false;
                Monitor.Pulse(this);
            }
            RunningTask.Wait();
        }

        private object RunOnceWait = new object();

        internal void RunOnce()
        {
            lock (RunOnceWait)
            {
                lock (this)
                {
                    Monitor.Pulse(this);
                }
                Monitor.Wait(RunOnceWait);
            }
        }

        private void Run()
        {
            while (IsRunning)
            {
                DoCheckpoint();
                foreach (Action action in actionCurrent)
                {
                    action();
                }
                lock (RunOnceWait)
                {
                    Monitor.PulseAll(RunOnceWait);
                }
                lock (this)
                {
                    if (actionPending.Count > 0)
                        continue; // 如果有未决的任务，马上开始下一次 DoCheckpoint。
                    Monitor.Wait(this, Period);
                }
            }
            logger.Fatal("final checkpoint start.");
            DoCheckpoint();
            logger.Fatal("final checkpoint end.");
        }

        private List<Action> actionCurrent;
        private List<Action> actionPending = new List<Action>();

        /// <summary>
        /// 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
        /// 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
        /// </summary>
        /// <param name="act"></param>
        internal void AddActionAndPulse(Action act)
        {
            FlushReadWriteLock.EnterReadLock();
            try
            {
                lock (this)
                {
                    actionPending.Add(act);
                    Monitor.Pulse(this);
                }
            }
            finally
            {
                FlushReadWriteLock.ExitReadLock();
            }
        }

        internal void WaitAllReady()
        {
            WaitHandle.WaitAll(readys);
        }

        private void DoCheckpoint()
        {
            // encodeN
            foreach (var db in databases)
            {
                db.EncodeN();
            }
            // snapshot
            {
                FlushReadWriteLock.EnterWriteLock();
                try
                {
                    actionCurrent = actionPending;
                    actionPending = new List<Action>();
                    foreach (var db in databases)
                    {
                        db.Snapshot();
                    }
                }
                finally
                {
                    FlushReadWriteLock.ExitWriteLock();
                }
            }
            // flush
            {
                readys = new ManualResetEvent[databases.Count];
                int i = 0;
                foreach (var v in databases)
                {
                    v.CommitReady.Reset();
                    readys[i++] = v.CommitReady;
                }
                i = 0;
                // 必须放在线程中执行，因为要支持多个数据库一起提交。db.Flush内部需要回调this.WaitAll。
                Task[] flushTasks = new Task[databases.Count];
                foreach (var db in databases)
                {
                    flushTasks[i++] = Task.Run(() => db.Flush(this));
                }
                Task.WaitAll(flushTasks);
                readys = null;
            }
            // cleanup
            foreach (var db in databases)
            {
                db.Cleanup();
            }
        }
    }
}
