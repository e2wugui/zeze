
using System;

namespace Zeze.Util
{
    public class Task
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static void Call(Action action, string actionName)
        {
            try
            {
                action();
            }
            catch (Exception ex)
            {
                logger.Error(ex, actionName);
            }
        }

        public static System.Threading.Tasks.Task Run(Action action, string actionName)
        {
            return System.Threading.Tasks.Task.Run(() => Call(action, actionName));
        }

        public static void LogAndStatistics(int result, Net.Protocol p)
        {
            var actionName = p.GetType().FullName;
            if (result != 0)
            {
                var logLevel = (null != p.Service.Zeze)
                    ? p.Service.Zeze.Config.ProcessReturnErrorLogLevel
                    : NLog.LogLevel.Info;

                logger.Log(logLevel,
                    "Task {0} Return={1}@{2}:{3} UserState={4}",
                    actionName,
                    result,
                    Zeze.Net.Protocol.GetModuleId(result),
                    Zeze.Net.Protocol.GetProtocolId(result),
                    p.UserState);
            }
#if ENABLE_STATISTICS
            Zeze.Transaction.ProcedureStatistics.Instance.GetOrAdd(actionName).GetOrAdd(result).IncrementAndGet();
#endif
        }

        public static void Call(Func<int> func, Net.Protocol p)
        {
            try
            {
                int result = func();
                LogAndStatistics(result, p);
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Task {0} Exception UserState={1}", p.GetType().FullName, p.UserState);
            }
        }

        public static System.Threading.Tasks.Task Run(Func<int> func, Zeze.Net.Protocol p)
        {
            return System.Threading.Tasks.Task.Run(() => Call(func, p));
        }

        public static void Call(Zeze.Transaction.Procedure procdure)
        {
            try
            {
                // 日志在Call里面记录。因为要支持嵌套。
                // 统计在Call里面实现。
                procdure.Call();
            }
            catch (Exception ex)
            {
                // 这里应该不可能会发生了，除非内部错误。
                logger.Error(ex, procdure.ActionName);
            }
        }

        public static System.Threading.Tasks.Task Run(Zeze.Transaction.Procedure procdure)
        {
            return System.Threading.Tasks.Task.Run(() => Call(procdure));
        }
    }
}
