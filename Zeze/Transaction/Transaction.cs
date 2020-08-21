using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Reflection.PortableExecutable;
using System.Threading.Tasks;
using Zeze.Transaction.Collections;
using System.Runtime.CompilerServices;

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
        public int Perform(Procedure procedure)
        {
            try
            {
                for (int tryCount = 0; tryCount < 256; ++tryCount) // 最多尝试次数
                {
                    try
                    {
                        int result = procedure.Call();
                        if ((result == Procedure.Success && savepoints.Count != 1) || (result != Procedure.Success && savepoints.Count != 0))
                        {
                            // 这个错误不应该重做
                            logger.Fatal("Transaction.Perform:{0}. savepoints.Count != 1.", procedure);
                            break;
                        }
                        if (_lock_and_check_())
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
                    catch (Exception e)
                    {
                        logger.Error(e, "Transaction.Perform:{0} exception. run count:{1}", procedure, tryCount);
                        // 如果异常是因为 数据不一致引入，需要回滚重做，否则事务失败
                        if (savepoints.Count != 0)
                        {
                            // 这个错误不应该重做
                            logger.Fatal(e, "Transaction.Perform:{0}. exception. savepoints.Count != 1.", procedure);
                            break;
                        }
#if DEBUG
                        // 对于 unit test 的异常特殊处理，与unit test框架能搭配工作
                        if (e.GetType().Name == "AssertFailedException")
                        {
                            throw;
                        }
#endif
                        if (_lock_and_check_())
                        {
                            return Procedure.Excption;
                        }
                        // retry
                    }
                    finally
                    {
                        // retry 保持已有的锁，清除记录和保存点。
                        cachedRecords.Clear();
                        savepoints.Clear();
                    }
                }
                logger.Error("Transaction.Perform:{0}. too many try.", procedure);
                return Procedure.TooManyTry;
            }
            finally
            {
                foreach (var holdLock in holdLocks)
                {
                    holdLock.Exit();
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

                foreach (var e in cachedRecords)
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

        public class CachedRecord
        {
            public Record OriginRecord { get; }
            public long Timestamp { get; }
            public Bean PutValue { get; private set; }
            public bool PutValueChannged { get; private set; } // 使用 bool 变量表示是否改过，减少 new 对象。
            public bool Dirty { get; set; }

            public Bean NewValue => PutValueChannged ? PutValue : OriginRecord.Value;

            public void Put(Bean putValue)
            {
                PutValueChannged = true;
                PutValue = putValue;
                Dirty = true;
            }

            public void Remove()
            {
                Put(null);
            }

            public CachedRecord(Record originRecord)
            {
                OriginRecord = originRecord;
                Timestamp = originRecord.Timestamp;
                // PutValue = null;
                // PutValueChannged = false;
                // Dirty = false;
            }
        }

        private readonly SortedDictionary<TableKey, CachedRecord> cachedRecords = new SortedDictionary<TableKey, CachedRecord>();
        private readonly List<Savepoint> savepoints = new List<Savepoint>();

        /// <summary>
        /// 只能添加一次。
        /// </summary>
        /// <param name="key"></param>
        /// <param name="cr"></param>
        internal void AddCachedRecord(TableKey key, CachedRecord cr)
        {
            cachedRecords.Add(key, cr);
        }

        internal CachedRecord GetCachedRecord(TableKey key)
        {
            if (cachedRecords.TryGetValue(key, out var record))
            {
                return record;
            }
            return null;
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        private bool _lock_and_check_timestamp_(KeyValuePair<TableKey, CachedRecord> e)
        {
            Lockey lck = e.Key.Lockey;
            //bool writeLock = e.Value.Dirty;
            lck.Enter();
            holdLocks.Add(lck);
            return e.Value.Timestamp != e.Value.OriginRecord.Timestamp;
        }

        private bool _lock_and_check_()
        {
            // 将modified fields 的 root 标记为 dirty
            if (savepoints.Count > 0) // 全部 Rollback 时 Count 为 0；最后提交时 Count 必须为 1；其他情况属于Begin,Commit,Rollback不匹配。外面检查。 
            {
                foreach (var log in savepoints[^1].Logs.Values)
                {
                    TableKey tkey = log.Bean.TableKey;
                    if (cachedRecords.TryGetValue(tkey, out var record))
                        record.Dirty = true;
                    else
                        logger.Fatal("impossible! record not found."); // 只有测试代码会把非 Managed 的 Bean 的日志加进来。
                }
            }

            bool conflict = false; // 冲突了，也继续加锁，为重做做准备！！！
            if (holdLocks.Count == 0)
            {
                foreach (var e in cachedRecords)
                {
                    conflict |= _lock_and_check_timestamp_(e);
                }
                return !conflict;
            }

            int index = 0;
            int n = holdLocks.Count;
            foreach (var e in cachedRecords)
            {
                // 如果 holdLocks 全部被对比完毕，直接锁定它
                if (index >= n)
                {
                    conflict |= _lock_and_check_timestamp_(e);
                    continue;
                }

                Lockey curLock = holdLocks[index];
                int c = curLock.TableKey.CompareTo(e.Key);

                // holdlocks a  b  ...
                // needlocks a  b  ...
                if (c == 0)
                {
                    /*
                    if (writeLock && !curLock.WriteLock) // 如果需要持有写锁，但当前仅持有读锁
                    {
                        // 理论上，读锁提升为写锁，不应该发生timestamp改变的。
                        // 不过实现上有可能是先放了读锁，再重新加写锁，因此有一定机会timestamp发生变化
                        conflict |= _lock_and_check_timestamp_(e);
                    }
                    */
                    // 已经锁定了，跳过
                    ++index;
                    continue;
                }
                // holdlocks a  b  ...
                // needlocks a  c  ...
                if (c < 0)
                {
                    // TODO 理论上有优化空间，可以先 TryLock 尝试加锁，失败后再放锁。但概率不高，意义不大？
                    // 释放掉 比当前锁序小的锁，因为当前事务中不再需要这些锁
                    int unlockEndIndex = index;
                    for (; unlockEndIndex < n && holdLocks[unlockEndIndex].TableKey.CompareTo(e.Key) < 0; ++unlockEndIndex)
                    {
                        var toUnlockLocker = holdLocks[unlockEndIndex];
                        toUnlockLocker.Exit();
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
                    toUnlockLocker.Exit();
                }
                holdLocks.RemoveRange(index, n - index);
                n = holdLocks.Count;
            }
            return !conflict;
        }
    }
}
