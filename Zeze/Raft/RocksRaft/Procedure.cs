using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
    public class Procedure
    {
        public Func<long> Func { get; set; }
        public Rocks Rocks { get; set; }

        public Procedure()
        {

        }

        public Procedure(Rocks rocks, Func<long> func)
        {
            Rocks = rocks;
            Func = func;
        }

        protected virtual long Process()
        {
            if (null != Func)
                return Func();
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public long Call()
        {
            if (null == Transaction.Current)
            {
                try
                {
                    return Transaction.Create().Perform(this);
                }
                finally
                {
                    Transaction.Destory();
                }
            }
            var currentT = Transaction.Current;
            currentT.Begin();
            try
            {
                var result = Process();
                if (0 == result)
                {
                    currentT.Commit();
                    return 0;
                }
                currentT.Rollback();
                return result;
            }
            catch (Exception ex)
            {
                currentT.Rollback();

                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }

                return ex is TaskCanceledException
                    ? Zeze.Transaction.Procedure.CancelException
                    : Zeze.Transaction.Procedure.Exception;
            }
            finally
            {

            }
        }
        }
}
