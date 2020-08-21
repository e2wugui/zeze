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
            return null != Action ? Action.ToString() : this.GetType().FullName;
        }
    }
}
