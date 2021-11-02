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
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Dictionary<string, Database> Databases { get; private set; } = new Dictionary<string, Database>();
        public Config Config { get; }
        public bool IsStart { get; private set; }
        public Agent ServiceManagerAgent { get; private set; }
        internal GlobalAgent GlobalAgent { get; }

        // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
        internal Util.SimpleThreadPool InternalThreadPool;
        internal Locks Locks { get; } = new Locks();

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

        internal ConcurrentDictionary<TableKey, TaskCompletionSource<int>> FlushWhenReduceFutures
            = new ConcurrentDictionary<TableKey, TaskCompletionSource<int>>();

        public bool TryWaitFlushWhenReduce(TableKey tkey)
        {
            _checkpoint.RunOnce();
            return true;
            // TODO
            /*
            if (FlushWhenReduceFutures.TryGetValue(tkey, out var future))
            {
                future.Task.Wait();
                return true;
            }
            return false;
            */
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
            _checkpoint = new Checkpoint(Config.CheckpointMode, Databases.Values);
            ServiceManagerAgent = new Agent(this);
        }

        public void AddTable(string dbName, Transaction.Table table)
        {
            if (Databases.TryGetValue(dbName, out var db))
            {
                db.AddTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public void RemoveTable(string dbName, Transaction.Table table)
        {
            if (Databases.TryGetValue(dbName, out var db))
            {
                db.RemoveTable(table);
                return;
            }
            throw new Exception($"database not found dbName={dbName}");
        }

        public Table GetTable(string name)
        {
            // TODO 优化
            foreach (Database db in Databases.Values)
            {
                Table t = db.GetTable(name);
                if (null != t)
                    return t;
            }
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

        public Procedure NewProcedure(Func<int> action, string actionName, object userState = null)
        {
            if (IsStart)
            {
                return new Procedure(this, action, actionName, userState);
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

                if (Config.GlobalCacheManagerHostNameOrAddress.Length > 0)
                {
                    GlobalAgent.Start(Config.GlobalCacheManagerHostNameOrAddress, Config.GlobalCacheManagerPort);
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
                            SchemasPrevious = null;
                            logger.Error(ex, "Schemas Implement Changed?");
                        }
                        if (false == Schemas.IsCompatible(SchemasPrevious, Config))
                            throw new Exception("Database Struct Not Compatible!");
                    }
                    var newdata = Serialize.ByteBuffer.Allocate();
                    Schemas.Encode(newdata);
                    if (defaultDb.DirectOperates.SaveDataWithSameVersion(keyOfSchemas, newdata, ref version))
                        break;
                }
            }
        }

        public void Stop()
        {
            lock (this)
            {
                GlobalAgent?.Stop(); // 关闭时需要生成新的SessionId，这个现在使用AutoKey，需要事务支持。

                if (false == IsStart)
                    return;
                Config?.ClearInUseAndIAmSureAppStopped(this, Databases);
                IsStart = false;

                Checkpoint?.StopAndJoin();
                foreach (var db in Databases.Values)
                {
                    db.Close();
                }
                Databases.Clear();
                ServiceManagerAgent.Stop();
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

        public TaskCompletionSource<int> Run(Func<int> func, string actionName, TransactionModes mode, object oneByOneKey = null)
        {
            var future = new TaskCompletionSource<int>();
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
