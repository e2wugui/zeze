using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class CollMap2<K, V> : CollMap<K, V>
		where V : Bean, new()
	{
		public static PropertyInfo PropertyMapKey { get; }

		static CollMap2()
		{
			PropertyMapKey = typeof(V).GetProperty($"_{typeof(K).Name}MapKey_");
		}

		public override void Put(K key, V value)
		{
			if (IsManaged)
            {
				value.InitRootInfo(RootInfo, this);
				PropertyMapKey?.SetValue(value, key);

				var maplog = (LogMap2<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Put(key, value);
			}
			else
            {
				map = map.SetItem(key, value);
            }
		}

		public override void Remove(K key)
		{
			if (IsManaged)
            {
				var maplog = (LogMap2<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Remove(key);
			}
			else
            {
				map = map.Remove(key);
            }
		}

		public override void Clear()
		{
			if (IsManaged)
			{
				var maplog = (LogMap2<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Clear();
			}
			else
			{
				map = map.Clear();
			}
		}

		public override void FollowerApply(Log _log)
		{
			var log = (LogMap2<K, V>)_log;
			var tmp = map;
			tmp = tmp.SetItems(log.Putted);
			tmp = tmp.RemoveRange(log.Removed);

			// apply changed
			foreach (var e in log.ChangedWithKey)
			{
				if (tmp.TryGetValue(e.Key, out var value))
				{
					value.FollowerApply(e.Value);
				}
				else
				{
					Rocks.logger.Error($"Not Exist! Key={e.Key} Value={e.Value}");
				}
			}
			map = tmp;
		}

		public override void LeaderApplyNoRecursive(Log _log)
		{
			var log = (LogMap2<K, V>)_log;
			map = log.Value;
		}

		public override LogBean CreateLogBean()
		{
			var log = new LogMap2<K, V>();
			log.Belong = Parent;
			log.Owner = this;
			log.VariableId = VariableId;
			log.Value = map;
			return log;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{
			foreach (var v in map.Values)
			{
				v.InitRootInfo(root, this);
			}
		}
	}
}
