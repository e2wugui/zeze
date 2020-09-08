using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;
using Zeze.Transaction;

namespace Zeze
{
    public class Application
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Dictionary<string, Database> Databases { get; private set; } = new Dictionary<string, Database>();
        public Config Config { get; }
        public bool IsStart { get; private set; }
        internal TableSys TableSys { get; private set; }
        internal GlobalAgent GlobalAgent { get; }

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

        public Application(Config config = null)
        {
            Config = config;
            if (null == Config)
                Config = Config.Load();
            Config.CreateDatabase(Databases);
            GlobalAgent = new GlobalAgent(this);
            _checkpoint = new Checkpoint(Databases.Values);
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

        public Procedure NewProcedure(Func<int> func)
        {
            return new Procedure(_checkpoint, func);
        }

        public void Start()
        {
            lock (this)
            {
                if (IsStart)
                    return;
                IsStart = true;

                if (Config.GlobalCacheManagerHostNameOrAddress.Length > 0)
                {
                    GlobalAgent.Start(Config.GlobalCacheManagerHostNameOrAddress, Config.GlobalCacheManagerPort);
                }

                // 由于 AutoKey，TableSys需要先打开。TableSys肯定在defaultDb中。并且要在其他database都初始化table后再加入。
                TableSys = new TableSys();
                Database defaultDb = GetDatabase("");
                defaultDb.storages.Add(TableSys.Open(this, defaultDb));
                foreach (var db in Databases.Values)
                {
                    db.Open(this);
                }
                defaultDb.AddTable(TableSys);

                Checkpoint.Start(Config.CheckpointPeriod);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (false == IsStart)
                    return;
                IsStart = false;
                Checkpoint.StopAndJoin();
                GlobalAgent.Stop();
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
    }
}
