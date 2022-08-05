using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Zeze.Transaction.Collections;
using System.Runtime.CompilerServices;
using Zeze.Serialize;
using Zeze.Services;
using System.Threading;

namespace Zeze.Transaction
{
    public sealed class Transaction
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private static readonly AsyncLocal<Transaction> asyncLocal = new();

        public static Transaction Current => asyncLocal.Value;

        // 嵌套存储过程栈。
        public List<Procedure> ProcedureStack { get; } = new List<Procedure>();

        public Procedure TopProcedure => ProcedureStack.Count == 0 ? null : ProcedureStack[^1];

        /*
        private void ReuseTransaction()
        {
            this.Created = false;

            this.AccessedRecords.Clear();
            //this.holdLocks.Clear(); // 执行完肯定清理了。
            this.State = TransactionState.Running;
            this.ProcedureStack.Clear();
            this.LastRollbackActions = null;
            this.Savepoints.Clear();
        }
        */

        private Locks Locks;

        public static Transaction Create(Locks locks)
        {
            if (null == asyncLocal.Value)
            {
                var tmp = new Transaction
                {
                    Locks = locks
                };
                asyncLocal.Value = tmp;
                return tmp;
            }
            throw new Exception("Transaction Has Created!");
            /*
            else
            {
                var tmp = asyncLocal.Value;
                tmp.Locks = locks;
                tmp.Created = true;
                return tmp;
            }
            */
        }

        public static void Destroy()
        {
            asyncLocal.Value = null; //.ReuseTransaction();
        }

        public void Begin()
        {
            Savepoint sp = Savepoints.Count > 0 ? Savepoints[^1].Duplicate() : new Savepoint();
            Savepoints.Add(sp);
        }

        public void Commit()
        {
            if (Savepoints.Count > 1)
            {
                // 嵌套事务，把日志合并到上一层。
                int lastIndex = Savepoints.Count - 1;
                Savepoint last = Savepoints[lastIndex];
                Savepoints.RemoveAt(lastIndex);
                Savepoints[lastIndex - 1].MergeFrom(last, true);
            }
            /*
            else
            {
                // 最外层存储过程提交在 Perform 中处理
            }
            */
        }

        public void Rollback()
        {
            int lastIndex = Savepoints.Count - 1;
            Savepoint last = Savepoints[lastIndex];
            Savepoints.RemoveAt(lastIndex);
            last.Rollback();

            if (lastIndex > 0)
            {
                Savepoints[lastIndex - 1].MergeFrom(last, false);
            }
            else
            {
                // 最后一个Savepoint Rollback的时候需要保存一下，用来触发回调。ugly。
                LastRollbackActions = last.RollbackActions;
            }
        }

        public bool TryGetLog(long logKey, out Log log)
        {
            log = GetLog(logKey);
            return null != log;
        }

        public Log LogGetOrAdd(long logKey, Func<Log> logFactory)
        {
            var log = GetLog(logKey);
            if (null == log)
            {
                log = logFactory();
                PutLog(log);
            }
            return log;
        }

        public Log GetLog(long key)
        {
            VerifyRunningOrCompleted();
            // 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
            return Savepoints.Count > 0 ? Savepoints[^1].GetLog(key) : null;
        }

        public void PutLog(Log log)
        {
            VerifyRunning();
            Savepoints[^1].PutLog(log);
        }

        private List<Action> LastRollbackActions;
        private readonly List<Action> RedoActions = new();

        private void TriggerRedoActions()
        {
            foreach (var a in RedoActions)
                a(); // redo action 的回调不处理异常。向外面抛出并中断事务。
        }

        public static void WhileRollback(Action action)
        {
            Current.RunWhileRollback(action);
        }

        public static void WhileCommit(Action action)
        {
            Current.RunWhileCommit(action);
        }

        internal static void WhileRedo(Action action)
        {
            Current.RedoActions.Add(action);
        }

        public void RunWhileCommit(Action action)
        {
            VerifyRunning();
            Savepoints[^1].CommitActions.Add(action);
        }

        public void RunWhileRollback(Action action)
        {
            VerifyRunning();
            Savepoints[^1].RollbackActions.Add(action);
        }

        private bool AlwaysReleaseLockWhenRedo = false;
        internal void SetAlwaysReleaseLockWhenRedo()
        {
            AlwaysReleaseLockWhenRedo = true;
            if (holdLocks.Count > 0)
                this.ThrowRedo();
        }

        /// <summary>
        /// Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
        /// </summary>
        /// <param name="procedure"></param>
        internal async Task<long> Perform(Procedure procedure)
        {
            try
            {
                for (int tryCount = 0; tryCount < 256; ++tryCount) // 最多尝试次数
                {
                    // 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
                    var checkpoint = procedure.Zeze.Checkpoint;
                    if (checkpoint == null)
                        return Procedure.Closed;
                    checkpoint.EnterFlushReadLock();
                    try
                    {
                        for (/* out loop */; tryCount < 256; ++tryCount) // 最多尝试次数
                        {
                            CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
                            try
                            {
                                var result = await procedure.CallAsync();
                                switch (State)
                                {
                                    case TransactionState.Running:
                                        if ((result == Procedure.Success && Savepoints.Count != 1)
                                            || (result != Procedure.Success && Savepoints.Count != 0))
                                        {
                                            // 这个错误不应该重做
                                            logger.Fatal("Transaction.Perform:{0}. savepoints.Count != 1.", procedure);
                                            FinalRollback(procedure);
                                            return Procedure.ErrorSavepoint;
                                        }
                                        checkResult = await LockAndCheck(procedure.TransactionLevel);
                                        if (checkResult == CheckResult.Success)
                                        {
                                            if (result == Procedure.Success)
                                            {
                                                await FinalCommit(procedure);
#if ENABLE_STATISTICS
                                                if (tryCount > 0)
                                                {
                                                    // 正常一次成功的不统计，用来观察redo多不多。
                                                    // 失败在 Procedure.cs 中的统计。
                                                    ProcedureStatistics.Instance.GetOrAdd("Zeze.Transaction.TryCount").GetOrAdd(tryCount).IncrementAndGet();
                                                }
#endif
                                                return Procedure.Success;
                                            }
                                            FinalRollback(procedure);
                                            return result;
                                        }
                                        break; // retry

                                    case TransactionState.Abort:
                                        logger.Debug("Transaction.Perform: Abort");
                                        FinalRollback(procedure);
                                        return Procedure.AbortException;

                                    case TransactionState.Redo:
                                        checkResult = CheckResult.Redo;
                                        break; // retry

                                    case TransactionState.RedoAndReleaseLock:
                                        checkResult = CheckResult.RedoAndReleaseLock;
                                        break; // retry
                                }
                                TriggerRedoActions();
                                // retry clear in finally
                            }
                            catch (Exception e)
                            {
                                // Procedure.Call 里面已经处理了异常。只有 unit test 或者重做或者内部错误会到达这里。
                                // 在 unit test 下，异常日志会被记录两次。
                                switch (State)
                                {
                                    case TransactionState.Running:
                                        logger.Error(e, "Transaction.Perform:{0} exception. run count:{1}", procedure, tryCount);
                                        if (Savepoints.Count != 0)
                                        {
                                            // 这个错误不应该重做
                                            logger.Fatal(e, "Transaction.Perform:{0}. exception. savepoints.Count != 0.", procedure);
                                            FinalRollback(procedure);
                                            return Procedure.ErrorSavepoint;
                                        }
#if DEBUG
                                        // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                                        if (e.GetType().Name == "AssertFailedException")
                                        {
                                            FinalRollback(procedure);
                                            throw;
                                        }
#endif
                                        checkResult = await LockAndCheck(procedure.TransactionLevel);
                                        if (checkResult == CheckResult.Success)
                                        {
                                            FinalRollback(procedure);
                                            return Procedure.Exception;
                                        }
                                        break; // retry

                                    case TransactionState.Abort:
                                        logger.Debug("Transaction.Perform: Abort");
                                        FinalRollback(procedure);
                                        return Procedure.AbortException;

                                    case TransactionState.Redo:
                                        checkResult = CheckResult.Redo;
                                        break;

                                    case TransactionState.RedoAndReleaseLock:
                                        checkResult = CheckResult.RedoAndReleaseLock;
                                        break;
                                }
                                // retry
                                if (AlwaysReleaseLockWhenRedo && checkResult == CheckResult.Redo)
                                    checkResult = CheckResult.RedoAndReleaseLock;
                                TriggerRedoActions();
                            }
                            finally
                            {
                                if (checkResult == CheckResult.RedoAndReleaseLock)
                                {
                                    foreach (var holdLock in holdLocks)
                                    {
                                        holdLock.Release();
                                    }
                                    holdLocks.Clear();
                                }
                                // retry 可能保持已有的锁，清除记录和保存点。
                                AccessedRecords.Clear();
                                Savepoints.Clear();
                                 RedoActions.Clear();

                                State = TransactionState.Running; // prepare to retry
                            }

                            if (checkResult == CheckResult.RedoAndReleaseLock)
                            {
                                //logger.Debug("CheckResult.RedoAndReleaseLock break {0}", procedure);
                                break;
                            }
                        }
                    }
                    finally
                    {
                        checkpoint.ExitFlushReadLock();
                    }
                    //logger.Debug("Checkpoint.WaitRun {0}", procedure);

                    // 实现Fresh队列以后删除Sleep。
                    Thread.Sleep(Util.Random.Instance.Next(80) + 20);
                }
                logger.Error("Transaction.Perform:{0}. too many try.", procedure);
                FinalRollback(procedure);
                return Procedure.TooManyTry;
            }
            finally
            {
                foreach (var holdLock in holdLocks)
                {
                    holdLock.Release();
                }
                holdLocks.Clear();
            }
        }

        private static void TriggerCommitActions(Procedure procedure, Savepoint lastSavepoint)
        {
            foreach (Action action in lastSavepoint.CommitActions)
            {
                try
                {
                    action();
                }
                catch (Exception e)
                {
                    logger.Error(e, "Commit Procedure {0} Action {1}", procedure, action.Method.Name);
#if DEBUG
                    // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                    if (e.GetType().Name == "AssertFailedException")
                    {
                        throw;
                    }
#endif
                }
            }
        }

        private async Task FinalCommit(Procedure procedure)
        {
            // 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
            // 可以在最终提交里可以实现每事务checkpoint。
            var lastsp = Savepoints[0];
            await RelativeRecordSet.TryUpdateAndCheckpoint(this, procedure, () =>
            {
                try
                {
                    lastsp.Commit();
                    foreach (var e in AccessedRecords)
                    {
                        e.Value.Origin.SetNotFresh();
                        if (e.Value.Dirty)
                        {
                            e.Value.Origin.Commit(e.Value);
                        }
                    }
                }
                catch (Exception e)
                {
                    logger.Error(e, "Transaction.FinalCommit {0}", procedure);
                    NLog.LogManager.Shutdown();
                    Process.GetCurrentProcess().Kill();
                }
            });

            // 禁止在listener回调中访问表格的操作。除了回调参数中给定的记录可以访问。
            // 不再支持在回调中再次执行事务。
            State = TransactionState.Completed; // 在Notify之前设置的。

            // collect logs and notify listeners
            try
            {
                var cc = new Changes(this);
                foreach (var log in lastsp.Logs.Values)
                {
                    // 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
                    if (log.Belong == null || false == log.Belong.IsManaged)
                        continue;

                    // 当changes.Collect在日志往上一级传递时调用，
                    // 第一个参数Owner为null，表示bean属于record，到达root了。
                    cc.Collect(log.Belong, log);
                }

                foreach (var ar in AccessedRecords.Values)
                {
                    if (ar.Dirty)
                        cc.CollectRecord(ar);
                }
                cc.NotifyListener();
            }
            catch (Exception ex)
            {
                logger.Error(ex);
            }

            TriggerCommitActions(procedure, lastsp);
        }

        private void FinalRollback(Procedure procedure)
        {
            foreach (var e in AccessedRecords)
            {
                e.Value.Origin.SetNotFresh();
            }
            Savepoints.Clear(); // 这里可以安全的清除日志，这样如果 rollback_action 需要读取数据，将读到原始的。
            State = TransactionState.Completed;
            if (null != LastRollbackActions)
            {
                foreach (Action action in LastRollbackActions)
                {
                    try
                    {
                        action();
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "Rollback Procedure {0} Action {1}", procedure, action.Method.Name);
                    }
                }
                LastRollbackActions = null;
            }
        }

        private readonly List<LockAsync> holdLocks = new(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。

        public class RecordAccessed : Bean
        {
            public Record Origin { get; }
            public long Timestamp { get; }
            public bool Dirty { get; set; }

            public Bean NewestValue()
            {
                PutLog log = (PutLog)Current.GetLog(ObjectId);
                if (null != log)
                    return log.Value;
                return Origin.Value;
            }

            // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。
            public PutLog CommittedPutLog { get; private set; }

            public class PutLog : Log<Bean>
            {
                public PutLog(RecordAccessed self, Bean putValue)
                {
                    Belong = self;
                    Value = putValue;
                }

                public override void Commit()
                {
                    RecordAccessed host = (RecordAccessed)Belong;
                    host.CommittedPutLog = this; // 肯定最多只有一个 PutLog。由 LogKey 保证。
                }
            }

            public RecordAccessed(Record originRecord)
            {
                Origin = originRecord;
                Timestamp = originRecord.Timestamp;
            }

            public void Put(Transaction current, Bean putValue)
            {
                current.PutLog(new PutLog(this, putValue));
            }

            public void Remove(Transaction current)
            {
                Put(current, null);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }

            protected override void ResetChildrenRootInfo()
            {
            }

            public override void Decode(ByteBuffer bb)
            {
            }

            public override void Encode(ByteBuffer bb)
            {
            }

            public override void FollowerApply(Log log)
            {
                throw new NotImplementedException();
            }
        }

        internal SortedDictionary<TableKey, RecordAccessed> AccessedRecords { get; }
            = new SortedDictionary<TableKey, RecordAccessed>();
        private readonly List<Savepoint> Savepoints = new ();

        /// <summary>
        /// 只能添加一次。
        /// </summary>
        /// <param name="key"></param>
        /// <param name="r"></param>
        internal void AddRecordAccessed(Record.RootInfo root, RecordAccessed r)
        {
            VerifyRunning();
            r.InitRootInfo(root, null);
            AccessedRecords.Add(root.TableKey, r);
        }

        internal RecordAccessed GetRecordAccessed(TableKey key)
        {
            // 允许读取事务内访问过的记录。
            VerifyRunningOrCompleted();

            if (AccessedRecords.TryGetValue(key, out var record))
                return record;

            return null;
        }

        public void VerifyRecordAccessed(Bean bean, bool isRead = false)
        {
            if (isRead) // && Zeze.Config.AllowReadWhenRecordNotAccessed)
                return; // allow read

            if (bean.RootInfo.Record.State == GlobalCacheManagerServer.StateRemoved)
                ThrowRedo(); // 这个错误需要redo。不是逻辑错误。

            var ra = GetRecordAccessed(bean.TableKey);
            if (ra == null)
                throw new Exception($"VerifyRecordAccessed: Record Not Control Under Current Transaction. {bean.TableKey}");
            if (bean.RootInfo.Record != ra.Origin)
                throw new Exception($"VerifyRecordAccessed: Record Reloaded.{bean.TableKey}");
            // 事务结束后可能会触发Listener，此时Commit已经完成，Timestamp已经改变，
            // 这种情况下不做RedoCheck，当然Listener的访问数据是只读的。
            // 【注意】这个提前检测更容易忙等，因为都没去尝试锁（这会阻塞）。
            if (ra.Origin.Table.Zeze.Config.FastRedoWhenConflict
                && State != TransactionState.Completed && ra.Origin.Timestamp != ra.Timestamp)
            {
                ThrowRedo();
            }
        }

        enum CheckResult
        {
            Success,
            Redo,
            RedoAndReleaseLock
        }
        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private async Task<CheckResult> Check(bool writeLock, RecordAccessed e)
        {
            var lockr = await e.Origin.Mutex.LockAsync();
            try
            {
                if (writeLock)
                {
                    switch (e.Origin.State)
                    {
                        case GlobalCacheManagerServer.StateRemoved:
                            // 被从cache中清除，不持有该记录的Global锁，简单重做即可。
                            return CheckResult.Redo;

                        case GlobalCacheManagerServer.StateInvalid:
                            return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，肯定有Reduce请求。

                        case GlobalCacheManagerServer.StateModify:
                            return e.Timestamp != e.Origin.Timestamp
                                ? CheckResult.Redo : CheckResult.Success;

                        case GlobalCacheManagerServer.StateShare:
                            // 这里可能死锁：另一个先获得提升的请求要求本机Reduce，但是本机Checkpoint无法进行下去，被当前事务挡住了。
                            // 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
                            var (_, ResultState) = await e.Origin.Acquire(GlobalCacheManagerServer.StateModify, e.Origin.fresh);
                            if (ResultState  != GlobalCacheManagerServer.StateModify)
                            {
                                e.Origin.SetNotFresh(); // 抢失败不再新鲜。
                                logger.Warn("Acquire Failed. Maybe DeadLock Found {0}", e.Origin);
                                e.Origin.State = GlobalCacheManagerServer.StateInvalid;
                                return CheckResult.RedoAndReleaseLock;
                            }
                            e.Origin.State = GlobalCacheManagerServer.StateModify;
                            return e.Timestamp != e.Origin.Timestamp ? CheckResult.Redo : CheckResult.Success;
                    }
                    return e.Timestamp != e.Origin.Timestamp
                        ? CheckResult.Redo : CheckResult.Success; // impossible
                }
                else
                {
                    switch (e.Origin.State)
                    {
                        case GlobalCacheManagerServer.StateRemoved:
                            // 被从cache中清除，不持有该记录的Global锁，简单重做即可。
                            return CheckResult.Redo;

                        case GlobalCacheManagerServer.StateInvalid:
                            // 发现Invalid，肯定有Reduce请求或者被Cache清理，此时保险起见释放锁。
                            return CheckResult.RedoAndReleaseLock;
                    }
                    return e.Timestamp != e.Origin.Timestamp
                        ? CheckResult.Redo : CheckResult.Success;
                }
            }
            finally
            {
                lockr.Dispose();
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private async Task<CheckResult> LockAndCheck(KeyValuePair<TableKey, RecordAccessed> e)
        {
            var lockey = Locks.Get(e.Key);
            bool writeLock = e.Value.Dirty;
            await lockey.EnterLockAsync(writeLock);
            holdLocks.Add(lockey);
            return await Check(writeLock, e.Value);
        }

        private async Task<CheckResult> LockAndCheck(TransactionLevel level)
        {
            bool allRead = true;
            if (Savepoints.Count == 1)
            {
                // 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；
                // 其他情况属于Begin,Commit,Rollback不匹配。外面检查。
                foreach (var log in Savepoints[0].Logs.Values)
                {
                    TableKey tkey = log.Belong?.TableKey;
                    if (tkey == null)
                        continue;

                    if (AccessedRecords.TryGetValue(tkey, out var record))
                    {
                        record.Dirty = true;
                        allRead = false;
                    }
                    else
                    {
                        // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
                        logger.Fatal("impossible! record not found."); 
                    }
                }
            }

            if (allRead && level == TransactionLevel.AllowDirtyWhenAllRead)
                return CheckResult.Success; // 使用一个新的enum表示一下？

            bool conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
            if (holdLocks.Count == 0)
            {
                foreach (var e in AccessedRecords)
                {
                    var checkResult = await LockAndCheck(e);
                    switch (checkResult)
                    {
                        case CheckResult.Success:
                            break;

                        case CheckResult.Redo:
                            conflict = true;
                            break; // continue lock

                        case CheckResult.RedoAndReleaseLock:
                            return CheckResult.RedoAndReleaseLock;
                    }
                }
                return conflict ? CheckResult.Redo : CheckResult.Success;
            }

            int index = 0;
            int n = holdLocks.Count;
            var itar = AccessedRecords.GetEnumerator();
            var hasNext = itar.MoveNext();
            while (hasNext)
            {
                var e = itar.Current;
                // 如果 holdLocks 全部被对比完毕，直接锁定它
                if (index >= n)
                {
                    var checkResult = await LockAndCheck(e);
                    switch (checkResult)
                    {
                        case CheckResult.Success:
                            break;

                        case CheckResult.Redo:
                            conflict = true;
                            break; // continue lock

                        case CheckResult.RedoAndReleaseLock:
                            return CheckResult.RedoAndReleaseLock;
                    }
                    hasNext = itar.MoveNext();
                    continue;
                }

                var curLock = holdLocks[index];
                int c = curLock.Lockey.TableKey.CompareTo(e.Key);

                // holdlocks a  b  ...
                // needlocks a  b  ...
                if (c == 0)
                {
                    // 这里可能发生读写锁提升
                    if (e.Value.Dirty && 2 != curLock.AcquiredType)
                    {
                        // 必须先全部释放，再升级当前记录锁，再锁后面的记录。
                        // 直接 unlockRead，lockWrite会死锁。
                        n = UnlockStart(index, n);
                        // 从当前index之后都是新加锁，并且index和n都不会再发生变化。
                        // 重新从当前 e 继续锁。
                        continue;
                    }
                    // BUG 即使锁内。Record.Global.State 可能没有提升到需要水平。需要重新_check_。
                    var checkResult = await Check(e.Value.Dirty, e.Value);
                    switch (checkResult)
                    {
                        case CheckResult.Success:
                            // 已经锁内，所以肯定不会冲突，多数情况是这个。
                            break;

                        case CheckResult.Redo:
                            // Impossible!
                            conflict = true;
                            break; // continue lock

                        case CheckResult.RedoAndReleaseLock:
                            // _check_可能需要到Global提升状态，这里可能发生GLOBAL-DEAD-LOCK。
                            return CheckResult.RedoAndReleaseLock;
                    }
                    ++index;
                    hasNext = itar.MoveNext();
                    continue;
                }
                // holdlocks a  b  ...
                // needlocks a  c  ...
                if (c < 0)
                {
                    // 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
                    int unlockEndIndex = index;
                    for (; unlockEndIndex < n && holdLocks[unlockEndIndex].Lockey.TableKey.CompareTo(e.Key) < 0; ++unlockEndIndex)
                    {
                        var toUnlockLocker = holdLocks[unlockEndIndex];
                        toUnlockLocker.Release();
                    }
                    holdLocks.RemoveRange(index, unlockEndIndex - index);
                    n = holdLocks.Count;
                    // 重新从当前 e 继续锁。
                    continue;
                }

                // holdlocks a  c  ...
                // needlocks a  b  ...
                // 为了不违背锁序，释放从当前锁开始的所有锁
                n = UnlockStart(index, n);
                // 重新从当前 e 继续锁。
            }
            return conflict ? CheckResult.Redo : CheckResult.Success;
        }

        private int UnlockStart(int index, int nLast)
        {
            for (int i = index; i < nLast; ++i)
            {
                var toUnlockLocker = holdLocks[i];
                toUnlockLocker.Release();
            }
            holdLocks.RemoveRange(index, nLast - index);
            return holdLocks.Count;
        }

        public TransactionState State { get; private set; } = TransactionState.Running;

        public void ThrowAbort(string msg = null, Exception cause = null)
        {
            if (State != TransactionState.Running)
                throw new InvalidOperationException("Abort: State Is Not Running.");
            State = TransactionState.Abort;
            throw new GoBackZezeException(msg, cause);
        }

        public void ThrowRedoAndReleaseLock(string msg = null, Exception cause = null)
        {
            if (State != TransactionState.Running)
                throw new InvalidOperationException("RedoAndReleaseLock: State Is Not Running.");
            State = TransactionState.RedoAndReleaseLock;
#if ENABLE_STATISTICS
            ProcedureStatistics.Instance.GetOrAdd(TopProcedure.ActionName).GetOrAdd(Procedure.RedoAndRelease).IncrementAndGet();
#endif
            throw new GoBackZezeException(msg, cause);
        }

        public void ThrowRedo()
        {
            if (State != TransactionState.Running)
                throw new InvalidOperationException("RedoAndReleaseLock: State Is Not Running.");
            State = TransactionState.Redo;
            throw new GoBackZezeException("Redo");
        }

        public void VerifyRunning()
        {
            switch (State)
            {
                case TransactionState.Running:
                    return;
                default:
                    throw new InvalidOperationException("State Is Not Running");
            }
        }

        public void VerifyRunningOrCompleted()
        {
            switch (State)
            {
                case TransactionState.Running:
                case TransactionState.Completed:
                    return;
                default:
                    throw new InvalidOperationException("State Is Not RunningOrCompleted");
            }
        }
    }
}
