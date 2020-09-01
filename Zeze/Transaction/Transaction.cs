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
    public class Transaction
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
            // 允许没有 savepoint 时返回 null.
            return savepoints.Count > 0 ? savepoints[^1].GetLog(key) : null;
        }

        public void PutLog(Log log)
        {
            savepoints[^1].PutLog(log);
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
                    procedure.Checkpoint.FlushReadWriteLock.EnterReadLock();
                    CheckResult checkResult = CheckResult.Redo; // 用来决定是否释放锁，除非 _lock_and_check_ 明确返回需要释放锁，否则都不释放。
                    try
                    {
                        int result = procedure.Call();
                        if ((result == Procedure.Success && savepoints.Count != 1) || (result != Procedure.Success && savepoints.Count != 0))
                        {
                            // 这个错误不应该重做
                            logger.Fatal("Transaction.Perform:{0}. savepoints.Count != 1.", procedure);
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
                            return result;
                        }
                        // retry
                        //logger.Trace("Transaction.Perform:{0} retry {1}", procedure, tryCount);
                    }
                    catch (RedoAndReleaseLockException)
                    {
                        checkResult = CheckResult.RedoAndReleaseLock;
                        logger.Debug("RedoAndReleaseLockException");
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "Transaction.Perform:{0} exception. run count:{1}", procedure, tryCount);
                        // 如果异常是因为 数据不一致引入，需要回滚重做，否则事务失败
                        if (savepoints.Count != 0)
                        {
                            // 这个错误不应该重做
                            logger.Fatal(e, "Transaction.Perform:{0}. exception. savepoints.Count != 1.", procedure);
                            return Procedure.ErrorSavepoint;
                        }
#if DEBUG
                        // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                        if (e.GetType().Name == "AssertFailedException")
                        {
                            throw;
                        }
#endif
                        checkResult = _lock_and_check_();
                        if (checkResult == CheckResult.Success)
                        {
                            return Procedure.Excption;
                        }
                        // retry
                    }
                    finally
                    {
                        // retry 保持已有的锁，清除记录和保存点。
                        procedure.Checkpoint.FlushReadWriteLock.ExitReadLock();
                        accessedRecords.Clear();
                        savepoints.Clear();
                        if (checkResult == CheckResult.RedoAndReleaseLock)
                        {
                            foreach (var holdLock in holdLocks)
                            {
                                holdLock.ExitLock();
                            }
                            holdLocks.Clear();
                        }
                    }
                    if (checkResult == CheckResult.RedoAndReleaseLock)
                        procedure.Checkpoint.WaitRun(); // XXX
                }
                logger.Error("Transaction.Perform:{0}. too many try.", procedure);
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
            // if any changes
            // 有可能出现fieldLoggers.count == 0 而 beanRootLogger.count > 0 的情况
            // 还有可能 put 新记录，这时 fieldLogger和beanRootLogger都为空
            //if (cacheRecords.Values.Any(r => r.Dirty))

            // 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
            // 在最终提交里可以实现每事务checkpoint。
            try
            {
                Savepoint last = savepoints[^1];
                last.Commit();
                //savepoints.Clear();

                foreach (var e in accessedRecords)
                {
                    if (e.Value.Dirty)
                    {
                        e.Value.OriginRecord.Commit(e.Value);
                    }
                }

                //cachedRecords.Clear();
            }
            catch (Exception e)
            {
                logger.Error(e, "Transaction._final_commit_ {0}", procedure);
                Environment.Exit(54321);
            }
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
            accessedRecords.Add(key, r);
            r.InitTableKey(key);
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
                    case GlobalCacheManager.StateInvalid:
                        return CheckResult.Redo; // AndReleaseLock; // 写锁发现Invalid，肯定有Reduce请求。

                    case GlobalCacheManager.StateModify:
                        return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success;

                    case GlobalCacheManager.StateShare:
                        // 这里可能死锁：另一个先获得提升的请求要求本机Recude，但是本机Checkpoint无法进行下去，被当前事务挡住了。
                        // 通过 GlobalCacheManager 检查死锁，返回失败;需要重做并释放锁。
                        if (e.OriginRecord.Acquire(GlobalCacheManager.StateModify) != GlobalCacheManager.StateModify)
                        {
                            logger.Warn("Acquire Faild. Maybe DeadLock Found");
                            return CheckResult.RedoAndReleaseLock;
                        }
                        e.OriginRecord.State = GlobalCacheManager.StateModify;
                        return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success;
                }
                return e.Timestamp != e.OriginRecord.Timestamp ? CheckResult.Redo : CheckResult.Success; // imposible
            }
            else
            {
                if (e.OriginRecord.State == GlobalCacheManager.StateInvalid)
                    return CheckResult.Redo;
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
