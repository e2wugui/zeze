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
        private Util.SchedulerTask checkpointTask;
        internal GlobalAgent GlobalAgent { get; } = new GlobalAgent();

        public Application(Config config = null)
        {
            Config = config;
            if (null == Config)
                Config = Config.Load();
            Config.CreateDatabase(Databases);
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

        public Database GetDatabase(string name)
        {
            if (Databases.TryGetValue(name, out var exist))
            {
                return exist;
            }
            throw new Exception($"database not exist name={name}");
        }

        public void Start()
        {
            lock (this)
            {
                if (IsStart)
                    return;
                IsStart = true;

                // TODO 根据配置启动GlobalAgent。

                // 由于 AutoKey，TableSys需要先打开。TableSys肯定在defaultDb中。并且要在其他database都初始化table后再加入。
                TableSys = new TableSys();
                Database defaultDb = GetDatabase("");
                defaultDb.storages.Add(TableSys.Open(this, defaultDb));
                foreach (var db in Databases.Values)
                {
                    db.Open(this);
                }
                defaultDb.AddTable(TableSys);

                if (Config.CheckpointPeriod > 0)
                {
                    checkpointTask = Util.Scheduler.Instance.Schedule(Checkpoint, Config.CheckpointPeriod, Config.CheckpointPeriod);
                }
            }
        }

        public void Stop()
        {
            checkpointTask?.Cancel();
            checkpointTask = null;

            logger.Fatal("final checkpoint start.");
            Checkpoint();
            logger.Fatal("final checkpoint end.");

            lock (this)
            {
                if (false == IsStart)
                    return;
                IsStart = false;

                foreach (var db in Databases.Values)
                {
                    db.Close();
                }
                TableSys = null;
                Databases.Clear();
            }
        }

        public void Checkpoint()
        {
            new Checkpoint(Databases.Values).Run();
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
