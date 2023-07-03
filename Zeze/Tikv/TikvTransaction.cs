using System;
using Zeze.Util;

namespace Zeze.Tikv
{
    public class TikvTransaction : IDisposable
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(TikvTransaction));

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
                // log rollback error only
                logger.Error(ex, "TiKv Transaction Rollback");
            }
        }

        public void Dispose()
        {
            try
            {
                Rollback();
            }
            catch(Exception ex)
            {
                logger.Error(ex);
            }
        }
    }
}
