using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;

// MESI？
namespace Zeze.Transaction
{
    public class TableCache<K, V> where V : Bean, new()
    {
        public Table<K, V> Table { get; }
        public int Capacity { get; set; } // 不加锁了

        private readonly ConcurrentDictionary<K, Record<K, V>> map = new ConcurrentDictionary<K, Record<K, V>>();

        public TableCache(Application zeze, Table<K, V> table)
        {
            this.Table = table;
            Config.TableConf tableConf = zeze.Config.GetTableConf(table.Name);
            this.Capacity = tableConf.CacheCapaicty;
            if (Capacity < 0)
                throw new ArgumentException();

            int delay = tableConf.CacheCleanPeriod;
            // 为了使清除任务不会集中在某个时间点执行，简单处理一下：随机初始化延迟时间。（不算什么好方法）
            int initialDelay = Util.Random.Instance.Next(delay);
            Util.Scheduler.Instance.Schedule(CleanNow, initialDelay, delay);
        }

        public Record<K, V> Get(K key)
        {
            if (map.TryGetValue(key, out var r))
            {
                r.AccessTimeTicks = DateTime.Now.Ticks;
                return r;
            }
            return null;
        }

        public Record<K, V> GetOrAdd(K key, Record<K, V> r)
        {
            Record<K, V> exist = map.GetOrAdd(key, r);
            if (exist == r)
                exist.IsInCache = true;
            exist.AccessTimeTicks = DateTime.Now.Ticks;
            return exist;
        }

        // 考虑不再提供单个删除，由 Cleaner 集中清理。
        /*
        public void Remove(K key)
        {
            map.Remove(key, out var notused);
        }
        */

        public void CleanNow()
        {
            lock(this)
            {
                if (Capacity <= 0)
                    return; // 容量不限

                int size = map.Count;
                if (size <= Capacity)
                    return; // 容量足够

                List<AccessTimeRecord> sorted = new List<AccessTimeRecord>(size);
                foreach (var p in map)
                {
                    sorted.Add(new AccessTimeRecord(p));
                }
                sorted.Sort();

                // 每次多回收 255个
                int nclean = size - Capacity + 255;
                foreach (var r in sorted)
                {
                    if (nclean <= 0)
                        break;

                    if (r.accessTimeTicks != r.p.Value.AccessTimeTicks)
                        continue; // 排序后，记录时戳发生了更新，直接跳过。

                    if (TryRemoveRecord(r.p))
                    {
                        --nclean;
                    }
                    // 运行的不频繁：不管删除是否成功，都继续循环。
                }
            }
        }

        // under lock
        private bool Remove(KeyValuePair<K, Record<K, V>> p)
        {
            if (map.TryRemove(p.Key, out var _))
            {
                p.Value.IsInCache = false;
                return true;
            }
            return false;
        }

        private bool TryRemoveRecord(KeyValuePair<K, Record<K, V>> p)
        {
            TableKey tkey = new TableKey(this.Table.Id, p.Key);
            Lockey lockey = Locks.Instance.Get(tkey);
            if (false == lockey.TryEnterWriteLock(0))
            {
                return false;
            }
            try
            {
                var storage = Table.Storage;
                if (null == storage)
                    return Remove(p);

                if (storage.IsRecordChanged(p.Key)) // 在记录里面维持一个 Dirty 标志是可行的，但是由于 Cache.CleanNow 执行的不频繁，无所谓了。
                    return false;

                return Remove(p);
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            return false;
        }

        class AccessTimeRecord : System.IComparable<AccessTimeRecord>
        {
            internal long accessTimeTicks;
            internal KeyValuePair<K, Record<K, V>> p;

            internal AccessTimeRecord(KeyValuePair<K, Record<K, V>> p)
            {
                this.accessTimeTicks = p.Value.AccessTimeTicks; // 易变的，拷贝一份.
                this.p = p;
            }

            public int CompareTo([AllowNull] AccessTimeRecord other)
            {
                return accessTimeTicks.CompareTo(other.accessTimeTicks);
            }
        }
    }
}
