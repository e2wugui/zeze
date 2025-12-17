using System.Collections.Generic;
using Zeze.Serialize;
using System.Collections.Concurrent;
using Zeze.Services;
using System.Threading.Tasks;
using System.Threading;
using DotNext.Threading;
using Zeze.Util;

namespace Zeze.Transaction
{
    public abstract class Record
    {
        public class RootInfo
        {
            public Record Record { get; }
            public TableKey TableKey { get; }

            public RootInfo(Record record, TableKey tableKey)
            {
                Record = record;
                TableKey = tableKey;
            }
        }

        public RootInfo CreateRootInfoIfNeed(TableKey tkey)
        {
            var cur = Value?.RootInfo;
            if (null == cur)
                cur = new RootInfo(this, tkey);
            return cur;
        }

        private long _Timestamp; // CS0677 volatile cannot apply to long
        internal long Timestamp
        {
            get { return Interlocked.Read(ref _Timestamp); }
            set { Interlocked.Exchange(ref _Timestamp, value); }
        }

        /// <summary>
        /// Record.Dirty 的问题
        /// 对于新的CheckpointMode，需要实现新的Dirty。
        /// CheckpointMode.Period
        /// Snapshot时记住timestamp，Cleanup的时候ClearDirty(snapshot_timestamp)，需要记录锁。
        /// CheckpointMode.Immediately
        /// Commit完成以后马上进行不需要锁的ClearDirty. (实际实现为根本不修改Dirty)
        /// CheckpointMode.Table
        /// Flush(rrs): foreach (r in rrs) r.ClearDirty 不需要锁。
        /// </summary>
        internal bool Dirty { get; set; } = false;
        private volatile Bean _Value;
        internal Bean Value
        {
            get { return _Value; }
            set { _Value = value; }
        }
        private volatile int _State;
        internal int State
        {
            get { return _State; }
            set { _State = value; }
        }

        public abstract Table Table { get; }
        private volatile RelativeRecordSet RelativeRecordSetPrivate = new();

        internal RelativeRecordSet RelativeRecordSet
        {
            get { return RelativeRecordSetPrivate; }
            set { RelativeRecordSetPrivate = value; }
        }

        public Record(Bean value)
        {
            State = GlobalCacheManagerServer.StateInvalid;
            Value = value;
            //Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 可能发生数据变化，这里初始化一次不够。
        }

        // 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
        // 0 保留给不存在记录的的时戳。
        private static readonly Util.AtomicLong TimestampGen = new();
        protected static readonly Util.AtomicLong ExistInDbGen = new();
        internal static long NextTimestamp => TimestampGen.IncrementAndGet();

        internal abstract void Commit(Transaction.RecordAccessed accessed);

        internal abstract Task<(long, int)> Acquire(int state, bool fresh);

        internal abstract void Encode0();
        internal abstract Task Flush(Database.ITransaction t, Dictionary<Database, Database.TransactionAsync> tss);
        internal abstract Task Flush(Database.ITransaction t);
        internal abstract Task Cleanup();

        internal Database.ITransaction DatabaseTransactionTmp { get; set; }
        internal Database.ITransaction DatabaseTransactionOldTmp { get; set; }
        internal abstract void SetDirty();
        internal AsyncLock Mutex = AsyncLock.Exclusive();

        internal bool fresh;
        private long acquireTime;

        public void SetNotFresh()
        {
            fresh = false;
        }

        public void SetFreshAcquire()
        {
            acquireTime = Util.Time.NowUnixMillis;
            fresh = true;
        }

        public bool IsFreshAcquire()
        {
            return fresh && Util.Time.NowUnixMillis - acquireTime < 1000;
        }
    }

    public class Record<K, V> : Record where V : Bean, new()
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(Record));

        public K Key { get; }
        public Table<K, V> TTable { get;  }
        public override Table Table => TTable;

        public V ValueTyped => (V)Value;
   
        public Record(Table<K, V> table, K key, V value) : base(value)
        {
            this.TTable = table;
            this.Key = key;
        }

        public override string ToString()
        {
            return $"T {TTable.Name} K {Key} S {State} T {Timestamp} Dirty {Dirty}";// V {Value}";
            // 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
        }

        internal async override Task<(long, int)> Acquire(int state, bool fresh)
        {
            if (null == TTable.TStorage || null == TTable.Zeze.GlobalAgent)
            {
                // 不支持内存表cache同步。
                return (0, state);
            }

            var gkey = TTable.EncodeGlobalKey(Key);
            logger.Debug("Acquire NewState={0} {1}", state, this);
#if ENABLE_STATISTICS
            var stat = TableStatistics.Instance.GetOrAdd(TTable.Id);
            switch (state)
            {
                case GlobalCacheManagerServer.StateInvalid:
                    stat.GlobalAcquireInvalid.IncrementAndGet();
                    break;

                case GlobalCacheManagerServer.StateShare:
                    stat.GlobalAcquireShare.IncrementAndGet();
                    break;

                case GlobalCacheManagerServer.StateModify:
                    stat.GlobalAcquireModify.IncrementAndGet();
                    break;
            }
#endif
            return await TTable.Zeze.GlobalAgent.Acquire(gkey, state, fresh);
        }

        internal long SavedTimestampForCheckpointPeriod { get; set; }
        //internal bool ExistInBackDatabase { get; set; }
        //internal bool ExistInBackDatabaseSavedForFlushRemove { get; set; }

        internal override void Commit(Transaction.RecordAccessed accessed)
        {
            if (null != accessed.CommittedPutLog)
            {
                Value = accessed.CommittedPutLog.Value;
                if (TTable.IsMemory && null == Value)
                {
                    TTable.Cache.Remove(KeyValuePair.Create(Key, this));
                    return; // 内存表已经删除，done
                }
            }
            Timestamp = NextTimestamp; // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。
            SetDirty();
        }

        internal override void SetDirty()
        {
            switch (TTable.Zeze.Checkpoint.CheckpointMode)
            {
                case CheckpointMode.Period:
                    Dirty = true;
                    TTable.TStorage?.OnRecordChanged(this);
                    break;
                case CheckpointMode.Table:
                    Dirty = true;
                    break;
                case CheckpointMode.Immediately:
                    // 立即模式需要马上保存到RocksCache中。在下面两个地方保存：
                    // 1. 在public void Flush(Iterable<Record> rs)流程中直接保存。
                    // 2. TableX.Load。
                    break;
            }
        }

        private ByteBuffer snapshotKey;
        private ByteBuffer snapshotValue;

        internal bool TryEncodeN(
            ConcurrentDictionary<K, Record<K, V>> changed,
            ConcurrentDictionary<K, Record<K, V>> encoded)
        {
            var lockey = TTable.Zeze.Locks.Get(new TableKey(TTable.Id, Key));
            if (false == lockey.TryEnterReadLock())
                return false;
            try
            {
                Encode0();
                encoded[Key] = this;
                changed.TryRemove(Key, out var _);
                return true;
            }
            finally
            {
                lockey.Release();
            }
        }

        internal override void Encode0()
        {
            if (false == Dirty)
                return;

            // Under Lock：this.TryEncodeN & Storage.Snapshot

            // 【注意】可能保存多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
            // 从 Storage.Snapshot 里面修改移到这里，避免Snapshot遍历，减少FlushWriteLock时间。
            SavedTimestampForCheckpointPeriod = Timestamp;

            // 可能编码多次：TryEncodeN 记录读锁；Snapshot FlushWriteLock;
            snapshotKey = TTable.EncodeKey(Key);
            snapshotValue = Value != null ? TTable.EncodeValue(ValueTyped) : null;

            // 【注意】
            // 这个标志本来应该在真正写到Database之后修改才是最合适的；
            // 但这样需要再次锁定记录写锁，并发效率比较低，增加Flush时间；
            // 由于Encode0()之后肯定会进行写Database操作，而写Database是不会并发的，
            // ExistInBackDatabase也仅在写Database操作使用，所以提前到这里修改；
            // 【并发简单分析】
            // 1) FindInCacheOrStorage
            //    第一次装载时，只会装载一次，记录读锁+lock(record)；
            // 2.1) CheckpointMode.Period
            //    a) TryEncodeN 记录读锁，看起来这个锁定是不够的，
            //       但是由于记录在TableCache中存在时，不会引起再次装载，
            //       所以实际上不会和FindInCacheOrStorage并发;
            //    b) Snapshot FlushWriteLock
            //       此时世界都暂停了，改一点状态完全没问题。
            // 2.2) CheckpointMode.Table
            //    rrs.lock()，使得Encode0()不会并发，其他理由同上面2.1)a)，
            //    这种模式也是可以直接修改的。
            //【ExistInBackDatabaseSavedForFlushRemove】
            //    由于这里提前修改，所以需要保存一个副本后面写Database时用。
            //    see this.Flush
            //ExistInBackDatabaseSavedForFlushRemove = ExistInBackDatabase;
            //ExistInBackDatabase = null != snapshotValue;
        }

        /*
        internal void Snapshot()
        {

        }
        */

        internal override async Task Flush(Database.ITransaction t, Dictionary<Database, Database.TransactionAsync> tss)
        {
            if (null != tss && null != TTable.OldTable)
            {
                // will clear in Cleanup.
                if (tss.TryGetValue(TTable.OldTable.Database, out var tmp))
                    DatabaseTransactionOldTmp = tmp.ITransaction;
            }
            await Flush(t);
        }

        internal override async Task Flush(Database.ITransaction t)
        {
            if (false == Dirty)
                return;

            if (null != snapshotValue)
            {
                // changed
                await Table.Storage?.TableAsync.ReplaceAsync(t, snapshotKey, snapshotValue);
            }
            else
            {
                // removed
                //if (ExistInBackDatabaseSavedForFlushRemove) // 优化，仅在后台db存在时才去删除。
                    await Table.Storage?.TableAsync.RemoveAsync(t, snapshotKey);

                // 需要同步删除OldTable，否则下一次查找又会找到。
                // 这个违背了OldTable不修改的原则，但没办法了。
                if (null != DatabaseTransactionOldTmp)
                {
                    await TTable.OldTable.RemoveAsync(DatabaseTransactionOldTmp, snapshotKey);
                }
            }
        }

        internal override async Task Cleanup()
        {
            DatabaseTransactionTmp = null;
            DatabaseTransactionOldTmp = null;

            if (TTable.Zeze.Checkpoint.CheckpointMode == CheckpointMode.Period)
            {
                var tkey = new TableKey(Table.Id, Key);
                var lockey = await TTable.Zeze.Locks.Get(tkey).WriterLockAsync();
                try
                {
                    if (SavedTimestampForCheckpointPeriod == base.Timestamp)
                    {
                        Dirty = false;
                    }
                    snapshotKey = null;
                    snapshotValue = null;
                    return;
                }
                finally
                {
                    lockey.Release();
                }
            }
            // CheckpointMode.Table
            snapshotKey = null;
            snapshotValue = null;
        }

        private volatile ConcurrentDictionary<K, Record<K, V>> _LruNode;
        public ConcurrentDictionary<K, Record<K, V>> LruNode
        { 
            get { return _LruNode; }
            set { _LruNode = value; }
        }
        
        public ConcurrentDictionary<K, Record<K, V>> GetAndSetLruNodeNull()
        {
            return Interlocked.Exchange(ref _LruNode, null);
        }

        public bool CompareAndSetLruNodeNull(ConcurrentDictionary<K, Record<K, V>> c)
        {
            return Interlocked.CompareExchange(ref _LruNode, null, c) == c;
        }
    }
}
