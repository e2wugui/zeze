using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Zeze.Util
{
    /// <summary>
    /// 定时延期执行任务。有 System.Threading.Timer，这个没必要了。
    /// </summary>
    public sealed class Scheduler
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private readonly SortedDictionary<SchedulerTask, SchedulerTask> scheduled = new();
        private readonly Thread SchedulerThread;
        private volatile bool IsRunning;

        internal static Scheduler Instance { get; } = new Scheduler();

        public Scheduler()
        {
            IsRunning = true;
            SchedulerThread = new Thread(ThreadRun)
            {
                IsBackground = true
            };
            SchedulerThread.Start();
        }

        /// <summary>
        /// 调度一个执行 action。
        /// </summary>
        /// <param name="action"></param>
        /// <param name="initialDelay">the time to delay first execution. Milliseconds</param>
        /// <param name="period">the period between successive executions. Milliseconds</param>
        /// <returns></returns>
        public static SchedulerTask Schedule(Action<SchedulerTask> action, long initialDelay, long period = -1)
        {
            if (initialDelay < 0)
                throw new ArgumentException("initialDelay < 0");

            return Instance.Schedule(new SchedulerTaskAction(action, initialDelay, period));
        }

        public static SchedulerTask Schedule(Func<SchedulerTask, Task> action, long initialDelay, long period = -1)
        {
            if (initialDelay < 0)
                throw new ArgumentException("initialDelay < 0");

            return Instance.Schedule(new SchedulerTaskAsyncAction(action, initialDelay, period));
        }

        public static bool Unschedule(SchedulerTask task)
        {
            return Instance.UnschedulePrivate(task);
        }

        /// <summary>
        /// 设置停止标志，并等待调度线程结束。不是必须调用。
        /// </summary>
        public static void StopAndJoin()
        {
            Instance.IsRunning = false;
            lock (Instance)
            {
                Monitor.Pulse(Instance);
            }
            Instance.SchedulerThread.Join();
        }

        internal SchedulerTask Schedule(SchedulerTask t)
        {
            lock (this)
            {
                scheduled.Add(t, t);
                Monitor.Pulse(this);
                return t;
            }
        }

        private bool UnschedulePrivate(SchedulerTask task)
        {
            lock (this)
            {
                return scheduled.Remove(task);
            }
        }

        private void ThreadRun()
        {
            while (IsRunning)
            {
                var willRun = new List<SchedulerTask>(scheduled.Count);
                long nextTime = -1;
                long now = Time.NowUnixMillis;

                lock (this)
                {
                    foreach (SchedulerTask k in scheduled.Keys)
                    {
                        if (k.Time <= now)
                        {
                            willRun.Add(k);
                            continue;
                        }
                        nextTime = k.Time;
                        break;
                    }
                    foreach (SchedulerTask k in willRun)
                        scheduled.Remove(k);
                }

                foreach (SchedulerTask k in willRun)
                {
                    k.Run();
                }

                if (willRun.Count > 0) // 如果执行了任务，可能有重新调度的Task，马上再次检测。
                    continue;

                lock (this)
                {
                    int waitTime = Timeout.Infinite;
                    if (nextTime > now)
                        waitTime = (int)(nextTime - now);
                    Monitor.Wait(this, waitTime); // wait until new task or nextTime.
                }
            }
        }
    }

    public class SchedulerTaskAction : SchedulerTask
    {
        public Action<SchedulerTask> Action { get; }

        internal SchedulerTaskAction(Action<SchedulerTask> action, long initialDelay, long period)
            : base(initialDelay, period)
        {
            Action = action;
        }

        internal override void Dispatch()
        {
            // 派发出去运行，让系统管理大量任务的线程问题。
            Task.Run(() =>
            {
                try
                {
                    Action(this);
                }
                catch (Exception ex)
                {
                    Scheduler.logger.Error(ex);
                }
            });
        }
    }

    public class SchedulerTaskAsyncAction : SchedulerTask
    {
        public Func<SchedulerTask, Task> AsyncFunc { get; }

        internal SchedulerTaskAsyncAction(Func<SchedulerTask, Task> asyncFunc, long initialDelay, long period)
            : base(initialDelay, period)
        {
            AsyncFunc = asyncFunc;
        }

        internal override void Dispatch()
        {
            _ = Mission.CallAsync(async () => { await AsyncFunc(this); return 0; }, "SchedulerTaskAsyncAction");
        }
    }

    public abstract class SchedulerTask : IComparable<SchedulerTask>
    {
        public long Time { get; private set; }
        public long Period { get; private set; }
        public long SequenceNumber { get; private set; }

        private volatile bool canceled;

        private static readonly AtomicLong sequencer = new();

        internal SchedulerTask(long initialDelay, long period)
        {
            this.Time = Util.Time.NowUnixMillis + initialDelay;
            this.Period = period;
            this.SequenceNumber = sequencer.IncrementAndGet();
            this.canceled = false;
        }

        public void Cancel()
        {
            this.canceled = true;
            Scheduler.Unschedule(this);
        }

        internal abstract void Dispatch();

        internal void Run()
        {
            if (this.canceled)
                return;

            Dispatch();

            if (this.Period > 0)
            {
                this.Time += this.Period;
                Scheduler.Instance.Schedule(this);
            }
        }

        public int CompareTo(SchedulerTask other)
        {
            if (other == null) // 不可能吧
                return 1;

            if (other == this)
                return 0;

            long diff = Time - other.Time;
            if (diff < 0)
                return -1;

            if (diff > 0)
                return 1;

            if (this.SequenceNumber < other.SequenceNumber)
                return -1;

            return 1;
        }
    }
}
