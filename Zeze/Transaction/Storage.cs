using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    public interface Storage
    {
		public Database.Table DatabaseTable { get; }

		public int EncodeN();

		public int Encode0();

		public int Snapshot();

		public int Flush();

		public void Cleanup();

        public void Close();
	}

    public sealed class Storage<K, V> : Storage where V : Bean, new()
    {
        public Table Table { get; }
        public Database.Table DatabaseTable { get; }

        public Storage(Table<K, V> table, Database database, string tableName)
        {
            Table = table;
            DatabaseTable = database.OpenTable(tableName);
        }

        private ConcurrentDictionary<K, Record<K, V>> changed = new ConcurrentDictionary<K, Record<K, V>>();
        private ConcurrentDictionary<K, Record<K, V>> encoded = new ConcurrentDictionary<K, Record<K, V>>();
        private ConcurrentDictionary<K, Record<K, V>> snapshot = new ConcurrentDictionary<K, Record<K, V>>();
        private System.Threading.ReaderWriterLockSlim snapshotLock = new System.Threading.ReaderWriterLockSlim();

        internal void OnRecordChanged(Record<K, V> r)
        {
            changed[r.Key] = r;
        }

        internal bool IsRecordChanged(K key)
        {
            if (changed.TryGetValue(key, out var _))
                return true;
            if (encoded.TryGetValue(key, out var _))
                return true;
            return false;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用，同时只有一个线程执行。
        /// 没有得到任何锁。
        /// </summary>
        /// <returns></returns>
        public int EncodeN()
        {
            int c = 0;
            foreach (var e in changed)
            {
                if (e.Value.TryEncodeN(changed, encoded))
                    ++c;
            }
            return c;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
        /// </summary>
        /// <returns></returns>
        public int Encode0()
        {
            foreach (var e in changed)
            {
                e.Value.Encode0();
                encoded[e.Key] = e.Value;
            }
            int cc = changed.Count;
            changed.Clear();
            return cc;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
        /// </summary>
        /// <returns></returns>
        public int Snapshot()
        {
            var tmp = snapshot;
            snapshot = encoded;
            encoded = tmp;
            int cc = snapshot.Count;
            /*
            foreach (var e in snapshot)
            {
                e.Value.Snapshot();
            }
            */
            return cc;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用。
        /// 没有拥有任何锁。
        /// </summary>
        /// <returns></returns>
        public int Flush()
        {
            int count = 0;
            foreach (var e in snapshot)
            {
                if (e.Value.Flush(this))
                {
                    ++count;
                }
            }
            return count;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用。
        /// 没有拥有任何锁。
        /// </summary>
        public void Cleanup()
        {
            ConcurrentDictionary<K, Record<K, V>> tmp = null;
            snapshotLock.EnterWriteLock();
            try
            {
                tmp = snapshot;
                snapshot = new ConcurrentDictionary<K, Record<K, V>>();
            }
            finally
            {
                snapshotLock.ExitWriteLock();
            }

            foreach (var e in tmp)
            {
                e.Value.Cleanup();
            }
        }

        public V Find(K key, Table<K, V> table)
        {
            ByteBuffer value = null;
            bool foundInSnapshot = false;

            snapshotLock.EnterReadLock();
            try
            {
                Record<K, V> r;
                foundInSnapshot = snapshot.TryGetValue(key, out r);
                if (foundInSnapshot)
                {
                    value = r.FindSnapshot();
                }
            }
            finally
            {
                snapshotLock.ExitReadLock();
            }

            if (foundInSnapshot)
            {
                return null != value ? table.DecodeValue(value) : null;
            }

            value = DatabaseTable.Find(table.EncodeKey(key));
            return null != value ? table.DecodeValue(value) : null;
        }

        public void Close()
        {
            DatabaseTable.Close();
        }
    }
}
