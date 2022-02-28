using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogMap1<K, V> : LogMap
	{
		public Dictionary<K, V> Putted { get; } = new Dictionary<K, V>();
		public ISet<K> Removed { get; } = new HashSet<K>();

		public V Get(K key, CollMap<K, V> map)
		{
			if (Putted.TryGetValue(key, out V value))
				return value;
			if (Removed.Contains(key))
				return default(V);
			return map._Get(key);
		}

		public void Put(K key, V value)
		{
			Putted[key] = value;
			Removed.Remove(key);
		}

		public void Remove(K key)
		{
			Putted.Remove(key);
			Removed.Add(key);
		}

		public override void Decode(ByteBuffer bb)
		{
			Putted.Clear();
			for (int i = bb.ReadInt(); i >= 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				var value = SerializeHelper<V>.Decode(bb);
				Putted.Add(key, value);
			}

			Removed.Clear();
			for (int i = bb.ReadInt(); i >= 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				Removed.Add(key);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Putted.Count);
			foreach (var p in Putted)
			{
				SerializeHelper<K>.Encode(bb, p.Key);
				SerializeHelper<V>.Encode(bb, p.Value);
			}

			bb.WriteInt(Removed.Count);
			foreach (var r in Removed)
			{
				SerializeHelper<K>.Encode(bb, r);
			}
		}

		internal override void MergeTo(Savepoint currentsp)
		{
			if (currentsp.Logs.TryGetValue(LogKey, out var log))
			{
				((LogMap1<K, V>)log).Merge(this);
			}
			else
            {
				currentsp.Logs[LogKey] = this;
			}
		}

		private void Merge(LogMap1<K, V> another)
        {
			// Put,Remove 需要确认有没有顺序问题
			// this: replace 1,3 remove 2,4 nest: replace 2 remove 1
			foreach (var e in another.Putted) Put(e.Key, e.Value); // replace 1,2,3 remove 4
			foreach (var e in another.Removed) Remove(e); // replace 2,3 remove 1,4
		}

        internal override Log Duplicate()
        {
			var dup = new LogMap1<K, V>();
			dup.Bean = Bean;
			dup.VariableId = VariableId;

			foreach (var e in Putted)
			{
				dup.Putted.Add(e.Key, e.Value);
			}
			foreach (var e in Removed)
			{
				dup.Removed.Add(e);
			}
			return dup;
		}
	}
}
