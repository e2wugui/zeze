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
        // 自动倒库，当新库(DatabaseName)没有找到记录时，从旧库(DatabaseOldName)中读取，
        // Open 的时候找到旧库并打开Database.Table用来读取。
        // 内存表不支持倒库。
        public virtual string DatabaseName { get; } = "";
        public virtual string DatabaseOldName { get; } = "";
        public virtual int DatabaseOldMode { get; } = 0; // 0 none; 1 如果新库没有找到记录，尝试从旧库读取;

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
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
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
                /* lockey ????
                TableKey tkey = new TableKey(Id, key);
                Lockey lockey = Locks.Instance.Get(tkey);
                lockey.EnterReadLock();
                try
                */
                {
                    if (r.State == GlobalCacheManager.StateShare || r.State == GlobalCacheManager.StateModify)
                        return r;

                    // Invalid 状态，不可能发生 Reduce 操作。
                    r.State = r.Acquire(GlobalCacheManager.StateShare);
                    if (r.State == GlobalCacheManager.StateInvalid)
                        throw new RedoAndReleaseLockException();

                    r.Timestamp = Record.NextTimestamp;
                }
                /*
                finally
                {
                    lockey.ExitReadLock();
                }
                */
                if (null != Storage)
                {
                    r.Value = Storage.Find(key, this); // r.Value still maybe null
                    if (null == r.Value && null != OldTable)
                    {
                        ByteBuffer old = OldTable.Find(EncodeKey(key));
                        if (null != old)
                        {
                            r.Value = DecodeValue(old);
                            Storage.OnRecordChanged(r); // XXX 倒过来以后，准备提交到新库中。这里调用OnRecordChanged安全吗？
                        }
                    }
                    if (null != r.Value)
                    {
                        r.Value.InitTableKey(new TableKey(Id, key));
                    }
                }
                //Console.WriteLine($"FindInCacheOrStorage {r}");
                return r;
            }
        }

        internal override int ReduceShare(Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            TableKey tkey = new TableKey(Id, key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            Record<K, V> r = null;
            try
            {
                // TODO 这个要放锁内吗？
                r = Cache.Get(key);
                Console.WriteLine($"Reduce NewState={rpc.Argument.State} {r}");
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                    rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManager.StateInvalid:
                        rpc.Result.State = GlobalCacheManager.StateInvalid;
                        rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
                        return 0;

                    case GlobalCacheManager.StateShare:
                        rpc.Result.State = GlobalCacheManager.StateShare;
                        rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsShare);
                        return 0;

                    case GlobalCacheManager.StateModify:
                        r.State = GlobalCacheManager.StateShare; // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
                        r.Timestamp = Record.NextTimestamp;
                        break;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            // TODO 收集。
            logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
            Zeze.CheckpointRun();
            logger.Warn("ReduceShare checkpoint end. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManager.StateShare;
            //Thread.Sleep(10);
            rpc.SendResult();
            return 0;
        }

        internal override int ReduceInvalid(Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            TableKey tkey = new TableKey(Id, key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            Record<K, V> r = null;
            try
            {
                // TODO 这个要放锁内吗？
                r = Cache.Get(key);
                Console.WriteLine($"Reduce NewState={rpc.Argument.State} {r}");
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                    rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManager.StateInvalid:
                        rpc.Result.State = GlobalCacheManager.StateInvalid;
                        rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
                        return 0;

                    case GlobalCacheManager.StateShare:
                        r.State = GlobalCacheManager.StateInvalid;
                        r.Timestamp = Record.NextTimestamp;
                        // StateShare 应该是干净的，肯定能删除成功。
                        if (Cache.RemoeIfNotDirty(key))
                        {
                            rpc.SendResult();
                            return 0;
                        }
                        break;

                    case GlobalCacheManager.StateModify:
                        r.State = GlobalCacheManager.StateInvalid;
                        r.Timestamp = Record.NextTimestamp;
                        //Cache.RemoeIfNotDirty(key); // Modify 一般来说是脏的，这里不调用删除了，让CleanNow以后处理。
                        break;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            // TODO 收集。
            logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
            Zeze.CheckpointRun();
            logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManager.StateInvalid;
            //Thread.Sleep(10);
            rpc.SendResult();
            return 0;
        }

        public V Get(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                return (V)cr.NewestValue();
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
                V crv = (V)cr.NewestValue();
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
        private Database.Table OldTable;

        internal override Storage Open(Application zeze, Database database)
        {
            if (null != Storage)
                throw new Exception("table has opened." + Name);
            Zeze = zeze;
            if (this.IsAutoKey)
                AutoKey = zeze.TableSys.AutoKeys.GetAutoKey(Name);
            Cache = new TableCache<K, V>(zeze, this);

            Storage = IsMemory ? null : new Storage<K, V>(this, database, Name);
            OldTable = DatabaseOldMode == 1 ? zeze.GetDatabase(DatabaseOldName).OpenTable(Name) : null;
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
