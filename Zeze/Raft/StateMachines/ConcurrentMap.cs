using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zeze.Raft.StateMachines
{
    public interface Copyable<T>
    {
        public T Copy();
    }

    public abstract class ConcurrentMap<K, V>
        : StateMachine
        where V : Copyable<V>
    {
        public enum Operate
        {
            Update,
            Add,
            Remove,
        }

        class SnapshotValue
        {
            public V Value { get; }
            public Operate Operate { get; }

            public SnapshotValue(V value, Operate operate)
            {
                Value = value;
                Operate = operate;
            }
        }

        class Log
        {
            public K Key { get; }
            public V Value { get; }
            public Operate State { get; }
            public Action<V> Updator { get; }

            public Log(K key, V value, Operate state, Action<V> updator)
            {
                Key = key;
                Value = value;
                State = state;
                Updator = updator;
            }
        }

        private ConcurrentDictionary<K, V> Map = new ConcurrentDictionary<K, V>();
        private ConcurrentDictionary<K, SnapshotValue> SnapshotCopyOnWrite = new ConcurrentDictionary<K, SnapshotValue>();

        // 这个保护snapshot的开始结束等，没想好，直接想到方法是读写锁。
        // 读锁用于正常的数据操作，允许并发，
        // 写锁用于snapshot（仅设置标志，copytostream仍然在锁外）。
        // 并发的需求：正常操作能并发，snapshot仅一个线程操作?
        private AtomicBool Snapshoting = new AtomicBool();

        // Log 也没完全想好。
        private AtomicLong LogIndex = new AtomicLong();
        private ConcurrentDictionary<long, Log> Logs = new ConcurrentDictionary<long, Log>();

        /// <summary>
        /// 并发的得到一条记录引用。
        /// 需要lock(v)进一步实现记录级别互斥。
        /// 由于并发删除（Remove），还需要处理得到v在lock之前被删除的情况。
        /// </summary>
        /// <param name="key"></param>
        /// <param name="valueFactory"></param>
        /// <returns></returns>
        public V GetOrAdd(K key, Func<K, V> valueFactory)
        {
            return Map.GetOrAdd(key,
                (k) =>
                {
                    V v = valueFactory(k);
                    long logindex = LogIndex.IncrementAndGet();
                    Logs.TryAdd(logindex, new Log(k, v, Operate.Add, null));
                    if (Snapshoting.Get())
                    {
                        SnapshotCopyOnWrite.TryAdd(key, new SnapshotValue(v, Operate.Add));
                    }
                    return v;
                });
        }

        /// <summary>
        /// MUST IN lock(v)
        /// </summary>
        /// <param name="v"></param>
        /// <param name="updator"></param>
        public void Update(K k, V v, Action<V> updator)
        {
            long logindex = LogIndex.IncrementAndGet();
            // 只会在第一次修改时复制.
            if (Snapshoting.Get())
            {
                SnapshotCopyOnWrite.GetOrAdd(k,
                    (key) => new SnapshotValue(v.Copy(), Operate.Update));
            }
            updator(v);
            Logs.TryAdd(logindex, new Log(k, v, Operate.Update, updator));
        }

        public void Remove(K k)
        {
            long logindex = LogIndex.IncrementAndGet();
            if (Map.TryRemove(k, out var removed))
            {
                // Remove After Add Or Remove After Update 不需要记录。
                if (Snapshoting.Get())
                {
                    SnapshotCopyOnWrite.TryAdd(k, new SnapshotValue(removed, Operate.Remove));
                }
                // removed 对于log应该是不需要的。
                Logs.TryAdd(logindex, new Log(k, default(V), Operate.Remove, null));
            }
        }

        public override void Snapshot()
        {
            Snapshoting.GetAndSet(true);
            SnapshotCopyOnWrite.Clear();
            long logindex = LogIndex.Get();
            Logs.Clear();
            //return logindex;
        }

        public void SnapshotCopyToStream()
        {
            foreach (var cur in Map)
            {
                if (SnapshotCopyOnWrite.TryGetValue(cur.Key, out var changed))
                {
                    switch (changed.Operate)
                    {
                        case Operate.Add:
                            // skip
                            break;
                        case Operate.Remove:
                            // copy to stream
                            break;
                        case Operate.Update:
                            // copy to stream.
                            break;
                    }
                }
            }
        }

        public void SnapshotEnd()
        {
            Snapshoting.CompareAndExchange(true, false);
            SnapshotCopyOnWrite.Clear();
        }
    }
}
