using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Reflection.PortableExecutable;
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

        private static System.Threading.ThreadLocal<Transaction> threadLocal = new System.Threading.ThreadLocal<Transaction>();

        public static Transaction Current => threadLocal.Value;

        public static Transaction Create()
        {
            if (null == threadLocal.Value)
                threadLocal.Value = new Transaction();
            return threadLocal.Value;
        }

        public static void Destroy()
        {
            threadLocal.Value = null;
        }

        public void Begin()
        {
            Savepoint sp = savepoints.Count > 0 ? savepoints[^1].Duplicate() : new Savepoint();
            savepoints.Add(sp);
        }

        public void Commit()
        {
            if (savepoints.Count > 1)
            {
                // 嵌套事务，把日志合并到上一层。
                int lastIndex = savepoints.Count - 1;
                Savepoint last = savepoints[lastIndex];
                savepoints.RemoveAt(lastIndex);
                savepoints[^1].Merge(last);
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
            int lastIndex = savepoints.Count - 1;
            Savepoint last = savepoints[lastIndex];
            savepoints.RemoveAt(lastIndex);
            last.Rollback();
        }

        public Log GetLog(long key)
        {
            // 允许没有 savepoint 时返回 null. 就是说允许在保存点不存在时进行读取操作。
            return savepoints.Count > 0 ? savepoints[^1].GetLog(key) : null;
        }

        public void PutLog(Log log)
        {
            savepoints[^1].PutLog(log);
        }

        public ChangeNote GetOrAddChangeNote(long key, Func<ChangeNote> factory)
        {
            // 必须存在 Savepoint. 可能是为了修改。
            return savepoints[^1].GetOrAddChangeNote(key, factory);
        }

        /*
        public void PutChangeNote(long key, ChangeNote note)
        {
            savepoints[~1].PutChangeNote(key, note);
        }
        */

        private readonly List<Action> CommitActions = new List<Action>();
        private readonly List<Action> RollbackActions = new List<Action>();

        public void RunWhileCommit(Action action)
        {
            CommitActions.Add(action);
        }

        public void RunWhileRollback(Action action)
        {
            RollbackActions.Add(action);
        }

        /// <summary>
        /// Procedure 第一层入口，总的处理流程，包括重做和所有错误处理。
        /// </summary>
        /// <param name="procedure"></param>
        internal int Perform(Procedure procedure)
        {
            try
            {
                for (int tryCount = 0; tryCount < 256; ++tryCount) // 最多尝试次数
                {
                    try
                    {
                        // 默认在锁内重复尝试，除非CheckResult.RedoAndReleaseLock，否则由于CheckResult.Redo保持锁会导致死锁。
                        procedure.Checkpoint.FlushReadWriteLock.EnterReadLock();
                        for (/* out loop */; tryCount < 256; ++tryCount) // 最多尝试次数
                        {
                            CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
                            try
                            {
                                int result = procedure.Call();
                                if ((result == Procedure.Success && savepoints.Count != 1) || (result != Procedure.Success && savepoints.Count != 0))
                                {
                                    // 这个错误不应该重做
                                    logger.Fatal("Transaction.Perform:{0}. savepoints.Count != 1.", procedure);
                                    _final_rollback_(procedure);
                                    return Procedure.ErrorSavepoint;
                                }
                                checkResult = _lock_and_check_();
                                if (checkResult == CheckResult.Success)
                                {
                                    if (result == Procedure.Success)
                                    {
                                        _final_commit_(procedure);
                                        return Procedure.Success;
                                    }
                                    _final_rollback_(procedure);
                                    return result;
                                }
                                // retry
                            }
                            catch (RedoAndReleaseLockException redorelease)
                            {
                                checkResult = CheckResult.RedoAndReleaseLock;
                                logger.Debug(redorelease, "RedoAndReleaseLockException");
                            }
                            catch (Exception e)
                            {
                                // 如果异常是因为 数据不一致引入，需要回滚重做，否则事务失败
                                logger.Error(e, "Transaction.Perform:{0} exception. run count:{1}", procedure, tryCount);
                                if (savepoints.Count != 0)
                                {
                                    // 这个错误不应该重做
                                    logger.Fatal(e, "Transaction.Perform:{0}. exception. savepoints.Count != 0.", procedure);
                                    _final_rollback_(procedure);
                                    return Procedure.ErrorSavepoint;
                                }
#if DEBUG
                                // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                                if (e.GetType().Name == "AssertFailedException")
                                {
                                    _final_rollback_(procedure);
                                    throw;
                                }
#endif
                                checkResult = _lock_and_check_();
                                if (checkResult == CheckResult.Success)
                                {
                                    _final_rollback_(procedure);
                                    return Procedure.Excption;
                                }
                                // retry
                            }
                            finally
                            {
                                if (checkResult == CheckResult.RedoAndReleaseLock)
                                {
                                    foreach (var holdLock in holdLocks)
                                    {
                                        holdLock.ExitLock();
                                    }
                                    holdLocks.Clear();
                                }
                                // retry 可能保持已有的锁，清除记录和保存点。
                                accessedRecords.Clear();
                                savepoints.Clear();
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
                        procedure.Checkpoint.FlushReadWriteLock.ExitReadLock();
                    }
                    //logger.Debug("Checkpoint.WaitRun {0}", procedure);
                    procedure.Checkpoint.WaitRun();
                }
                logger.Error("Transaction.Perform:{0}. too many try.", procedure);
                _final_rollback_(procedure);
                return Procedure.TooManyTry;
            }
            finally
            {
                foreach (var holdLock in holdLocks)
                {
                    holdLock.ExitLock();
                }
                holdLocks.Clear();
            }
        }

        private void _final_commit_(Procedure procedure)
        {
            // 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
            // 可以在最终提交里可以实现每事务checkpoint。
            ChangeCollector cc = new ChangeCollector();
            try
            {
                savepoints[^1].Commit();
                foreach (var e in accessedRecords)
                {
                    if (e.Value.Dirty)
                    {
                        e.Value.OriginRecord.Commit(e.Value);
                        cc.CollectChanged(e.Key, e.Value, null); // 首先对脏记录创建Table,Record相关Collector。
                    }
                }
            }
            catch (Exception e)
            {
                logger.Error(e, "Transaction._final_commit_ {0}", procedure);
                Environment.Exit(54321);
            }

            Savepoint sp = savepoints[^1];
            foreach (Log log in sp.Logs.Values)
            {
                if (log.Bean == null)
                    continue; // 特殊日志没有Bean。

                if (false == accessedRecords.TryGetValue(log.Bean.TableKey, out var ar))
                    continue; // 应该不会出现找不到的情况，先判断一下，慢慢考虑。

                if (false == ar.Dirty)
                    continue; // 应该不会出现有log但是又不是dirty的，先判断一下，慢慢考虑。

                // 写成回调是为了优化，仅在需要的时候才创建path。
                cc.CollectChanged(log.Bean.TableKey, ar, (out List<KeyValuePair<Bean, int>>  path, out ChangeNote note) =>
                {
                    path = new List<KeyValuePair<Bean, int>>();
                    note = null;
                    path.Add(KeyValuePair.Create(log.Bean, log.VariableId));
                    log.Bean.BuildChangeListenerPath(path);
                });
            }
            foreach (ChangeNote cn in sp.ChangeNotes.Values)
            {
                if (cn.Bean == null)
                    continue;

                if (false == accessedRecords.TryGetValue(cn.Bean.TableKey, out var ar))
                    continue; // 应该不会出现找不到的情况，先判断一下，慢慢考虑。

                if (false == ar.Dirty)
                    continue; // 应该不会出现有log但是又不是dirty的，先判断一下，慢慢考虑。

                // 写成回调是为了优化，仅在需要的时候才创建path。
                cc.CollectChanged(cn.Bean.TableKey, ar, (out List<KeyValuePair<Bean, int>> path, out ChangeNote note) =>
                {
                    path = new List<KeyValuePair<Bean, int>>();
                    note = cn;
                    path.Add(KeyValuePair.Create(cn.Bean.Parent, cn.Bean.VariableId));
                    cn.Bean.Parent.BuildChangeListenerPath(path);
                });
            }

            savepoints.Clear(); // not need
            accessedRecords.Clear(); // not need
            cc.Notify();

            foreach (Action action in CommitActions)
            {
                try
                {
                    action();
                }
                catch (Exception e)
                {
                    logger.Error(e, "Commit Procedure {0} Action {1}", procedure, action.Method.Name);
                }
            }
            CommitActions.Clear();
        }

        private void _final_rollback_(Procedure procedure)
        {
            foreach (Action action in RollbackActions)
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
            RollbackActions.Clear();
        }

        private readonly List<Lockey> holdLocks = new List<Lockey>(); // 读写锁的话需要一个包装类，用来记录当前维持的是哪个锁。

        public class RecordAccessed : Bean
        {
            public Record OriginRecord { get; }
            public long Timestamp { get; }
            public bool Dirty { get; set; }

            public Bean NewestValue()
            {
                PutLog log = (PutLog)Current.GetLog(ObjectId);
                if (null != log)
                    return log.Value;
                return OriginRecord.Value;
            }

            // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。
            public PutLog CommittedPutLog { get; private set; }

            public class PutLog : Log<RecordAccessed, Bean>
            {
                public PutLog(RecordAccessed bean, Bean putValue) : base(bean, putValue)
                {
                }

                public override long LogKey => Bean.ObjectId;

                public override void Commit()
                {
                    RecordAccessed host = (RecordAccessed)Bean;
                    host.CommittedPutLog = this; // 肯定最多只有一个 PutLog。由 LogKey 保证。
                }
            }

            public RecordAccessed(Record originRecord)
            {
                OriginRecord = originRecord;
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

            protected override void InitChildrenTableKey(TableKey root)
            {
            }

            public override void Decode(ByteBuffer bb)
            {
            }

            public override void Encode(ByteBuffer bb)
            {
            }
        }

        private readonly SortedDictionary<TableKey, RecordAccessed> accessedRecords = new SortedDictionary<TableKey, RecordAccessed>();
        private readonly List<Savepoint> savepoints = new List<Savepoint>();

        /// <summary>
        /// 只能添加一次。
        /// </summary>
        /// <param name="key"></param>
        /// <param name="r"></param>
        internal void AddRecordAccessed(TableKey key, RecordAccessed r)
        {
            r.InitTableKey(key, null);
            accessedRecords.Add(key, r);
        }

        internal RecordAccessed GetRecordAccessed(TableKey key)
        {
            if (accessedRecords.TryGetValue(key, out var record))
            {
                return record;
            }
            return null;
        }

        enum CheckResult
        {
            Success,
            Redo,
            RedoAndReleaseLock
        }
        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private CheckResult _check_(bool writeLock, RecordAccessed e)
        {
            if (writeLock)
            {
                switch (e.OriginRecord.State)
                {
                    case GlobalCacheManager.StateRemoved:
                        // fall down
                    case GlobalCacheManager.StateInvalid:
                        return CheckResult.RedoAndReleaseLock; // 写锁发现Invalid，肯定有Reduce请求。

                    case GlobalCacheManager.StateModify:
                        return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success;

                    case GlobalCacheManager.StateShare:
                        // 这里可能死锁：另一个先获得提升的请求要求本机Recude，但是本机Checkpoint无法进行下去，被当前事务挡住了。
                        // 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
                        if (e.OriginRecord.Acquire(GlobalCacheManager.StateModify) != GlobalCacheManager.StateModify)
                        {
                            logger.Warn("Acquire Faild. Maybe DeadLock Found {0}", e.OriginRecord);
                            e.OriginRecord.State = GlobalCacheManager.StateInvalid;
                            return CheckResult.RedoAndReleaseLock;
                        }
                        e.OriginRecord.State = GlobalCacheManager.StateModify;
                        return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success;
                }
                return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success; // imposible
            }
            else
            {
                if (e.OriginRecord.State == GlobalCacheManager.StateInvalid
                    || e.OriginRecord.State == GlobalCacheManager.StateRemoved)
                    return CheckResult.RedoAndReleaseLock; // 发现Invalid，肯定有Reduce请求或者被Cache清理，此时保险起见释放锁。
                return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success;
            }
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private CheckResult _lock_and_check_(KeyValuePair<TableKey, RecordAccessed> e)
        {
            Lockey lockey = Locks.Instance.Get(e.Key);
            bool writeLock = e.Value.Dirty;
            lockey.EnterLock(writeLock);
            holdLocks.Add(lockey);
            return _check_(writeLock, e.Value);
        }

        private CheckResult _lock_and_check_()
        {
            if (savepoints.Count > 0)
            {
                // 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；其他情况属于Begin,Commit,Rollback不匹配。外面检查。
                foreach (var log in savepoints[^1].Logs.Values)
                {
                    if (log.Bean == null)
                        continue; // 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。现在不会有这种情况，保留给未来扩展需要。

                    TableKey tkey = log.Bean.TableKey;
                    if (accessedRecords.TryGetValue(tkey, out var record))
                    {
                        record.Dirty = true;
                    }
                    else
                    { 
                        logger.Fatal("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
                    }
                }
            }

            bool conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
            if (holdLocks.Count == 0)
            {
                foreach (var e in accessedRecords)
                {
                    switch (_lock_and_check_(e))
                    {
                        case CheckResult.Success: break;
                        case CheckResult.Redo: conflict = true; break; // continue lock
                        case CheckResult.RedoAndReleaseLock: return CheckResult.RedoAndReleaseLock;
                    }
                }
                return conflict ? CheckResult.Redo : CheckResult.Success;
            }

            int index = 0;
            int n = holdLocks.Count;
            foreach (var e in accessedRecords)
            {
                // 如果 holdLocks 全部被对比完毕，直接锁定它
                if (index >= n)
                {
                    switch (_lock_and_check_(e))
                    {
                        case CheckResult.Success: break;
                        case CheckResult.Redo: conflict = true; break; // continue lock
                        case CheckResult.RedoAndReleaseLock: return CheckResult.RedoAndReleaseLock;
                    }
                    continue;
                }

                Lockey curLock = holdLocks[index];
                int c = curLock.TableKey.CompareTo(e.Key);

                // holdlocks a  b  ...
                // needlocks a  b  ...
                if (c == 0)
                {
                    // 这里可能发生读写锁提升
                    if (e.Value.Dirty && false == curLock.isWriteLockHeld())
                    {
                        curLock.EnterLock(true);
                        switch (_check_(true, e.Value))
                        {
                            case CheckResult.Success: break;
                            case CheckResult.Redo: conflict = true; break; // continue lock
                            case CheckResult.RedoAndReleaseLock: return CheckResult.RedoAndReleaseLock;
                        }
                    }
                    // else 已经持有读锁，不可能被修改也不可能降级(reduce)，所以不做检测了。
                    // 已经锁定了，跳过
                    ++index;
                    continue;
                }
                // holdlocks a  b  ...
                // needlocks a  c  ...
                if (c < 0)
                {
                    // 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
                    int unlockEndIndex = index;
                    for (; unlockEndIndex < n && holdLocks[unlockEndIndex].TableKey.CompareTo(e.Key) < 0; ++unlockEndIndex)
                    {
                        var toUnlockLocker = holdLocks[unlockEndIndex];
                        toUnlockLocker.ExitLock();
                    }
                    holdLocks.RemoveRange(index, unlockEndIndex - index);
                    n = holdLocks.Count;
                    continue;
                }

                // holdlocks a  c  ...
                // needlocks a  b  ...
                // 为了不违背锁序，释放从当前锁开始的所有锁
                for (int i = index; i < n; ++i)
                {
                    var toUnlockLocker = holdLocks[i];
                    toUnlockLocker.ExitLock();
                }
                holdLocks.RemoveRange(index, n - index);
                n = holdLocks.Count;
            }
            return conflict ? CheckResult.Redo : CheckResult.Success;
        }
    }
}
