using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class CollMap<K, V> : Collection
	{
		internal ImmutableDictionary<K, V> map = ImmutableDictionary<K, V>.Empty;

		public abstract V Get(K key);
		public abstract void Put(K key, V value);
		public abstract void Remove(K key);
		public abstract void Apply(LogMap log);
		public abstract void Clear();

		internal V _Get(K key)
		{
			if (map.TryGetValue(key, out var tmp))
				return tmp;
			return default(V);
		}

        public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, map);
            return sb.ToString();
        }
    }
}
