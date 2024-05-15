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
        internal static readonly ILogger logger = LogManager.GetLogger(typeof(Rocks));

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

        internal async Task UpdateAtomicLongs(Dictionary<int, long> to)
        {
            using (await Raft.Monitor.EnterAsync())
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

        public Procedure NewProcedure(Func<Task<long>> func)
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

        internal async Task FollowerApply(Changes changes)
        {
            var rs = new List<Record>();
            foreach (var e in changes.Records)
            {
                rs.Add(await e.Value.Table.FollowerApply(e.Key.Key, e.Value));
            }
            await Flush(rs, changes, true);
        }

        public string DbHome => Raft.RaftConfig.DbHome;
        public bool IsLeader => Raft.IsLeader;

        public WriteOptions WriteOptions { get; }

        internal AsyncRocksDb Storage;
        private readonly ConcurrentDictionary<string, ColumnFamilyHandle> Columns = new();

        public Rocks(RocksMode mode = RocksMode.Pessimism, bool RocksDbWriteOptionSync = false)
        {
            RocksMode = mode;

            if (mode != RocksMode.Pessimism)
                throw new Exception("Unsupported RocksMode = " + mode);

            RegisterLog<LogBean>();
            RegisterLog<Log<int>>();
            RegisterLog<Log<long>>();
            AddFactory(new Changes(this).TypeId, () => new Changes(this));

            WriteOptions = new WriteOptions().SetSync(RocksDbWriteOptionSync);
            AppDomain.CurrentDomain.ProcessExit += ProcessExit;
        }

        public async Task<Rocks> OpenAsync(
            string raftName = null, // 这个参数会覆盖RaftConfig.Name，这样应用可以共享同一个配置文件。
            RaftConfig raftConfig = null, // "raft.xml"
            Config config = null // "zeze.xml"
            )
        {
            return await OpenAsync((_raft, _name, _config) => new Server(_raft, _name, _config), raftName, raftConfig, config);
        }

        public async Task<Rocks> OpenAsync(
            Func<Raft, string, Config, Server> serverFactory,
            string raftName = null, // 这个参数会覆盖RaftConfig.Name，这样应用可以共享同一个配置文件。
            RaftConfig raftConfig = null, // "raft.xml"
            Config config = null // "zeze.xml"
            )
        {
            if (null != base.Raft)
                throw new InvalidOperationException($"{raftName} Has Opened.");

            // 这个赋值是不必要的，new Raft(...)内部会赋值。有点奇怪。
            base.Raft = new Raft(this);
            await base.Raft.OpenAsync(serverFactory, raftName, raftConfig, config);
            base.Raft.AtFatalKills += () => Storage?.RocksDb.Dispose();
            base.Raft.LogSequence.WriteOptions = WriteOptions;

            // Raft 在有快照的时候，会调用LoadSnapshot-Restore-OpenDb。
            // 如果Storage没有创建，需要主动打开。
            if (Storage == null)
            {
                await OpenDb();
            }
            return this;
        }

        private async Task OpenDb()
        {
            var options = new DbOptions().SetCreateIfMissing(true);
            var dbName = Path.Combine(DbHome, "statemachine");

            var columns = new ColumnFamilies();
            if (Directory.Exists(dbName))
            {
                foreach (var column in RocksDb.ListColumnFamilies(options, dbName))
                {
                    columns.Add(column, new ColumnFamilyOptions());
                }
            }

            Storage = await Util.AsyncRocksDb.OpenAsync(options, dbName, columns, Raft.AsyncExecutor);
            Columns.Clear();
            foreach (var col in columns)
            {
                Columns[col.Name] = Storage.RocksDb.GetColumnFamily(col.Name);
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
                return Storage.RocksDb.CreateColumnFamily(ops, name);
            });
        }

        /// <summary>
        /// </summary>
        /// <returns>(checkpointDir, lastApplied.Term, lastApplied.Index)</returns>
        public async Task<(string, long, long)> Checkpoint()
        {
            var checkpointDir = Path.Combine(DbHome, "checkpoint_" + DateTime.Now.Ticks);

            // fast checkpoint, will stop application apply.
            using (await Raft.Monitor.EnterAsync())
            {
                var lastAppliedLog = await Raft.LogSequence.LastAppliedLogTermIndex();
                var cp = Storage.RocksDb.Checkpoint();
                cp.Save(checkpointDir);
                cp.Dispose();
                return (checkpointDir, lastAppliedLog.Term, lastAppliedLog.Index);
            }
        }

        public static bool Backup(string checkpintDir, string backupDir)
        {
            var Rocks = Native.Instance;

            // backup now.
            IntPtr dbOption = Rocks.rocksdb_options_create();
            IntPtr familyOption = Rocks.rocksdb_options_create();
            IntPtr src = IntPtr.Zero;
            IntPtr backup = IntPtr.Zero;
            IntPtr[] familyHandles;
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

        public async Task<bool> Restore(string backupdir)
        {
            var N = Native.Instance;

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
                Storage?.RocksDb.Dispose();

                // restore
                var dbName = Path.Combine(DbHome, "statemachine");
                N.rocksdb_backup_engine_restore_db_from_latest_backup(
                    backup, dbName, dbName, restore_options, out err);
                if (err != IntPtr.Zero)
                    return false;

                // reopen
                await OpenDb();
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

        public void Dispose()
        {
            AppDomain.CurrentDomain.ProcessExit -= ProcessExit;
            GC.SuppressFinalize(this);
            DisposeAsync().Wait();
        }

        public async Task DisposeAsync()
        {
            await Raft?.Shutdown();
            Raft = null;
            Storage?.RocksDb.Dispose();
            Storage = null;
        }

        public override async Task<(bool, long, long)> Snapshot(string path)
        {
            var (cphome, lastIncludedTerm, lastIncludedIndex) = await Checkpoint();
            var backupdir = Path.Combine(DbHome, "backup");
            Directory.CreateDirectory(backupdir);
            Backup(cphome, backupdir);
            FileSystem.DeleteDirectory(cphome);
            ZipFile.CreateFromDirectory(backupdir, path);
            await Raft.LogSequence.CommitSnapshot(path, lastIncludedIndex);
            return (true, lastIncludedTerm, lastIncludedIndex);
        }

        public override async Task LoadSnapshot(string path)
        {
            var backupdir = Path.Combine(DbHome, "backup");
            if (File.GetLastWriteTime(path) > Directory.GetLastWriteTime(backupdir))
            {
                FileSystem.DeleteDirectory(backupdir);
                ZipFile.ExtractToDirectory(path, backupdir);
            }
            await Restore(backupdir);
        }

        private ColumnFamilyHandle AtomicLongsColumnFamily;

        internal async Task Flush(IEnumerable<Record> rs, Changes changes, bool FollowerApply = false)
        {
            using var batch = new WriteBatch();
            foreach (var r in rs)
            {
                r.Flush(batch);
            }
            var key = ByteBuffer.Allocate(5);
            var value = ByteBuffer.Allocate(9);
            foreach (var a in changes.AtomicLongs)
            {
                key.WriteIndex = 0;
                key.WriteUInt(a.Key);
                value.WriteIndex = 0;
                value.WriteLong(a.Value);
                batch.Put(key.Bytes, (ulong)key.Size, value.Bytes, (ulong)value.Size, AtomicLongsColumnFamily);
                if (FollowerApply)
                    AtomicLongSet(a.Key, a.Value);
            }
            if (batch.Count() > 0)
                await Storage.WriteAsync(batch, WriteOptions);
        }

        public static void RegisterLog<T>() where T : Log, new()
        {
            Log.Factorys.TryAdd(new T().TypeId, () => new T());
        }

        private void ProcessExit(object sender, EventArgs e)
        {
            Dispose();
        }
    }
}
