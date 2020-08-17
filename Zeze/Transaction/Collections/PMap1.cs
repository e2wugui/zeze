using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public sealed class PMap1<K, V> : PMap<K, V>
    {
        public PMap1(Func<ImmutableDictionary<K, V>, Log> logFactory) : base(logFactory)
        {
        }

        public override V this[K key]
        {
            get => Data[key];
            set
            {
                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                    var newv = oldv.SetItem(key, value);
                    if (newv != oldv)
                    {
                        txn.PutLog(this, NewLog(newv));
                    }
                }
                else
                {
                    map = map.SetItem(key, value);
                }

            }
        }

        public override void Add(K key, V value)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                var newv = oldv.Add(key, value);
                if (newv != oldv)
                {
                    txn.PutLog(this, NewLog(newv));
                }
            }
            else
            {
                map = map.Add(key, value);
            }
        }

        public override void Add(KeyValuePair<K, V> item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                var newv = oldv.Add(item.Key, item.Value);
                if (newv != oldv)
                {
                    txn.PutLog(this, NewLog(newv));
                }
            }
            else
            {
                map = map.Add(item.Key, item.Value);
            }
        }

        public override void Clear()
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                if (!oldv.IsEmpty)
                {
                    txn.PutLog(this, NewLog(ImmutableDictionary<K, V>.Empty));
                }
            }
            else
            {
                map = ImmutableDictionary<K, V>.Empty;
            }
        }

        public override bool Remove(K key)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                var newv = oldv.Remove(key);
                if (newv != oldv)
                {
                    txn.PutLog(this, NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var old = map;
                map = map.Remove(key);
                return old != map;
            }
        }

        public override bool Remove(KeyValuePair<K, V> item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : map;
                // TODO equals 处有box，能否优化掉？
                if (oldv.TryGetValue(item.Key, out var olde) && olde.Equals(item.Value))
                {
                    var newv = oldv.Remove(item.Key);
                    txn.PutLog(this, NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                // TODO equals处有box
                if (map.TryGetValue(item.Key, out var oldv) && oldv.Equals(item.Value))
                {
                    map = map.Remove(item.Key);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }

        protected override void InitChildrenTableKey(TableKey tableKey)
        {

        }
    }
}
