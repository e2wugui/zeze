using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class CollMap1<K, V> : CollMap<K, V>
	{
		public override void Add(K key, V value)
		{
			if (IsManaged)
			{
				var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Add(key, value);
			}
			else
			{
				_map = _map.Add(key, value);
			}
		}

		public override void Put(K key, V value)
		{
			if (IsManaged)
            {
				var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Put(key, value);
			}
			else
            {
				_map = _map.SetItem(key, value);
            }
		}

		public override void Remove(K key)
		{
			if (IsManaged)
			{
				var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Remove(key);
			}
			else
			{
				_map = _map.Remove(key);
			}
		}

		public override void Clear()
        {
			if (IsManaged)
			{
				var maplog = (LogMap1<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Clear();
			}
			else
			{
				_map = _map.Clear();
			}
		}

        public override void FollowerApply(Log _log)
        {
			var log = (LogMap1<K, V>)_log;
			var tmp = _map;
			tmp = tmp.SetItems(log.Putted);
			tmp = tmp.RemoveRange(log.Removed);
			_map = tmp;
		}

		public override void LeaderApplyNoRecursive(Log _log)
		{
			var log = (LogMap1<K, V>)_log;
			_map = log.Value;
		}

		public override LogBean CreateLogBean()
		{
			var log = new LogMap1<K, V>();
			log.Belong = Parent;
			log.This = this;
			log.VariableId = VariableId;
			log.Value = _map;
			return log;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{ 
		}

		public override CollMap1<K, V> Copy()
		{
			var copy = new CollMap1<K, V>();
			copy._map = _map;
			return copy;
		}
	}
}
