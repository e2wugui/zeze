using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;
using Zeze.Transaction;

namespace Zeze
{
    public class Zeze
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public Transaction.Database Database { get; private set; }
        public Config Config { get; set; }
        public bool IsStart { get; private set; }
        internal TableSys TableSys { get; private set; }

        private Dictionary<string, Transaction.Table> tables = new Dictionary<string, Transaction.Table>();
        private List<Transaction.Storage> storages = new List<Transaction.Storage>();

        public void AddTable(Transaction.Table table)
        {
            tables.Add(table.Name, table);
        }

        public Transaction.Table GetTable(string name)
        {
            if (tables.TryGetValue(name, out var table))
            {
                return table;
            }
            return null;
        }

        public void Start()
        {
            lock (this)
            {
                if (IsStart)
                    return;

                IsStart = true;

                if (null == Config)
                    Config = Config.Load();

                switch (Config.DatabaseType)
                {
                    case Config.DbType.Memory:
                        Database = new Transaction.DatabaseMemory();
                        break;
                    case Config.DbType.MySql:
                        // TODO add mysql
                        break;
                    default:
                        throw new Exception("unknown database type.");
                }
                // 由于 AutoKey，TableSys需要先打开。
                TableSys = new TableSys();
                storages.Add(TableSys.Open(this, Database));
                foreach (Transaction.Table table in tables.Values)
                {
                    Transaction.Storage storage = table.Open(this, Database);
                    if (null != storage)
                        storages.Add(storage);
                }
                AddTable(TableSys);
                if (Config.CheckpointPeriod > 0)
                {
                    Util.Scheduler.Instance.Schedule(Checkpoint, Config.CheckpointPeriod, Config.CheckpointPeriod);
                }
            }
        }

        public void Stop()
        {
            logger.Fatal("final checkpoint start.");
            Checkpoint();
            logger.Fatal("final checkpoint end.");

            lock (this)
            {
                if (false == IsStart)
                    return;

                IsStart = false;
                foreach (Transaction.Table table in tables.Values)
                {
                    table.Close();
                }
                tables.Clear();
                storages.Clear();
                Database.Close();
                TableSys = null;
                Database = null;
            }
        }

        public void Checkpoint()
        {
            lock (this)
            {
                if (false == IsStart)
                    return;

                // try Encode. 可以多趟。
                for (int i = 1; i <= 1; ++i)
                {
                    int countEncodeN = 0;
                    foreach (Transaction.Storage storage in storages)
                    {
                        countEncodeN += storage.EncodeN();
                    }
                    logger.Info("Checkpoint EncodeN {0}/{1}", i, countEncodeN);
                }
                // snapshot
                {
                    int countEncode0 = 0;
                    int countSnapshot = 0;
                    Transaction.Transaction.FlushReadWriteLock.EnterWriteLock();
                    try
                    {
                        foreach (Transaction.Storage storage in storages)
                        {
                            countEncode0 += storage.Encode0();
                        }
                        foreach (Transaction.Storage storage in storages)
                        {
                            countSnapshot += storage.Snapshot();
                        }
                    }
                    finally
                    {
                        Transaction.Transaction.FlushReadWriteLock.ExitWriteLock();
                    }

                    logger.Info("Checkpoint Encode0 And Snapshot countEncode0={0} countSnapshot={1}", countEncode0, countSnapshot);
                }
                // flush
                // TODO 如果和真正的数据库连接断开，这里应该重做。
                int countFlush = 0;
                foreach (Transaction.Storage storage in storages)
                {
                    countFlush += storage.Flush();
                }
                logger.Info("Checkpoint Flush count={0}", countFlush);
                // checkpoint
                Database.Checkpoint();
            }
        }

        public Zeze()
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
