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

        internal ReaderWriterLockSlim FlushReadWriteLock { get; } = new ReaderWriterLockSlim();
        public bool IsRunning { get; private set; }
        public int Period { get; private set; }
        private Task RunningTask;

        public Checkpoint()
        {
        }

        public Checkpoint(IEnumerable<Database> dbs)
        {
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
            // 严格来说，这里应该是等待一次正在进行的checkpoint，如果没有在执行中应该不启动新的checkpoint。
            // 但是由于时间窗口的原因，可能开始执行waitrun时，checkpoint还没开始，没办法进行等待。
            // 先使用RunOnce。
            this.RunOnce();
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

        internal void RunOnce()
        {
            TaskCompletionSource<int> source = new TaskCompletionSource<int>();
            AddActionAndPulse(() => source.SetResult(0));
            source.Task.Wait();
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
                if (databases.Count > 1)
                {
                    i = 0;
                    Task[] flushTasks = new Task[databases.Count];
                    // 必须放在线程中执行，因为要支持多个数据库一起提交。db.Flush内部需要回调this.WaitAll。
                    // flush 必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。先创建线程执行吧，看看怎么把线程缓存下来。
                    foreach (var db in databases)
                    {
                        TaskCompletionSource<bool> future = new TaskCompletionSource<bool>();
                        Thread flushThread = new Thread(() =>
                        {
                            db.Flush(this);
                            future.SetResult(true);
                        });
                        flushThread.IsBackground = true;
                        flushTasks[i++] = future.Task;
                        flushThread.Start();
                    }
                    Task.WaitAll(flushTasks);
                }
                else
                {
                    // 只有一个Database。同步执行。
                    foreach (var db in databases)
                    {
                        db.Flush(this);
                    }
                }
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
