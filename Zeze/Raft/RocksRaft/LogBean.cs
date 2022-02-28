using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class LogBean : Log
	{
		public Dictionary<int, Log> VariableLogs { get; } = new Dictionary<int, Log>();

		public override void Decode(ByteBuffer bb)
		{
			throw new NotImplementedException();
		}

		public override void Encode(ByteBuffer bb)
		{
			throw new NotImplementedException();
		}

		public TLog Get<TLog>(int varid) where TLog : Log
		{
			if (VariableLogs.TryGetValue(varid, out var tmp))
				return (TLog)tmp;
			return default(TLog);
		}

		public void Put(int varid, Log log)
		{
			VariableLogs[varid] = log;
		}

		public TLog GetOrAdd<TLog>(int varid) where TLog : Log, new()
		{
			if (VariableLogs.TryGetValue(varid, out var tmp))
				return (TLog)tmp;
			tmp = new TLog();
			VariableLogs.Add(varid, tmp);
			return (TLog)tmp;
		}

		public override void Apply(Bean holder)
		{
			foreach (var vlog in VariableLogs.Values)
			{
				vlog.Apply(holder);
			}
		}
	}
}
