using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using Zeze.Services;

// MESI？
namespace Zeze.Transaction
{
    /// <summary>
    /// ConcurrentLruLike
	/// 普通Lru一般把最新访问的放在列表一端，这直接导致并发上不去。
	/// 基本思路是按块（用ConcurrentDictionary）保存最近访问。
    /// 定时添加新块。
	/// 访问需要访问1 ~3次ConcurrentDictionary。
    /// 
    /// 通用类的写法需要在V外面包装一层。这里直接使用Record来达到这个目的。
    /// 这样，这个类就不通用了。通用类需要包装，多创建一个对象，还需要包装接口。
    /// 
    /// </summary>
    /// <typeparam name="K"></typeparam>
    /// <typeparam name="V"></typeparam>
    public class TableCache<K, V> where V : Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        internal Util.HugeConcurrentDictionary<K, Record<K, V>> DataMap { get; }

        private ConcurrentQueue<Util.HugeConcurrentDictionary<K, Record<K, V>>> LruQueue { get; }
            = new ConcurrentQueue<Util.HugeConcurrentDictionary<K, Record<K, V>>>();

        private Util.HugeConcurrentDictionary<K, Record<K, V>> LruHot { get; set; }

        public Table<K, V> Table { get; }

        public TableCache(Application app, Table<K, V> table)
        {
            this.Table = table;
            DataMap = new Util.HugeConcurrentDictionary<K, Record<K, V>>(
                GetCacheBuckets(), GetCacheConcurrencyLevel(), GetCacheInitialCapaicty());
            NewLruHot();
            Util.Scheduler.Instance.Schedule((task) =>
            {
                // 访问很少的时候不创建新的热点。这个选项没什么意思。
                if (LruHot.Count > table.TableConf.CacheNewAccessHotThreshold)
                {
                    NewLruHot();
                }
            }, table.TableConf.CacheNewLruHotPeriod, table.TableConf.CacheNewLruHotPeriod);
            Util.Scheduler.Instance.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
        }

        private int GetCacheBuckets()
        {
            return Table.TableConf.CacheBuckets < 16 ? 16 : Table.TableConf.CacheBuckets;
        }

        private int GetCacheConcurrencyLevel()
        {
            // 这样写，当配置修改，可以使用的时候马上生效。
            return Table.TableConf.CacheConcurrencyLevel > Environment.ProcessorCount
                ? Table.TableConf.CacheConcurrencyLevel : Environment.ProcessorCount;
        }

        private long GetCacheInitialCapaicty()
        {
            // 31 from c# document
            // 这样写，当配置修改，可以使用的时候马上生效。
            return Table.TableConf.CacheInitialCapaicty < 31
                ? 31 : Table.TableConf.CacheInitialCapaicty;
        }

        private long GetLruInitialCapaicty()
        {
            long c = (long)(GetCacheInitialCapaicty() * 0.2);
            return c < 31 ? 31 : c;
        }

        private void NewLruHot()
        {
            LruHot = new Util.HugeConcurrentDictionary<K, Record<K, V>>(
                GetCacheBuckets(), GetCacheConcurrencyLevel(), GetLruInitialCapaicty());
            LruQueue.Enqueue(LruHot);
        }

        public Record<K, V> GetOrAdd(K key, Func<K, Record<K, V>> valueFactory)
        {
            bool isNew = false;
            Record<K, V> result = DataMap.GetOrAdd(key,
                (k) =>
                {
                    var r = valueFactory(k);
                    LruHot[k] = r; // replace: add or update see this.Remove
                    r.LruNode = LruHot;
                    isNew = true;
                    return r;
                });

            if (false == isNew && result.LruNode != LruHot)
            {
                result.LruNode.TryRemove(KeyValuePair.Create(key, result));
                if (LruHot.TryAdd(key, result))
                {
                    result.LruNode = LruHot;
                }
                // else maybe fail in concurrent access.
                // 并发访问导致重复的TryAdd，这里先这样写吧。可能会快点。
                // LruHot[key] = result;
                // result.LruNode = LruHot;
            }
            return result;
        }

        /// <summary>
        /// 内部特殊使用，不调整 Lru。
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        internal Record<K, V> Get(K key)
        {
            if (DataMap.TryGetValue(key, out var r))
                return r;
            return null;
        }

        // 不再提供删除，由 Cleaner 集中清理。
        // under lockey.writelock
        /*
        internal void Remove(K key)
        {
            map.Remove(key, out var _);
        }
        */

        public void CleanNow(Zeze.Util.SchedulerTask ThisTask)
        {
            // 这个任务的执行时间可能很长，
            // 不直接使用 Scheduler 的定时任务，
            // 每次执行完重新调度。

            if (Table.TableConf.CacheCapacity <= 0)
            {
                Util.Scheduler.Instance.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
                return; // 容量不限
            }

            while (DataMap.Count > Table.TableConf.CacheCapacity) // 超出容量，循环尝试
            {
                if (false == LruQueue.TryPeek(out var node))
                    break;

                if (node == LruHot) // 热点。不回收。
                    break;

                foreach (var e in node)
                {
                    if (false == TryRemoveRecord(e))
                    {
                        // 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
                        Table.Zeze.CheckpointRun();
                    }
                }
                if (node.Count == 0)
                {
                    LruQueue.TryDequeue(out var _);
                }
                else
                {
                    logger.Warn($"remain record when clean oldest lrunode.");
                }

                if (Table.TableConf.CacheCleanPeriodWhenExceedCapacity > 0)
                    System.Threading.Thread.Sleep(Table.TableConf.CacheCleanPeriodWhenExceedCapacity);
            }
            Util.Scheduler.Instance.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
        }

        // under lockey.writelock
        private bool Remove(KeyValuePair<K, Record<K, V>> p)
        {
            if (DataMap.TryRemove(p.Key, out var e))
            {
                // 这里有个时间窗口：先删除DataMap再去掉Lru引用，
                // 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
                // see GetOrAdd
                p.Value.State = GlobalCacheManager.StateRemoved;
                // 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
                e.LruNode.TryRemove(p);
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
                var storage = Table.TStorage;
                if (null == storage)
                {
                    /* 不支持内存表cache同步。
                    if (p.Value.Acquire(GlobalCacheManager.StateInvalid) != GlobalCacheManager.StateInvalid)
                        return false;
                    */
                    return Remove(p);
                }

                // 这个变量的修改操作在不同 CheckpointMode 下并发模式不同。
                // case CheckpointMode.Immediately
                // 永远不会为false。记录Commit的时候就Flush到数据库。
                // case CheckpointMode.Period
                // 修改的时候需要记录锁（lockey）。
                // 这里只是读取，就不加锁了。
                // case CheckpointMode.Table 修改的时候需要RelativeRecordSet锁。
                // （修改为true的时也在记录锁（lockey）下）。
                // 这里只是读取，就不加锁了。

                if (p.Value.Dirty)
                    return false;

                if (p.Value.State != GlobalCacheManager.StateInvalid)
                {
                    if (p.Value.Acquire(GlobalCacheManager.StateInvalid)
                        != GlobalCacheManager.StateInvalid)
                    {
                        return false;
                    }
                }
                return Remove(p);
            }
            finally
            {
                lockey.ExitWriteLock();
            }
        }
    }
}
