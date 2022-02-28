using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollMap1<K, V> : CollMap<K, V>
		where K : Serializable, new()
		where V : Serializable, new()
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
			var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
			maplog.Put(key, value);
		}

		public override void Remove(K key)
		{
			var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
			maplog.Remove(key);
		}

		public override void Apply(LogMap _log)
		{
			var log = (LogMap1<K, V>)_log;
			var tmp = map;
			tmp = tmp.RemoveRange(log.Removed);
			tmp = tmp.AddRange(log.Putted);
			map = tmp;
		}

		public override LogBean CreateLogBean()
		{
			return new LogMap1<K, V>();
		}
	}
}
