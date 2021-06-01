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

        private ConcurrentDictionary<K, V> Map;

        private ConcurrentDictionary<K, SnapshotValue> SnapshotCopyOnWrite
            = new ConcurrentDictionary<K, SnapshotValue>();

        public int Count => Map.Count;

        // 需要外面更大锁来保护。Raft.StateMachine 的子类内加锁。
        private bool Snapshoting = false;

        public ConcurrentMap()
        { 
            Map = new ConcurrentDictionary<K, V>();
        }

        public ConcurrentMap(int concurrentLevel, int defaultCapacity)
        {
            Map = new ConcurrentDictionary<K, V>(concurrentLevel, defaultCapacity);
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
                    if (Snapshoting)
                    {
                        SnapshotCopyOnWrite.TryAdd(key,
                            new SnapshotValue(v, Operate.Add));
                    }
                    return v;
                });
        }

        public void Update(K k, Action<V> updator)
        {
            Update(k, GetOrAdd(k), updator);
        }

        public void Update(K k, V v, Action<V> updator)
        {
            if (Snapshoting)
            {
                // 只会在第一次修改时复制.
                SnapshotCopyOnWrite.GetOrAdd(k,
                    (key) => new SnapshotValue(v.Copy(), Operate.Update));
            }
            updator(v);
        }

        public void Remove(K k)
        {
            if (Map.TryRemove(k, out var removed))
            {
                if (Snapshoting)
                {
                    // Remove After Add Or Remove After Update 不需要记录。
                    SnapshotCopyOnWrite.TryAdd(k,
                        new SnapshotValue(removed, Operate.Remove));
                }
            }
        }

        // 线程不安全，需要外面更大的锁来保护。
        public bool StartSerialize()
        {
            if (Snapshoting)
                return false;
            Snapshoting = true;
            SnapshotCopyOnWrite.Clear();
            return true;
        }

        // 线程不安全，需要外面更大的锁来保护。
        public void EndSerialize()
        {
            Snapshoting = false;
            SnapshotCopyOnWrite.Clear();
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

        private long WriteInt4To(System.IO.Stream stream, int i, long offset = -1)
        {
            if (offset >= 0)
                stream.Seek(offset, System.IO.SeekOrigin.Begin);
            var position = stream.Position;
            stream.Write(BitConverter.GetBytes(i));
            if (offset >= 0)
                stream.Seek(0, System.IO.SeekOrigin.End);
            return position;
        }

        private int ReadInt4From(System.IO.Stream stream)
        {
            var bytes = new byte[4];
            stream.Read(bytes);
            return BitConverter.ToInt32(bytes);
        }

        public void ConcurrentSerializeTo(System.IO.Stream stream)
        {
            var position = WriteInt4To(stream, Map.Count);

            int SnapshotCount = 0;
            foreach (var cur in Map)
            {
                if (SnapshotCopyOnWrite.TryGetValue(cur.Key, out var changed))
                {
                    switch (changed.Operate)
                    {
                        case Operate.Add:
                            // 新增的记录不需要写出去。
                            continue;
                        case Operate.Remove:
                            // changed 里面保存的是删除前的项。
                            WriteTo(stream, cur.Key, changed.Value);
                            break;
                        case Operate.Update:
                            // changed 里面保存的是Update前的项。
                            WriteTo(stream, cur.Key, changed.Value);
                            break;
                    }
                }
                else
                {
                    WriteTo(stream, cur.Key, cur.Value);
                }
                ++SnapshotCount;
            }
            WriteInt4To(stream, SnapshotCount, position);
            stream.Seek(0, System.IO.SeekOrigin.End);
        }

        // 线程不安全，需要外面更大的锁保护。
        public void UnSerializeFrom(System.IO.Stream stream)
        {
            if (Snapshoting)
                throw new Exception("Coucurrent Error: In Snapshoting");

            Map.Clear();
            for (int count = ReadInt4From(stream); count > 0; --count)
            {
                int kvsize = ReadInt4From(stream);
                var kvbytes = new byte[kvsize];
                stream.Read(kvbytes);

                var bb = ByteBuffer.Wrap(kvbytes);

                K key = SerializeHelper<K>.Decode(bb);
                V value = new V();
                value.Decode(bb);
                Map.TryAdd(key, value); // ignore result
            }
        }

    }

    /*
    public sealed class IntKey : Serializable
    {
        public int Value { get; private set; }

        public IntKey()
        {
        }

        public IntKey(int value)
        {
            Value = value;
        }

        public void Decode(ByteBuffer bb)
        {
            Value = bb.ReadInt();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteInt(Value);
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is IntKey other)
                return Value.Equals(other.Value);
            return false;
        }
    }

    public sealed class LongKey : Serializable
    {
        public long Value { get; private set; }

        public LongKey()
        {
        }

        public LongKey(long value)
        {
            Value = value;
        }

        public void Decode(ByteBuffer bb)
        {
            Value = bb.ReadLong();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteLong(Value);
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is LongKey other)
                return Value.Equals(other.Value);
            return false;
        }
    }

    public sealed class StringKey : Serializable
    {
        public string Value { get; private set; }

        public StringKey()
        {
        }

        public StringKey(string value)
        {
            Value = value;
        }

        public void Decode(ByteBuffer bb)
        {
            Value = bb.ReadString();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteString(Value);
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is StringKey other)
                return Value.Equals(other.Value);
            return false;
        }
    }

    public sealed class BinaryKey : Serializable
    {
        public Zeze.Net.Binary Value { get; private set; }

        public BinaryKey()
        {
        }

        public BinaryKey(Zeze.Net.Binary value)
        {
            Value = value;
        }

        public void Decode(ByteBuffer bb)
        {
            Value = bb.ReadBinary();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteBinary(Value);
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;
            if (obj is BinaryKey other)
                return Value.Equals(other.Value);
            return false;
        }
    }
    */
}
