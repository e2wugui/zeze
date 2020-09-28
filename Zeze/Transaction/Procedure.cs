using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public class Procedure
    {
        public const int Success = 0;
        public const int Excption = -1;
        public const int TooManyTry = -2;
        public const int NotImplement = -3;
        public const int Unknown = -4;
        public const int ErrorSavepoint = -5;
        public const int LogicError = -6;
        // >0 用户自定义。

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Checkpoint Checkpoint { get; }

        public Func<int> Action { get; set; }

        public string ActionName { get; } // 用来统计或者调试

        // 用于继承方式实现 Procedure。
        public Procedure(Checkpoint checkpoint)
        {
            Checkpoint = checkpoint;
        }

        public Procedure(Checkpoint checkpoint, Func<int> action, string actionName)
        {
            Checkpoint = checkpoint;
            Action = action;
            ActionName = actionName;
        }

        /// <summary>
        /// 创建 Savepoint 并执行。
        /// 嵌套 Procedure 实现，
        /// </summary>
        /// <returns></returns>
        public int Call()
        {
            if (null == Transaction.Current)
            {
                try
                {
                    // 有点奇怪，Perform 里面又会回调这个方法。这是为了把主要流程都写到 Transaction 中。
                    return Transaction.Create().Perform(this);
                }
                finally
                {
                    Transaction.Destroy();
                }
            }

            Transaction currentT = Transaction.Current;
            currentT.Begin();

            try
            {
                int result = Process();
                if (Success == result)
                {
                    currentT.Commit();
                    return Success;
                }
                currentT.Rollback();
                return result;
            }
            catch (RedoAndReleaseLockException redo)
            {
                currentT.Rollback();
                throw redo;
            }
            catch (Exception e)
            {
                currentT.Rollback();
                logger.Error(e, "Procedure.Process");
#if DEBUG
                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                if (e.GetType().Name == "AssertFailedException")
                {
                    throw;
                }
#endif
                return Excption;
            }
        }

        protected virtual int Process()
        {
            if (null != Action)
                return Action();
            return NotImplement;
        }

        public override string ToString()
        {
            // GetType().FullName 仅在用继承的方式实现 Procedure 才有意义。
            return $"{ActionName} {(null != Action ? Action.Method.Name : this.GetType().FullName)}";
        }
    }
}
