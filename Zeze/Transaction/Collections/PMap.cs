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

                    return txn.GetLog(LogKey) is LogV log ? log.Value : map;
                }
                else
                {
                    return map;
                }
            }
        }

        [Obsolete("Don't use this, please use Keys2", true)]
        public ICollection<K> Keys { get { throw new NotImplementedException(); } }
        [Obsolete("Don't use this, please use Values2", true)]
        public ICollection<V> Values { get { throw new NotImplementedException(); } }

        public IEnumerable<K> Keys2 => Data.Keys;

        public IEnumerable<V> Values2 => Data.Values;

        public int Count => Data.Count;

        public override string ToString()
        {
            return $"PMap{Data}";
        }

        public abstract V this[K key] { get; set; }

        public bool IsReadOnly => false;

        public abstract void Add(K key, V value);
        public abstract void Add(KeyValuePair<K, V> item);
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
}
