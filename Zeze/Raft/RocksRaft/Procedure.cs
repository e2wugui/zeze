using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
    public class Procedure
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Func<long> Func { get; set; }
        public Rocks Rocks { get; set; }
        public Zeze.Net.Protocol UniqueRequest { get; set; }
        public Zeze.Net.Protocol AutoResponse { get; set; }
        public long ResultCode { get; set; }

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
            catch (Zeze.Util.ThrowAgainException)
            {
                currentT.Rollback();
                throw;
            }
            catch (RaftRetryException)
            {
                currentT.Rollback();
                throw;
            }
            catch (Exception ex)
            {
                currentT.Rollback();

                if (ex.GetType().Name == "AssertFailedException")
                {
                    throw;
                }

                logger.Error(ex);

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
