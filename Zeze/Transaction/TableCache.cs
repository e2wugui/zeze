﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using Zeze.Services;

// MESI？
namespace Zeze.Transaction
{
    public sealed class TableCache<K, V> where V : Bean, new()
    {
        public Table<K, V> Table { get; }
        public int Capacity { get; set; } // 不加锁了

        internal readonly ConcurrentDictionary<K, Record<K, V>> map = new ConcurrentDictionary<K, Record<K, V>>();

        public TableCache(Application app, Table<K, V> table)
        {
            this.Table = table;
            Config.TableConf tableConf = app.Config.GetTableConf(table.Name);
            this.Capacity = tableConf.CacheCapaicty;
            if (Capacity < 0)
                throw new ArgumentException();

            int delay = tableConf.CacheCleanPeriod;
            // 为了使清除任务不会集中在某个时间点执行，简单处理一下：随机初始化延迟时间。（不算什么好方法）
            int initialDelay = Util.Random.Instance.Next(delay);
            Util.Scheduler.Instance.Schedule(CleanNow, initialDelay, delay);
        }

        public Record<K, V> GetOrAdd(K key, Func<K, Record<K, V>> valueFactory)
        {
            Record<K, V> exist = map.GetOrAdd(key, valueFactory);
            exist.AccessTimeTicks.GetAndSet(DateTime.Now.Ticks);
            return exist;
        }

        internal Record<K, V> Get(K key)
        {
            if (map.TryGetValue(key, out var r))
                return r;
            return null;

        }

        // 考虑不再提供单个删除，由 Cleaner 集中清理。
        // under lockey.writelock
        /*
        internal void Remove(K key)
        {
            map.Remove(key, out var _);
        }
        */


        public void CleanNow(Zeze.Util.SchedulerTask ThisTask)
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

                    if (r.p.Value.AccessTimeTicks.Get() != r.accessTimeTicks)
                        continue; // 排序后，记录时戳发生了更新，直接跳过。

                    if (TryRemoveRecord(r.p))
                    {
                        --nclean;
                    }
                    // 运行的不频繁：不管删除是否成功，都继续循环。
                }
            }
        }

        // under lockey.writelock
        private bool Remove(KeyValuePair<K, Record<K, V>> p)
        {
            if (map.TryRemove(p.Key, out var _))
            {
                p.Value.State = GlobalCacheManager.StateRemoved;
                return true;
            }
            return false;
        }

        // under lockey.writelock
        /*
        internal bool RemoeIfNotDirty(K key)
        {
            var storage = Table.Storage;
            if (null == storage)
                return false; // 内存表不该发生Reduce.

            if (storage.IsRecordChanged(key)) // 在记录里面维持一个 Dirty 标志是可行的，但是由于 Cache.CleanNow 执行的不频繁，无所谓了。
                return false;

            return map.TryRemove(key, out var _);
        }
        */

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
                {
                    /* 不支持内存表cache同步。
                    if (p.Value.Acquire(GlobalCacheManager.StateInvalid) != GlobalCacheManager.StateInvalid)
                        return false;
                    */
                    return Remove(p);
                }

                if (storage.IsRecordChanged(p.Key)) // 在记录里面维持一个 Dirty 标志是可行的，但是由于 Cache.CleanNow 执行的不频繁，无所谓了。
                    return false;

                if (p.Value.State != GlobalCacheManager.StateInvalid)
                {
                    if (p.Value.Acquire(GlobalCacheManager.StateInvalid) != GlobalCacheManager.StateInvalid)
                        return false;
                }
                return Remove(p);
            }
            finally
            {
                lockey.ExitWriteLock();
            }
        }

        class AccessTimeRecord : System.IComparable<AccessTimeRecord>
        {
            internal long accessTimeTicks;
            internal KeyValuePair<K, Record<K, V>> p;

            internal AccessTimeRecord(KeyValuePair<K, Record<K, V>> p)
            {
                this.accessTimeTicks = p.Value.AccessTimeTicks.Get(); // 易变的，拷贝一份.
                this.p = p;
            }

            public int CompareTo(AccessTimeRecord other)
            {
                return accessTimeTicks.CompareTo(other.accessTimeTicks);
            }
        }
    }
}
