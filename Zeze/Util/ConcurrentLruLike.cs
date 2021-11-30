using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class ConcurrentLruLike<K, V>
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        class LruItem
        {
            public V Value { get; }
            public ConcurrentDictionary<K, LruItem> LruNode { get; set; }

            public LruItem(V value, ConcurrentDictionary<K, LruItem> lruNode)
            {
                Value = value;
                LruNode = lruNode;
            }
        }

        private ConcurrentDictionary<K, LruItem> DataMap { get; }
        private ConcurrentQueue<ConcurrentDictionary<K, LruItem>> LruQueue { get; }
            = new ConcurrentQueue<ConcurrentDictionary<K, LruItem>>();
        private volatile ConcurrentDictionary<K, LruItem> LruHot;

        public int Capacity { get; set; }
        public int InitialCapacity { get; set; } // 创建以后再修改，只影响lru，不影响cache。
        public int ConcurrencyLevel { get; set; } // 创建以后再修改，只影响lru，不影响cache。
        public int MaxLruInitialCapaicty { get; set; } = 100000;

        public long NewLruHotPeriod { get; set; } = 10000;
        public long CleanPeriod { get; set; } = 10000;

        public int CleanPeriodWhenExceedCapacity { get; set; } = 1000;
        public bool ContinueWhenTryRemoveCallbackFail { get; set; } = true;
        public Func<K, V, bool> TryRemoveCallback { get; set; } = null;

        public ConcurrentLruLike(int capacity,
            // 自定义删除。
            Func<K, V, bool> tryRemove = null,
            // 调度参数
            long newLruHotPeriod = 200,
            long cleanPeriod = 2000,
            // 其他初始化参数
            int initialCapacity = 31, int concurrencyLevel = 1024)
        {
            Capacity = capacity;
            TryRemoveCallback = tryRemove;
            NewLruHotPeriod = newLruHotPeriod;
            CleanPeriod = cleanPeriod;
            InitialCapacity = initialCapacity;
            ConcurrencyLevel = concurrencyLevel;

            DataMap = new ConcurrentDictionary<K, LruItem>(concurrencyLevel, initialCapacity);
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
                var volatiletmp = LruHot;
                var lruItem = new LruItem(value, volatiletmp);
                volatiletmp[k] = lruItem; // MUST replace
                return lruItem;
            });

            if (false == isNew)
            {
                AdjustLru(k, lruItem);
            }
            return lruItem.Value;
        }

        private void AdjustLru(K key, LruItem lruItem)
        {
            var volatiletmp = LruHot;
            if (lruItem.LruNode != volatiletmp)
            {
                // compare key and value
                lruItem.LruNode.TryRemove(KeyValuePair.Create(key, lruItem));
                if (volatiletmp.TryAdd(key, lruItem)) // maybe fail
                {
                    lruItem.LruNode = volatiletmp;
                }
            }
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="key"></param>
        /// <param name="value"></param>
        /// <param name="adjustLru"> 是否调整lru </param>
        /// <returns></returns>
        public bool TryGetValue(K key, out V value, bool adjustLru = true)
        {
            if (DataMap.TryGetValue(key, out var lruItem))
            {
                if (adjustLru)
                    AdjustLru(key, lruItem);
                value = lruItem.Value;
                return true;
            }
            value = default(V);
            return false;
        }

        private int GetLruInitialCapaicty()
        {
            int lruInitialCapacity = (int)(InitialCapacity * 0.2);
            return lruInitialCapacity < MaxLruInitialCapaicty
                ? lruInitialCapacity : MaxLruInitialCapaicty;
        }

        private void NewLruHot()
        {
            var volatiletmp = new ConcurrentDictionary<K, LruItem>(ConcurrencyLevel, GetLruInitialCapaicty());
            LruHot = volatiletmp;
            LruQueue.Enqueue(volatiletmp);
        }

        // 自定义TryRemoveCallback时，需要调用这个方法真正删除。
        public bool TryRemove(K key, out V value)
        {
            if (DataMap.TryRemove(key, out var e))
            {
                // 这里有个时间窗口：先删除DataMap再去掉Lru引用，
                // 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
                // 1. GetOrAdd 需要 replace 更新
                // 2. 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
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

            try
            {
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

                    int sleepms = CleanPeriodWhenExceedCapacity > 1000
                        ? CleanPeriodWhenExceedCapacity : 1000;
                    System.Threading.Thread.Sleep(sleepms);
                }
            }
            finally
            {
                Util.Scheduler.Instance.Schedule(CleanNow, CleanPeriod);
            }
        }
    }
}
