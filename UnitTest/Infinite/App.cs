using System.Collections.Generic;
using Zeze.Util;

namespace Infinite
{
    public class App
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        internal demo.App app;
        Zeze.Config config;

        public App(int serverId)
        {
            config = Zeze.Config.Load("zeze.xml");
            config.ServerId = serverId;
            config.FastRedoWhenConflict = false;
            config.CheckpointPeriod = 1000;

            var tdef = config.DefaultTableConf;
            // 提高并发
            tdef.CacheCleanPeriod = 1000;
            // 超出容量时，快速尝试。
            tdef.CacheCleanPeriodWhenExceedCapacity = 0;
            // 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
            tdef.CacheCapacity = Simulate.CacheCapacity;

            var tflush = config.TableConfMap["demo_Module1_tflush"];
            // 提高并发
            tflush.CacheCleanPeriod = 1000;
            // 超出容量时，快速尝试。
            tflush.CacheCleanPeriodWhenExceedCapacity = 0;
            // 减少容量，实际使用记录数要超过一些。让TableCache.Cleanup能并发起来。
            tflush.CacheCapacity = Tasks.tflushInt1Trade.CacheCapacity;

            app = new demo.App();
        }

        public void Start()
        {
            app.Start(config);
        }

        public void Stop()
        {
            app.Stop();
        }

        public List<System.Threading.Tasks.Task> RunningTasks = new(Simulate.BatchTaskCount);

        public void Run(Tasks.Task task)
        {
            task.App = app;
            int keyNumber = task.getKeyNumber();
            int keyBound = task.getKeyBound();
            while (task.Keys.Count < keyNumber)
                task.Keys.Add(Random.Instance.Next(keyBound));
            foreach (var key in task.Keys)
                Tasks.getRunCounter(task.GetType().FullName, key).IncrementAndGet();
            if (task.IsProcedure())
                RunningTasks.Add(Task.Run(app.Zeze.NewProcedure(() => task.call(), task.GetType().FullName)));
            else
                RunningTasks.Add(Task.Run(() => task.call(), task.GetType().FullName));
        }

        public void WaitAllRunningTasksAndClear()
        {
            System.Threading.Tasks.Task.WaitAll(RunningTasks.ToArray());
            RunningTasks.Clear();
        }

        public static void Main(string[] args)
        {
            var logTarget = new NLog.Targets.FileTarget("infinite");
            logTarget.Layout = "${longdate} ${threadid} ${callsite} ${level} ${message} ${exception: format=Message,StackTrace}";
            logTarget.FileName = "infinite.log";

            var loggingRule = new NLog.Config.LoggingRule("*", NLog.LogLevel.Trace, logTarget);
            NLog.LogManager.Configuration.AddTarget("LogFile", logTarget);
            NLog.LogManager.Configuration.LoggingRules.Add(loggingRule);
            NLog.LogManager.ReconfigExistingLoggers();

            var simulate = new Simulate();
            simulate.Infinite = false; // 一直执行。
            simulate.Before();
            try
            {
                simulate.testMain();
            }
            finally
            {
                simulate.After();
            }
        }
    }
}
