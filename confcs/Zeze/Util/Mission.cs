using System;
using System.Threading.Tasks;
using Zeze.Net;

namespace Zeze.Util
{
    public class Mission
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(Mission));

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
                logger.Error(ex, actionName);
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
                logger.Error(ex, actionName);
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (ex.GetType().Name == "AssertFailedException")
                    throw;
#endif
            }
        }

        public static volatile Action<LogLevel, Exception, long, string> LogAction = DefaultLogAction;

        public static void LogAndStatistics(Exception ex, long result, Protocol p, bool IsRequestSaved,
            string aName = null)
        {
            var actionName = aName ?? p.GetType().FullName;
            if (IsRequestSaved == false)
                actionName += ":Response";

            var ll = p?.Service?.Zeze != null
                ? p.Service.Zeze.Config.ProcessReturnErrorLogLevel
                : LogLevel.Trace;
            LogAction?.Invoke(ll, ex, result, $"Action={actionName} {p}");
        }

        public static void DefaultLogAction(LogLevel level, Exception ex, long result, string message)
        {
            // exception -> Error
            // 0 != result -> level from parameter
            // others -> Trace
            var ll = ex != null ? LogLevel.Error : result != 0 ? level : LogLevel.Trace;
            var module = "";
            if (result > 0)
                module = "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result);
            logger.Log(ll, ex, $"Return={result}{module} {message}");
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
                logger.Error(ex, actionName);
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
    }
}
