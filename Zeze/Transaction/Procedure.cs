using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public class Procedure
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Func<bool> Action{ get; set; }

        public Procedure()
        {

        }

        public Procedure(Func<bool> action)
        {
            Action = action;
        }

        /// <summary>
        /// 创建 Savepoint 并执行。
        /// 嵌套 Procedure 实现，
        /// </summary>
        /// <returns></returns>
        public bool Call()
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
                if (Process())
                {
                    currentT.Commit();
                    return true;
                }
                currentT.Rollback();
                return false;
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
            }
            return false;
        }

        protected virtual bool Process()
        {
            if (null != Action)
                return Action();
            return false;
        }

        public override string ToString()
        {
            return this.GetType().FullName;
        }
    }
}
