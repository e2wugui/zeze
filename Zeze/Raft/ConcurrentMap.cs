using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Raft
{
    // 并发说明
    // 如果你的数据可以用 ConcurrentDictionary 管理，并且只使用下面的方法：
    // 1. value = GetOrAdd
    // 2. lock (value) { read_write_value; }
    // 3. Remove
    // 没有单独的 TryAdd 操作。多个不同的项的访问是并发的。
    // 此时可以使用下面的 ConcurrentMap。它使用 copy-on-write 的机制
    // 实现并发的Raft要求的Snapshot。
    // 【注意】GetOrAdd 引起的Add操作不会使用Raft日志同步状态，
    // 每个Raft-Node都使用一致的GetOrAdd得到相同的记录。
    //
    // 【注意】GetOrAdd 和 lock(value) 之间存在时间窗口，使得可能拿到被删除的项。
    // 这种情况一般都是有问题的，此时需要自己在 value 里面设置标志并检查。
    // 伪码如下：
    // void SomeRemove()
    // {
    //      var value = map.GetOrAdd(key);
    //      lock (value)
    //      {
    //          if (checkAndNeedRemove())
    //          {
    //              value.State = Removed; // last State
    //              map.Remove(key); // real remove。safe。
    //          }
    //      }
    // }
    // 
    // void SomeProcess()
    // {
    //      while (true)
    //      {
    //          var value = map.GetOrAdd(key);
    //          lock (value)
    //          {
    //              if (value.State == Removed)
    //                  continue; // GetOrAdd again
    //              normal_process;
    //              return; // end of process
    //          }
    //      }
    // }

    public interface Copyable<T> : Serializable
    {
        public T Copy();
    }

    public class ConcurrentMap<K, V>
        where V : Copyable<V>, new()
    {
        private HugeConcurrentDictionary<K, V> Map { get; }

        enum SnapshotState
        {
            Zero,
            Add,
            Update,
            Remove,
        }
        class SnapshotValue
        {
            internal V Value { get; set; }
            internal SnapshotState State { get; set; } = SnapshotState.Zero;
        }

        private ConcurrentDictionary<K, SnapshotValue> SnapshotLogs { get; }
            = new ConcurrentDictionary<K, SnapshotValue>();
        /// <summary>
        /// 【Snapshot状态和操作】
        /// State | add remove update
        /// Zero  | Add Remove Update
        /// Add   | _   _      _
        /// Update| x   Remove _
        /// Remove| _   _      _   (x: Error _: No Change)
        /// 
        /// Zero:
        /// 1. SnapshotValue第一次创建时：根据操作设置对应状态。
        /// 2. Add再Remove时设置成这个状态：此时只会发生新的Add操作。
        /// Add 和 Remove:
        /// 黑洞，一旦进入就不会再改变。
        /// </summary>
        private void SnapshotLog(SnapshotState op, K k, V value)
        {
            if (false == Snapshoting)
                return;

            var ss = SnapshotLogs.GetOrAdd(k, (_) => new SnapshotValue());
            lock (ss)
            {
                switch (ss.State)
                {
                    case SnapshotState.Zero:
                        switch (op)
                        {
                            case SnapshotState.Add:
                                ss.State = SnapshotState.Add;
                                ss.Value = value;
                                break;
                            case SnapshotState.Update:
                                ss.State = SnapshotState.Update;
                                ss.Value = value.Copy();
                                break;
                            case SnapshotState.Remove:
                                ss.State = SnapshotState.Remove;
                                ss.Value = value;
                                break;
                        }
                        break;
                    case SnapshotState.Add:
                        // all no change
                        break;
                    case SnapshotState.Update:
                        switch (op)
                        {
                            case SnapshotState.Add:
                                throw new Exception("Update->Add Impossible");
                            case SnapshotState.Update:
                                break; // no change
                            case SnapshotState.Remove:
                                // Value no change
                                ss.State = SnapshotState.Remove;
                                break;
                        }
                        break;
                    case SnapshotState.Remove:
                        // all no change
                        break;
                }
            }
        }

        public long Count => Map.Count;

        // 需要外面更大锁来保护。Raft.StateMachine 的子类内加锁。
        private bool Snapshoting = false;

        public ConcurrentMap(int buckets = 16, int concurrencyLevel = 1024, long initialCapacity = 1000000)
        { 
            Map = new HugeConcurrentDictionary<K, V>(buckets, concurrencyLevel, initialCapacity);
        }

        public V GetOrAdd(K key)
        {
            return GetOrAdd(key, (_) => new V());
        }

        public V GetOrAdd(K key, Func<K, V> valueFactory)
        {
            return Map.GetOrAdd(key,
                (k) =>
                {
                    V v = valueFactory(k);
                    SnapshotLog(SnapshotState.Add, k, v);
                    return v;
                });
        }

        public void Update(K k, Action<V> updator)
        {
            Update(k, GetOrAdd(k), updator);
        }

        public void Update(K k, V v, Action<V> updator)
        {
            SnapshotLog(SnapshotState.Update, k, v);
            // log before real update
            updator(v);
        }

        public void Remove(K k)
        {
            if (Map.TryRemove(k, out var removed))
            {
                SnapshotLog(SnapshotState.Remove, k, removed);
            }
        }

        // 线程不安全，需要外面更大的锁来保护。
        public bool StartSerialize()
        {
            if (Snapshoting)
                return false;
            Snapshoting = true;
            SnapshotLogs.Clear();
            return true;
        }

        // 线程不安全，需要外面更大的锁来保护。
        public void EndSerialize()
        {
            Snapshoting = false;
            SnapshotLogs.Clear();
        }

        private void WriteTo(System.IO.Stream stream, K k, V v)
        {
            // 外面使用 Update 修改记录，第一次修改时会复制，所以这个 lock 不是必要的。
            lock (v)
            {
                var bb = ByteBuffer.Allocate();

                bb.BeginWriteWithSize4(out var state);
                SerializeHelper<K>.Encode(bb, k);
                v.Encode(bb);
                bb.EndWriteWithSize4(state);

                stream.Write(bb.Bytes, bb.ReadIndex, bb.Size);
            }
        }

        private long WriteLong8To(System.IO.Stream stream, long i, long offset = -1)
        {
            if (offset >= 0)
                stream.Seek(offset, System.IO.SeekOrigin.Begin);
            var position = stream.Position;
            stream.Write(BitConverter.GetBytes(i));
            if (offset >= 0)
                stream.Seek(0, System.IO.SeekOrigin.End);
            return position;
        }

        private long ReadLong8From(System.IO.Stream stream)
        {
            var bytes = new byte[8];
            stream.Read(bytes);
            return BitConverter.ToInt64(bytes);
        }

        private int ReadInt4From(System.IO.Stream stream)
        {
            var bytes = new byte[4];
            stream.Read(bytes);
            return BitConverter.ToInt32(bytes);
        }

        public void SerializeTo(System.IO.Stream stream)
        {
            var position = WriteLong8To(stream, Map.Count);

            long SnapshotCount = 0;
            foreach (var cur in Map)
            {
                if (SnapshotLogs.TryGetValue(cur.Key, out var log))
                {
                    switch (log.State)
                    {
                        case SnapshotState.Add:
                            // 新增的记录不需要写出去。
                            continue;
                        case SnapshotState.Remove:
                            // Remove状态后面统一处理
                            break;
                        case SnapshotState.Update:
                            // changed 里面保存的是Update前的项。
                            WriteTo(stream, cur.Key, log.Value);
                            break;
                    }
                }
                else
                {
                    WriteTo(stream, cur.Key, cur.Value);
                }
                ++SnapshotCount;
            }
            foreach (var e in SnapshotLogs)
            {
                if (e.Value.State == SnapshotState.Remove)
                {
                    // 删除前的项。
                    WriteTo(stream, e.Key, e.Value.Value);
                    ++SnapshotCount;
                }
            }
            WriteLong8To(stream, SnapshotCount, position);
            stream.Seek(0, System.IO.SeekOrigin.End);
        }

        // 线程不安全，需要外面更大的锁保护。
        public void UnSerializeFrom(System.IO.Stream stream)
        {
            if (Snapshoting)
                throw new Exception("Coucurrent Error: In Snapshoting");

            Map.Clear();
            for (long count = ReadLong8From(stream); count > 0; --count)
            {
                int kvsize = ReadInt4From(stream);
                var kvbytes = new byte[kvsize];
                stream.Read(kvbytes);

                var bb = ByteBuffer.Wrap(kvbytes);

                K key = SerializeHelper<K>.Decode(bb);
                V value = new V();
                value.Decode(bb);
                Map[key] = value; // ignore result
            }
        }
    }
}
