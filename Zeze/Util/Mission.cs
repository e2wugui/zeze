using System;
using System.Threading.Tasks;
using Zeze.Net;
#if !USE_CONFCS
using Zeze.Transaction;
#endif

namespace Zeze.Util
{
    public class Mission
    {
#if HAS_NLOG
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
#elif HAS_MYLOG
        private static readonly Zeze.MyLog logger = Zeze.MyLog.GetLogger(typeof(Mission));
#endif
        // 任何任务执行异常都会设置这个，用于单元测试程序报错用。

        public static async Task AwaitNullableTask(Task task)
        {
            if (task != null)
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
#if HAS_NLOG || HAS_MYLOG
                logger.Error(ex, actionName);
#endif
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                    throw;
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
#if HAS_NLOG || HAS_MYLOG
                logger.Error(ex, actionName);
#endif
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                    throw;
#endif
            }
        }

        public static volatile Action<Config.LogLevel, Exception, long, string> LogAction = DefaultLogAction;

#if HAS_NLOG
        public static NLog.LogLevel NlogLogLevel(Config.LogLevel ll)
        {
            switch (ll)
            {
                case Config.LogLevel.Trace: return NLog.LogLevel.Trace;
                case Config.LogLevel.Debug: return NLog.LogLevel.Debug;
                case Config.LogLevel.Info: return NLog.LogLevel.Info;
                case Config.LogLevel.Warn: return NLog.LogLevel.Warn;
                case Config.LogLevel.Error: return NLog.LogLevel.Error;
                case Config.LogLevel.Fatal: return NLog.LogLevel.Fatal;
                case Config.LogLevel.Off: return NLog.LogLevel.Off;
            }
            throw new Exception($"Unknown LogLevel {ll}");
        }
#endif

        public static void LogAndStatistics(Exception ex, long result, Protocol p, bool IsRequestSaved,
            string aName = null)
        {
            var actionName = aName ?? p.GetType().FullName;
            if (IsRequestSaved == false)
                actionName += ":Response";

            var ll = (null != p?.Service?.Zeze)
                ? p.Service.Zeze.Config.ProcessReturnErrorLogLevel
                : Config.LogLevel.Trace;
            LogAction?.Invoke(ll, ex, result, $"Action={actionName} {p}");

#if ENABLE_STATISTICS
            ProcedureStatistics.Instance.GetOrAdd(actionName).GetOrAdd(result).IncrementAndGet();
#endif
        }

        public static void DefaultLogAction(Config.LogLevel level, Exception ex, long result, string message)
        {
#if HAS_NLOG || HAS_MYLOG
            // exception -> Error
            // 0 != result -> level from parameter
            // others -> Trace
            Config.LogLevel ll = ex != null ? Config.LogLevel.Error : (0 != result) ? level : Config.LogLevel.Trace;
            var module = "";
            if (result > 0)
                module = "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result);
#endif
#if HAS_NLOG
            logger.Log(NlogLogLevel(ll), ex, $"Return={result}{module} {message}");
#elif HAS_MYLOG
            logger.Log(ll, ex, $"Return={result}{module} {message}");
#endif
        }

        public static async Task<long> CallAsync(Func<Task> aa, string actionName)
        {
            return await CallAsync(async () =>
            {
                await aa();
                return 0;
            }, actionName);
        }

        public static async Task<long> CallAsync(Func<Task<long>> aa, string actionName)
        {
            try
            {
                return await aa();
            }
            catch (Exception ex)
            {
#if HAS_NLOG || HAS_MYLOG
                logger.Error(ex, actionName);
#endif
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                    throw;
#endif
                return ResultCode.Exception;
            }
        }

        public static async Task<long> CallAsync(Func<Protocol, Task<long>> pHandle, Protocol p,
            Action<Protocol, long> actionWhenError = null, string name = null)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                long result = await pHandle(p);
                if (result != 0 && IsRequestSaved)
                    actionWhenError?.Invoke(p, result);
                LogAndStatistics(null, result, p, IsRequestSaved, name);
                return result;
            }
            catch (Exception ex)
            {
                while (true)
                {
                    var inner = ex.InnerException;
                    if (inner == null)
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
                    throw;
#endif
                return errorCode;
            }
        }

        public static long Call(Func<long> func, Protocol p, Action<Protocol, long> actionWhenError = null)
        {
            bool IsRequestSaved = p.IsRequest; // 记住这个，以后可能会被改变。
            try
            {
                long result = func();
                if (result != 0 && IsRequestSaved)
                    actionWhenError?.Invoke(p, result);
                LogAndStatistics(null, result, p, IsRequestSaved);
                return result;
            }
            catch (Exception ex)
            {
                while (true)
                {
                    var inner = ex.InnerException;
                    if (inner == null)
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
                    throw;
#endif
                return errorCode;
            }
        }

#if !USE_CONFCS
        public static async Task<long> CallAsync(Procedure procedure, Net.Protocol from = null,
            Action<Net.Protocol, long> actionWhenError = null)
        {
            bool? isRequestSaved = from?.IsRequest;
            try
            {
                // 日志在Call里面记录。因为要支持嵌套。
                // 统计在Call里面实现。
                long result = await procedure.CallAsync();
                if (result != 0 && null != isRequestSaved && isRequestSaved.Value)
                    actionWhenError?.Invoke(from, result);
                return result;
            }
            catch (Exception ex)
            {
                // Procedure.Call处理了所有错误。除非内部错误或者单元测试异常，不会到这里。
                if (isRequestSaved != null && isRequestSaved.Value)
                    actionWhenError?.Invoke(from, ResultCode.Exception);
                LogAction?.Invoke(Config.LogLevel.Error, ex, ResultCode.Exception, procedure.ActionName);
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                    throw;
#endif
                return ResultCode.Exception;
            }
        }
#endif
    }
}
