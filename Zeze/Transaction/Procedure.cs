using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class Procedure
    {
        public const long Success = 0;
        public const long Excption = -1;
        public const long TooManyTry = -2;
        public const long NotImplement = -3;
        public const long Unknown = -4;
        public const long ErrorSavepoint = -5;
        public const long LogicError = -6;
        public const long RedoAndRelease = -7;
        public const long AbortException = -8;
        public const long ProviderNotExist = -9;
        public const long Timeout = -10;
        public const long CancelExcption = -11;
        public const long DuplicateRequest = -12;
        public const long ErrorRequestId = -13;
        // >0 用户自定义。

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Application Zeze { get; }

        public Func<long> Action { get; set; }

        public string ActionName { get; set; } // 用来统计或者日志

        // 用于继承方式实现 Procedure。
        public Procedure(Application app)
        {
            Zeze = app;
        }

        public object UserState { get; set; }

        public Procedure(Application app, Func<long> action, string actionName, object userState)
        {
            Zeze = app;
            Action = action;
            ActionName = actionName;
            UserState = userState;
            if (null == UserState) // 没指定，就从当前存储过程继承。嵌套时发生。
                UserState = Transaction.Current?.TopProcedure?.UserState;
        }

        /// <summary>
        /// 创建 Savepoint 并执行。
        /// 嵌套 Procedure 实现，
        /// </summary>
        /// <returns></returns>
        public long Call()
        {
            if (null == Transaction.Current)
            {
                try
                {
                    // 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
                    return Transaction.Create(Zeze.Locks).Perform(this);
                }
                finally
                {
                    Transaction.Destroy();
                }
            }

            Transaction currentT = Transaction.Current;
            currentT.Begin();
            currentT.ProcedureStack.Add(this);

            try
            {
                long result = Process();
                if (Success == result)
                {
                    currentT.Commit();
#if ENABLE_STATISTICS
                    ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(result).IncrementAndGet();
#endif
                    return Success;
                }
                currentT.Rollback();

                var module = "";
                if (result > 0)
                    module = "@" + IModule.GetModuleId(result)
                        + ":" + IModule.GetErrorCode(result);
                logger.Log(Zeze.Config.ProcessReturnErrorLogLevel,
                    "Procedure {0} Return{1}@{2} UserState={3}",
                    ToString(), result, module, UserState);
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(result).IncrementAndGet();
#endif
                return result;
            }
            catch (RedoException)
            {
                currentT.Rollback();
                throw; // 抛出这个异常，重做，提前检测数据发生了改变。
            }
            catch (AbortException)
            {
                currentT.Rollback();
                throw; // 抛出这个异常，中断事务，跳过所有嵌套过程直到最外面。
            }
            catch (RedoAndReleaseLockException)
            {
                currentT.Rollback();
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(RedoAndRelease).IncrementAndGet();
#endif
                throw; // 抛出这个异常，打断事务，跳过所有嵌套过程直到最外面。会重做。
            }
            catch (TaskCanceledException ce)
            {
                currentT.Rollback();
                logger.Error(ce, "Procedure {0} Exception UserState={1}", ToString(), UserState);
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(Excption).IncrementAndGet();
#endif
                return CancelExcption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
            }
            catch (Exception e)
            {
                currentT.Rollback();
                logger.Error(e, "Procedure {0} Exception UserState={1}", ToString(), UserState);
#if ENABLE_STATISTICS
                ProcedureStatistics.Instance.GetOrAdd(ActionName).GetOrAdd(Excption).IncrementAndGet();
#endif
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (e.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return Excption; // 回滚当前存储过程，不中断事务，外层存储过程判断结果自己决定是否继续。
            }
            finally
            {
                currentT.ProcedureStack.RemoveAt(currentT.ProcedureStack.Count - 1);
            }
        }

        protected virtual long Process()
        {
            if (null != Action)
                return Action();
            return NotImplement;
        }

        public override string ToString()
        {
            // GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
            return (null != Action) ? ActionName : this.GetType().FullName;
        }
    }
}
