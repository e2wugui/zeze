using System;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Serialize;
using Zeze.Util;
using RocksDbSharp;

namespace Zeze.Raft.RocksRaft
{
    public class Rocks
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public ConcurrentDictionary<string, Table> Tables { get; } = new ConcurrentDictionary<string, Table>();

        public void Apply(Changes changes)
        {
            foreach (var r in changes.Records)
            {
                if (Tables.TryGetValue(r.Key.Name, out var table))
                {
                    table.Apply(r.Key.Key, r.Value);
                }
                else
                {
                    logger.Error($"table not found {r.Key.Name}");
                }
            }
        }

        public string DbHome { get; }
        internal RocksDb Db;
        private ConcurrentDictionary<string, ColumnFamilyHandle> Columns
            = new ConcurrentDictionary<string, ColumnFamilyHandle>();
        private Raft Raft;
        public WriteOptions WriteOptions { get; }

        public Rocks(string dbHome, bool sync = false)
        {
            DbHome = dbHome;
            WriteOptions = new WriteOptions().SetSync(sync);

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
                }
            }

            Db = RocksDb.Open(options, dbName, columns);
            Columns.Clear();
            foreach (var col in columns)
            {
                Columns[col.Name] = Db.GetColumnFamily(col.Name);
            }

            foreach (var table in Tables.Values)
            {
                table.Open();
            }
        }

        internal ColumnFamilyHandle OpenFamily(string name)
        {
            return Columns.GetOrAdd(name, (_) =>
            {
                var ops = new ColumnFamilyOptions();
                return Db.CreateColumnFamily(ops, name);
            });
        }

        public Table<K, V> OpenTable<K, V>(string name, int capacity = 1_0000)
            where V : Bean, new()
        {
            return (Table<K, V>)Tables.GetOrAdd(name, (key) => new Table<K, V>(this, name, capacity));
        }

        public void Snapshot(string path)
        {
            var cphome = Checkpoint(out long lastIncludedIndex, out long lastIncludedTerm);
            var backupdir = Path.Combine(DbHome, "backup");
            Backup(cphome, backupdir);

            // 把 backupdir 目录打包到文件 path 中。

            Raft.LogSequence.CommitSnapshot(path, lastIncludedIndex);
        }

        public void LoadFromSnapshot(string path)
        {
            var backupdir = Path.Combine(DbHome, "backup");
            FileSystem.DeleteDirectory(backupdir);

            // 从文件path解包到目录backupdir。

            Restore(backupdir);
        }

        public string Checkpoint(out long lastIncludedIndex, out long lastIncludedTerm)
        {
            var checkpintDir = Path.Combine(DbHome, "checkpoint_" + DateTime.Now.Ticks);

            // fast checkpoint, will stop application apply.
            lock (Raft)
            {
                var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
                lastIncludedIndex = lastAppliedLog.Index;
                lastIncludedTerm = lastAppliedLog.Term;

                var cp = Db.Checkpoint();
                cp.Save(checkpintDir);
                cp.Dispose();
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
            var N = Native.Instance;

            lock (Raft)
            {
                IntPtr backup = IntPtr.Zero;
                IntPtr options = N.rocksdb_options_create();
                IntPtr restore_options = N.rocksdb_restore_options_create();
                try
                {
                    var err = IntPtr.Zero;
                    backup = N.rocksdb_backup_engine_open(options, backupdir, out err);
                    if (err != IntPtr.Zero)
                        return false;

                    // close current
                    Db.Dispose();

                    // restore
                    var dbName = Path.Combine(DbHome, "datas");
                    N.rocksdb_backup_engine_restore_db_from_latest_backup(
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
                        N.rocksdb_backup_engine_close(backup);
                    if (restore_options != IntPtr.Zero)
                        N.rocksdb_restore_options_destroy(restore_options);
                    if (options != IntPtr.Zero)
                        N.rocksdb_options_destroy(options);
                }
            }
        }
    }
}
