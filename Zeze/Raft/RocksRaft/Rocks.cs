using System;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Serialize;
using Zeze.Util;
using RocksDbSharp;
using System.IO.Compression;
using System.Collections.Generic;
using System.Linq;

namespace Zeze.Raft.RocksRaft
{
    public class Rocks : StateMachine, IDisposable
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public ConcurrentDictionary<string, Table> Tables { get; } = new ConcurrentDictionary<string, Table>();

        public Procedure NewProcedure(Func<long> func)
        {
            return new Procedure(this, func);
        }

        internal void FollowerApply(Changes changes)
        {
            var rs = new List<Record>();
            foreach (var e in changes.Records)
            {
                rs.Add(e.Value.Table.FollowerApply(e.Key.Key, e.Value));
            }
            Flush(rs);
        }

        public string DbHome => Raft.RaftConfig.DbHome;
        public bool IsLeader => Raft.IsLeader;

        public WriteOptions WriteOptions { get; }

        internal RocksDb Storage;
        private ConcurrentDictionary<string, ColumnFamilyHandle> Columns
            = new ConcurrentDictionary<string, ColumnFamilyHandle>();

        public Rocks(
            string raftName = null, // 这个参数会覆盖RaftConfig.Name，这样应用可以共享同一个配置文件。
            RaftConfig raftConfig = null, // "raft.xml"
            Config config = null, // "zeze.xml"
            bool RocksDbWriteOptionSync = false)
        {
            RegisterLog<LogBean>();
            RegisterLog<Log<int>>();
            RegisterLog<Log<long>>();
            AddFactory(new Changes(this).TypeId, () => new Changes(this));

            WriteOptions = new WriteOptions().SetSync(RocksDbWriteOptionSync);
            // 这个赋值是不必要的，new Raft(...)内部会赋值。有点奇怪。
            base.Raft = new Raft(this, raftName, raftConfig, config);
            base.Raft.AtFatalKills += () => Storage?.Dispose();
            base.Raft.LogSequence.WriteOptions.SetSync(RocksDbWriteOptionSync);

            // Raft 在有快照的时候，会调用LoadSnapshot-Restore-OpenDb。
            // 如果Storage没有创建，需要主动打开。
            if (Storage == null)
            {
                OpenDb();
            }
        }

        private void OpenDb()
        {
            var options = new DbOptions().SetCreateIfMissing(true);
            var dbName = Path.Combine(DbHome, "rocksraft");

            var columns = new ColumnFamilies();
            if (Directory.Exists(dbName))
            {
                foreach (var column in RocksDb.ListColumnFamilies(options, dbName))
                {
                    columns.Add(column, new ColumnFamilyOptions());
                }
            }

            Storage = RocksDb.Open(options, dbName, columns);
            Columns.Clear();
            foreach (var col in columns)
            {
                Columns[col.Name] = Storage.GetColumnFamily(col.Name);
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
                return Storage.CreateColumnFamily(ops, name);
            });
        }

        public Table<K, V> OpenTable<K, V>(string name, int capacity = 10000)
            where V : Bean, new()
        {
            return OpenTable<K, V>(name, 0, capacity);
        }

        public Table<K, V> OpenTable<K, V>(string name, int family, int capacity)
            where V : Bean, new()
        {
            return (Table<K, V>)Tables.GetOrAdd(name, (key) => new Table<K, V>(this, $"{name}#{family}", capacity));
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

                var cp = Storage.Checkpoint();
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
                    Storage?.Dispose();

                    // restore
                    var dbName = Path.Combine(DbHome, "statemachine");
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

        public void Dispose()
        {
            lock (this) // 简单保护一下。
            {
                Raft?.Shutdown();
                Raft = null;
                Storage?.Dispose();
                Storage = null;
            }
        }

        public override bool Snapshot(string path, out long lastIncludedIndex, out long lastIncludedTerm)
        {
            var cphome = Checkpoint(out lastIncludedIndex, out lastIncludedTerm);
            var backupdir = Path.Combine(DbHome, "backup");
            Backup(cphome, backupdir);
            FileSystem.DeleteDirectory(cphome);
            ZipFile.CreateFromDirectory(backupdir, path);
            Raft.LogSequence.CommitSnapshot(path, lastIncludedIndex);
            return true;
        }

        public override void LoadSnapshot(string path)
        {
            var backupdir = Path.Combine(DbHome, "backup");
            if (File.GetLastWriteTime(path) > Directory.GetLastWriteTime(backupdir))
            {
                FileSystem.DeleteDirectory(backupdir);
                ZipFile.ExtractToDirectory(path, backupdir);
            }
            Restore(backupdir);
        }

        internal void Flush(IEnumerable<Record> rs)
        {
            using WriteBatch batch = new WriteBatch();
            foreach (var r in rs)
            {
                r.Flush(batch);
            }
            if (batch.Count() > 0)
                Storage.Write(batch, WriteOptions);
        }

        public void RegisterLog<T>() where T : Log, new()
        {
            Log.Factorys.TryAdd(new T().TypeId, () => new T());
        }
    }
}
