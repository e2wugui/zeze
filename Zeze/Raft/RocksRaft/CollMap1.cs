using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollMap1<K, V> : CollMap<K, V>
	{
		public override V Get(K key)
		{
			if (false == Transaction.Current.LogTryGet(Parent.ObjectId + VariableId, out var log))
				return _Get(key);
			var maplog = (LogMap1<K, V>)log;
			return maplog.Get(key, this);
		}

		public override void Put(K key, V value)
		{
			var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, MapLogFactory);
			maplog.Put(key, value);
		}

		public override void Remove(K key)
		{
			var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, MapLogFactory);
			maplog.Remove(key);
		}

		public override void Apply(LogMap1<K, V> log)
		{
			var tmp = map;
			tmp = tmp.RemoveRange(log.Removed);
			tmp = tmp.AddRange(log.Putted);
			map = tmp;
		}

		private Log MapLogFactory(int varid)
		{
			return new LogMap1<K, V>() { VariableId = varid, };
		}
	}
}
