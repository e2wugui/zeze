using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Reflection.PortableExecutable;
using System.Threading.Tasks;
using Zeze.Transaction.Collections;

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
        public bool Perform(Procedure procedure)
        {
            try
            {
                for (int tryCount = 0; tryCount < 256; ++tryCount) // 最多尝试次数
                {
                    try
                    {
                        ++tryCount;
                        bool procedureResult = procedure.Call();
                        if (_lock_and_check_())
                        {
                            if (procedureResult)
                            {
                                _final_commit_(procedure);
                                return true;
                            }
                            _final_rollback_();
                            return false;
                        }
                        _final_rollback_();
                        // retry
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "Transaction.Perform:{0} exception. run count:{1}", procedure, tryCount);
                        _final_rollback_();

                        // 如果异常是因为 数据不一致引入，需要回滚重做
                        // 否则事务失败
                        if (_lock_and_check_())
                        {
                            return false;
                        }
                        // retry
                    }
                    finally
                    {
                        // retry 保持已有的锁，清除记录和保存点。
                        cacheRecords.Clear();
                        savepoints.Clear();
                    }
                }
                _final_rollback_();
                return false;
            }
            finally
            {
                // 执行最终退出，释放锁。
                /*
                    foreach (var holdLock in _holdLocks)
                    {
                        var storageRecord = holdLock.StorageRecord;
                        storageRecord.ReleaseLock(holdLock.Locker);
                    }
                    _holdLocks.Clear();
                */
            }
        }

        private void _final_commit_(Procedure procedure)
        {
            if (savepoints.Count != 1)
                throw new Exception("savepoints.Count != 1");

            // 下面不允许失败了，因为最终提交失败，数据可能不一致，而且没法恢复。
            // 在最终提交里可以实现每事务checkpoint。
            try
            {
                Savepoint last = savepoints[^1];
                last.Commit();
            }
            catch (Exception e)
            {
                logger.Error(e, "Transaction._final_commit_ {0}", procedure);
                Environment.Exit(54321);
            }
        }

        private void _final_rollback_()
        {
            // 现在没有实现 Log.Rollback。不需要再做什么，保留接口，以后实现Rollback时再处理。
        }

        private readonly List<Record> holdLocks = new List<Record>();
        private readonly SortedDictionary<TableKey, Record> cacheRecords = new SortedDictionary<TableKey, Record>();
        private readonly List<Savepoint> savepoints = new List<Savepoint>();

        internal bool _lock_and_check_()
        {
            // 将modified fields 的 root 标记为 dirty
            /*
            foreach (var log in logs.Values)
            {
                TableKey tkey = log.Bean.TableKey;
                var record = cacheRecords[tkey];
                //record.Dirty = true;
            }
            */

            bool conflict = false;

            if (holdLocks.Count == 0)
            {
                foreach (var e in cacheRecords)
                {
                    var tkey = e.Key;
                    /*
                    bool writeLock = e.Value.Dirty;
                    var storageRecord = e.Value.StorageRecord;
                    (long newTimestamp, var releaser) = await storageRecord.GetTimestampAndLockAsync(writeLock, null);
                    _holdLocks.Add(new HoldLockInfo(tkey, storageRecord, writeLock, releaser));
                    conflict |= e.Value.Timestamp != newTimestamp;
                    logger.Trace("[new] add lock. table:{table} key:{key} oldtimestamp:{old} newtimestamp:{new} conflict:{conflict}",
                        TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, newTimestamp, conflict);
                    */
                }
            }
            else
            {
                int index = 0;
                int n = holdLocks.Count;
                foreach (var e in cacheRecords)
                {
                    TableKey tkey = e.Key;
                    object objKey = tkey.Key;
                    //bool writeLock = e.Value.Dirty;
                    //var storageRecord = e.Value.StorageRecord;

                    // 如果 holdLocks 全部被对比完毕，直接锁定它
                    if (index >= n)
                    {
                        // lock it!
                        /*
                        (long newTimestamp, var locker) = await storageRecord.GetTimestampAndLockAsync(writeLock, null);
                        _holdLocks.Add(new HoldLockInfo(tkey, storageRecord, writeLock, locker));
                        conflict |= e.Value.Timestamp != newTimestamp;
                        logger.Trace("[new] add lock. table:{table} key:{key} oldtimestamp:{old} newtimestamp:{new} conflict:{conflict}",
                            TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, newTimestamp, conflict);
                        */
                        continue;
                    }

                    //HoldLockInfo curLock = _holdLocks[index];
                    int c = 0;// curLock.Key.CompareTo(tkey);

                    // holdlocks a  b  ...
                    // needlocks a  b  ...
                    if (c == 0)
                    {
                        /*
                        if (writeLock && !curLock.WriteLock)
                        {
                            // 如果需要持有写锁，但当前仅持有读锁
                            (long newTimestamp, var newLocker) = await storageRecord.GetTimestampAndLockAsync(true, curLock.Locker);

                            curLock.WriteLock = true;
                            _holdLocks[index] = new HoldLockInfo(tkey, storageRecord, true, newLocker);
                            // 理论上，读锁提升为写锁，不应该发生timestamp改变的。
                            // 不过实现上有可能是先放了读锁，再重新加写锁，因此有一定机会timestamp发生
                            // 变化
                            conflict |= e.Value.Timestamp != newTimestamp;
                            logger.Trace("[upgrade] add lock. table:{table} key:{key} oldtimestamp:{old} newtimestamp:{new} conflict:{conflict}",
                                TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, newTimestamp, conflict);
                        }
                        else
                        {
                            logger.Trace("[nochange] add lock. table:{table} key:{key} oldtimestamp:{old} conflict:{conflict}",
                                Table.GetTable(tkey.TableId).Name, tkey.Key, e.Value.Timestamp, conflict);
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
                        //for (; unlockEndIndex < n && holdLocks[unlockEndIndex].Key.CompareTo(tkey) < 0; ++unlockEndIndex)
                        {
                            var toUnlockLocker = holdLocks[unlockEndIndex];
                            /*
                            toUnlockLocker.StorageRecord.ReleaseLock(toUnlockLocker.Locker);
                            logger.Trace("[remove] unlock not need lock. table:{table} key:{key} ",
                                TxnTable.GetTable(toUnlockLocker.Key.TableId).Name, toUnlockLocker.Key.ObjectKey);
                            */
                        }

                        holdLocks.RemoveRange(index, unlockEndIndex - index);
                        n = holdLocks.Count;
                    }
                    else
                    {
                        // holdlocks a  c  ...
                        // needlocks a  b  ...
                        // 为了不违背锁序，释放从当前锁开始的所有锁
                        for (int i = index; i < n; ++i)
                        {
                            var toUnlockLocker = holdLocks[i];
                            /*
                            toUnlockLocker.StorageRecord.ReleaseLock(toUnlockLocker.Locker);
                            logger.Trace("[remove] unlock for not violate lock order. table:{table} key:{key} ",
                                TxnTable.GetTable(toUnlockLocker.Key.TableId).Name, toUnlockLocker.Key.ObjectKey);
                            */
                        }
                        holdLocks.RemoveRange(index, n - index);
                        n = holdLocks.Count;
                    }
                }
            }
            return !conflict;
        }

        private void ProcessLog(Log log)
        {
            log.Commit();

            TableKey tkey = log.Bean.TableKey;
            var record = cacheRecords[tkey];
            // CHANGE by WALON
            //record.FieldLoggers.Add(log);
            //if (record.NewData == log.Host)
            //{
            //    record.FieldLoggers.Add(log);
            //    s_logger.Trace("FieldLogger add. key:{key} log fielid:{id} value:{log}", tkey, log.FieldId, log);
            //}
            //else
            //{
            //    s_logger.Trace("FieldLogger drop. key:{key} log fielid:{id} value:{log}", tkey, log.FieldId, log);
            //}
        }

        internal void Commit2()
        {
            /*
            foreach (var log in logs.Values)
            {
                ProcessLog(log);
            }
            */

            logger.Trace("holdlocks count:{count}", holdLocks.Count);

            // TODO 
            // 需要限制等待commit的事务数量
            // 避免Storage发生故障时，造成大量事务积压
            // 

            // if any changes
            // 有可能出现fieldLoggers.count == 0 而 beanRootLogger.count > 0 的情况
            // 还有可能 put 新记录，这时 fieldLogger和beanRootLogger都为空
            /*
            if (cacheRecords.Values.Any(r => r.Dirty))
            {
                // TODO 可以把 持久化 blog 的时机推迟，减少持锁时间
                var makeLogPromise = new TaskCompletionSource<BlobLog>();
                Task commitTask = StorageManager.Ins.CommitLogAsync(makeLogPromise.Task);
                var blog = new BlobLog();

                _writeRecords.Clear();
                foreach (var holdLock in _holdLocks)
                {
                    TKey tkey = holdLock.Key;
                    var objKey = tkey.ObjectKey;
                    var storageRecord = holdLock.StorageRecord;
                    if (holdLock.WriteLock)
                    {
                        var record = _cacheRecords[tkey];
                        if (record.Dirty)
                        {
                            _writeRecords.Add(record);
                            record.WriteToBlobLog(blog);
                            var newTimestamp = record.NewTimestamp;
                            var newData = record.NewData;
                            storageRecord.UpdateAndReleaseLock(newData, newTimestamp, record.SnapshotTimestamp == newTimestamp, holdLock.Locker);
                            logger.Trace("commit.UpdateAndReleaseWriteLock. key:{key} value:{value} log:{log} ", tkey, newData, newTimestamp);
                        }
                        else
                        {
                            storageRecord.ReleaseLock(holdLock.Locker);
                            logger.Trace("commit.ReleaseReadLock. key:{key} ", tkey);
                        }
                    }
                    else
                    {
                        Debug.Assert(!_cacheRecords[tkey].Dirty);
                        storageRecord.ReleaseLock(holdLock.Locker);
                        logger.Trace("commit.ReleaseReadLock. key:{key} ", tkey);
                    }
                }
                makeLogPromise.SetResult(blog);
                _holdLocks.Clear();
                try
                {
                    await commitTask;
                }
                catch (Exception e)
                {
                    // 理论上 CommitLog是不允许失败的，必须持续提交，直到成功为止
                    logger.Error(e, "fatal error. commit log fail!");
                }
                // 潜在非常容易被忽略并发错误
                // 释放锁后，有可能以下记录已经被其他进程改过了
                foreach (var record in _writeRecords)
                {
                    record.StorageRecord.Commit(record.NewTimestamp);
                    logger.Trace("commit.Commit. key:{key} timestamp:{timestamp} ", record.Key, record.NewTimestamp);
                }
            }
            else
            {
                foreach (var holdLock in _holdLocks)
                {
                    TKey tkey = holdLock.Key;
                    var storageRecord = holdLock.StorageRecord;
                    storageRecord.ReleaseLock(holdLock.Locker);
                    logger.Trace("commit.ReleaseReadLock. key:{key} ", tkey);
                }
                _holdLocks.Clear();
            }



            foreach (var task in _txnTasks)
            {
                try
                {
                    task.Commit();
                    logger.Trace("txn task:{task} commit", task);
                }
                catch (Exception e)
                {
                    logger.Error(e, "commit txn task");
                }
            }

            foreach (var task in _txnCommitTasks)
            {
                try
                {
                    task();
                    logger.Trace("txn task:{task} commit", task);
                }
                catch (Exception e)
                {
                    logger.Error(e, "commit txn task");
                }
            }
            */
        }

        public void AddCommitTask(Action action)
        {
            //_txnCommitTasks.Add(action);
        }

        public void AddRollbackTask(Action action)
        {
            //_txnRollbackTasks.Add(action);
        }

        internal bool GetOrigin(TableKey key, out Bean data)
        {
            if (cacheRecords.TryGetValue(key, out var value))
            {
                data = null;// value.Data;
                return true;
            }

            data = null;
            return false;
        }

        /*
        internal void PutOrigin(TableKey key, Bean value, long latestSnapshotTimestamp, long timestamp, AbstractRecord storageRecord)
        {
            //cacheRecords.Add(key, new RecordInfo(key, value, latestSnapshotTimestamp, timestamp, storageRecord));
        }
        internal void PutRecord(TKey key, ReplaceRecordLogger data)
        {
            var rec = cacheRecords[key];
            rec.ChangeLogger = data;
            rec.Dirty = true;
        }
        */

        internal bool GetCacheRecord(TableKey key, out Bean value)
        {
            if (cacheRecords.TryGetValue(key, out var record))
            {
                value = null;
                //value = record.ChangeLogger != null ? record.ChangeLogger.Value.Data : record.Data;
                return true;
            }
            else
            {
                value = null;
                return false;
            }
        }
    }
}
