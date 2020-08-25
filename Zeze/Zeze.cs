using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;

namespace Zeze
{
    public class Zeze
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private Dictionary<string, Transaction.Table> tables = new Dictionary<string, Transaction.Table>();

        public Transaction.Database Database { get; private set; }
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
                if (null != Database)
                    return;

                Database = new Transaction.DatabaseMemory(); // TODO 根据配置选择不同的实现。
                foreach (Transaction.Table table in tables.Values)
                {
                    Transaction.Storage storage = table.Open(this, Database);
                    if (null != storage)
                        storages.Add(storage);
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

                foreach (Transaction.Table table in tables.Values)
                {
                    table.Close();
                }
                // tables.Clear(); // restart?
                storages.Clear();
                Database.Close();
                Database = null;
            }
        }

        public void Checkpoint()
        {
            lock (this)
            {
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
