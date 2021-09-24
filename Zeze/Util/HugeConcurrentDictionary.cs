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
    public class HugeConcurrentDictionary<K, V> : IEnumerable<KeyValuePair<K, V>>
    {
        private ConcurrentDictionary<K, V>[] Buckets { get; }

        public int BucketCount { get; }
        public int ConcurrencyLevel { get; }
        public long InitialCapacity { get; }

        public HugeConcurrentDictionary(int buckets, int concurrencyLevel, long capacity)
        {
            BucketCount = buckets;
            ConcurrencyLevel = concurrencyLevel;
            InitialCapacity = capacity;

            Buckets = new ConcurrentDictionary<K, V>[buckets];
            long bucketsCapacity = capacity / Buckets.Length;
            if (bucketsCapacity > int.MaxValue)
                throw new Exception("capacity / buckets > int.MaxValue. Please Increace buckets.");
            for (int i = 0; i < Buckets.Length; ++i)
            {
                Buckets[i] = new ConcurrentDictionary<K, V>(concurrencyLevel, (int)bucketsCapacity);
            }
        }

        public void Clear()
        {
            foreach (var b in Buckets)
            {
                b.Clear();
            }
        }

        public bool TryGetValue(K key, out V value)
        {
            uint hash = (uint)key.GetHashCode();
            uint i = hash % (uint)Buckets.Length;
            return Buckets[i].TryGetValue(key, out value);
        }

        public bool TryAdd(K key, V value)
        {
            uint hash = (uint)key.GetHashCode();
            uint i = hash % (uint)Buckets.Length;
            return Buckets[i].TryAdd(key, value);
        }

        public V GetOrAdd(K key, Func<K, V> factory)
        {
            uint hash = (uint)key.GetHashCode();
            uint i = hash % (uint)Buckets.Length;
            return Buckets[i].GetOrAdd(key, factory);
        }

        public bool TryRemove(KeyValuePair<K, V> pair)
        {
            uint hash = (uint)pair.Key.GetHashCode();
            uint i = hash % (uint)Buckets.Length;
            return Buckets[i].TryRemove(pair);
        }

        public bool TryRemove(K key, out V r)
        {
            uint hash = (uint)key.GetHashCode();
            uint i = hash % (uint)Buckets.Length;
            return Buckets[i].TryRemove(key, out r);
        }

        public V this[K key]
        {
            get
            {
                uint hash = (uint)key.GetHashCode();
                uint i = hash % (uint)Buckets.Length;
                return Buckets[i][key];
            }
            set
            {
                uint hash = (uint)key.GetHashCode();
                uint i = hash % (uint)Buckets.Length;
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

        public IEnumerator<KeyValuePair<K, V>> GetEnumerator()
        {
            return new Enumerator(this);
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return new Enumerator(this);
        }

        private class Enumerator : IEnumerator<KeyValuePair<K, V>>
        {
            private IEnumerator<KeyValuePair<K, V>>[] Entrys { get; }
            private int Index { get; set; }

            private KeyValuePair<K, V> _Current;
            public KeyValuePair<K, V> Current => _Current;

            object IEnumerator.Current => _Current;

            public Enumerator(HugeConcurrentDictionary<K, V> huge)
            {
                Entrys = new IEnumerator<KeyValuePair<K, V>>[huge.Buckets.Length];
                for (int i = 0; i < Entrys.Length; ++i)
                {
                    Entrys[i] = huge.Buckets[i].GetEnumerator();
                }
            }

            public bool MoveNext()
            {
                while (Index < Entrys.Length)
                {
                    var e = Entrys[Index];
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
                foreach (var e in Entrys)
                {
                    e.Reset();
                }
                Index = 0;
            }

            public void Dispose()
            {
                foreach (var e in Entrys)
                {
                    e.Dispose();
                }
            }
        }
    }
}
