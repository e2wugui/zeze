using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class TikvTransaction
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public TikvConnection Connection { get; }
        public int TransactionId { get; }
        public bool CommitDone { get; private set; } = false;

        internal TikvTransaction(TikvConnection conn)
        {
            Connection = conn;
            TransactionId = Tikv.Begin(conn.ClientId);
        }

        public void Commit()
        {
            if (CommitDone)
                return;

            Tikv.Commit(TransactionId);
            CommitDone = true;
        }

        public void Rollback()
        {
            try
            {
                Tikv.Rollback(TransactionId);
            }
            catch (Exception ex)
            {
                // skip rollback error
                logger.Error(ex, "TiKv Transaction Rollback");
            }
        }
    }
}
