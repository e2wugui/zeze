using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Services;
using System.Threading;
using Zeze.Services.GlobalCacheManager;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public abstract class Table
    {
        public Table(string name)
        {
            this.Name = name;
        }

        public string Name { get; }
        public Application Zeze { get; protected set; }

        public virtual bool IsMemory => true;
        public virtual bool IsAutoKey => false;

        public Config.TableConf TableConf { get; protected set; }

        internal abstract Storage Open(Application app, Database database);
        internal abstract void Close();

        internal virtual int ReduceShare(Reduce rpc)
        {
            throw new NotImplementedException();
        }

        internal virtual int ReduceInvalid(Reduce rpc)
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

        protected Zeze.Services.ServiceManager.Agent.AutoKey AutoKey { get; private set;  }

        private Record<K, V> FindInCacheOrStorage(K key)
        {
            TableKey tkey = new TableKey(Name, key);
            while (true)
            {
                Record<K, V> r = Cache.GetOrAdd(key,
                    (key) => new Record<K, V>(this, key, null));
                lock (r) // 对同一个记录，不允许重入。
                {
                    if (r.State == GlobalCacheManagerServer.StateRemoved)
                        continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。

                    if (r.State == GlobalCacheManagerServer.StateShare
                        || r.State == GlobalCacheManagerServer.StateModify)
                    {
                        return r;
                    }

                    r.State = r.Acquire(GlobalCacheManagerServer.StateShare);
                    if (r.State == GlobalCacheManagerServer.StateInvalid)
                    {
                        throw new RedoAndReleaseLockException(tkey,
                            tkey.ToString() + ":" + r.ToString());
                        //throw new RedoAndReleaseLockException();
                    }

                    r.Timestamp = Record.NextTimestamp;

                    if (null != TStorage)
                    {
#if ENABLE_STATISTICS
                            TableStatistics.Instance.GetOrAdd(Name).StorageFindCount.IncrementAndGet();
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
                return r;
            }
        }

        internal override int ReduceShare(Reduce rpc)
        {
            logger.Debug("ReduceShare NewState={0}", rpc.Argument.State);

            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            //logger.Debug("Reduce NewState={0}", rpc.Argument.State);

            TableKey tkey = new TableKey(Name, key);
            var flushFuture = new TaskCompletionSource<int>();
            if (false == Zeze.FlushWhenReduceFutures.TryAdd(tkey, flushFuture))
            {
                rpc.Result.State = GlobalCacheManagerServer.StateReduceDuplicate;
                logger.Debug("ReduceShare SendResult 0");
                rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
                return 0;
            }

            Record<K, V> r = null;
            Lockey lockey = Zeze.Locks.Get(tkey);
            lockey.EnterWriteLock();
            try
            {
                r = Cache.Get(key);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                    logger.Debug("ReduceShare SendResult 1 {0}", r);
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
                    flushFuture.SetResult(0);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
                    case GlobalCacheManagerServer.StateInvalid:
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        logger.Debug("ReduceShare SendResult 2 {0}", r);
                        Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                        rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
                        flushFuture.SetResult(0);
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        rpc.Result.State = GlobalCacheManagerServer.StateShare;
                        rpc.ResultCode = GlobalCacheManagerServer.ReduceShareAlreadyIsShare;
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceShare SendResult 3 {0}", r);
                        Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                        rpc.SendResult();
                        flushFuture.SetResult(0);
                        return 0;

                    case GlobalCacheManagerServer.StateModify:
                        r.State = GlobalCacheManagerServer.StateShare; // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceShare SendResult * {0}", r);
                        rpc.SendResult();
                        return 0;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            //logger.Warn("ReduceShare checkpoint begin. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManagerServer.StateShare;
            FlushWhenReduce(r, () =>
            {
                logger.Debug("ReduceShare SendResult 4 {0}", r);
                // Must before SendResult
                Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                rpc.SendResult();
                flushFuture.SetResult(0);
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

        internal override int ReduceInvalid(Reduce rpc)
        {
            logger.Debug("ReduceInvalid NewState={0}", rpc.Argument.State);

            rpc.Result = rpc.Argument;
            K key = DecodeKey(ByteBuffer.Wrap(rpc.Argument.GlobalTableKey.Key));

            TableKey tkey = new TableKey(Name, key);
            var flushFuture = new TaskCompletionSource<int>();
            if (false == Zeze.FlushWhenReduceFutures.TryAdd(tkey, flushFuture))
            {
                rpc.Result.State = GlobalCacheManagerServer.StateReduceDuplicate;
                logger.Debug("ReduceInvalid SendResult 0");
                rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
                return 0;
            }

            Record<K, V> r = null;
            Lockey lockey = Zeze.Locks.Get(tkey);
            lockey.EnterWriteLock();
            try
            {
                r = Cache.Get(key);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                    logger.Debug("ReduceInvalid SendResult 1 {0}", r);
                    Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
                    flushFuture.SetResult(0);
                    return 0;
                }
                switch (r.State)
                {
                    case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
                    case GlobalCacheManagerServer.StateInvalid:
                        Console.WriteLine($"ReduceInvalid 1 Local=Invalid Change Now.");
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        rpc.ResultCode = GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid;
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceInvalid SendResult 2 {0}", r);
                        Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                        rpc.SendResult();
                        flushFuture.SetResult(0);
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        Console.WriteLine($"ReduceInvalid 2 Local=Share Change Now.");
                        r.State = GlobalCacheManagerServer.StateInvalid;
                        // 不删除记录，让TableCache.CleanNow处理。 
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceInvalid SendResult 3 {0}", r);
                        Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                        rpc.SendResult();
                        flushFuture.SetResult(0);
                        return 0;

                    case GlobalCacheManagerServer.StateModify:
                        Console.WriteLine($"ReduceInvalid 3 Local=Modify Change Now.");
                        r.State = GlobalCacheManagerServer.StateInvalid;
                        if (r.Dirty)
                            break;
                        Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                        rpc.SendResult();
                        flushFuture.SetResult(0);
                        return 0;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }
            //logger.Warn("ReduceInvalid checkpoint begin. id={0} {1}", r, tkey);
            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
            FlushWhenReduce(r, () =>
            {
                logger.Debug("ReduceInvalid SendResult 4 {0} ", r);
                // Must before SendResult
                Zeze.FlushWhenReduceFutures.TryRemove(tkey, out _);
                rpc.SendResult();
                flushFuture.SetResult(0);
            });
            //logger.Warn("ReduceInvalid checkpoint end. id={0} {1}", r, tkey);
            return 0;
        }

        internal override void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
            foreach (var e in Cache.DataMap)
            {
                var gkey = new GlobalTableKey(Name, EncodeKey(e.Key));
                if (Zeze.GlobalAgent.GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex)
                {
                    // 不是断开连接的GlobalCacheManager。跳过。
                    continue;
                }

                TableKey tkey = new TableKey(Name, e.Key);
                Lockey lockey = Zeze.Locks.Get(tkey);
                lockey.EnterWriteLock();
                try
                {
                    Console.WriteLine($"ReduceInvalidAllLocalOnly {e.Value}.");
                    // 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
                    e.Value.State = GlobalCacheManagerServer.StateInvalid;
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
            TableKey tkey = new TableKey(Name, key);

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
            TableKey tkey = new TableKey(Name, key);

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
            TableKey tkey = new TableKey(Name, key);
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
            TableKey tkey = new TableKey(Name, key);

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
            TableKey tkey = new TableKey(Name, key);

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
                    TableKey tkey = new TableKey(Name, k);
                    Lockey lockey = Zeze.Locks.Get(tkey);
                    lockey.EnterReadLock();
                    try
                    {
                        Record<K, V> r = Cache.Get(k);
                        if (null != r && r.State != GlobalCacheManagerServer.StateRemoved)
                        {
                            if (r.State == GlobalCacheManagerServer.StateShare
                                || r.State == GlobalCacheManagerServer.StateModify)
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
        /// 获得记录的拷贝。
        /// 1. 一般在事务外使用。
        /// 2. 如果在事务内使用：
        ///    a)已经访问过的记录，得到最新值的拷贝。不建议这种用法。
        ///    b)没有访问过的记录，从后台查询并拷贝，但不会加入RecordAccessed。
        /// 3. 得到的结果一般不用于修改，应用传递时可以使用ReadOnly接口修饰保护一下。
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public V SelectCopy(K key)
        {
            var tkey = new TableKey(Name, key);
            Transaction currentT = Transaction.Current;
            if (null != currentT)
            {
                Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
                if (null != cr)
                {
                    return (V)cr.NewestValue()?.CopyBean();
                }
                throw new Exception("SelectCopy A Not Accessed Record In Transaction Is Danger!");
            }

            var lockey = Zeze.Locks.Get(tkey);
            lockey.EnterReadLock();
            try
            {
                var r = FindInCacheOrStorage(key);
                return (V)r.Value.CopyBean();
            }
            finally
            {
                lockey.ExitReadLock();
            }
        }
    }
}
