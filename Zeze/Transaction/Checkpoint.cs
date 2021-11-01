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

        public bool IsRunning { get; private set; }
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

        internal void WaitRun()
        {
            // 严格来说，这里应该是等待一次正在进行的checkpoint，如果没有在执行中应该不启动新的checkpoint。
            // 但是由于时间窗口的原因，可能开始执行waitrun时，checkpoint还没开始，没办法进行等待。
            // 先使用RunOnce。
            this.RunOnce();
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
        private List<Action> actionPending = new List<Action>();

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
            // cleanup
            foreach (var db in Databases)
            {
                db.Cleanup();
            }
        }

        internal void Flush(Transaction trans)
        {
            Flush(from ra in trans.AccessedRecords.Values where ra.Dirty select ra.OriginRecord);
        }

        internal void Flush(IEnumerable<Record> rs)
        {
            var dts = new Dictionary<Database, Database.Transaction>();
            // prepare: 编码并且为每一个数据库创建一个数据库事务。
            foreach (var r in rs)
            {
                Database database = r.Table.Storage.DatabaseTable.Database;
                if (false == dts.TryGetValue(database, out var t))
                {
                    t = database.BeginTransaction();
                    dts.Add(database, t);
                }
                r.DatabaseTransactionTmp = t;
            }
            try
            {
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
                // 清除状态
                foreach (var r in rs)
                {
                    r.Cleanup();
                }
            }
            catch (Exception)
            {
                foreach (var t in dts.Values)
                {
                    t.Rollback();
                }
                throw;
            }
            finally
            {
                foreach (var t in dts.Values)
                {
                    t.Dispose();
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
