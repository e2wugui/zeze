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

        internal abstract void Initialize(Storage storage);
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        public Table(string name) : base(name)
        {
            cache = new TableCache<K, V>(this);
        }

        private Record<K, V> FindInCacheOrStorage(K key)
        {
            Record<K, V> r = cache.Get(key);
            if (null != r)
                return r;

            // 同一个记录可能会从storage装载两次，看storage内部实现有没有保护。
            V value = (null != storage) ? storage.Find(key, this) : null;
            return cache.GetOrAdd(key, new Record<K, V>(this, key, value));
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

            Record<K, V> r = FindInCacheOrStorage(key);
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
                Record<K, V> r = FindInCacheOrStorage(key);
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

        public void Inser(K key, V value)
        {
            if (null != Get(key))
            {
                throw new ArgumentException($"table:{GetType().FullName} insert key:{key} exists");
            }
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);
            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            value.InitTableKey(tkey);
            cr.Put(currentT, value);
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

            Record<K, V> r = FindInCacheOrStorage(key);
            cr = new Transaction.RecordAccessed(r);
            cr.Put(currentT, value);
            currentT.AddRecordAccessed(tkey, cr);
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

            Record<K, V> r = FindInCacheOrStorage(key);
            cr = new Transaction.RecordAccessed(r);
            cr.Put(currentT, null);
            currentT.AddRecordAccessed(tkey, cr);
        }

        private TableCache<K, V> cache;
        private Storage<K, V> storage;

        internal override void Initialize(Storage storage)
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
