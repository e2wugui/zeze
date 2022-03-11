using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Transactions;
using Zeze.Transaction;
using Zeze.Services.ServiceManager;
using System.Collections.Concurrent;

namespace Zeze
{
    public sealed class Application
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Dictionary<string, Database> Databases { get; private set; } = new Dictionary<string, Database>();
        public Config Config { get; private set; }
        public bool IsStart { get; private set; }
        public Agent ServiceManagerAgent { get; private set; }
        internal GlobalAgent GlobalAgent { get; }
        internal Services.GlobalCacheManagerWithRaftAgent GlobalAgentRaft { get; }

        // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
        internal Util.SimpleThreadPool InternalThreadPool;
        internal Locks Locks { get; private set; }

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

        internal class LastFlushWhenReduce
        {
            public TableKey Key { get; set; }
            public long LastGlobalSerialId { get; set; }
            public long Ticks { get; set; }
            public bool Removed { get; set; } = false;

            public LastFlushWhenReduce(TableKey tkey)
            {
                Key = tkey;
            }
        }

        private ConcurrentDictionary<TableKey, LastFlushWhenReduce> FlushWhenReduce { get; }
            = new ConcurrentDictionary<TableKey, LastFlushWhenReduce>();
        private ConcurrentDictionary<long, Util.IdentityHashSet<LastFlushWhenReduce>> FlushWhenReduceActives { get; }
            = new ConcurrentDictionary<long, Util.IdentityHashSet<LastFlushWhenReduce>>();
        private Util.SchedulerTask FlushWhenReduceTimerTask;

        internal void SetLastGlobalSerialId(TableKey tkey, long globalSerialId)
        {
            while (true)
            {
                var last = FlushWhenReduce.GetOrAdd(tkey, (k) => new LastFlushWhenReduce(k));
                lock (last)
                {
                    if (last.Removed)
                        continue;

                    last.LastGlobalSerialId = globalSerialId;
                    last.Ticks = DateTime.Now.Ticks;
                    Monitor.PulseAll(last);
                    var minutes = last.Ticks / TimeSpan.TicksPerMinute;
                    FlushWhenReduceActives.GetOrAdd(minutes, (key) => new Util.IdentityHashSet<LastFlushWhenReduce>()).Add(last);
                    return;
                }
            }
        }

        internal bool TryWaitFlushWhenReduce(TableKey tkey, long hope)
        {
            while (true)
            {
                var last = FlushWhenReduce.GetOrAdd(tkey, (k) => new LastFlushWhenReduce(k));
                lock (last)
                {
                    if (last.Removed)
                        continue;

                    while (last.LastGlobalSerialId < hope)
                    {
                        // 超时的时候，马上返回。
                        // 这个机制的是为了防止忙等。
                        // 所以不需要严格等待成功。
                        if (false == Monitor.Wait(last, 5000))
                            return false;
                    }
                    return true;
                }
            }
        }

        public const long FlushWhenReduceIdleMinuts = 30;

        private void FlushWhenReduceTimer(Util.SchedulerTask ThisTask)
        {
            var minuts = DateTime.Now.Ticks / TimeSpan.TicksPerMinute;

            foreach (var active in FlushWhenReduceActives)
            {
                if (active.Key - minuts > FlushWhenReduceIdleMinuts)
                {
                    foreach (var last in active.Value)
                    {
                        lock (last)
                        {
                            if (last.Removed)
                                continue;

                            if (last.Ticks / TimeSpan.TicksPerMinute > FlushWhenReduceIdleMinuts)
                            {
                                if (FlushWhenReduce.TryRemove(last.Key, out _))
                                {
                                    last.Removed = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        public Schemas Schemas { get; set; } // no thread protected
        public string SolutionName { get; }

        public Application(string solutionName, Config config = null)
        {
            SolutionName = solutionName;

            Config = config;
            if (null == Config)
                Config = Config.Load();
            InternalThreadPool = new Util.SimpleThreadPool(
                Config.InternalThreadPoolWorkerCount, "ZezeSpecialThreadPool");

            int workerMin, ioMin;
            //int workerMax, ioMax;
            ThreadPool.GetMinThreads(out workerMin, out ioMin);
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
            GlobalAgent = new GlobalAgent(this);
            GlobalAgentRaft = new Services.GlobalCacheManagerWithRaftAgent(this);
            _checkpoint = new Checkpoint(Config.CheckpointMode, Databases.Values);
            ServiceManagerAgent = new Agent(this);
        }

        private ConcurrentDictionary<string, Table> Tables = new ConcurrentDictionary<string, Table>();

        public void AddTable(string dbName, Transaction.Table table)
        {
            if (Databases.TryGetValue(dbName, out var db))
            {
                if (false == Tables.TryAdd(table.Name, table))
                    throw new Exception($"duplicate table name={table.Name}");
                db.AddTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public void RemoveTable(string dbName, Transaction.Table table)
        {
            Tables.TryRemove(table.Name, out _);
            if (Databases.TryGetValue(dbName, out var db))
            {
                db.RemoveTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public Table GetTable(string name)
        {
            if (Tables.TryGetValue(name, out var table))
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

        public Procedure NewProcedure(Func<long> action, string actionName,
            TransactionLevel level = TransactionLevel.Serializable,
            object userState = null)
        {
            if (IsStart)
            {
                return new Procedure(this, action, actionName, level, userState);
            }
            throw new Exception("App Not Start");
        }

        public void Start()
        {
            lock (this)
            {
                Config?.ClearInUseAndIAmSureAppStopped(this, Databases); // XXX REMOVE ME!
                foreach (var db in Databases.Values)
                {
                    db.DirectOperates.SetInUse(Config.ServerId, Config.GlobalCacheManagerHostNameOrAddress);
                }

                if (IsStart)
                    return;
                IsStart = true;

                Locks = new Locks();

                var serviceConf = Config.GetServiceConf(Agent.DefaultServiceName);
                if (null != serviceConf) {
                    ServiceManagerAgent.Client.Start();
                    ServiceManagerAgent.WaitConnectorReady();
                }
                Database defaultDb = GetDatabase("");
                foreach (var db in Databases.Values)
                {
                    db.Open(this);
                }

                var hosts = Config.GlobalCacheManagerHostNameOrAddress.Split(';');
                if (hosts.Length > 0)
                {
                    var israft = hosts[0].EndsWith(".xml");
                    if (false == israft)
                    {
                        GlobalAgent.Start(hosts, Config.GlobalCacheManagerPort);
                    }
                    else
                    {
                        GlobalAgentRaft.Start(hosts);
                    }
                }

                Checkpoint.Start(Config.CheckpointPeriod); // 定时模式可以和其他模式混用。

                /////////////////////////////////////////////////////
                /// Schemas Check
                Schemas.Compile();
                var keyOfSchemas = Zeze.Serialize.ByteBuffer.Allocate();
                keyOfSchemas.WriteString("zeze.Schemas." + Config.ServerId);
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
                            logger.Error(ex);
                            SchemasPrevious = null;
                            logger.Error(ex, "Schemas Implement Changed?");
                        }
                        Schemas.CheckCompatible(SchemasPrevious, this);
                    }
                    var newdata = Serialize.ByteBuffer.Allocate();
                    Schemas.Encode(newdata);
                    if (defaultDb.DirectOperates.SaveDataWithSameVersion(keyOfSchemas, newdata, ref version))
                        break;
                }
                FlushWhenReduceTimerTask = Util.Scheduler.Instance.Schedule(FlushWhenReduceTimer, 60 * 1000, 60 * 1000);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                var domain = AppDomain.CurrentDomain;
                domain.UnhandledException -= UnhandledExceptionEventHandler;
                domain.ProcessExit -= ProcessExit;

                GlobalAgent?.Stop(); // 关闭时需要生成新的SessionId，这个现在使用AutoKey，需要事务支持。

                if (false == IsStart)
                    return;
                FlushWhenReduceTimerTask?.Cancel();
                FlushWhenReduceTimerTask = null;

                Config?.ClearInUseAndIAmSureAppStopped(this, Databases);
                IsStart = false;

                _checkpoint?.StopAndJoin();
                _checkpoint = null;
                foreach (var db in Databases.Values)
                {
                    db.Close();
                }
                Databases.Clear();
                ServiceManagerAgent.Stop();
                InternalThreadPool = null;
                Locks = null;
                Config = null;
            }
        }
 
        public void CheckpointRun()
        {
            _checkpoint.RunOnce();
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

        public Zeze.Util.TaskOneByOneByKey TaskOneByOneByKey { get; } = new Zeze.Util.TaskOneByOneByKey();

        public TaskCompletionSource<long> Run(Func<long> func, string actionName, TransactionModes mode, object oneByOneKey = null)
        {
            var future = new TaskCompletionSource<long>();
            switch (mode)
            {
                case TransactionModes.ExecuteInTheCallerTransaction:
                    future.SetResult(func());
                    break;

                case TransactionModes.ExecuteInNestedCall:
                    future.SetResult(NewProcedure(func, actionName).Call());
                    break;

                case TransactionModes.ExecuteInAnotherThread:
                    if (null != oneByOneKey)
                    {
                        TaskOneByOneByKey.Execute(oneByOneKey,
                            () => future.SetResult(NewProcedure(func, actionName).Call()),
                            actionName);
                    }
                    else
                    {
                        Zeze.Util.Task.Run(
                            () => future.SetResult(NewProcedure(func, actionName).Call()),
                            actionName);
                    }
                    break;
            }
            return future;
        }
    }
}
