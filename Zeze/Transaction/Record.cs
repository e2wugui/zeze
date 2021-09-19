using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
using Zeze.Services;

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
            State = GlobalCacheManager.StateInvalid;
            Value = value;
            //Timestamp = NextTimestamp; // Table.FindInCacheOrStorage 初始化
        }

        // 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
        // 0 保留给不存在记录的的时戳。
        private static global::Zeze.Util.AtomicLong _TimestampGen = new global::Zeze.Util.AtomicLong();
        internal static long NextTimestamp => _TimestampGen.IncrementAndGet();

        internal abstract void Commit(Transaction.RecordAccessed accessed);

        internal abstract int Acquire(int state);

        internal abstract void Encode0();
        internal abstract bool Flush(Database.Table table, Database.Transaction t);
        internal abstract void Cleanup();

        internal Database.Transaction DatabaseTransactionTmp { get; set; }
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
            return $"T {TTable.Id}:{TTable.Name} K {Key} S {State} T {Timestamp}";// V {Value}";
            // 记录的log可能在Transaction.AddRecordAccessed之前进行，不能再访问了。
        }

        internal override int Acquire(int state)
        {
            if (null == TTable.TStorage)
                return state; // 不支持内存表cache同步。

            GlobalCacheManager.GlobalTableKey gkey = new GlobalCacheManager.GlobalTableKey(TTable.Name, TTable.EncodeKey(Key));
            logger.Debug("Acquire NewState={0} {1}", state, this);
#if ENABLE_STATISTICS
            var stat = TableStatistics.Instance.GetOrAdd(TTable.Id);
            switch (state)
            {
                case GlobalCacheManager.StateInvalid:
                    stat.GlobalAcquireInvalid.IncrementAndGet();
                    break;

                case GlobalCacheManager.StateShare:
                    stat.GlobalAcquireShare.IncrementAndGet();
                    break;

                case GlobalCacheManager.StateModify:
                    stat.GlobalAcquireModify.IncrementAndGet();
                    break;
            }
#endif
            return TTable.Zeze.GlobalAgent.Acquire(gkey, state);
        }

        internal long SavedTimestampForCheckpointPeriod { get; set; }
        internal bool ExistInBackDatabase { get; set; }

        internal override void Commit(Transaction.RecordAccessed accessed)
        {
            if (null != accessed.CommittedPutLog)
            {
                Value = accessed.CommittedPutLog.Value;
            }
            Timestamp = NextTimestamp; // 必须在 Value = 之后设置。防止出现新的事务得到新的Timestamp，但是数据时旧的。

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

        internal bool TryEncodeN(ConcurrentDictionary<K, Record<K, V>> changed, ConcurrentDictionary<K, Record<K, V>> encoded)
        {
            Lockey lockey = Locks.Instance.Get(new TableKey(TTable.Id, Key));
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
            snapshotKey = TTable.EncodeKey(Key);
            snapshotValue = Value != null ? TTable.EncodeValue(ValueTyped) : null;
        }

        /*
        internal void Snapshot()
        {

        }
        */

        internal override bool Flush(Database.Table table, Database.Transaction t)
        {
            if (null != snapshotValue)
            {
                // changed
                table.Replace(t, snapshotKey, snapshotValue);
            }
            else
            {
                // removed
                if (ExistInBackDatabase) // 优化，仅在后台db存在时才去删除。
                    table.Remove(t, snapshotKey);
            }
            return true;
        }

        internal override void Cleanup()
        {
            TableKey tkey = new TableKey(Table.Id, Key);
            Lockey lockey = Locks.Instance.Get(tkey);
            lockey.EnterWriteLock();
            try
            {
                if (SavedTimestampForCheckpointPeriod == base.Timestamp)
                    Dirty = false;

                // ExistInBackDatabase = null != snapshotValue;
                // 修改很少，下面这样会更快？
                if (null != snapshotValue)
                {
                    // replace
                    if (false == ExistInBackDatabase)
                        ExistInBackDatabase = true;
                }
                else
                {
                    // remove
                    if (ExistInBackDatabase)
                        ExistInBackDatabase = false;
                }
            }
            finally
            {
                lockey.ExitWriteLock();
            }

            snapshotKey = null;
            snapshotValue = null;
        }

        public ConcurrentDictionary<K, Record<K, V>> LruNode { get; set; }
    }
}
