using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Services;
using System.Threading;

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

        internal virtual int ReduceShare(Reduce rpc)
        {
            throw new NotImplementedException();
        }

        internal virtual int ReduceInvalid(Reduce rpc)
        {
            throw new NotImplementedException();
        }
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        public Table(string name) : base(name)
        {
        }
        public Application Zeze { get; private set; }

        protected AutoKey AutoKey { get; private set;  }

        private Record<K, V> FindInCacheOrStorage(K key)
        {
            Record<K, V> r = Cache.GetOrAdd(key, (key) => new Record<K, V>(this, key, null));
            lock (r)
            {
                if (r.State == GlobalCacheManager.StateShare || r.State == GlobalCacheManager.StateModify)
                    return r;
                // Invalid 状态，不可能发生 Reduce 操作，可以在锁内执行。
                r.State = r.Acquire(GlobalCacheManager.StateShare);
                if (null != Storage)
                    r.Value = Storage.Find(key, this); // r.Value still maybe null
                return r;
            }
        }

        internal override int ReduceShare(Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            bool reduceModifyToShare = false;
            Record<K, V> r = Cache.Get(key);
            if (null == r)
            {
                rpc.Result.State = GlobalCacheManager.StateInvalid;
                rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
                return 0;
            }
            lock (r)
            {
                switch (r.State)
                {
                    case GlobalCacheManager.StateShare:
                        rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsShare);
                        break;

                    case GlobalCacheManager.StateModify:
                        r.State = GlobalCacheManager.StateShare; // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
                        reduceModifyToShare = true;
                        break;
                }
            }
            if (reduceModifyToShare)
            {
                // TODO 保存变量用来收集 commit action。
                Checkpoint cp = new Checkpoint(Zeze.Databases.Values);
                cp.TryAddActionAfterCommit(() =>
                {
                    rpc.Result.State = GlobalCacheManager.StateShare;
                    rpc.SendResult();
                }
                );
                cp.Run();
            }
            return 0;
        }

        internal override int ReduceInvalid(Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            rpc.Result.State = GlobalCacheManager.StateInvalid;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));
            bool reduceModifyToInvalid = false;
            Record<K, V> r = Cache.Get(key);
            if (null == r)
            {
                rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
                return 0;
            }
            switch (r.State)
            {
                case GlobalCacheManager.StateShare:
                    r.State = GlobalCacheManager.StateInvalid;
                    // 这里不能安全的删除，统一交给TableCacle.ClenaupNow处理。
                    // 安全删除需要lockey才行，但是lockey在事务重做时会被保留，会出现死锁
                    // Cache.Remove(key); 
                    rpc.SendResult();
                    break;

                case GlobalCacheManager.StateModify:
                    r.State = GlobalCacheManager.StateInvalid;
                    // 这里不能安全的删除，统一交给TableCacle.ClenaupNow处理。
                    // 安全删除需要lockey才行，但是lockey在事务重做时会被保留，会出现死锁
                    // Cache.Remove(key);
                    reduceModifyToInvalid = true;
                    break;
            }

            if (reduceModifyToInvalid)
            {
                // TODO 保存变量用来收集 commit action。
                Checkpoint cp = new Checkpoint(Zeze.Databases.Values);
                cp.TryAddActionAfterCommit(() =>
                {
                    rpc.SendResult();
                }
                );
                cp.Run();
            }
            return 0;
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
            Zeze = zeze;
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
