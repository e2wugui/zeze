using System;
using System.Collections.Concurrent;
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

        internal static Scheduler Instance { get; } = new Scheduler();

        private ConcurrentDictionary<SchedulerTask, SchedulerTask> Timers { get; } = new();

        public Scheduler()
        {
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

            var task = new SchedulerTaskAction(action);
            if (false == Instance.Timers.TryAdd(task, task))
                throw new Exception("Impossible!");
            // 首先启动用一分钟创建Timer，然后Change成真正的参数，确保task.Timer在Timer触发的时候已被初始化。
            task.Timer = new Timer(task.Run, period, 60 * 1000, Timeout.Infinite);
            task.Timer.Change(initialDelay, period >= 0 ? period : Timeout.Infinite);
            return task;
        }

        public static SchedulerTask ScheduleAt(Action<SchedulerTask> action, int hour, int minute, long period = -1)
        {
            var now = new DateTime();
            var at = new DateTime(now.Year, now.Month, now.Day, hour, minute, 0);
            if (at.CompareTo(now) < 0)
                at = at.AddDays(1);
            long delay = Time.DateTimeToUnixMillis(at) - Time.DateTimeToUnixMillis(now);
            return Schedule(action, delay, period);
        }

        public static SchedulerTask Schedule(Func<SchedulerTask, Task> action, long initialDelay, long period = -1)
        {
            if (initialDelay < 0)
                throw new ArgumentException("initialDelay < 0");

            var task = new SchedulerTaskAsyncAction(action);
            if (false == Instance.Timers.TryAdd(task, task))
                throw new Exception("Impossible!");
            // 首先启动用一分钟创建Timer，然后Change成真正的参数，确保task.Timer在Timer触发的时候已被初始化。
            task.Timer = new Timer(task.Run, period, 60 * 1000, Timeout.Infinite);
            task.Timer.Change(initialDelay, period >= 0 ? period : Timeout.Infinite);
            return task;
        }

        public static bool Unschedule(SchedulerTask task)
        {
            task.Timer.Dispose();
            return Instance.Timers.TryRemove(task, out _);
        }

        public class SchedulerTaskAction : SchedulerTask
        {
            public Action<SchedulerTask> Action { get; }

            internal SchedulerTaskAction(Action<SchedulerTask> action)
            {
                Action = action;
            }

            public override void Process()
            {
                try
                {
                    Action(this);
                }
                catch (Exception ex)
                {
                    Scheduler.logger.Error(ex);
                }
            }
        }

        public class SchedulerTaskAsyncAction : SchedulerTask
        {
            public Func<SchedulerTask, Task> AsyncFunc { get; }

            internal SchedulerTaskAsyncAction(Func<SchedulerTask, Task> asyncFunc)
            {
                AsyncFunc = asyncFunc;
            }

            public override async void Process()
            {
                await Mission.CallAsync(async () => { await AsyncFunc(this); return 0; }, "SchedulerTaskAsyncAction");
            }
        }
    }

    public abstract class SchedulerTask
    {
        public Timer Timer { get; internal set; }

        public void Cancel()
        {
            Scheduler.Unschedule(this);
        }

        public void Run(object param)
        { 
            try
            {
                Process();
            }
            finally
            {
                var period = (long)param;
                if (period < 0)
                    Cancel();
            }
        }

        public abstract void Process();
    }
}