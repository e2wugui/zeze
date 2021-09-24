using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class HugeConcurrentLruLike<K, V>
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        class LruItem
        {
            public V Value { get; }
            public HugeConcurrentDictionary<K, LruItem> LruNode { get; set; }

            public LruItem(V value, HugeConcurrentDictionary<K, LruItem> lruNode)
            {
                Value = value;
                LruNode = lruNode;
            }
        }

        private HugeConcurrentDictionary<K, LruItem> DataMap { get; }
        private ConcurrentQueue<Util.HugeConcurrentDictionary<K, LruItem>> LruQueue { get; }
            = new ConcurrentQueue<Util.HugeConcurrentDictionary<K, LruItem>>();
        private HugeConcurrentDictionary<K, LruItem> LruHot { get; set; }

        public long Capacity { get; set; }

        public long NewLruHotPeriod { get; set; } = 200;
        public long CleanPeriod { get; set; } = 2000;

        public int CleanPeriodWhenExceedCapacity { get; set; } = 0;
        public bool ContinueWhenTryRemoveCallbackFail { get; set; } = true;
        public Func<K, V, bool> TryRemoveCallback { get; set; } = null;

        public HugeConcurrentLruLike(long capacity,
            // 自定义删除。
            Func<K, V, bool> tryRemove = null,
            // 调度参数
            long newLruHotPeriod = 200,
            long cleanPeriod = 2000,
            // 其他初始化参数
            long initialCapacity = 31, int buckets = 16, int concurrencyLevel = 1024)
        {
            Capacity = capacity;
            TryRemoveCallback = tryRemove;
            NewLruHotPeriod = newLruHotPeriod;
            CleanPeriod = cleanPeriod;

            DataMap = new HugeConcurrentDictionary<K, LruItem>(buckets, concurrencyLevel, initialCapacity);
            NewLruHot();

            Scheduler.Instance.Schedule((task) =>
            {
                // 访问很少的时候不创建新的热点。这个选项没什么意思。
                if (LruHot.Count > GetLruInitialCapaicty() / 2)
                {
                    NewLruHot();
                }
            }, NewLruHotPeriod, NewLruHotPeriod);
            Util.Scheduler.Instance.Schedule(CleanNow, CleanPeriod);
        }

        public V GetOrAdd(K k, Func<K, V> factory)
        {
            bool isNew = false;
            var lruItem = DataMap.GetOrAdd(k, (k) =>
            {
                V value = factory(k);
                isNew = true;
                var lruItem = new LruItem(value, LruHot);
                LruHot[k] = lruItem;
                return lruItem;
            });

            if (false == isNew && lruItem.LruNode != LruHot)
            {
                lruItem.LruNode.TryRemove(k, out var _);
                if (LruHot.TryAdd(k, lruItem))
                {
                    lruItem.LruNode = LruHot;
                }
            }
            return lruItem.Value;
        }

        public bool TryGetValue(K key, out V value)
        {
            if (DataMap.TryGetValue(key, out var lruItem))
            {
                value = lruItem.Value;
                return true;
            }
            value = default(V);
            return false;
        }

        private long GetLruInitialCapaicty()
        {
            long lruInitialCapacity = (long)(DataMap.InitialCapacity * 0.2);
            return lruInitialCapacity < 31 ? 31 : lruInitialCapacity;
        }

        private void NewLruHot()
        {
            LruHot = new Util.HugeConcurrentDictionary<K, LruItem>(
                DataMap.BucketCount, DataMap.ConcurrencyLevel, GetLruInitialCapaicty());
            LruQueue.Enqueue(LruHot);
        }

        // 自定义删除时，需要注意并发问题。
        public bool TryRemove(K key, out V value)
        {
            if (DataMap.TryRemove(key, out var e))
            {
                // 这里有个时间窗口：先删除DataMap再去掉Lru引用，
                // 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
                // see GetOrAdd
                // 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
                e.LruNode.TryRemove(KeyValuePair.Create(key, e));
                value = e.Value;
                return true;
            }
            value = default(V);
            return false;
        }

        public void CleanNow(SchedulerTask taskNotUsed)
        {
            // 这个任务的执行时间可能很长，
            // 不直接使用 Scheduler 的定时任务，
            // 每次执行完重新调度。

            if (Capacity <= 0)
            {
                Scheduler.Instance.Schedule(CleanNow, CleanPeriod);
                return; // 容量不限
            }

            while (DataMap.Count > Capacity) // 超出容量，循环尝试
            {
                if (false == LruQueue.TryPeek(out var node))
                    break;

                if (node == LruHot) // 热点。不回收。
                    break;

                foreach (var e in node)
                {
                    if (null != TryRemoveCallback)
                    {
                        if (TryRemoveCallback(e.Key, e.Value.Value))
                            continue;
                        if (ContinueWhenTryRemoveCallbackFail)
                            continue;
                        break;
                    }
                    TryRemove(e.Key, out var _);
                }
                if (node.Count == 0)
                {
                    LruQueue.TryDequeue(out var _);
                }
                else
                {
                    logger.Warn($"remain record when clean oldest lrunode.");
                }

                if (CleanPeriodWhenExceedCapacity > 0)
                    System.Threading.Thread.Sleep(CleanPeriodWhenExceedCapacity);
            }
            Util.Scheduler.Instance.Schedule(CleanNow, CleanPeriod);
        }
    }
}
