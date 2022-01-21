using System;
using System.Collections.Generic;
using System.Threading;

namespace Zeze.Util
{
    /// <summary>
    /// 定时延期执行任务。有 System.Threading.Timer，这个没必要了。
    /// </summary>
    public sealed class Scheduler
    {
        private readonly SortedDictionary<SchedulerTask, SchedulerTask> scheduled = new SortedDictionary<SchedulerTask, SchedulerTask>();
        private readonly Thread thread;
        private volatile bool isRunning;

        public static Scheduler Instance { get; } = new Scheduler();

        public Scheduler()
        {
            isRunning = true;
            thread = new Thread(ThreadRun)
            {
                IsBackground = true
            };
            thread.Start();
        }

        /// <summary>
        /// 调度一个执行 action。
        /// </summary>
        /// <param name="action"></param>
        /// <param name="initialDelay">the time to delay first execution. Milliseconds</param>
        /// <param name="period">the period between successive executions. Milliseconds</param>
        /// <returns></returns>
        public SchedulerTask Schedule(Action<SchedulerTask> action, long initialDelay, long period = -1)
        {
            lock (this)
            {
                if (initialDelay < 0)
                    throw new ArgumentException();

                SchedulerTask t = new SchedulerTask(this, action, initialDelay, period);
                scheduled.Add(t, t);
                Monitor.Pulse(this);
                return t;
            }
        }

        /// <summary>
        /// 设置停止标志，并等待调度线程结束。不是必须调用。
        /// </summary>
        public void StopAndJoin()
        {
            isRunning = false;
            lock (this)
            {
                Monitor.Pulse(this);
            }
            thread.Join();
        }

        internal void Scheule(SchedulerTask t)
        {
            lock (this)
            {
                scheduled.Add(t, t);
                Monitor.Pulse(this);
            }
        }

        private void ThreadRun()
        {
            while (isRunning)
            {
                List<SchedulerTask> willRun = new List<SchedulerTask>(scheduled.Count);
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
    public class SchedulerTask : IComparable<SchedulerTask>
    {
        public Scheduler Scheduler { get; private set; }
        public long Time { get; private set; }
        public long Period { get; private set; }
        public long SequenceNumber { get; private set; }

        private volatile bool canceled;
        private readonly Action<SchedulerTask> action;

        private static readonly AtomicLong sequencer = new AtomicLong();

        internal SchedulerTask(Scheduler scheduler, Action<SchedulerTask> action, long initialDelay, long period)
        {
            this.Scheduler = scheduler;
            this.action = action;
            this.Time = Util.Time.NowUnixMillis + initialDelay;
            this.Period = period;
            this.SequenceNumber = sequencer.IncrementAndGet();
            this.canceled = false;
        }

        public void Cancel()
        {
            this.canceled = true;
        }
 
        internal void Run()
        {
            if (this.canceled)
                return;

            // 派发出去运行，让系统管理大量任务的线程问题。
            Task.Run(() => action(this), "SchedulerTask.Run");

            if (this.Period > 0)
            {
                this.Time += this.Period;
                this.Scheduler.Scheule(this);
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
