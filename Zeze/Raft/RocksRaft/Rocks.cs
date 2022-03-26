using System;
using System.Collections.Concurrent;
using System.IO;
using Zeze.Util;
using RocksDbSharp;
using System.IO.Compression;
using System.Collections.Generic;
using Zeze.Serialize;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
    public class Rocks : StateMachine, IDisposable
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public ConcurrentDictionary<string, TableTemplate> TableTemplates { get; } = new ConcurrentDictionary<string, TableTemplate>();  
        public ConcurrentDictionary<string, Table> Tables { get; } = new ConcurrentDictionary<string, Table>();
        public ConcurrentDictionary<int, AtomicLong> AtomicLongs { get; } = new();
        public RocksMode RocksMode { get; }

        public long AtomicLongIncrementAndGet(int index)
        { 
            return AtomicLongs.GetOrAdd(index, (_) => new AtomicLong()).IncrementAndGet();
        }

        public long AtomicLongGet(int index)
        {
            return AtomicLongs.GetOrAdd(index, (_) => new AtomicLong()).Get();
        }

        // 应用只能递增，这个方法仅 Follower 用来更新计数器。
        private void AtomicLongSet(int index, long value)
        {
            AtomicLongs.GetOrAdd(index, (_) => new AtomicLong()).GetAndSet(value);
        }

        private readonly Dictionary<int, long> LastUpdated = new();
        internal void UpdateAtomicLongs(Dictionary<int, long> to)
        {
            lock (Raft)
            {
                foreach (var a in AtomicLongs)
                {
                    if (false == LastUpdated.TryGetValue(a.Key, out var last))
                        last = 0;

                    long newest = a.Value.Get();
                    if (newest > last)
                    {
                        LastUpdated[a.Key] = newest;
                        to.Add(a.Key, newest);
                    }
                }
            }
        }

        public Procedure NewProcedure(Func<long> func)
        {
            return new Procedure(this, func);
        }

        public TableTemplate GetTableTemplate(string tableTemplateName)
        {
            if (TableTemplates.TryGetValue(tableTemplateName, out var tpl))
                return tpl;
            return null;
        }

        public void RegisterTableTemplate<K, V>(string tableTemplateName)
            where V : Bean, new()
        {
            TableTemplates.GetOrAdd(tableTemplateName, (key) => new TableTemplate<K, V>(this, key));
        }

        internal void FollowerApply(Changes changes)
        {
            var rs = new List<Record>();
            foreach (var e in changes.Records)
            {
                rs.Add(e.Value.Table.FollowerApply(e.Key.Key, e.Value));
            }
            Flush(rs, changes, true);
        }

        public string DbHome => Raft.RaftConfig.DbHome;
        public bool IsLeader => Raft.IsLeader;

        public WriteOptions WriteOptions { get; }

        internal RocksDb Storage;
        private readonly ConcurrentDictionary<string, ColumnFamilyHandle> Columns = new();

        public Rocks(
            string raftName = null, // 这个参数会覆盖RaftConfig.Name，这样应用可以共享同一个配置文件。
            RocksMode mode = RocksMode.Pessimism,
            RaftConfig raftConfig = null, // "raft.xml"
            Config config = null, // "zeze.xml"
            bool RocksDbWriteOptionSync = false)
        {
            RocksMode = mode;

            if (mode != RocksMode.Pessimism)
                throw new Exception("Unsupported RocksMode = " + mode);

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

            AtomicLongsColumnFamily = OpenFamily("Zeze.Raft.RocksRaft.AtomicLongs");

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

        /// <summary>
        /// </summary>
        /// <returns>(checkpointDir, lastApplied.Term, lastApplied.Index)</returns>
        public async Task<(string, long, long)> Checkpoint()
        {
            var checkpointDir = Path.Combine(DbHome, "checkpoint_" + DateTime.Now.Ticks);

            // fast checkpoint, will stop application apply.
            using var lockraft = await Raft.Monitor.EnterAsync();

            var lastAppliedLog = Raft.LogSequence.LastAppliedLogTermIndex();
            var cp = Storage.Checkpoint();
            cp.Save(checkpointDir);
            cp.Dispose();

            return (checkpointDir, lastAppliedLog.Term, lastAppliedLog.Index);
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

        public override async Task<(bool, long, long)> Snapshot(string path)
        {
            var (cphome, lastIncludedTerm, lastIncludedIndex) = await Checkpoint();
            var backupdir = Path.Combine(DbHome, "backup");
            Backup(cphome, backupdir);
            FileSystem.DeleteDirectory(cphome);
            ZipFile.CreateFromDirectory(backupdir, path);
            await Raft.LogSequence.CommitSnapshot(path, lastIncludedIndex);
            return (true, lastIncludedTerm, lastIncludedIndex);
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

        private ColumnFamilyHandle AtomicLongsColumnFamily;

        internal void Flush(IEnumerable<Record> rs, Changes changes, bool FollowerApply = false)
        {
            using var batch = new WriteBatch();
            foreach (var r in rs)
            {
                r.Flush(batch);
            }
            foreach (var a in changes.AtomicLongs)
            {
                var key = ByteBuffer.Allocate();
                var value = ByteBuffer.Allocate();
                SerializeHelper<int>.Encode(key, a.Key);
                SerializeHelper<long>.Encode(value, a.Value);
                batch.Put(key.Bytes, (ulong)key.Size, value.Bytes, (ulong)value.Size, AtomicLongsColumnFamily);
                if (FollowerApply)
                    AtomicLongSet(a.Key, a.Value);
            }
            if (batch.Count() > 0)
                Storage.Write(batch, WriteOptions);
        }

        public static void RegisterLog<T>() where T : Log, new()
        {
            Log.Factorys.TryAdd(new T().TypeId, () => new T());
        }
    }
}
