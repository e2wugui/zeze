using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public abstract class Table
	{
		public abstract void Apply(object key, LogBean log);
	}

	public class Table<K, V> : Table where V : Bean, new()
	{
		ConcurrentDictionary<K, V> table;

		public override void Apply(object key, LogBean log)
		{
			log.Apply(table.GetOrAdd((K)key, (_) => new V()));
		}
	}

}
