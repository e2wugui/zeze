using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public abstract class Table
    {
        private static List<Table> Tables { get; } = new List<Table>(); // TODO 线程安全，静态变量
        public static Table GetTable(int id) => Tables[id];

        public Table(string name)
        {
            this.Name = name;
            this.Id = Tables.Count;
            Tables.Add(this);
        }

        public string Name { get; }
        public int Id { get; }

        internal abstract void Initialize(IStorage storage);
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        public Table(string name) : base(name)
        { 
        }

        public V Get(K key)
        {
            Transaction current = Transaction.Current;

            return null;
        }

        internal override void Initialize(IStorage storage)
        { 
        }


        // Key 都是简单变量，系列化方法都不一样，需要生成。
        public abstract Zeze.Serialize.ByteBuffer EncodeKey(K key);
        public abstract K DecodeKey(Zeze.Serialize.ByteBuffer bb);

        public V NewValue()
        {
            return new V();
        }

        public Zeze.Serialize.ByteBuffer EncodeValue(V value)
        {
            Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Allocate(value.CapacityHintOfByteBuffer);
            value.Encode(bb);
            return bb;
        }
 
        /// <summary>
        /// 解码系列化的数据到对象。
        /// </summary>
        /// <param name="bb">bean encoded data</param>
        /// <returns></returns>
        public V DecodeValue(Zeze.Serialize.ByteBuffer bb)
        {
            V value = NewValue();
            value.Decode(bb);
            return value;
        }
    }
}
