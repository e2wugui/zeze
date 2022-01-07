using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Linq;

namespace Zeze.Transaction
{
    public sealed class Checkpoint
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private HashSet<Database> Databases { get; } = new HashSet<Database>();

        private ReaderWriterLockSlim FlushReadWriteLock { get; } = new ReaderWriterLockSlim();

        private volatile bool _IsRunning;
        public bool IsRunning
        {
            get { return _IsRunning; }
            private set { _IsRunning = value; }
        }
        public int Period { get; private set; }

        public CheckpointMode CheckpointMode { get; }
        private Thread CheckpointThread;

        public Checkpoint(CheckpointMode mode)
        {
            CheckpointMode = mode;
        }

        public Checkpoint(CheckpointMode mode, IEnumerable<Database> dbs)
        {
            CheckpointMode = mode;
            Add(dbs);
        }

        internal void EnterFlushReadLock()
        {
            if (CheckpointMode == CheckpointMode.Period)
                FlushReadWriteLock.EnterReadLock();
        }

        internal void ExitFlushReadLock()
        {
            if (CheckpointMode == CheckpointMode.Period)
                FlushReadWriteLock.ExitReadLock();
        }

        public Checkpoint Add(IEnumerable<Database> databases)
        {
            foreach (var db in databases)
            {
                this.Databases.Add(db);
            }
            return this;
        }

        internal void Start(int period)
        {
            lock (this)
            {
                if (IsRunning)
                    return;

                IsRunning = true;
                Period = period;
                CheckpointThread = new Thread(() => Zeze.Util.Task.Call(Run, "Checkpoint.Run"));
                CheckpointThread.Name = "CheckpointThread";
                CheckpointThread.Start();
            }
        }

        internal void StopAndJoin()
        {
            lock (this)
            {
                IsRunning = false;
                Monitor.Pulse(this);
            }
            CheckpointThread?.Join();
        }

        internal void RunOnce()
        {
            switch (CheckpointMode)
            {
                case CheckpointMode.Immediately:
                    break;

                case CheckpointMode.Period:
                    TaskCompletionSource<int> source = new TaskCompletionSource<int>();
                    AddActionAndPulse(() => source.SetResult(0));
                    source.Task.Wait();
                    break;

                case CheckpointMode.Table:
                    RelativeRecordSet.FlushWhenCheckpoint(this);
                    break;
            }
        }

        private void Run()
        {
            while (IsRunning)
            {
                try
                {
                    switch (CheckpointMode)
                    {
                        case CheckpointMode.Period:
                            CheckpointPeriod();
                            foreach (Action action in actionCurrent)
                            {
                                action();
                            }
                            lock (this)
                            {
                                if (actionPending.Count > 0)
                                    continue; // 如果有未决的任务，马上开始下一次 DoCheckpoint。
                            }
                            break;

                        case CheckpointMode.Table:
                            RelativeRecordSet.FlushWhenCheckpoint(this);
                            break;
                    }
                    lock (this)
                    {
                        Monitor.Wait(this, Period);
                    }
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
            }
            //logger.Fatal("final checkpoint start.");
            switch (CheckpointMode)
            {
                case CheckpointMode.Period:
                    CheckpointPeriod();
                    break;

                case CheckpointMode.Table:
                    RelativeRecordSet.FlushWhenCheckpoint(this);
                    break;
            }
            logger.Fatal("final checkpoint end.");
        }

        private List<Action> actionCurrent;
        private volatile List<Action> actionPending = new List<Action>();

        /// <summary>
        /// 增加 checkpoint 完成一次以后执行的动作，每次 FlushReadWriteLock.EnterWriteLock()
        /// 之前的动作在本次checkpoint完成时执行，之后的动作在下一次DoCheckpoint后执行。
        /// </summary>
        /// <param name="act"></param>
        internal void AddActionAndPulse(Action act)
        {
            FlushReadWriteLock.EnterReadLock();
            try
            {
                lock (this)
                {
                    actionPending.Add(act);
                    Monitor.Pulse(this);
                }
            }
            finally
            {
                FlushReadWriteLock.ExitReadLock();
            }
        }

        private void CheckpointPeriod()
        {
            // encodeN
            foreach (var db in Databases)
            {
                db.EncodeN();
            }
            // snapshot
            {
                FlushReadWriteLock.EnterWriteLock();
                try
                {
                    actionCurrent = actionPending;
                    actionPending = new List<Action>();
                    foreach (var db in Databases)
                    {
                        db.Snapshot();
                    }
                }
                finally
                {
                    FlushReadWriteLock.ExitWriteLock();
                }
            }
            // flush
            var dts = new Dictionary<Database, Database.Transaction>();
            try
            {
                foreach (var db in Databases)
                {
                    dts[db] = db.BeginTransaction();
                }
                foreach (var e in dts)
                {
                    e.Key.Flush(e.Value);
                }
                foreach (var e in dts)
                {
                    e.Value.Commit();
                }
                try
                {
                    // cleanup
                    foreach (var db in Databases)
                    {
                        db.Cleanup();
                    }
                }
                catch (Exception ex)
                {
                    logger.Fatal(ex);
                    Environment.Exit(54321);
                }
            }
            catch (Exception)
            {
                foreach (var t in dts.Values)
                {
                    try
                    {
                        t.Rollback();
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
                throw;
            }
            finally
            {
                foreach (var dt in dts)
                {
                    try
                    {
                        dt.Value.Dispose();
                    }
                    catch (Exception e)
                    {
                        logger.Error(e);
                    }
                }
            }
        }

        internal void Flush(Transaction trans)
        {
            Flush(from ra in trans.AccessedRecords.Values
                  where ra.Dirty select ra.OriginRecord);
        }

        internal void Flush(IEnumerable<Record> rs)
        {
            var dts = new Dictionary<Database, Database.Transaction>();
            try
            {
                // prepare: 编码并且为每一个数据库创建一个数据库事务。
                foreach (var r in rs)
                {
                    if (r.Table.IsMemory)
                        continue;

                    Database database = r.Table.Storage.DatabaseTable.Database;
                    if (false == dts.TryGetValue(database, out var t))
                    {
                        t = database.BeginTransaction();
                        dts.Add(database, t);
                    }
                    r.DatabaseTransactionTmp = t;
                }
                // 编码
                foreach (var r in rs)
                {
                    r.Encode0();
                }
                // 保存到数据库中
                foreach (var r in rs)
                {
                    r.Flush(r.DatabaseTransactionTmp);
                }
                // 提交。
                foreach (var t in dts.Values)
                {
                    t.Commit();
                }
                try
                {
                    // 清除状态
                    foreach (var r in rs)
                    {
                        r.Cleanup();
                    }
                }
                catch (Exception ex)
                {
                    logger.Fatal(ex);
                    Environment.Exit(54321);
                }
            }
            catch (Exception)
            {
                foreach (var t in dts.Values)
                {
                    try
                    {
                        t.Rollback();
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
                throw;
            }
            finally
            {
                foreach (var t in dts.Values)
                {
                    try
                    {
                        t.Dispose();
                    }
                    catch (Exception e)
                    {
                        logger.Error(e);
                    }
                }
            }
        }

        // under lock(rs)
        internal void Flush(RelativeRecordSet rs)
        {
            // rs.MergeTo == null &&  check outside
            if (rs.RecordSet != null)
            {
                Flush(rs.RecordSet);
                foreach (var r in rs.RecordSet)
                {
                    r.Dirty = false;
                }
            }
        }
    }
}
