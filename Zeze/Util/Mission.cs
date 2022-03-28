using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Zeze.Util
{
    public class Mission
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public static async Task AwaitNullableTask(Task task)
        {
            if (null != task)
                await task;
        }

        public static long Call(Func<long> action, string actionName)
        {
            try
            {
                return action();
            }
            catch (Exception ex)
            {
                logger.Error(ex, actionName);
                return Procedure.Exception;
            }
        }

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

        public static volatile Action<NLog.LogLevel, Exception, long, string> LogAction = DefaultLogAction;

        public static void LogAndStatistics(Exception ex, long result, Net.Protocol p, bool IsRequestSaved)
        {
            var actionName = p.GetType().FullName;
            if (IsRequestSaved == false)
                actionName += ":Response";

            var ll = (null != p.Service.Zeze) ? p.Service.Zeze.Config.ProcessReturnErrorLogLevel : NLog.LogLevel.Trace;
            LogAction?.Invoke(ll, ex, result, $"Action={actionName} {p}");

#if ENABLE_STATISTICS
            ProcedureStatistics.Instance.GetOrAdd(actionName).GetOrAdd(result).IncrementAndGet();
#endif
        }

        public static void DefaultLogAction(NLog.LogLevel level, Exception ex, long result, string message)
        {
            // exception -> Error
            // 0 != result -> level from parameter
            // others -> Trace
            NLog.LogLevel ll = (null != ex) ? NLog.LogLevel.Error : (0 != result) ? level : NLog.LogLevel.Trace;
            var module = "";
            if (result > 0)
                module = "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result);

            logger.Log(ll, ex, $"Return={result}{module} {message}");
        }

        public static async Task<long> CallAsync(Func<Task<long>> aa, string actionName)
        {
            try
            {
                return await aa();
            }
            catch (Exception ex)
            {
                logger.Error(ex, actionName);
                return Procedure.Exception;
            }
        }

        public static async Task<long> CallAsync(Func<Net.Protocol, Task<long>> phandle, Net.Protocol p,
            Action<Net.Protocol, long> actionWhenError = null)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                long result = await phandle(p);
                if (result != 0 && IsRequestSaved)
                {
                    actionWhenError?.Invoke(p, result);
                }
                LogAndStatistics(null, result, p, IsRequestSaved);
                return result;
            }
            catch (Exception ex)
            {
                while (true)
                {
                    var inner = ex.InnerException;
                    if (null == inner)
                        break;
                    ex = inner;
                }

                var errorCode = Procedure.Exception;
                if (ex is TaskCanceledException)
                    errorCode = Procedure.CancelException;
                else if (ex is Raft.RaftRetryException)
                    errorCode = Procedure.RaftRetry;

                if (IsRequestSaved)
                    actionWhenError?.Invoke(p, errorCode);
                // use last inner cause
                LogAndStatistics(ex, errorCode, p, IsRequestSaved);
                return errorCode;
            }
        }

        public static long Call(Func<long> func, Net.Protocol p,
            Action<Net.Protocol, long> actionWhenError = null)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                long result = func();
                if (result != 0 && IsRequestSaved)
                {
                    actionWhenError?.Invoke(p, result);
                }
                LogAndStatistics(null, result, p, IsRequestSaved);
                return result;
            }
            catch (Exception ex)
            {
                while (true)
                {
                    var inner = ex.InnerException;
                    if (null == inner)
                        break;
                    ex = inner;
                }

                var errorCode = Procedure.Exception;
                if (ex is TaskCanceledException)
                    errorCode = Procedure.CancelException;
                else if (ex is Raft.RaftRetryException)
                    errorCode = Procedure.RaftRetry;

                if (IsRequestSaved)
                    actionWhenError?.Invoke(p, errorCode);
                // use last inner cause
                LogAndStatistics(ex, errorCode, p, IsRequestSaved);
                return errorCode;
            }
        }

        public static async Task<long> CallAsync(
            Procedure procedure,
            Net.Protocol from = null,
            Action<Net.Protocol, long> actionWhenError = null)
        {
            bool? isRequestSaved = from?.IsRequest;
            try
            {
                // 日志在Call里面记录。因为要支持嵌套。
                // 统计在Call里面实现。
                long result = await procedure.CallAsync();
                if (result != 0 && null != isRequestSaved && isRequestSaved.Value)
                {
                    actionWhenError?.Invoke(from, result);
                }
                return result;
            }
            catch (Exception ex)
            {
                // Procedure.Call处理了所有错误。除非内部错误，不会到这里。
                if (null != isRequestSaved && isRequestSaved.Value)
                {
                    actionWhenError?.Invoke(from, Procedure.Exception);
                }
                LogAction?.Invoke(NLog.LogLevel.Error, ex, Procedure.Exception, procedure.ActionName);
                return Procedure.Exception;
            }
        }
    }
}
