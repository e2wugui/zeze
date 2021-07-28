using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class TikvTransaction : IDisposable
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public TikvConnection Connection { get; }
        public long TransactionId { get; }
        public int FinishState { get; private set; }

        internal TikvTransaction(TikvConnection conn)
        {
            Connection = conn;
            TransactionId = Tikv.Driver.Begin(conn.ClientId);
        }

        public void Commit()
        {
            if (FinishState != 0)
                return;

            FinishState = 1;
            Tikv.Driver.Commit(TransactionId);
        }

        public void Rollback()
        {
            if (FinishState != 0)
                return;

            FinishState = 2;
            try
            {
                Tikv.Driver.Rollback(TransactionId);
            }
            catch (Exception ex)
            {
                // long rollback error only
                logger.Error(ex, "TiKv Transaction Rollback");
            }
        }

        public void Dispose()
        {
            Rollback();
        }
    }
}
