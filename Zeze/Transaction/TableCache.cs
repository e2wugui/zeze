using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;
using Zeze.Services;
using System.Threading.Tasks;
using System.Threading;
using Zeze.Util;

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
        private static readonly ILogger logger = LogManager.GetLogger(typeof(TableCache<K, V>));

        internal ConcurrentDictionary<K, Record<K, V>> DataMap { get; }

        private ConcurrentQueue<ConcurrentDictionary<K, Record<K, V>>> LruQueue { get; }
            = new ConcurrentQueue<ConcurrentDictionary<K, Record<K, V>>>();

        private volatile ConcurrentDictionary<K, Record<K, V>> LruHot;

        public Table<K, V> Table { get; }
        private Util.SchedulerTask TimerNewHot;
        private Util.SchedulerTask TimerClean;

        public TableCache(Application _, Table<K, V> table)
        {
            this.Table = table;
            DataMap = new ConcurrentDictionary<K, Record<K, V>>(
                GetCacheConcurrencyLevel(), GetCacheInitialCapacity());
            NewLruHot();
            TimerNewHot = Util.Scheduler.Schedule((task) =>
            {
                // 访问很少的时候不创建新的热点。这个选项没什么意思。
                if (LruHot.Count > table.TableConf.CacheNewAccessHotThreshold)
                {
                    NewLruHot();
                }
            }, table.TableConf.CacheNewLruHotPeriod, table.TableConf.CacheNewLruHotPeriod);
            TimerClean = Util.Scheduler.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
        }

        public async Task<long> WalkKeyAsync(Func<K, Task<bool>> callback)
        {
            long cw = 0;
            foreach (var e in DataMap)
            {
                if (false == await callback(e.Key))
                    return cw;
                ++cw;
            }
            return cw;
        }

        public long WalkKey(Func<K, bool> callback)
        {
            long cw = 0;
            foreach (var e in DataMap)
            {
                if (false == callback(e.Key))
                    return cw;
                ++cw;
            }
            return cw;
        }

        public void Close()
        {
            TimerNewHot?.Cancel();
            TimerNewHot = null;
            TimerClean?.Cancel();
            TimerClean = null;
        }
        private int GetCacheConcurrencyLevel()
        {
            // 这样写，当配置修改，可以使用的时候马上生效。
            return Table.TableConf.CacheConcurrencyLevel > Environment.ProcessorCount
                ? Table.TableConf.CacheConcurrencyLevel : Environment.ProcessorCount;
        }

        private int GetCacheInitialCapacity()
        {
            // 31 from c# document
            // 这样写，当配置修改，可以使用的时候马上生效。
            return Table.TableConf.CacheInitialCapacity < 31
                ? 31 : Table.TableConf.CacheInitialCapacity;
        }

        private int GetLruInitialCapacity()
        {
            int c = (int)(GetCacheInitialCapacity() * 0.2);
            return c < Table.TableConf.CacheMaxLruInitialCapacity
                ? c : Table.TableConf.CacheMaxLruInitialCapacity;
        }

        private void NewLruHot()
        {
            var volatiletmp = new ConcurrentDictionary<K, Record<K, V>>(GetCacheConcurrencyLevel(), GetLruInitialCapacity());
            LruHot = volatiletmp;
            LruQueue.Enqueue(volatiletmp);
        }

        public Record<K, V> GetOrAdd(K key, Func<K, Record<K, V>> valueFactory)
        {
            var lruHot = LruHot;
            var result = DataMap.GetOrAdd(key, k =>
            {
                var r = valueFactory(k);
                lruHot[k] = r; // replace: add or update see this.Remove
                r.LruNode = lruHot;
                return r;
            });

            if (result.LruNode != lruHot)
            {
                // 旧纪录 && 优化热点执行调整
                // 下面在发生LruHot变动+并发GetOrAdd时，哪个后执行，就调整到哪个node，不严格调整到真正的LruHot。
                // 注意，这把锁仅用于这里，最好不要跟事务锁重合。
                var oldNode = result.GetAndSetLruNodeNull();
                if (oldNode != null)
                {
                    oldNode.TryRemove(key, out _);
                    if (lruHot.TryAdd(key, result))
                        result.LruNode = lruHot;
                }
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

        private void TryPollLruQueue()
        {
            var cap = LruQueue.Count - 8640;
            if (cap <= 0)
                return;

            var polls = new List<ConcurrentDictionary<K, Record<K, V>>>(cap);
            while (LruQueue.Count > 8640)
            {
                // 大概，删除超过一天的节点。
                if (false == LruQueue.TryDequeue(out var node))
                    break;
                polls.Add(node);
            }

            // 把被删除掉的node里面的记录迁移到当前最老(head)的node里面。
            if (false == LruQueue.TryPeek(out var head))
                throw new Exception("Impossible!");
            foreach (var poll in polls)
            {
                foreach (var e in poll)
                {
                    // concurrent see GetOrAdd
                    var r = e.Value;
                    if (r.CompareAndSetLruNodeNull(poll) && head.TryAdd(e.Key, r)) // 并发访问导致这个记录已经被迁移走。
                        r.LruNode = head;                    
                }
            }
        }

        private async void CleanNow(Zeze.Util.SchedulerTask ThisTask)
        {
            // 这个任务的执行时间可能很长，
            // 不直接使用 Scheduler 的定时任务，
            // 每次执行完重新调度。

            if (Table.TableConf.CacheCapacity <= 0)
            {
                TimerClean = Util.Scheduler.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
                TryPollLruQueue();
                return; // 容量不限
            }
            try
            {
                while (DataMap.Count > Table.TableConf.CacheCapacity && Table.Zeze.IsStart) // 超出容量，循环尝试
                {
                    if (false == LruQueue.TryPeek(out var node))
                        break;

                    if (node == LruHot) // 热点。不回收。
                        break;

                    foreach (var e in node)
                    {
                        await TryRemoveRecord(e);
                    }
                    if (node.IsEmpty)
                    {
                        LruQueue.TryDequeue(out var _);
                    }
                    else
                    {
                        // 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
                        Table.Zeze.Checkpoint.CheckpointNow().Wait();
                        logger.Warn($"remain {node.Count} records when clean oldest lrunode.");
                        System.Threading.Thread.Sleep(Table.TableConf.CacheCleanPeriodWhenExceedCapacity);
                    }
                }
                TryPollLruQueue();
            }
            finally
            {
                TimerClean = Util.Scheduler.Schedule(CleanNow, Table.TableConf.CacheCleanPeriod);
            }
        }

        // under lockey.writelock
        internal bool Remove(KeyValuePair<K, Record<K, V>> p)
        {
            if (DataMap.TryRemove(p))
            {
                // 这里有个时间窗口：先删除DataMap再去掉Lru引用，
                // 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
                // see GetOrAdd
                p.Value.State = GlobalCacheManagerServer.StateRemoved;
                // 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
                p.Value.LruNode?.TryRemove(p);
                return true;
            }
            else
            {
                p.Value.State = GlobalCacheManagerServer.StateRemoved; // always setup
            }
            return false;
        }

        private bool TryRemoveRecordUnderLock(KeyValuePair<K, Record<K, V>> p)
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

            if (p.Value.IsFreshAcquire())
                return false;

            if (p.Value.State != GlobalCacheManagerServer.StateInvalid)
            {
                try
                {
                    var task = p.Value.Acquire(GlobalCacheManagerServer.StateInvalid, false);
                    task.Wait();
                    var (ResultCode, ResultState) = task.Result;
                    if (ResultCode != 0 || ResultState != GlobalCacheManagerServer.StateInvalid)
                        return false;
                }
                catch (Exception e)
                {
                    logger.Error(e, "Acquire exception:");
                    // 此时GlobalServer可能已经改成StateInvalid了, 无论如何还是当成已经Invalid保证安全
                }
            }
            return Remove(p);
        }

        private async Task<bool> TryRemoveRecord(KeyValuePair<K, Record<K, V>> p)
        {
            // lockey 第一优先，和事务并发。
            var tkey = new TableKey(this.Table.Id, p.Key);
            var lockey = Table.Zeze.Locks.Get(tkey);

            if (false == lockey.TryEnterWriteLock())
                return false;
            try
            {
                // record.lock 和事务并发。
                using (var holder = await p.Value.Mutex.TryAcquireAsync(CancellationToken.None)) // 最好使用TryLock，先这样了。
                {
                    if (holder.IsEmpty)
                        return false;

                    // rrs lock
                    var rrs = p.Value.RelativeRecordSet;
                    var lockrrs = rrs.LockTry();
                    if (null == lockrrs)
                        return false;
                    try
                    {
                        if (rrs.MergeTo != null)
                            return false; // 刚刚被合并或者删除（flushed）的记录认为是活跃的，不删除。

                        if (rrs.RecordSet != null && rrs.RecordSet.Count > 1)
                            return false; // 只包含自己的时候才可以删除，多个记录关联起来时不删除。

                        return TryRemoveRecordUnderLock(p);
                    }
                    finally
                    {
                        lockrrs.Dispose();
                    }
                }
            }
            finally
            {
                lockey.Release();
            }
        }
    }
}
