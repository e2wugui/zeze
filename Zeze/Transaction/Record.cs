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
        internal long Timestamp { get; set; }
        internal Bean Value { get; set; }
        internal int State { get; set; }
        internal long AccessTimeTicks { get; set; } // see TableCache 没有加锁。volatile

        public Record(Bean value)
        {
            State = GlobalCacheManager.StateInvalid;
            Value = value;
            Timestamp = NextTimestamp;
        }

        // 时戳生成器，运行时状态，需要持久化时，再考虑保存到数据库。
        // 0 保留给不存在记录的的时戳。
        private static global::Zeze.Util.AtomicLong _TimestampGen = new global::Zeze.Util.AtomicLong();
        internal static long NextTimestamp => _TimestampGen.IncrementAndGet();

        internal abstract void Commit(Transaction.RecordAccessed accessed);

        internal abstract void Acquire(int state);
    }

    public class Record<K, V> : Record where V : Bean, new()
    {
        public K Key { get; }
        public Table<K, V> Table { get;  }
        public V ValueTyped => (V)Value;
   
        public Record(Table<K, V> table, K key, V value) : base(value)
        {
            this.Table = table;
            this.Key = key;
        }

        internal override void Acquire(int state)
        {
            GlobalTableKey gkey = new GlobalTableKey(Table.Name, Table.EncodeKey(Key));
            GlobalAgent.Instance.Acquire(gkey, state);
            State = state;
        }

        // XXX 临时写个实现，以后调整。
        internal override void Commit(Transaction.RecordAccessed accessed)
        {
            if (null != accessed.CommittedPutLog)
            {
                Value = accessed.CommittedPutLog.Value;
            }
            Timestamp = NextTimestamp;
            Table.Storage?.OnRecordChanged(this);
        }

        private ByteBuffer snapshotKey;
        private ByteBuffer snapshotValue;

        internal bool TryEncodeN(ConcurrentDictionary<K, Record<K, V>> changed, ConcurrentDictionary<K, Record<K, V>> encoded)
        {
            Lockey lockey = Locks.Instance.Get(new TableKey(Table.Id, Key));
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

        internal void Encode0()
        {
            snapshotKey = Table.EncodeKey(Key);
            snapshotValue = Value != null ? Table.EncodeValue(ValueTyped) : null;
        }

        /*
        internal void Snapshot()
        {

        }
        */

        internal bool Flush(Storage<K, V> storage)
        {
            if (null != snapshotValue)
            {
                storage.DatabaseTable.Replace(snapshotKey, snapshotValue);
            }
            else
            {
                storage.DatabaseTable.Remove(snapshotKey);
            }
            return true;
        }

        internal void Cleanup()
        {
            snapshotKey = null;
            snapshotValue = null;
        }

        internal ByteBuffer FindSnapshot()
        {
            return snapshotValue;
        }
    }
}
