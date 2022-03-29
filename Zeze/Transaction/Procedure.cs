using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class Procedure
    {
        public const long Success = 0;
        public const long Exception = -1;
        public const long TooManyTry = -2;
        public const long NotImplement = -3;
        public const long Unknown = -4;
        public const long ErrorSavepoint = -5;
        public const long LogicError = -6;
        public const long RedoAndRelease = -7;
        public const long AbortException = -8;
        public const long ProviderNotExist = -9;
        public const long Timeout = -10;
        public const long CancelException = -11;
        public const long DuplicateRequest = -12;
        public const long ErrorRequestId = -13;
        public const long ErrorSendFail = -14;
        public const long RaftRetry = -15;
        public const long RaftApplied = -16;
        public const long RaftExpired = -17;
        // >0 用户自定义。

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Application Zeze { get; }

        public Func<Task<long>> Action { get; set; }

        public string ActionName { get; set; } // 用来统计或者日志
        public TransactionLevel TransactionLevel { get;}

        // 用于继承方式实现 Procedure。
        public Procedure(Application app)
        {
            Zeze = app;
        }

        public object UserState { get; set; }

        public Zeze.Net.Protocol Rpc { get; set; }

        public Procedure(Application app, Func<Task<long>> action, string actionName, TransactionLevel level, object userState)
        {
            Zeze = app;
            Action = action;
            ActionName = actionName;
            TransactionLevel = level;
            UserState = userState;
            if (null == UserState) // 没指定，就从当前存储过程继承。嵌套时发生。
                UserState = Transaction.Current?.TopProcedure?.UserState;
        }

        public static volatile Action<Exception, long, Procedure, string> LogAction = DefaultLogAction;

        public static void DefaultLogAction(Exception ex, long result, Procedure p, string message)
        {
            NLog.LogLevel ll = (null != ex) ? NLog.LogLevel.Error
                : (0 != result) ? p.Zeze.Config.ProcessReturnErrorLogLevel
                : NLog.LogLevel.Trace;

            var module = "";
            if (result > 0)
                module = "@" + IModule.GetModuleId(result) + ":" + IModule.GetErrorCode(result);

            logger.Log(ll, ex, $"Procedure={p} Return={result}{module} {message} UserState={p.UserState}");
        }

        public void Execute(Net.Protocol from = null, Action<Net.Protocol, long> actionWhenError = null)
        {
            ExecutionContext.SuppressFlow();
            Task.Run(async () => await Util.Mission.CallAsync(this, from, actionWhenError));
            ExecutionContext.RestoreFlow();
        }

        /// <summary>
        /// 创建 Savepoint 并执行。
        /// 嵌套 Procedure 实现，
        /// </summary>
        /// <returns></returns>
        public async Task<long> CallAsync()
        {
            if (null == Transaction.Current)
            {
                try
                {
                    // 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
                    return await Transaction.Create(Zeze.Locks).Perform(this);
                }
                finally
                {
                    // Transaction.AsyncLocal 到达这里肯定是null，作为概念，执行一次Destroy。
                    Transaction.Destroy();
                }
            }

            Transaction currentT = Transaction.Current;
            currentT.Begin();
            currentT.ProcedureStack.Add(this);

            try
            {
                long result = await Process();
                currentT.VerifyRunning(); // 防止应用抓住了异常，通过return方式返回。

                if (Success == result)
                {
                    currentT.Commit();
#if ENABLE_STATISTICS
                    ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(result).IncrementAndGet();
#endif
                    return Success;
                }
                currentT.Rollback();
                LogAction?.Invoke(null, result, this, "");
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(result).IncrementAndGet();
#endif
                return result;
            }
            catch (GoBackZezeException gobackzeze)
            {
                // 单独抓住这个异常，是为了能原样抛出，并且使用不同的级别记录日志。
                // 对状态正确性没有影响。
                currentT.Rollback();
                logger.Debug(gobackzeze);
                throw gobackzeze;
            }
            catch (Exception e)
            {
                currentT.Rollback();
                LogAction?.Invoke(e, Exception, this, "");
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(Exception).IncrementAndGet();
#endif
                currentT.VerifyRunning();
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (e.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return e is TaskCanceledException ? CancelException : Exception; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
            }
            finally
            {
                currentT.ProcedureStack.RemoveAt(currentT.ProcedureStack.Count - 1);
            }
        }

        protected virtual async Task<long> Process()
        {
            if (null != Action)
                return await Action();
            return NotImplement;
        }

        public long CallSynchronously()
        {
            var atask = CallAsync();
            atask.Wait();
            return atask.Result;
        }

        public override string ToString()
        {
            // GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
            return (null != Action) ? ActionName : this.GetType().FullName;
        }
    }
}
