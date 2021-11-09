using System;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Serialize;
using Zeze.Util;
using RocksDbSharp;

namespace Zeze.Raft
{
    public class ConcurrentMaps
    {
        public abstract class Map
        {
            public string Name { get; }

            public Map(string name)
            {
                Name = name;
            }

            internal abstract void ResetLru();
            internal abstract void Close();
        }

        public class Map<K, V> : Map where V : Serializable, new()
        {
            public ConcurrentMaps Maps { get; }
            private ConcurrentLruLike<K, Record> Lru { get; set; }
            private ColumnFamilyHandle ColumnFamily { get; }
            public class Record
            {
                public byte[] EncodedKey { get; internal set; }
                public V Value { get; internal set; }
                public bool Removed { get; internal set; } = false;
                public bool Loaded { get; internal set; } = false;
            }

            public Map(
                ConcurrentMaps maps,
                string name,
                int capacity,
                int initialCapacity,
                int concurrencyLevel)
                : base(name)
            {
                Maps = maps;
                Lru = new ConcurrentLruLike<K, Record>(
                    capacity, LruTryRemoveCallback, 200, 2000,
                    initialCapacity, concurrencyLevel);
                ColumnFamily = Maps.Db.GetColumnFamily(Name);
            }

            internal override void Close()
            {
            }

            public V GetOrAdd(K key)
            {
                return GetOrAdd(key, (_) => new V());
            }

            public V GetOrAdd(K key, Func<K, V> factory)
            {
                Maps.rwLock.EnterReadLock();
                try
                {
                    while (true)
                    {
                        var r = Lru.GetOrAdd(key, (_) => new Record());
                        lock (r)
                        {
                            if (r.Removed)
                                continue;
                            TryLoad(key, r);
                            // 新增Record.Value保留初始值，不马上写入Db，
                            if (r.Value == null)
                                r.Value = factory(key);
                            return r.Value;
                        }
                    }
                }
                finally
                {
                    Maps.rwLock.ExitReadLock();
                }
            }

            public void Update(K key, Action<V> updator)
            {
                Maps.rwLock.EnterReadLock();
                try
                {
                    while (true)
                    {
                        var r = Lru.GetOrAdd(key, (_) => new Record());
                        lock (r)
                        {
                            if (r.Removed)
                                continue;
                            TryLoad(key, r);
                            // 新增Record.Value保留初始值，不马上写入Db，
                            if (r.Value == null)
                                r.Value = new V();
                            updator(r.Value);
                            var bb = EncodeValue(r.Value);
                            Maps.Db.Put(r.EncodedKey, r.EncodedKey.Length, bb.Bytes, bb.Size, ColumnFamily);
                            return;
                        }
                    }
                }
                finally
                {
                    Maps.rwLock.ExitReadLock();
                }
            }

            public bool Remove(K key)
            {
                return Remove(key, (v) => null != v);
            }

            // checker(value) value maybe null if not exist
            public bool Remove(K key, Func<V, bool> checker)
            {
                Maps.rwLock.EnterReadLock();
                try
                {
                    while (true)
                    {
                        var r = Lru.GetOrAdd(key, (_) => new Record());
                        lock (r)
                        {
                            if (r.Removed)
                                continue; // 这个是从Cache中删除的标志。
                            TryLoad(key, r);
                            if (checker(r.Value))
                            {
                                Maps.Db.Remove(r.EncodedKey, ColumnFamily);
                                r.Value = default(V);
                                r.Removed = true;
                                Lru.TryRemove(key, out _);
                                return true;
                            }
                            return false;
                        }
                    }
                }
                finally
                {
                    Maps.rwLock.ExitReadLock();
                }
            }

            private bool TryLoad(K key, Record r)
            {
                if (false == r.Loaded)
                {
                    r.EncodedKey = EncodeKey(key).Copy();
                    var valueBytes = Maps.Db.Get(r.EncodedKey, ColumnFamily);
                    r.Value = DecodeValue(valueBytes);
                    r.Loaded = true;
                    return true;
                }
                return false;
            }

            private ByteBuffer EncodeKey(K key)
            {
                var bb = ByteBuffer.Allocate();
                SerializeHelper<K>.Encode(bb, key);
                return bb;
            }

            private ByteBuffer EncodeValue(V v)
            {
                var bb = ByteBuffer.Allocate();
                v.Encode(bb);
                return bb;
            }

            private V DecodeValue(byte[] bytes)
            {
                if (null == bytes)
                    return default(V);

                var bb = ByteBuffer.Wrap(bytes);
                var value = new V();
                value.Decode(bb);
                return value;
            }

            private bool LruTryRemoveCallback(K key, Record r)
            {
                Maps.rwLock.EnterReadLock();
                try
                {
                    lock (r)
                    {
                        r.Removed = true;
                        return Lru.TryRemove(key, out _);
                    }
                }
                finally
                {
                    Maps.rwLock.ExitReadLock();
                }
            }

            internal override void ResetLru()
            {
                Lru = new ConcurrentLruLike<K, Record>(
                    Lru.Capacity, LruTryRemoveCallback, 200, 2000,
                    Lru.InitialCapacity, Lru.ConcurrencyLevel);
            }
        }

        public string DbHome { get; }
        private RocksDb Db;
        private readonly System.Threading.ReaderWriterLockSlim rwLock
            = new System.Threading.ReaderWriterLockSlim(
                System.Threading.LockRecursionPolicy.SupportsRecursion);

        private ConcurrentDictionary<string, Map> maps = new ConcurrentDictionary<string, Map>();
        private ConcurrentDictionary<string, string> Columns = new ConcurrentDictionary<string, string>();
        public ConcurrentMaps(string dbHome)
        {
            DbHome = dbHome;
            OpenDb();
        }

        private void OpenDb()
        {
            var options = new DbOptions().SetCreateIfMissing(true);
            var dbName = Path.Combine(DbHome, "datas");

            var columns = new ColumnFamilies();
            if (Directory.Exists(dbName))
            {
                foreach (var column in RocksDb.ListColumnFamilies(options, dbName))
                {
                    columns.Add(column, new ColumnFamilyOptions());
                    Columns[column] = column;
                }
            }

            Db = RocksDb.Open(options, dbName, columns);
        }

        public Map<K, V> GetOrAdd<K, V>(
            string name,
            // 下面的参数仅在第一次创建Map时才被使用
            int capacity = 10000_0000,
            int initialCapacity = 10000_0000,
            int concurrentcyLevel = 1024
            )
            where V : Serializable, new()
        {
            return (Map<K, V>)maps.GetOrAdd(name, (key) =>
            {
                if (false == Columns.ContainsKey(name))
                {
                    var ops = new ColumnFamilyOptions();
                    Db.CreateColumnFamily(ops, name); // will get in Map constructor.
                }
                return new Map<K, V>(this, name, capacity, initialCapacity, concurrentcyLevel);
            });
        }

        public string Checkpoint()
        {
            var checkpintDir = Path.Combine(DbHome, "checkpoint_" + DateTime.Now.Ticks);

            // fast checkpoint, will stop application.
            rwLock.EnterWriteLock();
            try
            {
                var cp = Db.Checkpoint();
                cp.Save(checkpintDir);
                cp.Dispose();
            }
            finally
            {
                rwLock.ExitWriteLock();
            }
            return checkpintDir;
        }

        public bool Backup(string checkpintDir, string backupDir)
        {
            var Rocks = Native.Instance;

            // backup now.
            IntPtr options = Rocks.rocksdb_options_create();
            IntPtr src = IntPtr.Zero;
            IntPtr backup = IntPtr.Zero;
            try
            {
                IntPtr err = IntPtr.Zero;
                src = Rocks.rocksdb_open(options, checkpintDir, out err);
                if (err != IntPtr.Zero)
                    return false;

                Rocks.rocksdb_options_set_create_if_missing(options, true);
                backup = Rocks.rocksdb_backup_engine_open(options, backupDir, out err);
                if (err != IntPtr.Zero)
                    return false;

                Rocks.rocksdb_backup_engine_create_new_backup(backup, src, out err);
                if (err != IntPtr.Zero)
                    return false;

                return true;
            }
            finally
            {
                if (backup != IntPtr.Zero)
                    Rocks.rocksdb_backup_engine_close(backup);
                if (src != IntPtr.Zero)
                    Rocks.rocksdb_close(src);
                if (options != IntPtr.Zero)
                    Rocks.rocksdb_options_destroy(options);
            }
        }

        public bool Restore(string backupdir)
        {
            var Rocks = Native.Instance;

            rwLock.EnterWriteLock();
            try
            {
                IntPtr backup = IntPtr.Zero;
                IntPtr options = Rocks.rocksdb_options_create();
                IntPtr restore_options = Rocks.rocksdb_restore_options_create();
                try
                {
                    var err = IntPtr.Zero;
                    backup = Rocks.rocksdb_backup_engine_open(options, backupdir, out err);
                    if (err != IntPtr.Zero)
                        return false;

                    // close current
                    Db.Dispose();

                    // restore
                    var dbName = Path.Combine(DbHome, "datas");
                    Rocks.rocksdb_backup_engine_restore_db_from_latest_backup(
                        backup, dbName, dbName, restore_options, out err);
                    if (err != IntPtr.Zero)
                        return false;

                    // reopen
                    OpenDb();
                    foreach (var e in maps)
                    {
                        e.Value.ResetLru();
                    }
                    return true;
                }
                finally
                {
                    if (backup != IntPtr.Zero)
                        Rocks.rocksdb_backup_engine_close(backup);
                    if (restore_options != IntPtr.Zero)
                        Rocks.rocksdb_restore_options_destroy(restore_options);
                    if (options != IntPtr.Zero)
                        Rocks.rocksdb_options_destroy(options);
                }
            }
            finally
            {
                rwLock.ExitWriteLock();
            }
        }
    }
}
