using System;
using System.Collections.Generic;
using Zeze.Serialize;
using Zeze.Services;
using System.Threading;
using Zeze.Services.GlobalCacheManager;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Util;

namespace Zeze.Transaction
{
    public abstract class Table
    {
        public Table(string name)
        {
            this.Name = name;

            // 新增属性Id，为了影响最小，采用virtual方式定义。
            // AddTable不能在这里调用。
            // 该调用移到Application.AddTable。
            // 影响：允许Table.Id重复，只要它没有加入zeze-app。
        }

        public string Name { get; }
        public virtual int Id { get; } = 0;
        public Application Zeze { get; protected set; }

        public virtual bool IsMemory => true;
        public virtual bool IsAutoKey => false;

        public Config.TableConf TableConf { get; protected set; }

        internal abstract Storage Open(Application app, Database database);
        internal abstract void Close();

        internal virtual Task<int> ReduceShare(Reduce rpc, ByteBuffer bbkey)
        {
            throw new NotImplementedException();
        }

        internal virtual Task<int> ReduceInvalid(Reduce rpc, ByteBuffer bbkey)
        {
            throw new NotImplementedException();
        }

        internal virtual Task<int> ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
            throw new NotImplementedException();
        }

        public ChangeListenerMap ChangeListenerMap { get; } = new ChangeListenerMap();

        public abstract Storage Storage { get; }
        internal abstract Database.TableAsync OldTable { get; set; }

        public abstract bool IsNew { get; }
        public abstract ByteBuffer EncodeKey(object key);
        public abstract object DecodeObjectKey(ByteBuffer bb);
        public abstract Bean NewBeanValue();
        public abstract Task RemoveAsync(Binary encodedKey);
    }

    public abstract class Table<K, V> : Table where V : Bean, new()
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(Table));

        public Table(string name) : base(name)
        {
        }

        protected Zeze.Services.ServiceManager.Agent.AutoKey AutoKey { get; private set;  }

        private async Task<Record<K, V>> LoadAsync(K key)
        {
            var tkey = new TableKey(Id, key);
            while (true)
            {
                Record<K, V> r = Cache.GetOrAdd(key, (key) => new Record<K, V>(this, key, null));
                var lockr = await r.Mutex.AcquireAsync(CancellationToken.None);
                try
                {
                    if (r.State == GlobalCacheManagerServer.StateRemoved)
                        continue; // 正在被删除，重新 GetOrAdd 一次。以后 _lock_check_ 里面会再次检查这个状态。

                    if (r.State == GlobalCacheManagerServer.StateShare
                        || r.State == GlobalCacheManagerServer.StateModify)
                    {
                        return r;
                    }

                    var (ResultCode, ResultState) = await r.Acquire(GlobalCacheManagerServer.StateShare, false);
                    r.State = ResultState;
                    if (r.State == GlobalCacheManagerServer.StateInvalid)
                    {
                        var txn = Transaction.Current;
                        txn.ThrowRedoAndReleaseLock(tkey.ToString() + ":" + r.ToString());
                        //throw new RedoAndReleaseLockException();
                    }

                    r.Timestamp = Record.NextTimestamp;
                    r.SetFreshAcquire();

                    if (null != TStorage)
                    {
#if ENABLE_STATISTICS
                        TableStatistics.Instance.GetOrAdd(Id).StorageFindCount.IncrementAndGet();
#endif
                        r.Value = await TStorage.FindAsync(key, this); // r.Value still maybe null

                        // 【注意】这个变量不管 OldTable 中是否存在的情况。
                        //r.ExistInBackDatabase = null != r.Value;

                        // 当记录删除时需要同步删除 OldTable，否则下一次又会从 OldTable 中找到。
                        // see Record1.Flush
                        if (null == r.Value && null != OldTable)
                        {
                            var encodedKey = EncodeKey(key);
                            var old = await OldTable.FindAsync(encodedKey);
                            if (null != old)
                            {
                                r.Value = DecodeValue(old);
                                // 从旧表装载时，马上设为脏，使得可以写入新表。
                                // 需要马上保存，否则，直到这个记录被访问才有机会保存。
                                r.SetDirty();
                                // Immediately 需要特别在此单独处理。
                                if (Zeze.Checkpoint.CheckpointMode == CheckpointMode.Immediately)
                                {
                                    var t = OldTable.Database.BeginTransaction().ITransaction;
                                    try
                                    {
                                        await OldTable.ReplaceAsync(t, encodedKey, old);
                                        t.Commit();
                                    }
                                    catch (Exception)
                                    {
                                        t.Rollback();
                                        throw;
                                    }
                                    finally
                                    {
                                        t.Dispose();
                                    }
                                }
                            }
                        }
                        if (null != r.Value)
                        {
                            r.Value.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
                        }
                    }
                    logger.Debug("FindInCacheOrStorage {0}", r);
                }
                finally
                {
                    lockr.Dispose();
                }

                return r;
            }
        }

        internal override async Task<int> ReduceShare(Reduce rpc, ByteBuffer bbkey)
        {
            var fresh = rpc.ResultCode;
            rpc.ResultCode = 0;

            logger.Debug("ReduceShare NewState={0}", rpc.Argument.State);

            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
            rpc.Result.State = rpc.Argument.State;

            K key = DecodeKey(bbkey);

            //logger.Debug("Reduce NewState={0}", rpc.Argument.State);

            var tkey = new TableKey(Id, key);

            Record<K, V> r = null;
            var lockey = await Zeze.Locks.Get(tkey).WriterLockAsync();
            try
            {
                r = Cache.Get(key);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                    logger.Debug("ReduceShare SendResult 1 {0}", r);
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
                    return 0;
                }
                using var lockr = await r.Mutex.AcquireAsync(CancellationToken.None);
                if (fresh != GlobalCacheManagerServer.AcquireFreshSource && r.IsFreshAcquire())
                {
                    logger.Debug("Reduce SendResult fresh {}", r);
                    rpc.Result.State = GlobalCacheManagerServer.StateReduceErrorFreshAcquire;
                    rpc.SendResult();
                    return 0;
                }
                r.SetNotFresh(); // 被降级不再新鲜。
                switch (r.State)
                {
                    case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
                    case GlobalCacheManagerServer.StateInvalid:
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        logger.Debug("ReduceShare SendResult 2 {0}", r);
                        rpc.SendResultCode(GlobalCacheManagerServer.ReduceShareAlreadyIsInvalid);
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        rpc.Result.State = GlobalCacheManagerServer.StateShare;
                        rpc.ResultCode = GlobalCacheManagerServer.ReduceShareAlreadyIsShare;
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceShare SendResult 3 {0}", r);
                        rpc.SendResult();
                        return 0;

                    case GlobalCacheManagerServer.StateModify:
                        r.State = GlobalCacheManagerServer.StateShare; // 马上修改状态。事务如果要写会再次请求提升(Acquire)。
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceShare SendResult * {0}", r);
                        rpc.SendResult();
                        return 0;
                }
                rpc.Result.State = GlobalCacheManagerServer.StateShare;
                await FlushWhenReduce(r);
                logger.Debug("ReduceShare SendResult 4 {0}", r);
                rpc.SendResult();
            }
            finally
            {
                lockey.Release();
            }
            return 0;
        }

        private async Task FlushWhenReduce(Record r)
        {
            switch (Zeze.Config.CheckpointMode)
            {
                case CheckpointMode.Period:
                    //Zeze.Checkpoint.AddActionAndPulse(after);
                    //break;
                    throw new InvalidOperationException("Reduce Can Not Work With CheckpointMode.Period.");

                case CheckpointMode.Immediately:
                    break;

                case CheckpointMode.Table:
                    await RelativeRecordSet.FlushWhenReduce(r);
                    break;
            }
        }

        internal override async Task<int> ReduceInvalid(Reduce rpc, ByteBuffer bbkey)
        {
            var fresh = rpc.ResultCode;
            rpc.ResultCode = 0;

            logger.Debug("ReduceInvalid NewState={0}", rpc.Argument.State);

            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
            rpc.Result.State = rpc.Argument.State;

            K key = DecodeKey(bbkey);

            var tkey = new TableKey(Id, key);
            Record<K, V> r = null;
            var lockey = await Zeze.Locks.Get(tkey).WriterLockAsync();
            try
            {
                r = Cache.Get(key);
                if (null == r)
                {
                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                    logger.Debug("ReduceInvalid SendResult 1 {0}", r);
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid);
                    return 0;
                }
                using var lockr = await r.Mutex.AcquireAsync(CancellationToken.None);
                if (fresh != GlobalCacheManagerServer.AcquireFreshSource && r.IsFreshAcquire())
                {
                    logger.Debug("Reduce SendResult fresh {}", r);
                    rpc.Result.State = GlobalCacheManagerServer.StateReduceErrorFreshAcquire;
                    rpc.SendResult();
                    return 0;
                }
                r.SetNotFresh(); // 被降级不再新鲜。
                switch (r.State)
                {
                    case GlobalCacheManagerServer.StateRemoved: // impossible! safe only.
                    case GlobalCacheManagerServer.StateInvalid:
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        rpc.ResultCode = GlobalCacheManagerServer.ReduceInvalidAlreadyIsInvalid;
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceInvalid SendResult 2 {0}", r);
                        rpc.SendResult();
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        r.State = GlobalCacheManagerServer.StateInvalid;
                        // 不删除记录，让TableCache.CleanNow处理。 
                        if (r.Dirty)
                            break;
                        logger.Debug("ReduceInvalid SendResult 3 {0}", r);
                        rpc.SendResult();
                        return 0;

                    case GlobalCacheManagerServer.StateModify:
                        r.State = GlobalCacheManagerServer.StateInvalid;
                        if (r.Dirty)
                            break;
                        rpc.SendResult();
                        return 0;
                }
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                await FlushWhenReduce(r);
                logger.Debug("ReduceInvalid SendResult 4 {0} ", r);
                rpc.SendResult();
            }
            finally
            {
                lockey.Release();
            }
            return 0;
        }

        public Binary EncodeGlobalKey(K key)
        { 
            var bb = ByteBuffer.Allocate();
            bb.WriteInt4(Id);
            // 避免bbkey的拷贝需要修改EncodeKey定义。这个对象很小，暂时先这样。
            var bbkey = EncodeKey(key);
            bb.Append(bbkey.Bytes, bbkey.ReadIndex, bbkey.Size);
            return new Binary(bb);
        }

        internal override async Task<int> ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex)
        {
            var remain = new List<(TableKey, Record<K, V>)>(Cache.DataMap.Count);
            foreach (var e in Cache.DataMap)
            {
                var gkey = EncodeGlobalKey(e.Key);
                if (Zeze.GlobalAgent.GetGlobalCacheManagerHashIndex(gkey) != GlobalCacheManagerHashIndex)
                {
                    // 不是断开连接的GlobalCacheManager。跳过。
                    continue;
                }

                var tkey = new TableKey(Id, e.Key);
                var lockey = Zeze.Locks.Get(tkey);
                if (false == lockey.TryEnterWriteLock())
                {
                    remain.Add((tkey, e.Value));
                    continue;
                }
                try
                {
                    using (var holder = await e.Value.Mutex.TryAcquireAsync(TimeSpan.Zero, CancellationToken.None))
                    {
                        if (holder.IsEmpty)
                        { 
                            remain.Add((tkey, e.Value));
                        }
                        else
                        {
                            // 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
                            e.Value.State = GlobalCacheManagerServer.StateInvalid;
                            await FlushWhenReduce(e.Value);
                        }
                    }
                }
                finally
                {
                    lockey.Dispose();
                }
            }
            if (remain.Count > 0)
            {
                foreach (var e in remain)
                {
                    var lockey = Zeze.Locks.Get(e.Item1);
                    await lockey.WriterLockAsync();
                    try
                    {
                        using (await e.Item2.Mutex.AcquireAsync(CancellationToken.None))
                        {
                            // 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
                            e.Item2.State = GlobalCacheManagerServer.StateInvalid;
                            await FlushWhenReduce(e.Item2);
                        }
                    }
                    finally
                    {
                        lockey.Dispose();
                    }
                }
            }
            /*
            while (remain.Count > 0)
            {
                var remain2 = new List<(TableKey, Record<K, V>)>(remain.Count);
                foreach (var e in remain)
                {
                    var lockey = Zeze.Locks.Get(e.Item1);
                    if (false == lockey.TryEnterWriteLock())
                    {
                        remain2.Add(e);
                        continue;
                    }
                    try
                    {
                        using (await e.Item2.Mutex.LockAsync()) // TODO TryLock Need
                        {
                            // 只是需要设置Invalid，放弃资源，后面的所有访问都需要重新获取。
                            e.Item2.State = GlobalCacheManagerServer.StateInvalid;
                            await FlushWhenReduce(e.Item2);
                        }
                    }
                    finally
                    {
                        lockey.Dispose();
                    }
                }
                remain = remain2;
            }
            */
            return 0;
        }

        public async Task<bool> ContainsKey(K key)
        {
            return await GetAsync(key) != null;
        }

        public async Task<V> GetAsync(K key)
        {
            var currentT = Transaction.Current;
            var tkey = new TableKey(Id, key);

            var cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                return (V)cr.NewestValue();
            }

            var r = await LoadAsync(key);
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
            return r.ValueTyped;
        }

        public async Task<V> GetOrAddAsync(K key)
        {
            var currentT = Transaction.Current;
            var tkey = new TableKey(Id, key);

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
                var r = await LoadAsync(key);
                cr = new Transaction.RecordAccessed(r);
                currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);

                if (null != r.Value)
                    return r.ValueTyped;
                // add
            }

            V add = NewValue();
            add.InitRootInfo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, add);
            return add;
        }

        public async Task<bool> TryAddAsync(K key, V value)
        {
            if (null != await GetAsync(key))
                return false;

            var currentT = Transaction.Current;
            var tkey = new TableKey(Id, key);
            var cr = currentT.GetRecordAccessed(tkey);
            value.InitRootInfoWithRedo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, value);
            return true;
        }

        public async Task InsertAsync(K key, V value)
        {
            if (false == await TryAddAsync(key, value))
                throw new ArgumentException($"table:{GetType().FullName} insert key:{key} exists");
        }

        public async Task PutAsync(K key, V value)
        {
            var currentT = Transaction.Current;
            var tkey = new TableKey(Id, key);

            var cr = currentT.GetRecordAccessed(tkey);
            if (cr == null)
            {
                var r = await LoadAsync(key);
                cr = new Transaction.RecordAccessed(r);
                currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
            }
            value.InitRootInfoWithRedo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, value);
        }

        public override async Task RemoveAsync(Binary encodedKey)
        {
            var key = DecodeKey(ByteBuffer.Wrap(encodedKey));
            await RemoveAsync(key);
        }

        // 几乎和Put一样，还是独立开吧。
        public async Task RemoveAsync(K key)
        {
            var currentT = Transaction.Current;
            var tkey = new TableKey(Id, key);

            var cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                cr.Put(currentT, null);
                return;
            }

            var r = await LoadAsync(key);
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

        override internal Database.TableAsync OldTable { get; set; }
        internal Storage<K, V> TStorage { get; private set; }
        public Database Database { get; private set; }
        public override Storage Storage => TStorage;

        internal override Storage Open(Application app, Database database)
        {
            if (null != TStorage)
                throw new Exception("table has opened." + Name);
            Zeze = app;
            Database = database;
            if (this.IsAutoKey)
                AutoKey = app.ServiceManager.GetAutoKey(Name);

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
            Cache?.Close();
        }

        // Key 都是简单变量，系列化方法都不一样，需要生成。
        public abstract ByteBuffer EncodeKey(K key);
        public abstract K DecodeKey(ByteBuffer bb);

        public override object DecodeObjectKey(ByteBuffer bb)
        {
            return DecodeKey(bb);
        }

        public override Bean NewBeanValue()
        {
            return NewValue();
        }

        public async Task DelayRemoveAsync(K key)
        {
            await Zeze.DelayRemove.RemoveAsync(this, key);
        }

        public override ByteBuffer EncodeKey(object key)
        { 
            return EncodeKey((K)key);
        }

        public V NewValue()
        {
            return new V();
        }

        public ByteBuffer EncodeValue(V value)
        {
            var bb = ByteBuffer.Allocate(value.CapacityHintOfByteBuffer);
            value.Encode(bb);
            return bb;
        }
 
        /// <summary>
        /// 解码系列化的数据到对象。
        /// </summary>
        /// <param name="bb">bean encoded data</param>
        /// <returns></returns>
        public V DecodeValue(ByteBuffer bb)
        {
            V value = NewValue();
            value.Decode(bb);
            return value;
        }

        /// <summary>
        /// 事务外调用
        /// 遍历表格。能看到记录的最新数据。
        /// 【注意】这里看不到新增的但没有Checkpoint.Flush的记录。实现这个有点麻烦。
        /// 【并发】每个记录回调时加读锁，回调完成马上释放。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public async Task<long> WalkAsync(Func<K, V, bool> callback)
        {
            return await WalkAsync(callback, null);
        }

        private bool InvokeCallback(byte[] key, byte[] value, Func<K, V, bool> callback)
        {
            K k = DecodeKey(ByteBuffer.Wrap(key));
            var tkey = new TableKey(Id, k);
            var lockey = Zeze.Locks.Get(tkey);
            lockey.EnterReadLock();
            try
            {
                var r = Cache.Get(k);
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
            }
            finally
            {
                lockey.Release();
            }
            // 缓存中不存在或者正在被删除，使用数据库中的数据。
            // 不需要锁。提前释放。
            V v = DecodeValue(ByteBuffer.Wrap(value));
            return callback(k, v);
        }

        public async Task<long> WalkAsync(Func<K, V, bool> callback, Action actionNotLock)
        {
            if (Transaction.Current != null)
            {
                throw new Exception("must be called without transaction");
            }
            return await TStorage.TableAsync.WalkAsync(
                (key, value) => 
                {
                    if (InvokeCallback(key, value, callback))
                    {
                        actionNotLock?.Invoke();
                        return true;
                    }
                    return false;
                });
        }

        public long WalkCache(Func<K, V, bool> callback)
        {
            return WalkCache(callback, null);
        }

        /**
         * 事务外调用
         * 遍历缓存
         * @return count
         */
        public long WalkCache(Func<K, V, bool> callback, Action afterNotLock)
        {
            if (Transaction.Current != null)
            {
                throw new Exception("must be called without transaction");
            }
            long count = 0;
            foreach (var e in Cache.DataMap)
            {
                var tkey = new TableKey(Id, e.Key);
                var lockey = Zeze.Locks.Get(tkey);
                lockey.EnterReadLock();
                try
                {
                    var r = e.Value;
                    if (r.State == GlobalCacheManagerServer.StateShare || r.State == GlobalCacheManagerServer.StateModify)
                    {
                        if (r.Value == null)
                            continue; // deleted

                        count++;
                        if (false == callback(r.Key, r.ValueTyped))
                            break; // user break
                    }
                }
                finally
                {
                    lockey.Release();
                }
                afterNotLock?.Invoke();
            }
            return count;
        }

        /// <summary>
        /// 遍历数据库中的表。看不到本地缓存中的数据。
        /// 【并发】后台数据库处理并发。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public async Task<long> WalkDatabaseAsync(Func<byte[], byte[], bool> callback)
        {
            return await TStorage.TableAsync.WalkAsync(callback);
        }

        /// <summary>
        /// 遍历数据库中的表。看不到本地缓存中的数据。
        /// 【并发】后台数据库处理并发。
        /// </summary>
        /// <param name="callback"></param>
        /// <returns></returns>
        public async Task<long> WalkDatabaseAsync(Func<K, V, bool> callback)
        {
            return await TStorage.TableAsync.WalkAsync(
                (key, value) =>
                {
                    K k = DecodeKey(ByteBuffer.Wrap(key));
                    V v = DecodeValue(ByteBuffer.Wrap(value));
                    return callback(k, v);
                });
        }

        public long WalkCacheKey(Func<K, bool> callback)
        {
            return Cache.WalkKey(callback);
        }

        public async Task<long> WalkCacheKeyAsync(Func<K, Task<bool>> callback)
        {
            return await Cache.WalkKeyAsync(callback);
        }

        public async Task<long> WalkDatabaseKeyAsync(Func<K, bool> callback)
        {
            return await TStorage.TableAsync.WalkKeyAsync((key) =>
            {
                K k = DecodeKey(ByteBuffer.Wrap(key));
                return callback(k);
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
        public async Task<V> SelectCopyAsync(K key)
        {
            var tkey = new TableKey(Id, key);
            Transaction currentT = Transaction.Current;
            if (null != currentT)
            {
                Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
                if (null != cr)
                {
                    return (V)cr.NewestValue()?.Copy();
                }
                currentT.SetAlwaysReleaseLockWhenRedo();
            }

            var lockey = await Zeze.Locks.Get(tkey).ReaderLockAsync();
            try
            {
                var r = await LoadAsync(key);
                return (V)r.Value.Copy();
            }
            finally
            {
                lockey.Release();
            }
        }

        public async Task<V> SelectDirtyAsync(K key)
        {
            var tkey = new TableKey(Id, key);
            Transaction currentT = Transaction.Current;
            if (null != currentT)
            {
                Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
                if (null != cr)
                {
                    return (V)cr.NewestValue();
                }
            }
            return (V)(await LoadAsync(key)).Value;
        }

        public override bool IsNew => TStorage == null || TStorage.TableAsync.IsNew;

        /// <summary>
        /// 这个方法用来编码服务器的ChangeListener，
        /// 客户端解码参见class ChangesRecord。
        /// </summary>
        /// <param name="specialName">指定一个名字，当它为null时，直接使用当前的名字</param>
        /// <param name="key"></param>
        /// <param name="r"></param>
        /// <returns></returns>
        public ByteBuffer EncodeChangeListenerWithSpecialName(string specialName, object key, Changes.Record r)
        {
            var bb = ByteBuffer.Allocate();
            bb.WriteString(null == specialName ? Name : specialName);
            bb.WriteByteBuffer(EncodeKey(key));
            r.Encode(bb);
            return bb;
        }
    }
}
