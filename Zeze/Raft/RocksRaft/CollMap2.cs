using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class CollMap2<K, V> : CollMap<K, V>
		where V : Bean, new()
	{
		public override V Get(K key)
		{
			if (IsManaged)
            {
				if (false == Transaction.Current.TryGetLog(Parent.ObjectId + VariableId, out var log))
					return _Get(key);
				var maplog = (LogMap2<K, V>)log;
				return maplog.Get(key);
			}
			else
            {
				return _Get(key);
            }

		}

		public override void Put(K key, V value)
		{
			if (IsManaged)
            {
				value.InitRootInfo(RootInfo, this);
				value.VariableId = VariableId;
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

		public void FollowerApply(LogMap _log)
		{
			var log = (LogMap2<K, V>)_log;
			map = map.AddRange(log.Putted);
			map = map.RemoveRange(log.Removed);
			//foreach () // update Changed
			{
			}
		}

		public void LeaderApply(LogMap _log)
		{
			var log = (LogMap2<K, V>)_log;
			map = log.Value;
		}

		public Func<LogMap2<K, V>> LogFactory { get; set; }

		public override LogBean CreateLogBean()
		{
			var log = LogFactory();
			log.Parent = Parent;
			log.VariableId = VariableId;
			log.Value = map;
			return log;
		}

		public override void Decode(ByteBuffer bb)
		{
			Clear();
			for (int i = bb.ReadInt(); i >= 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				var value = SerializeHelper<V>.Decode(bb);
				Put(key, value);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			var tmp = map;
			bb.WriteInt(tmp.Count);
			foreach (var e in tmp)
			{
				SerializeHelper<K>.Encode(bb, e.Key);
				SerializeHelper<V>.Encode(bb, e.Value);
			}
		}

		protected override void InitChildrenRootInfo(Record.RootInfo tableKey)
		{
			foreach (var v in map.Values)
			{
				v.InitRootInfo(RootInfo, Parent);
			}
		}
	}
}
