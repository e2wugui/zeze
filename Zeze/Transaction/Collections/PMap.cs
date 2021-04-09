using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public abstract class PMap<K, V> : PCollection, IDictionary<K, V>
    {
        private readonly Func<ImmutableDictionary<K, V>, Log> _logFactory;
        protected ImmutableDictionary<K, V> map;

        public PMap(long logKey, Func<ImmutableDictionary<K, V>, Log> logFactory) : base(logKey)
        {
            this._logFactory = logFactory;
            map = ImmutableDictionary<K, V>.Empty;
        }

        public Log NewLog(ImmutableDictionary<K, V> value)
        {
            return _logFactory(value);
        }

        public abstract class LogV : Log
        {
            internal ImmutableDictionary<K, V> Value;
            protected LogV(Bean bean,ImmutableDictionary<K, V> value) : base(bean)
            {
                Value = value;
            }

            protected void Commit(PMap<K, V> variable)
            {
                variable.map = Value;
            }
        }

        protected ImmutableDictionary<K, V> Data
        {
            get
            {
                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    if (txn == null)
                    {
                        return map;
                    }
                    txn.VerifyRecordAccessed(this, true);
                    return txn.GetLog(LogKey) is LogV log ? log.Value : map;
                }
                else
                {
                    return map;
                }
            }
        }

        [Obsolete("Don't use this, please use Keys2", true)]
        ICollection<K> IDictionary<K, V>.Keys => throw new NotImplementedException();
        [Obsolete("Don't use this, please use Values2", true)]
        ICollection<V> IDictionary<K, V>.Values => throw new NotImplementedException();

        public IEnumerable<K> Keys => Data.Keys;

        public IEnumerable<V> Values => Data.Values;

        public int Count => Data.Count;

        public override string ToString()
        {
            return $"PMap{Data}";
        }
        public bool IsReadOnly => false;

        public abstract V this[K key] { get; set; }
        public abstract void Add(K key, V value);
        public abstract void Add(KeyValuePair<K, V> item);
        public abstract void AddRange(IEnumerable<KeyValuePair<K, V>> pairs);
        public abstract void SetItem(K key, V value);
        public abstract void SetItems(IEnumerable<KeyValuePair<K, V>> items);
        public abstract void Clear();
        public abstract bool Remove(K key);
        public abstract bool Remove(KeyValuePair<K, V> item);

        public void CopyTo(KeyValuePair<K, V>[] array, int arrayIndex)
        {
            int index = arrayIndex;
            foreach (var e in Data)
            {
                array[index++] = e;
            }
        }

        public bool Contains(KeyValuePair<K, V> item)
        {
            return Data.Contains(item);
        }

        public bool ContainsKey(K key)
        {
            return Data.ContainsKey(key);
        }

        public bool TryGetValue(K key, out V value)
        {
            return Data.TryGetValue(key, out value);

        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        IEnumerator<KeyValuePair<K, V>> IEnumerable<KeyValuePair<K, V>>.GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        public ImmutableDictionary<K, V>.Enumerator GetEnumerator()
        {
            return Data.GetEnumerator();
        }
    }

    public class PMapReadOnly<K, V, P> : IReadOnlyDictionary<K, V> where P : V
    {
        private readonly PMap<K, P> _origin;

        public PMapReadOnly(PMap<K, P> origin)
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
