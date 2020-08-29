using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Services;

namespace Zeze.Transaction
{
    public abstract class Table
    {
        private static List<Table> Tables { get; } = new List<Table>(); // 全局。这样允许多个Zeze实例。线程安全：仅在初始化时保护一下。
        public static Table GetTable(int id) => Tables[id];

        public Table(string name)
        {
            this.Name = name;

            lock (Tables)
            {
                this.Id = Tables.Count;
                Tables.Add(this);
            }
        }

        public string Name { get; }
        public int Id { get; }
        public virtual bool IsMemory => true;
        public virtual bool IsAutoKey => false;

        internal abstract Storage Open(Application zeze, Database database);
        internal abstract void Close();
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        public Table(string name) : base(name)
        {
        }

        protected AutoKey AutoKey { get; private set;  }

        private Record<K, V> FindInCacheOrStorage(K key)
        {
            Record<K, V> r = Cache.GetOrAdd(key, (key) => new Record<K, V>(this, key, null));
            lock (r)
            {
                if (r.State == GlobalCacheManager.StateShare || r.State == GlobalCacheManager.StateModify)
                    return r;

                r.Acquire(GlobalCacheManager.StateShare);
                if (null != Storage)
                    r.Value = Storage.Find(key, this); // r.Value still maybe null
                return r;
            }
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

        public void Insert(K key, V value)
        {
            if (null != Get(key))
            {
                throw new ArgumentException($"table:{GetType().FullName} insert key:{key} exists");
            }
            if (key is long longkey)
                AutoKey?.Accept(longkey);
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
            if (key is long longkey)
                AutoKey?.Accept(longkey);
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

        internal TableCache<K, V> Cache { get; private set; }

        /// <summary>
        /// 开放出去仅仅为了测试。
        /// </summary>
        public Storage<K, V> Storage { get; private set; }

        internal override Storage Open(Application zeze, Database database)
        {
            if (null != Storage)
                throw new Exception("table has opened." + Name);

            if (this.IsAutoKey)
                AutoKey = zeze.TableSys.AutoKeys.GetAutoKey(Name);
            Cache = new TableCache<K, V>(zeze, this);

            Storage = IsMemory ? null : new Storage<K, V>(this, database, Name);
            return Storage;
        }

        internal override void Close()
        {
            Storage?.Close();
            Storage = null;
        }

        // Key 都是简单变量，系列化方法都不一样，需要生成。
        public abstract global::Zeze.Serialize.ByteBuffer EncodeKey(K key);
        public abstract K DecodeKey(global::Zeze.Serialize.ByteBuffer bb);

        public V NewValue()
        {
            return new V();
        }

        public global::Zeze.Serialize.ByteBuffer EncodeValue(V value)
        {
            global::Zeze.Serialize.ByteBuffer bb = global::Zeze.Serialize.ByteBuffer.Allocate(value.CapacityHintOfByteBuffer);
            value.Encode(bb);
            return bb;
        }
 
        /// <summary>
        /// 解码系列化的数据到对象。
        /// </summary>
        /// <param name="bb">bean encoded data</param>
        /// <returns></returns>
        public V DecodeValue(global::Zeze.Serialize.ByteBuffer bb)
        {
            V value = NewValue();
            value.Decode(bb);
            return value;
        }
    }
}
