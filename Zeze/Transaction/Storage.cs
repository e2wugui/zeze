﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using System.Threading.Channels;
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
	}

    public class Storage<K, V> : Storage where V : Bean, new()
    {
        private Database.Table databaseTable;
        public Table Table { get; }
        public Database.Table DatabaseTable => databaseTable;


        public Storage(Table<K, V> table, Database database, string tableName)
        {
            Table = table;
            databaseTable = new DatabaseMemory.TableMemory();
        }

        private ConcurrentDictionary<K, Record<K, V>> changed = new ConcurrentDictionary<K, Record<K, V>>();
        private ConcurrentDictionary<K, Record<K, V>> encoded = new ConcurrentDictionary<K, Record<K, V>>();
        private ConcurrentDictionary<K, Record<K, V>> snapshot = new ConcurrentDictionary<K, Record<K, V>>();
        private System.Threading.ReaderWriterLockSlim snapshotLock = new System.Threading.ReaderWriterLockSlim();

        public int EncodeN()
        {
            HashSet<K> removed = new HashSet<K>();
            foreach (var e in changed)
            {
                if (e.Value.TryEncodeN())
                {
                    encoded[e.Key] = e.Value;
                    removed.Add(e.Key);
                }
            }
            foreach (var e in removed)
            {
                changed.TryRemove(e, out var notused);
            }
            return removed.Count;
        }

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
                    value = r.FindSnapshot();
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
    }
}
