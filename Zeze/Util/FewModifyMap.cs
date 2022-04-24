using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class FewModifyMap<TKey, TValue> : IEnumerable<KeyValuePair<TKey, TValue>>, IEnumerable, IDictionary<TKey, TValue>, IReadOnlyCollection<KeyValuePair<TKey, TValue>>, IReadOnlyDictionary<TKey, TValue>
        where TKey : notnull
    {
        private volatile Dictionary<TKey, TValue> read;
        private Dictionary<TKey, TValue> write = new();

        private Dictionary<TKey, TValue> PrepareRead()
        {
            var tmp = read;
            if (null != tmp)
                return tmp;

            lock (write)
            {
                if (null == read)
                {
                    read = tmp = new();
                    foreach (var e in write)
                    {
                        read.Add(e.Key, e.Value);
                    }
                }
                return tmp;
            }
        }

        public TValue this[TKey key] => PrepareRead()[key];

        public int Count => PrepareRead().Count;

        public IEnumerable<TKey> Keys => PrepareRead().Keys;

        public IEnumerable<TValue> Values => PrepareRead().Values;

        ICollection<TKey> IDictionary<TKey, TValue>.Keys => throw new NotImplementedException();

        ICollection<TValue> IDictionary<TKey, TValue>.Values => throw new NotImplementedException();

        public bool IsReadOnly => false;

        TValue IDictionary<TKey, TValue>.this[TKey key]
        {
            get => PrepareRead()[key];
            set 
            {
                lock (write)
                {
                    write[key] = value;
                    read = null;
                }
            }
        }

        public void Add(KeyValuePair<TKey, TValue> item)
        {
            lock (write)
            {
                write.Add(item.Key, item.Value);
                read = null;
            }
        }

        public void Clear()
        {
            lock (write)
            {
                write.Clear();
                read = null;
            }
        }

        public bool Contains(KeyValuePair<TKey, TValue> item)
        {
            return PrepareRead().Contains(item);
        }

        public bool ContainsKey(TKey key)
        {
            return PrepareRead().ContainsKey(key);
        }

        public IEnumerator<KeyValuePair<TKey, TValue>> GetEnumerator()
        {
            return PrepareRead().GetEnumerator();
        }

        public bool TryGetValue(TKey key, [MaybeNullWhen(false)] out TValue value)
        {
            return PrepareRead().TryGetValue(key, out value);
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return PrepareRead().GetEnumerator();
        }

        public void Add(TKey key, TValue value)
        {
            lock (write)
            {
                write.Add(key, value);
                read = null;
            }
        }

        public bool Remove(TKey key)
        {
            lock (write)
            {
                if (write.Remove(key))
                {
                    read = null;
                    return true;
                }
                return false;
            }
        }

        public void CopyTo(KeyValuePair<TKey, TValue>[] array, int arrayIndex)
        {
            var read = PrepareRead();
            var index = arrayIndex;
            foreach (var e in read)
            {
                array[index++] = e;
            }
        }

        public bool Remove(KeyValuePair<TKey, TValue> item)
        {
            lock (write)
            {
                if (write.TryGetValue(item.Key, out var value))
                {
                    if (item.Value.Equals(value))
                    {
                        write.Remove(item.Key);
                    }
                    read = null;
                    return true;
                }
                return false;
            }
        }
    }
}
