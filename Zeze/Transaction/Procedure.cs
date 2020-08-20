using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public class Procedure
    {
        public const int ResultSuccess = 0;
        public const int ResultException = -1;
        public const int ResultTooManyTry = -2;
        public const int ResultNotImplement = -3;
        public const int ResultUnknown = -4;
        // >0 用户自定义。

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Func<int> Action{ get; set; }

        public Procedure()
        {

        }

        public Procedure(Func<int> action)
        {
            Action = action;
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
                if (ResultSuccess == result)
                {
                    currentT.Commit();
                    return ResultSuccess;
                }
                currentT.Rollback();
                return result;
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
                return ResultException;
            }
        }

        protected virtual int Process()
        {
            if (null != Action)
                return Action();
            return ResultNotImplement;
        }

        public override string ToString()
        {
            return this.GetType().FullName;
        }
    }
}
