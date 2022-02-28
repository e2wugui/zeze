using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogMap1<K, V> : LogBean
	{
		public Dictionary<K, V> Putted;
		public ISet<K> Removed;
		public Dictionary<K, V> Updated; // Updated

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
			throw new NotImplementedException();
		}

		public override void Encode(ByteBuffer bb)
		{
			throw new NotImplementedException();
		}
	}
}
