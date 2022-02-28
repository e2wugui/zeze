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
			Savepoint sp = Savepoints.Count > 0 ? Savepoints[Savepoints.Count - 1].BeginSavepoint() : new Savepoint();
			Savepoints.Add(sp);
		}

		public void Commit()
        {
			if (Savepoints.Count > 1)
			{
				// 嵌套事务，把日志合并到上一层。
				int lastIndex = Savepoints.Count - 1;
				Savepoint last = Savepoints[lastIndex];
				Savepoints.RemoveAt(lastIndex);
				Savepoints[Savepoints.Count - 1].CommitTo(last);
			}
			/*
            else
            {
                // 最外层存储过程提交在 Perform 中处理
            }
            */
		}

		public void Rollback()
        {
			int lastIndex = Savepoints.Count - 1;
			Savepoint last = Savepoints[lastIndex];
			Savepoints.RemoveAt(lastIndex);
			last.Rollback();
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
