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
        public virtual bool IsMemory => true;

        internal abstract void Initialize(IStorage storage);
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        public Table(string name) : base(name)
        {
            cache = new TableCache<K, V>(Id);
        }

        public V Get(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                return (V)cr.NewValue();
            }
            Record<K, V> r = cache.Get(key);
            if (null == r)
            {
                // 同一个记录可能会从storage装载两次，看storage内部实现有没有保护。
                /*
                if (null != storage)
                    storage.find();
                */
                r = new Record<K, V>(0, null); // 记录不存在也创建一个cache。使用value==null表示。看看是不是需要加状态。
                r = cache.GetOrAdd(key, r);
            }

            currentT.AddRecordAccessed(tkey, new Transaction.RecordAccessed(r));
            return r.ValueTyped;
        }

        public V GetOrAdd(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                V crv = (V)cr.NewValue();
                if (null != crv)
                {
                    return crv;
                }
                // add
            }
            else
            {
                Record<K, V> r = cache.Get(key);
                if (null == r)
                {
                    // 同一个记录可能会从storage装载两次，看storage内部实现有没有保护。
                    /*
                    if (null != storage)
                        storage.find();
                    */
                    r = new Record<K, V>(0, null); // 记录不存在也创建一个cache。使用value==null表示。看看是不是需要加状态。
                    r = cache.GetOrAdd(key, r);
                }

                cr = new Transaction.RecordAccessed(r);
                currentT.AddRecordAccessed(tkey, cr);

                if (null != r.Value)
                    return r.ValueTyped;
                // add
            }

            V add = NewValue();
            add.InitTableKey(tkey);
            cr.Put(currentT, add);
            return add;
        }

        public void Put(K key, V value)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                value.InitTableKey(tkey);
                cr.Put(currentT, value);
                return;
            }

            Record<K, V> r = cache.Get(key);
            if (null == r)
            {
                // 同一个记录可能会从storage装载两次，看storage内部实现有没有保护。
                /*
                if (null != storage)
                    storage.find();
                */
                r = new Record<K, V>(0, null); // 记录不存在也创建一个cache。使用value==null表示。看看是不是需要加状态。
                r = cache.GetOrAdd(key, r);
            }
            cr = new Transaction.RecordAccessed(r);
            currentT.AddRecordAccessed(tkey, cr);
            cr.Put(currentT, value);
        }

        // 几乎和Put一样，还是独立开吧。
        public void Remove(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                cr.Put(currentT, null);
                return;
            }

            Record<K, V> r = cache.Get(key);
            if (null == r)
            {
                // 同一个记录可能会从storage装载两次，看storage内部实现有没有保护。
                /*
                if (null != storage)
                    storage.find();
                */
                r = new Record<K, V>(0, null); // 记录不存在也创建一个cache。使用value==null表示。看看是不是需要加状态。
                r = cache.GetOrAdd(key, r);
            }
            cr = new Transaction.RecordAccessed(r);
            currentT.AddRecordAccessed(tkey, cr);
            cr.Put(currentT, null);
        }

        private TableCache<K, V> cache;
        private IStorage storage;

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
