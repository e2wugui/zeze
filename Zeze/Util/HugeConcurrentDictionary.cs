using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    // 使用哈希到多个ConcurrentDictionary的方式支持巨大内存。
    // 先不实现 IDictionary，要了再来添加。
    public class HugeConcurrentDictionary<K, V> : IEnumerable<K> // 只支持遍历Keys
    {
        private ConcurrentDictionary<K, V>[] Buckets { get; }

        public HugeConcurrentDictionary(int buckets, int concurrencyLevel, long capacity)
        {
            Buckets = new ConcurrentDictionary<K, V>[buckets];
            long ic = capacity / Buckets.Length;
            if (ic > int.MaxValue)
                throw new Exception("capacity / buckets > int.MaxValue. Please Increace buckets.");
            for (int i = 0; i < Buckets.Length; ++i)
            {
                Buckets[i] = new ConcurrentDictionary<K, V>(concurrencyLevel, (int)ic);
            }
        }

        public V GetOrAdd(K key, Func<K, V> factory)
        {
            int hash = key.GetHashCode();
            int i = hash % Buckets.Length;
            return Buckets[i].GetOrAdd(key, factory);
        }

        public bool TryRemove(K key, out V r)
        {
            int hash = key.GetHashCode();
            int i = hash % Buckets.Length;
            return Buckets[i].TryRemove(key, out r);
        }

        public V this[K key]
        {
            get
            {
                int hash = key.GetHashCode();
                int i = hash % Buckets.Length;
                return Buckets[i][key];
            }
            set
            {
                int hash = key.GetHashCode();
                int i = hash % Buckets.Length;
                Buckets[i][key] = value;
            }
        }

        public long Count
        {
            get
            {
                long count = 0;
                foreach (var dict in Buckets)
                {
                    count += dict.Count;
                }
                return count;
            }
        }

        public IEnumerator<K> GetEnumerator()
        {
            return new KeysEnumerator(this);
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return new KeysEnumerator(this);
        }

        public HugeConcurrentDictionary<K, V> Keys => this;

        private class KeysEnumerator : IEnumerator<K>
        {
            private IEnumerator<K>[] Keys { get; }
            private int Index { get; set; }

            private K _Current;
            public K Current => _Current;

            object IEnumerator.Current => _Current;

            public KeysEnumerator(HugeConcurrentDictionary<K, V> huge)
            {
                Keys = new IEnumerator<K>[huge.Buckets.Length];
                for (int i = 0; i < Keys.Length; ++i)
                {
                    Keys[i] = huge.Buckets[i].Keys.GetEnumerator();
                }
            }

            public bool MoveNext()
            {
                while (Index < Keys.Length)
                {
                    var e = Keys[Index];
                    if (e.MoveNext())
                    {
                        _Current = e.Current;
                        return true;
                    }
                    ++Index;
                }
                return false;
            }

            public void Reset()
            {
                foreach (var key in Keys)
                {
                    key.Reset();
                }
                Index = 0;
            }

            public void Dispose()
            {
                foreach (var key in Keys)
                {
                    key.Dispose();
                }
            }
        }
    }
}
