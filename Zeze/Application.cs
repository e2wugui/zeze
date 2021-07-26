using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Transactions;
using Zeze.Transaction;

namespace Zeze
{
    public sealed class Application
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Dictionary<string, Database> Databases { get; private set; } = new Dictionary<string, Database>();
        public Config Config { get; }
        public bool IsStart { get; private set; }
        internal TableSys TableSys { get; private set; }
        internal GlobalAgent GlobalAgent { get; }

        // 用来执行内部的一些重要任务，和系统默认 ThreadPool 分开，防止饥饿。
        internal Util.SimpleThreadPool InternalThreadPool;

        private Checkpoint _checkpoint;
        public Checkpoint Checkpoint
        {
            get
            {
                return _checkpoint;
            }
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
        }

        public Schemas Schemas { get; set; } // no thread protected

        public Application(Config config = null)
        {
            Config = config;
            if (null == Config)
                Config = Config.Load();
            InternalThreadPool = new Util.SimpleThreadPool(
                Config.InternalThreadPoolWorkerCount, "ZezeSpecialThreadPool");

            int workerThreads, completionPortThreads;
            ThreadPool.GetMinThreads(out workerThreads, out completionPortThreads);
            if (Config.WorkerThreads > 0)
                workerThreads = Config.WorkerThreads;
            if (Config.CompletionPortThreads > 0)
                completionPortThreads = Config.CompletionPortThreads;
            ThreadPool.SetMinThreads(workerThreads, completionPortThreads);

            Config.CreateDatabase(Databases);
            GlobalAgent = new GlobalAgent(this);
            _checkpoint = new Checkpoint(Config.CheckpointMode, Databases.Values);
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
                ClearInUseAndIAmSureAppStopped();
                foreach (var db in Databases.Values)
                {
                    db.DirectOperates.SetInUse(Config.AutoKeyLocalId, Config.GlobalCacheManagerHostNameOrAddress);
                }

                if (IsStart)
                    return;
                IsStart = true;

                // 由于 AutoKey，TableSys需要先打开。TableSys肯定在defaultDb中。并且要在其他database都初始化table后再加入。
                TableSys = new TableSys();
                Database defaultDb = GetDatabase("");
                defaultDb.storages.Add(TableSys.Open(this, defaultDb));
                foreach (var db in Databases.Values)
                {
                    db.Open(this);
                }
                defaultDb.AddTable(TableSys);

                if (Config.GlobalCacheManagerHostNameOrAddress.Length > 0)
                {
                    GlobalAgent.Start(Config.GlobalCacheManagerHostNameOrAddress, Config.GlobalCacheManagerPort);
                }

                Checkpoint.Start(Config.CheckpointPeriod);

                /////////////////////////////////////////////////////
                /// Schemas Check
                Schemas.Compile();
                var keyOfSchemas = Zeze.Serialize.ByteBuffer.Allocate();
                keyOfSchemas.WriteString("zeze.Schemas." + Config.AutoKeyLocalId);
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

        public void ClearInUseAndIAmSureAppStopped()
        {
            foreach (var db in Databases.Values)
            {
                db.DirectOperates.ClearInUse(Config.AutoKeyLocalId, Config.GlobalCacheManagerHostNameOrAddress);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (false == IsStart)
                    return;
                ClearInUseAndIAmSureAppStopped();

                IsStart = false;
                Checkpoint?.StopAndJoin();
                GlobalAgent?.Stop();
                foreach (var db in Databases.Values)
                {
                    db.Close();
                }
                TableSys = null;
                Databases.Clear();
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
