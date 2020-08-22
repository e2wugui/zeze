using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;

// MESI？
namespace Zeze.Transaction
{
    public class TableCache<K, V> where V : Bean
    {
        public int TableId { get; }
        public int Capacity { get; set; } // 不加锁了

        private readonly ConcurrentDictionary<K, Record<K, V>> map = new ConcurrentDictionary<K, Record<K, V>>();

        public TableCache(int tableId)
        {
            this.TableId = tableId;
        }

        public Record<K, V> Get(K key)
        {
            if (map.TryGetValue(key, out var r))
            {
                return r;
            }
            return null;
        }

        public Record<K, V> GetOrAdd(K key, Record<K, V> r)
        {
            return map.GetOrAdd(key, r);
        }

        // TODO 考虑不再提供单个删除，由 Cleaner 集中清理。
        /*
        public void Remove(K key)
        {
            map.Remove(key, out var notused);
        }
        */
    }
}
