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
		public abstract string Name { get; }
		internal abstract void Apply(object key, LogBean log);
	}

	public abstract class Table<K, V> : Table where V : Bean, new()
	{
		private ConcurrentDictionary<K, V> table = new ConcurrentDictionary<K, V>(); // TODO change to lru

		internal override void Apply(object key, LogBean log)
		{
			log.Apply(table.GetOrAdd((K)key, (_) => new V()));
		}

		public V GetOrAdd(K key)
        {
			return table.GetOrAdd(key, (_) => new V());
        }


	}

}
