using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Transaction;
using Zeze.Services.ServiceManager;
using System.Collections.Concurrent;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze
{
    public sealed class Application
    {
        internal static readonly ILogger logger = LogManager.GetLogger(typeof(Application));

        public Dictionary<string, Database> Databases { get; } = new Dictionary<string, Database>();
        public Config Config { get; private set; }
        public bool IsStart { get; private set; }
        public Agent ServiceManager { get; }
        public Arch.RedirectBase Redirect { get; set; }
        internal IGlobalAgent GlobalAgent { get; private set; }
        public AchillesHeelDaemon AchillesHeelDaemon { get; private set; }

        public Component.AutoKey.Module AutoKeys { get; private set; }
        public Collections.Queue.Module Queues { get; private set; }
        public Component.DelayRemove DelayRemove { get; private set; }
        internal Locks Locks { get; } = new();

        public Component.AutoKey GetAutoKey(string name)
        {
            return AutoKeys.GetOrAdd(name);
        }

        private Checkpoint _checkpoint;
        public Checkpoint Checkpoint
        {
            get
            {
                return _checkpoint;
            }
            /*
            set
            {
                lock (this)
                {
                    if (null == value)
                        throw new ArgumentNullException();
                    if (IsStart)
                        throw new Exception("Checkpoint only can setup before start.");
                    _checkpoint = value;
                }
            }
            */
        }

        public Schemas Schemas { get; set; } // no thread protected
        public string ProjectName { get; }

        public Application(string projectName, Config config = null)
        {
            ProjectName = projectName;

            Config = config;
            if (null == Config)
                Config = Config.Load();

            //int workerMax, ioMax;
            ThreadPool.GetMinThreads(out var workerMin, out var ioMin);
            //ThreadPool.GetMaxThreads(out workerMax, out ioMax);
            //Console.WriteLine($"worker ({workerMin}, {workerMax}) io({ioMin}, {ioMax})");
            if (Config.WorkerThreads > 0)
            {
                workerMin = Config.WorkerThreads;
                //workerMax = Config.WorkerThreads;
            }
            if (Config.CompletionPortThreads > 0)
            {
                ioMin = Config.CompletionPortThreads;
                //ioMax = Config.CompletionPortThreads;
            }
            ThreadPool.SetMinThreads(workerMin, ioMin);
            //ThreadPool.SetMaxThreads(workerMax, ioMax);

            Config.CreateDatabase(this, Databases);
            _checkpoint = new Checkpoint(this, Config.CheckpointMode, Databases.Values);
            ServiceManager = new Agent(this);

            var noDatabase = Config.NoDatabase || Config.ServerId < 0;
            if (false == noDatabase)
            {
                // Initialize Component
                AutoKeys = new(this);
                Queues = new(this);
                DelayRemove = new(this);
            }
        }

        private readonly ConcurrentDictionary<int, Table> Tables = new();

        public void OpenDynamicTable(string dbName, Table table)
        {
            lock (this)
            {
                AddTable(dbName, table).OpenDynamicTable(this, table);
            }
        }

        public Database AddTable(string dbName, Table table)
        {
            TableKey.Tables[table.Id] = table.Name;
            if (Databases.TryGetValue(dbName, out var db))
            {
                if (false == Tables.TryAdd(table.Id, table))
                    throw new Exception($"duplicate table name={table.Name}");
                db.AddTable(table);
                return db;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public void RemoveTable(string dbName, Table table)
        {
            Tables.TryRemove(table.Id, out _);
            if (Databases.TryGetValue(dbName, out var db))
            {
                db.RemoveTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public Table GetTable(int id)
        {
            if (Tables.TryGetValue(id, out var table))
                return table;
            return null;
        }

        public Table GetTableSlow(string name)
        {
            foreach (var table in Tables.Values)
                if (table.Name == name)
                    return table;
            return null;
        }

        public Database GetDatabase(string name)
        {
            if (Databases.TryGetValue(name, out var exist))
            {
                return exist;
            }
            throw new Exception($"database not exist name={name}");
        }

        public Procedure NewProcedure(Func<Task<long>> action, string actionName,
            TransactionLevel level = TransactionLevel.Serializable,
            object userState = null)
        {
            if (IsStart)
            {
                return new Procedure(this, action, actionName, level, userState);
            }
            throw new Exception("App Not Start");
        }

        public async Task StartAsync()
        {
            lock (this)
            {
                if (IsStart)
                    return;
                IsStart = true;
            }
            var noDatabase = Config.NoDatabase || Config.ServerId < 0;

            if (false == noDatabase)
            {
                if ("true".Equals(Environment.GetEnvironmentVariable(Services.Daemon.PropertyNameClearInUse)))
                    Config?.ClearInUseAndIAmSureAppStopped(this, Databases);

                // Set Databases InUse
                foreach (var db in Databases.Values)
                {
                    db.DirectOperates.SetInUse(Config.ServerId, Config.GlobalCacheManagerHostNameOrAddress);
                }
            }

            // Start ServiceManager
            var serviceConf = Config.GetServiceConf(Agent.DefaultServiceName);
            if (null != serviceConf) {
                ServiceManager.Client.Start();
                await ServiceManager.WaitConnectorReadyAsync();
            }

            if (false == noDatabase)
            {
                // Open Databases
                foreach (var db in Databases.Values)
                {
                    db.Open(this);
                }

                // Open Global
                var hosts = Config.GlobalCacheManagerHostNameOrAddress.Split(';');
                if (hosts.Length > 0)
                {
                    var isRaft = hosts[0].EndsWith(".xml");
                    if (false == isRaft)
                    {
                        var impl = new GlobalAgent(this, hosts, Config.GlobalCacheManagerPort);
                        GlobalAgent = impl;
                        AchillesHeelDaemon = new AchillesHeelDaemon(this, impl.Agents);
                        await impl.Start();
                    }
                    else
                    {
                        var impl = new Services.GlobalCacheManagerWithRaftAgent(this, hosts);
                        GlobalAgent = impl;
                        AchillesHeelDaemon = new AchillesHeelDaemon(this, impl.Agents);
                        await impl.Start();
                    }
                }

                // Start Checkpoint
                Checkpoint.Start(Config.CheckpointPeriod); // 定时模式可以和其他模式混用。
                
                /////////////////////////////////////////////////////
                // Schemas Check
                Schemas.Compile();
                var keyOfSchemas = ByteBuffer.Allocate();
                keyOfSchemas.WriteString("zeze.Schemas." + Config.ServerId);
                Database defaultDb = GetDatabase("");
                while (true)
                {
                    var (data, version) = defaultDb.DirectOperates.GetDataWithVersion(keyOfSchemas);
                    if (null != data)
                    {
                        var SchemasPrevious = new Schemas();
                        try
                        {
                            SchemasPrevious.Decode(data);
                            SchemasPrevious.Compile();
                        }
                        catch (Exception ex)
                        {
                            SchemasPrevious = null;
                            logger.Error(ex, "Schemas Implement Changed?");
                        }
                        Schemas.CheckCompatible(SchemasPrevious, this);
                    }
                    var newdata = ByteBuffer.Allocate();
                    Schemas.Encode(newdata);
                    if (defaultDb.DirectOperates.SaveDataWithSameVersion(keyOfSchemas, newdata, ref version))
                        break;
                }
                AchillesHeelDaemon.Start();
                DelayRemove.Start();
            }
        }

        public void Stop()
        {
            lock (this)
            {
                var domain = AppDomain.CurrentDomain;
                domain.UnhandledException -= UnhandledExceptionEventHandler;
                domain.ProcessExit -= ProcessExit;

                if (false == IsStart)
                    return;
                IsStart = false;

                DelayRemove?.Stop();
                DelayRemove = null;

                AchillesHeelDaemon?.StopAndJoin();
                GlobalAgent?.Dispose(); // 关闭时需要生成新的SessionId，这个现在使用AutoKey，需要事务支持。

                _checkpoint?.StopAndJoin();
                _checkpoint = null;
                Databases.Clear();
                ServiceManager.Stop();
                Config = null;

                Config?.ClearInUseAndIAmSureAppStopped(this, Databases);
                foreach (var db in Databases.Values)
                    db.Close();
            }
        }
 
        public async Task CheckpointNow()
        {
            await _checkpoint.CheckpointNow();
        }

        public Application()
        {
            var domain = AppDomain.CurrentDomain;
            domain.UnhandledException += UnhandledExceptionEventHandler;
            domain.ProcessExit += ProcessExit;
            // domain.DomainUnload += DomainUnload;
        }

        private void ProcessExit(object sender, EventArgs e)
        {
            Stop();
        }

        private void UnhandledExceptionEventHandler(object sender, UnhandledExceptionEventArgs args)
        {
            Exception e = (Exception)args.ExceptionObject;
            logger.Error(e, "UnhandledExceptionEventArgs");
        }

        public TaskOneByOneByKey TaskOneByOneByKey => TaskOneByOneByKey.Instance;
    }
}
