using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public class Transaction
	{
		public static Transaction Current => null;

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
	}

}
