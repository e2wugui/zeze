using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public abstract class CollMap<K, V> : Collection, IDictionary<K, V>, IEnumerable<KeyValuePair<K, V>>, IEnumerable
	{
		internal ImmutableDictionary<K, V> _map = ImmutableDictionary<K, V>.Empty;

		public V Get(K key)
        {
			if (Map.TryGetValue(key, out V v))
				return v;
			return default;
		}

		public abstract void Add(K key, V value);
		public abstract void Add(KeyValuePair<K, V> item);
		public abstract void AddRange(IEnumerable<KeyValuePair<K, V>> pairs);
		public abstract void SetItem(K key, V value);
		public abstract void SetItems(IEnumerable<KeyValuePair<K, V>> items);
		public abstract void Clear();
		public abstract bool Remove(K key);
		public abstract bool Remove(KeyValuePair<K, V> item);

		protected ImmutableDictionary<K, V> Map
        {
			get
            {
				if (IsManaged)
				{
					var txn = Transaction.Current;
					if (txn == null)
						return _map;
					txn.VerifyRecordAccessed(this, true);
					if (false == txn.TryGetLog(Parent.ObjectId + VariableId, out var log))
						return _map;
					var maplog = (LogMap<K, V>)log;
					return maplog.Value;
				}
				else
				{
					return _map;
				}
			}
		}

		public int Count => Map.Count;

		[Obsolete("Don't use this, please use Keys", true)]
		ICollection<K> IDictionary<K, V>.Keys => throw new NotImplementedException();
		[Obsolete("Don't use this, please use Values", true)]
		ICollection<V> IDictionary<K, V>.Values => throw new NotImplementedException();

		public IEnumerable<K> Keys => Map.Keys;

		public IEnumerable<V> Values => Map.Values;

		public bool IsReadOnly => false;

        public abstract V this[K key] { get; set; }

        IEnumerator IEnumerable.GetEnumerator()
		{
			return Map.GetEnumerator();
		}

		IEnumerator<KeyValuePair<K, V>> IEnumerable<KeyValuePair<K, V>>.GetEnumerator()
		{
			return Map.GetEnumerator();
		}

		public ImmutableDictionary<K, V>.Enumerator GetEnumerator()
		{
			return Map.GetEnumerator();
		}

		public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Map);
            return sb.ToString();
        }

		public override void Decode(ByteBuffer bb)
		{
			Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				var value = SerializeHelper<V>.Decode(bb);
				SetItem(key, value);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			var tmp = Map;
			bb.WriteUInt(tmp.Count);
			foreach (var e in tmp)
			{
				SerializeHelper<K>.Encode(bb, e.Key);
				SerializeHelper<V>.Encode(bb, e.Value);
			}
		}

        public bool ContainsKey(K key)
        {
            return Map.ContainsKey(key);
        }

        public bool TryGetValue(K key, [MaybeNullWhen(false)] out V value)
        {
            return Map.TryGetValue(key, out value);
        }

        public bool Contains(KeyValuePair<K, V> item)
        {
			return Map.Contains(item);
        }

        public void CopyTo(KeyValuePair<K, V>[] array, int arrayIndex)
        {
			int index = arrayIndex;
			foreach (var e in Map)
			{
				array[index++] = e;
			}
		}
    }

	public class CollMapReadOnly<K, V, P> : IReadOnlyDictionary<K, V> where P : V
	{
		private readonly CollMap<K, P> _origin;

		public CollMapReadOnly(CollMap<K, P> origin)
		{
			_origin = origin;
		}

		public V this[K key] => _origin[key];

		public IEnumerable<K> Keys => _origin.Keys;

		public IEnumerable<V> Values => (IEnumerable<V>)_origin.Values;

		public int Count => _origin.Count;

		public bool ContainsKey(K key) => _origin.ContainsKey(key);

		public IEnumerator<KeyValuePair<K, V>> GetEnumerator()
		{
			foreach (var e in _origin)
			{
				yield return new KeyValuePair<K, V>(e.Key, e.Value);
			}
		}

		IEnumerator IEnumerable.GetEnumerator() => ((IEnumerable)_origin).GetEnumerator();

		public bool TryGetValue(K key, out V value)
		{
			if (_origin.TryGetValue(key, out var cur))
			{
				value = cur;
				return true;
			}
			value = default;
			return false;
		}
	}
}
