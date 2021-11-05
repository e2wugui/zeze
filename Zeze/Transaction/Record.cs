using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
using Zeze.Services;
using Zeze.Services.GlobalCacheManager;

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

        internal long Timestamp { get; set; }

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

        internal Bean Value { get; set; }
        internal int State { get; set; }

        public abstract Table Table { get; }

        internal RelativeRecordSet RelativeRecordSet { get; set; } = new RelativeRecordSet();

        public Record(Bean value)
        {
            State = GlobalCacheManagerServer.StateInvalid;
            Value = value;
            //Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 初始化
        }

        // 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
        // 0 保留给不存在记录的的时戳。
        private static global::Zeze.Util.AtomicLong _TimestampGen = new global::Zeze.Util.AtomicLong();
        protected static global::Zeze.Util.AtomicLong _ExistInDbGen = new global::Zeze.Util.AtomicLong();
        internal static long NextTimestamp => _TimestampGen.IncrementAndGet();

        internal abstract void Commit(Transaction.RecordAccessed accessed);

        internal abstract int Acquire(int state);

        internal abstract void Encode0();
        internal abstract bool Flush(Database.Transaction t);
        internal abstract void Cleanup();

        internal Database.Transaction DatabaseTransactionTmp { get; set; }
        internal abstract void SetDirty();
        internal abstract void SetExistInBackDatabase(long timestamp, bool value);
    }

    public class Record<K, V> : Record where V : Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
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

        internal override int Acquire(int state)
        {
            if (null == TTable.TStorage)
                return state; // 不支持内存表cache同步。

            var gkey = new GlobalTableKey(TTable.Name, TTable.EncodeKey(Key));
            logger.Debug("Acquire NewState={0} {1}", state, this);
#if ENABLE_STATISTICS
            var stat = TableStatistics.Instance.GetOrAdd(TTable.Name);
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
            return TTable.Zeze.GlobalAgent.Acquire(gkey, state);
        }

        internal long SavedTimestampForCheckpointPeriod { get; set; }
        internal bool ExistInBackDatabase { get; set; }
        internal bool ExistInBackDatabaseSavedForFlushRemove { get; set; }

        internal override void Commit(Transaction.RecordAccessed accessed)
        {
            if (null != accessed.CommittedPutLog)
            {
                Value = accessed.CommittedPutLog.Value;
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
                    // do nothing
                    break;
            }
        }

        private ByteBuffer snapshotKey;
        private ByteBuffer snapshotValue;

        internal bool TryEncodeN(
            ConcurrentDictionary<K, Record<K, V>> changed,
            ConcurrentDictionary<K, Record<K, V>> encoded)
        {
            Lockey lockey = TTable.Zeze.Locks.Get(new TableKey(TTable.Name, Key));
            if (false == lockey.TryEnterReadLock(0))
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
                lockey.ExitReadLock();
            }
        }

        internal override void Encode0()
        {
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
            ExistInBackDatabaseSavedForFlushRemove = ExistInBackDatabase;
            ExistInBackDatabase = null != snapshotValue;
        }

        /*
        internal void Snapshot()
        {

        }
        */

        internal override bool Flush(Database.Transaction t)
        {
            if (null != snapshotValue)
            {
                // changed
                Table.Storage?.DatabaseTable.Replace(t, snapshotKey, snapshotValue);
            }
            else
            {
                // removed
                if (ExistInBackDatabaseSavedForFlushRemove) // 优化，仅在后台db存在时才去删除。
                    Table.Storage?.DatabaseTable.Remove(t, snapshotKey);

                // 需要同步删除OldTable，否则下一次查找又会找到。
                // 这个违背了OldTable不修改的原则，但没办法了。
                // XXX 从旧表中删除，使用独立临时事务。
                // 如果要纳入完整事务，有点麻烦。这里反正是个例外，那就再例外一次了。
                if (null != TTable.OldTable)
                {
                    var transTmp = TTable.OldTable.Database.BeginTransaction();
                    TTable.OldTable.Remove(transTmp, snapshotKey);
                    transTmp.Commit();
                }
            }
            return true;
        }

        internal override void Cleanup()
        {
            this.DatabaseTransactionTmp = null;

            if (TTable.Zeze.Checkpoint.CheckpointMode == CheckpointMode.Period)
            {
                var tkey = new TableKey(Table.Name, Key);
                var lockey = TTable.Zeze.Locks.Get(tkey);
                lockey.EnterWriteLock();
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
                    lockey.ExitWriteLock();
                }
            }
            // CheckpointMode.Table
            snapshotKey = null;
            snapshotValue = null;
        }

        private long ExistInBackDatabaseModifyTimestamp;

        internal override void SetExistInBackDatabase(long timestamp, bool value)
        {
            var tkey = new TableKey(Table.Name, Key);
            var lockey = TTable.Zeze.Locks.Get(tkey);
            lockey.EnterWriteLock();
            try
            {
                if (timestamp < ExistInBackDatabaseModifyTimestamp)
                    return;
                ExistInBackDatabaseModifyTimestamp = timestamp;
                ExistInBackDatabase = value;
            }
            finally
            {
                lockey.ExitWriteLock();
            }
        }

        public ConcurrentDictionary<K, Record<K, V>> LruNode { get; set; }
    }
}
