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

    public abstract class Table<TKey, TValue> : Table where TValue : Bean, new()
    {
        public Table(string name) : base(name)
        { 
        }

        internal override void Initialize(IStorage storage)
        { 
        }


        // Key 都是简单变量，系列化方法都不一样，需要生成。
        public abstract Zeze.Serialize.ByteBuffer EncodeKey(TKey key);
        public abstract TKey DecodeKey(Zeze.Serialize.ByteBuffer bb);

        public TValue NewValue()
        {
            return new TValue();
        }

        public Zeze.Serialize.ByteBuffer EncodeValue(TValue value)
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
        public TValue DecodeValue(Zeze.Serialize.ByteBuffer bb)
        {
            TValue value = NewValue();
            value.Decode(bb);
            return value;
        }
    }
}
