using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;

namespace Zeze.Transaction
{
    public interface Database
    {
        public void Close();
        public void Checkpoint();
        public Database.Table OpenTable(string name);

        public interface Table
        {
            public ByteBuffer Find(ByteBuffer key);
            public void Replace(ByteBuffer key, ByteBuffer value);
            public void Remove(ByteBuffer key);
            public void Walk(IWalk iw);
            public void Close();

            public interface IWalk
            {
                public bool OnRecord(byte[] key, byte[] value);
            }
        }
    }

    /// <summary>
    /// Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
    /// </summary>
    public class DatabaseMemory : Database
    {
        public void Checkpoint()
        {
        }

        public void Close()
        {
        }
        public Database.Table OpenTable(string name)
        {
            return new TableMemory();
        }

        public class TableMemory : Database.Table
        {
            public class ByteArrayComparer : IEqualityComparer<byte[]>
            {
                public bool Equals(byte[] left, byte[] right)
                {
                    if (left == null || right == null)
                    {
                        return left == right;
                    }
                    if (left.Length != right.Length)
                    {
                        return false;
                    }
                    for (int i = 0; i < left.Length; i++)
                    {
                        if (left[i] != right[i])
                        {
                            return false;
                        }
                    }
                    return true;
                }
                public int GetHashCode(byte[] key)
                {
                    int sum = 0;
                    foreach (byte cur in key)
                    {
                        sum += cur;
                    }
                    return sum;
                }
            }
            public ConcurrentDictionary<byte[], byte[]> Map { get; } = new ConcurrentDictionary<byte[], byte[]>(new ByteArrayComparer());

            public ByteBuffer Find(ByteBuffer key)
            {
                if (Map.TryGetValue(key.Copy(), out var value))
                {
                    return ByteBuffer.Wrap(value);
                }
                return null;
            }

            public void Remove(ByteBuffer key)
            {
                Map.Remove(key.Copy(), out var notused);
            }

            public void Replace(ByteBuffer key, ByteBuffer value)
            {
                Map[key.Copy()] = value.Copy();
            }

            public void Walk(Database.Table.IWalk iw)
            {
                lock (this)
                {
                    // 不允许并发？
                    foreach (var e in Map)
                    {
                        if (false == iw.OnRecord(e.Key, e.Value))
                            break;
                    }
                }
            }

            public void Close()
            {
            }
        }
    }

}
