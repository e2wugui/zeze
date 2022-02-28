using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public class Transaction
	{
		public static Transaction Current => threadLocal.Value;

		public bool LogTryGet(long logKey, out Log log)
		{
			log = null;
			return false;
		}

		public void LogPut(long logKey, Log log)
		{

		}

		public Log LogGetOrAdd(long logKey, Func<Log> logFactory)
		{
			return logFactory();
		}

		private readonly List<Savepoint> Savepoints = new List<Savepoint>();

		private static System.Threading.ThreadLocal<Transaction> threadLocal = new System.Threading.ThreadLocal<Transaction>();

		public static Transaction Create()
        {
			if (null == threadLocal.Value)
				threadLocal.Value = new Transaction();
			return threadLocal.Value;
        }

		public static void Destory()
        {
			threadLocal.Value = null;
        }

		public void Begin()
        {

        }

		public void Commit()
        {

        }

		public void Rollback()
        {

        }

		internal long Perform(Procedure proc)
        {
            try
            {
				var result = proc.Call();
				if (0 == result)
				{
					_final_commit();
					return 0;
				}
				_final_rollback();
				return result;
			}
			catch (Exception _)
            {
				return Zeze.Transaction.Procedure.Exception;
            }
		}

		private void _final_commit()
        {

        }

		private void _final_rollback()
        {

        }

	}

}
