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
        private static List<Table> Tables { get; } = new List<Table>(); // 全局。这样允许多个Zeze实例。线程不安全：仅在初始化时保护一下。
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

        public Config.TableConf TableConf { get; protected set; }

        internal abstract Storage Open(Application app, Database database);
        internal abstract void Close();

        internal virtual int ReduceShare(GlobalCacheManager.Reduce rpc)
        {
            throw new NotImplementedException();
        }

        internal virtual int ReduceInvalid(GlobalCacheManager.Reduce rpc)
        {
            throw new NotImplementedException();
        }

        internal virtual void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
            throw new NotImplementedException();
        }

        public ChangeListenerMap ChangeListenerMap { get; } = new ChangeListenerMap();

        public abstract ChangeVariableCollector CreateChangeVariableCollector(int variableId);

        public abstract Storage Storage { get; }
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Table(string name) : base(name)
        {
        }
        public Application Zeze { get; private set; }

        protected ServiceManager.Agent.AutoKey AutoKey { get; private set;  }

        private Record<K, V> FindInCacheOrStorage(K key, Action<V> copy = null)
        {
            TableKey tkey = new TableKey(Id, key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterReadLock();
            // 严格来说，这里应该是WriteLock,但是这会涉及Transaction持有的锁的升级问题，
            // 虽然这里只是临时锁一下也会和持有冲突。
            // 由于装载仅在StateInvalid或者第一次载入的时候发生，
            // 还有lock(r)限制线程的重入，所以这里仅加个读锁限制一下state的修改，
            // 防止和Reduce冲突（由于StateInvalid才会申请权限和从storage装载，
            // 应该是不会发生Reduce的，加这个锁为了保险起见）。
            try
            {
                while (true)
                {
                    Record<K, V> r = Cache.GetOrAdd(key,
                        (key) => new Record<K, V>(this, key, null));
                    lock (r) // 如果外面是 WriteLock 就不需要这个了。
                    {
                        if (r.State == GlobalCacheManager.StateRemoved)
                            continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。

                        if (r.State == GlobalCacheManager.StateShare
                            || r.State == GlobalCacheManager.StateModify)
                        {
                            return r;
                        }

                        r.State = r.Acquire(GlobalCacheManager.StateShare);
                        if (r.State == GlobalCacheManager.StateInvalid)
                        {
                            throw new RedoAndReleaseLockException(tkey.ToString() + ":" + r.ToString());
                            //throw new RedoAndReleaseLockException();
                        }

                        r.Timestamp = Record.NextTimestamp;

                        if (null != TStorage)
                        {
#if ENABLE_STATISTICS
                            TableStatistics.Instance.GetOrAdd(Id).StorageFindCount.IncrementAndGet();
#endif
                            r.Value = TStorage.Find(key, this); // r.Value still maybe null

                            // 【注意】这个变量不管 OldTable 中是否存在的情况。
                            r.ExistInBackDatabase = null != r.Value;

                            // 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
                            if (null == r.Value && null != OldTable)
                            {
                                ByteBuffer old = OldTable.Find(EncodeKey(key));
                                if (null != old)
                                {
                                    r.Value = DecodeValue(old);
                                    // 从旧表装载时，马上设为脏，使得可以写入新表。
                                    // TODO CheckpointMode.Immediately 需要特殊处理。
                                    r.SetDirty();
                                }
                            }
                            if (null != r.Value)
                            {
                                r.Value.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
                            }
                        }
                        logger.Debug("FindInCacheOrStorage {0}", r);
                    }
                    copy?.Invoke(r.ValueTyped);
                    return r;
                }
            }
            finally
            {
                lockey.ExitReadLock();
            }
        }

        internal override int ReduceShare(GlobalCacheManager.Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            //logger.Debug("Reduce NewState={0}", rpc.Argument.State);

            TableKey tkey = new TableKey(Id, key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            Record<K, V> r = null;
            try
            {
                r = Cache.Get(key);
                logger.Debug("Reduce NewState={0} {1}", rpc.Argument.State, r);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                    logger.Debug("Reduce SendResult 1 {0}", r);
                    rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManager.StateInvalid:
                        rpc.Result.State = GlobalCacheManager.StateInvalid;
                        logger.Debug("Reduce SendResult 2 {0}", r);
                        rpc.SendResultCode(GlobalCacheManager.ReduceShareAlreadyIsInvalid);
                        return 0;

                    case GlobalCacheManager.StateShare:
                        rpc.Result.State = GlobalCacheManager.StateShare;
                        logger.Debug("Reduce SendResult 3 {0}", r);
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
            //logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManager.StateShare;
            FlushWhenReduce(r, () =>
            {
                logger.Debug("Reduce SendResult 4 {0}", r);
                rpc.SendResult();
            });
            //logger.Warn("ReduceShare checkpoint end. id={0} {1}", r, tkey);
            return 0;
        }

        private void FlushWhenReduce(Record r, Action after)
        {
            switch (Zeze.Config.CheckpointMode)
            {
                case CheckpointMode.Period:
                    Zeze.Checkpoint.AddActionAndPulse(after);
                    break;

                case CheckpointMode.Immediately:
                    after();
                    break;

                case CheckpointMode.Table:
                    RelativeRecordSet.FlushWhenReduce(r, Zeze.Checkpoint, after);
                    break;
            }
        }

        internal override int ReduceInvalid(GlobalCacheManager.Reduce rpc)
        {
            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            //logger.Debug("Reduce NewState={0}", rpc.Argument.State);

            TableKey tkey = new TableKey(Id, key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            Record<K, V> r = null;
            try
            {
                r = Cache.Get(key);
                logger.Debug("Reduce NewState={0} {1}", rpc.Argument.State, r);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                    logger.Debug("Reduce SendResult 1 {0}", r);
                    rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManager.StateInvalid:
                        rpc.Result.State = GlobalCacheManager.StateInvalid;
                        logger.Debug("Reduce SendResult 2 {0}", r);
                        rpc.SendResultCode(GlobalCacheManager.ReduceInvalidAlreadyIsInvalid);
                        return 0;

                    case GlobalCacheManager.StateShare:
                        r.State = GlobalCacheManager.StateInvalid;
                        r.Timestamp = Record.NextTimestamp;
                        // 不删除记录，让TableCache.CleanNow处理。 
                        logger.Debug("Reduce SendResult 3 {0}", r);
                        rpc.SendResult();
                        return 0;

                    case GlobalCacheManager.StateModify:
                        r.State = GlobalCacheManager.StateInvalid;
                        r.Timestamp = Record.NextTimestamp;
                        break;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            //logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManager.StateInvalid;
            FlushWhenReduce(r, () =>
            {
                logger.Debug("Reduce SendResult 4 {0}", r);
                rpc.SendResult();
            });
            //logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
            return 0;
        }

        internal override void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
            foreach (var e in Cache.DataMap)
            {
                var gkey = new GlobalCacheManager.GlobalTableKey(Name, EncodeKey(e.Key));
                if (Zeze.GlobalAgent.GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex)
                {
                    // 不是断开连接的GlobalCacheManager。跳过。
                    continue;
                }

                TableKey tkey = new TableKey(Id, e.Key);
                Lockey lockey = Locks.Instance.Get(tkey);
                lockey.EnterWriteLock();
                try
                {
                    // 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
                    e.Value.State = GlobalCacheManager.StateInvalid;
                }
                finally
                {
                    lockey.ExitWriteLock();
                }
            }
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
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
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
                currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);

                if (null != r.Value)
                    return r.ValueTyped;
                // add
            }

            V add = NewValue();
            add.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, add);
            return add;
        }

        public bool TryAdd(K key, V value)
        {
            if (null != Get(key))
                return false;

            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);
            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            value.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, value);
            return true;
        }

        public void Insert(K key, V value)
        {
            if (false == TryAdd(key, value))
                throw new ArgumentException($"table:{GetType().FullName} insert key:{key} exists");
        }

        public void Put(K key, V value)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Id, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                value.InitRootInfo(cr.OriginRecord.CreateRootInfoIfNeed(tkey), null);
                cr.Put(currentT, value);
                return;
            }
            Record<K, V> r = FindInCacheOrStorage(key);
            cr = new Transaction.RecordAccessed(r);
            cr.Put(currentT, value);
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
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
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
        }

        internal TableCache<K, V> Cache { get; private set; }

        public Storage<K, V> GetStorageForTestOnly(string IAmSure)
        {
            if (!IAmSure.Equals("IKnownWhatIAmDoing"))
                throw new Exception();
            return TStorage;
        }

        internal Database.Table OldTable { get; private set; }
        internal Storage<K, V> TStorage { get; private set; }
        public override Storage Storage => TStorage;

        internal override Storage Open(Application app, Database database)
        {
            if (null != TStorage)
                throw new Exception("table has opened." + Name);
            Zeze = app;
            if (this.IsAutoKey)
                AutoKey = app.ServiceManagerAgent.GetAutoKey(Name);

            base.TableConf = app.Config.GetTableConf(Name);
            Cache = new TableCache<K, V>(app, this);

            TStorage = IsMemory ? null : new Storage<K, V>(this, database, Name);
            OldTable = TableConf.DatabaseOldMode == 1
                ? app.GetDatabase(TableConf.DatabaseOldName).OpenTable(Name) : null;
            return TStorage;
        }

        internal override void Close()
        {
            TStorage?.Close();
            TStorage = null;
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

        /// <summary>
        /// 遍历表格。能看到记录的最新数据。
        /// 【注意】这里看不到新增的但没有提交(checkpoint)的记录。实现这个有点麻烦。
        /// 【并发】每个记录回调时加读锁，回调完成马上释放。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public long Walk(Func<K, V, bool> callback)
        {
            return TStorage.DatabaseTable.Walk(
                (key, value) =>
                {
                    K k = DecodeKey(ByteBuffer.Wrap(key));
                    TableKey tkey = new TableKey(Id, k);
                    Lockey lockey = Locks.Instance.Get(tkey);
                    lockey.EnterReadLock();
                    try
                    {
                        Record<K, V> r = Cache.Get(k);
                        if (null != r && r.State != GlobalCacheManager.StateRemoved)
                        {
                            if (r.State == GlobalCacheManager.StateShare
                                || r.State == GlobalCacheManager.StateModify)
                            {
                                // 拥有正确的状态：
                                if (r.Value == null)
                                    return true; // 已经被删除，但是还没有checkpoint的记录看不到。
                                return callback(r.Key, r.ValueTyped);
                            }
                            // else GlobalCacheManager.StateInvalid
                            // 继续后面的处理：使用数据库中的数据。
                        }
                        // 缓存中不存在或者正在被删除，使用数据库中的数据。
                        V v = DecodeValue(ByteBuffer.Wrap(value));
                        return callback(k, v);
                    }
                    finally
                    {
                        lockey.ExitReadLock();
                    }
                });
        }

        /// <summary>
        /// 遍历数据库中的表。看不到本地缓存中的数据。
        /// 【并发】后台数据库处理并发。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public long WalkDatabase(Func<byte[], byte[], bool> callback)
        {
            return TStorage.DatabaseTable.Walk(callback);
        }

        /// <summary>
        /// 遍历数据库中的表。看不到本地缓存中的数据。
        /// 【并发】后台数据库处理并发。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public long WalkDatabase(Func<K, V, bool> callback)
        {
            return TStorage.DatabaseTable.Walk(
                (key, value) =>
                {
                    K k = DecodeKey(ByteBuffer.Wrap(key));
                    V v = DecodeValue(ByteBuffer.Wrap(value));
                    return callback(k, v);
                });
        }

        /// <summary>
        /// 获得记录的深拷贝。必须在事务外使用。
        /// 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public V GetCopyOutofTransaction(K key)
        {
            if (Transaction.Current != null)
                throw new Exception("Can Not Select In Transaction.");

            Bean copy = null;
            FindInCacheOrStorage(key, (v) => copy = v?.CopyBean());
            return (V)copy;
        }
    }
}
