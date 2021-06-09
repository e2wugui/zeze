
using System;
using System.Threading.Tasks;
using Zeze.Transaction;

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

        public static void LogAndStatistics(int result, Net.Protocol p, bool IsRequestSaved)
        {
            var actionName = p.GetType().FullName;
            if (IsRequestSaved == false)
                actionName = actionName + ":Response";

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
            ProcedureStatistics.Instance.GetOrAdd(actionName).GetOrAdd(result).IncrementAndGet();
#endif
        }

        public static void Call(Func<int> func, Net.Protocol p, bool sendResultCodeWhenError = false)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                int result = func();
                if (sendResultCodeWhenError
                    && result != 0
                    && IsRequestSaved)
                {
                    p.SendResultCode(result);
                }
                LogAndStatistics(result, p, IsRequestSaved);
            }
            catch (Exception ex)
            {
                while (ex is AggregateException)
                    ex = ex.InnerException;

                var errorCode = ex is TaskCanceledException
                    ? Procedure.CancelExcption
                    : Procedure.Excption;

                if (sendResultCodeWhenError && IsRequestSaved)
                    p.SendResultCode(errorCode);

                LogAndStatistics(errorCode, p, IsRequestSaved);
                // 使用 InnerException
                logger.Error(ex, "Task {0} Exception UserState={1}",
                    p.GetType().FullName, p.UserState);
            }
        }

        public static System.Threading.Tasks.Task Run(Func<int> func, Zeze.Net.Protocol p
            , bool sendResultCodeWhenError = false)
        {
            return System.Threading.Tasks.Task.Run(() => Call(func, p, sendResultCodeWhenError));
        }

        public static void Call(Zeze.Transaction.Procedure procdure, Net.Protocol from = null)
        {
            try
            {
                // 日志在Call里面记录。因为要支持嵌套。
                // 统计在Call里面实现。
                int result = procdure.Call();
                if (result != 0 && null != from && from.IsRequest)
                    from.SendResultCode(result);
            }
            catch (Exception ex)
            {
                // 这里应该不可能会发生了，除非内部错误。
                logger.Error(ex, procdure.ActionName);
            }
        }

        public static System.Threading.Tasks.Task Run(
            Zeze.Transaction.Procedure procdure,
            Net.Protocol from = null)
        {
            return System.Threading.Tasks.Task.Run(() => Call(procdure, from));
        }
    }
}
