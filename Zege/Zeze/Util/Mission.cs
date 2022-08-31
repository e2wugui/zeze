using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Zeze.Util
{
    public class Mission
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        // 任何任务执行异常都会设置这个，用于单元测试程序报错用。

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
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return ResultCode.Exception;
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
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
            }
        }

        public static volatile Action<NLog.LogLevel, Exception, long, string> LogAction = DefaultLogAction;

        public static void LogAndStatistics(Exception ex, long result, Net.Protocol p, bool IsRequestSaved, string aName = null)
        {
            var actionName = aName == null ? p.GetType().FullName : aName;
            if (IsRequestSaved == false)
                actionName += ":Response";

            var ll = (null != p?.Service?.Zeze) ? p.Service.Zeze.Config.ProcessReturnErrorLogLevel : NLog.LogLevel.Trace;
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

        public static async Task<long> CallAsync(Func<Task> aa, string actionName)
        {
            return await CallAsync(async () => { await aa(); return 0; }, actionName);
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
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return ResultCode.Exception;
            }
        }

        public static async Task<long> CallAsync(Func<Net.Protocol, Task<long>> phandle, Net.Protocol p,
            Action<Net.Protocol, long> actionWhenError = null, string name = null)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                long result = await phandle(p);
                if (result != 0 && IsRequestSaved)
                {
                    actionWhenError?.Invoke(p, result);
                }
                LogAndStatistics(null, result, p, IsRequestSaved, name);
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
                var errorCode = ResultCode.Exception;
                if (ex is TaskCanceledException)
                    errorCode = ResultCode.CancelException;
#if !USE_CONFCS
                else if (ex is Raft.RaftRetryException)
                    errorCode = ResultCode.RaftRetry;
#endif
                if (IsRequestSaved)
                    actionWhenError?.Invoke(p, errorCode);
                // use last inner cause
                LogAndStatistics(ex, errorCode, p, IsRequestSaved, name);
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
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

                var errorCode = ResultCode.Exception;
                if (ex is TaskCanceledException)
                    errorCode = ResultCode.CancelException;
#if !USE_CONFCS
                else if (ex is Raft.RaftRetryException)
                    errorCode = ResultCode.RaftRetry;
#endif
                if (IsRequestSaved)
                    actionWhenError?.Invoke(p, errorCode);
                // use last inner cause
                LogAndStatistics(ex, errorCode, p, IsRequestSaved);
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return errorCode;
            }
        }

#if !USE_CONFCS
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
                // Procedure.Call处理了所有错误。除非内部错误或者单元测试异常，不会到这里。
                if (null != isRequestSaved && isRequestSaved.Value)
                {
                    actionWhenError?.Invoke(from, ResultCode.Exception);
                }
                LogAction?.Invoke(NLog.LogLevel.Error, ex, ResultCode.Exception, procedure.ActionName);
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return ResultCode.Exception;
            }
        }
#endif
    }
}
