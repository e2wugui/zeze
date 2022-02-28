using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogMap1<K, V> : LogMap
		where V : Serializable, new()
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
				var value = new V();
				value.Decode(bb);
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
				p.Value.Encode(bb);
			}

			bb.WriteInt(Removed.Count);
			foreach (var r in Removed)
			{
				SerializeHelper<K>.Encode(bb, r);
			}
		}

	}
}
