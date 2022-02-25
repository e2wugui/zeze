using System;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Serialize;
using Zeze.Util;
using RocksDbSharp;

namespace Zeze.Raft
{
    public class RocksRaft
    {
        public abstract class Map
        {
            public string Name { get; }
            public bool Closed { get; protected set; } = false;

            public Map(string name)
            {
                Name = name;
            }

            internal abstract void OpenWithType();
            internal virtual void Close()
            {
                Closed = true;
            }
        }

        public class Map<K, V> : Map where V : Serializable, new()
        {
            public RocksRaft Maps { get; }
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
                RocksRaft maps,
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

            internal override void OpenWithType()
            {
                Maps.GetOrAdd<K, V>(Name, Lru.Capacity, Lru.InitialCapacity, Lru.ConcurrencyLevel);
            }

            public V GetOrAdd(K key)
            {
                return GetOrAdd(key, (_) => new V());
            }

            private void Verify()
            {
                if (Closed)
                    throw new Exception("Map Closed. maybe has a old Map reference.");

            }

            public V GetOrAdd(K key, Func<K, V> factory)
            {
                Verify();
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
                Verify();
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
                            Maps.Db.Put(r.EncodedKey, r.EncodedKey.Length, bb.Bytes, bb.Size, ColumnFamily, Maps.WriteOptions);
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
                Verify();
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
                                Maps.Db.Remove(r.EncodedKey, ColumnFamily, Maps.WriteOptions);
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

            public ByteBuffer EncodeKey(K key)
            {
                var bb = ByteBuffer.Allocate();
                SerializeHelper<K>.Encode(bb, key);
                return bb;
            }

            public ByteBuffer EncodeValue(V v)
            {
                var bb = ByteBuffer.Allocate();
                v.Encode(bb);
                return bb;
            }

            public V DecodeValue(byte[] bytes)
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
        }

        public string DbHome { get; }
        public bool Sync { get; }

        private RocksDb Db;
        private readonly System.Threading.ReaderWriterLockSlim rwLock
            = new System.Threading.ReaderWriterLockSlim(
                System.Threading.LockRecursionPolicy.SupportsRecursion);

        private ConcurrentDictionary<string, Map> Maps = new ConcurrentDictionary<string, Map>();
        private ConcurrentDictionary<string, string> Columns = new ConcurrentDictionary<string, string>();
        private WriteOptions WriteOptions;

        public RocksRaft(string dbHome, bool sync = true)
        {
            DbHome = dbHome;
            Sync = sync;
            WriteOptions = new WriteOptions().SetSync(Sync);

            OpenDb();
        }

        private void OpenDb()
        {
            var options = new DbOptions().SetCreateIfMissing(true);
            var dbName = Path.Combine(DbHome, "datas");

            Columns.Clear();
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

            // 第一次打开时，Maps是空的；
            // Restore时需要重置，调用一次GetOrAdd<K, V>
            var openedMaps = Maps;
            Maps = new ConcurrentDictionary<string, Map>();
            foreach (var map in openedMaps)
            {
                map.Value.OpenWithType();
            }
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
            return (Map<K, V>)Maps.GetOrAdd(name, (key) =>
            {
                if (false == Columns.ContainsKey(name))
                {
                    var ops = new ColumnFamilyOptions();
                    Db.CreateColumnFamily(ops, name); // will get in Map constructor.
                }
                return new Map<K, V>(this, name, capacity, initialCapacity, concurrentcyLevel);
            });
        }

        public string Checkpoint(Action action = null)
        {
            var checkpintDir = Path.Combine(DbHome, "checkpoint_" + DateTime.Now.Ticks);

            // fast checkpoint, will stop application.
            rwLock.EnterWriteLock();
            try
            {
                var cp = Db.Checkpoint();
                cp.Save(checkpintDir);
                cp.Dispose();
                action?.Invoke();
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
            IntPtr dbOption = Rocks.rocksdb_options_create();
            IntPtr familyOption = Rocks.rocksdb_options_create();
            IntPtr src = IntPtr.Zero;
            IntPtr backup = IntPtr.Zero;
            IntPtr[] familyHandles = null;
            try
            {
                IntPtr err = IntPtr.Zero;
                var families = Rocks.rocksdb_list_column_families(dbOption, checkpintDir);
                var familyOptions = new IntPtr[families.Length];
                for (int i = 0; i < families.Length; ++i)
                    familyOptions[i] = familyOption;
                familyHandles = new IntPtr[families.Length]; // out
                src = Rocks.rocksdb_open_column_families(dbOption, checkpintDir,
                    families.Length, families, familyOptions, familyHandles, out err);
                if (err != IntPtr.Zero)
                    return false;

                Rocks.rocksdb_options_set_create_if_missing(dbOption, true);
                backup = Rocks.rocksdb_backup_engine_open(dbOption, backupDir, out err);
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
                /*
                 // rocksdb_open_column_families 返回的，可能随rocksdb_close一起释放了。
                 // 需要确认！
                if (null != familyHandles)
                {
                    foreach (var cfh in familyHandles)
                    {
                        if (IntPtr.Zero != cfh)
                            Rocks.rocksdb_column_family_handle_destroy(cfh);
                    }
                }
                */
                if (familyOption != IntPtr.Zero)
                    Rocks.rocksdb_options_destroy(familyOption);
                if (dbOption != IntPtr.Zero)
                    Rocks.rocksdb_options_destroy(dbOption);
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
