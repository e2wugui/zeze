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
            if (threadLocal.IsValueCreated)
                throw new Exception("Transaction has created");
            threadLocal.Value = new Transaction();
            return threadLocal.Value;
        }
        public static void Destroy()
        {
            threadLocal.Value = null;
        }

        private readonly List<HoldLockInfo> holdLocks = new List<HoldLockInfo>();
        private readonly SortedDictionary<TableKey, RecordInfo> cacheRecords = new SortedDictionary<TableKey, RecordInfo>();
        private readonly Dictionary<long, Log> logs = new Dictionary<long, Log>();
        private readonly Dictionary<PCollection, Log> collectionLogs = new Dictionary<PCollection, Log>();

        internal void Begin()
        {

        }

        internal ValueTask<bool> LockAndCheck()
        {
            // 将modified fields 的 root 标记为 dirty
            foreach (var log in logs.Values)
            {
                TableKey tkey = log.Bean.TableKey;
                var record = cacheRecords[tkey];
                record.Dirty = true;
            }
            foreach (var log in collectionLogs.Values)
            {
                TableKey tkey = log.Bean.TableKey;
                var record = cacheRecords[tkey];
                record.Dirty = true;
            }

            bool conflict = false;

            if (holdLocks.Count == 0)
            {
                foreach (var e in cacheRecords)
                {
                    var tkey = e.Key;
                    bool writeLock = e.Value.Dirty;
                    var storageRecord = e.Value.StorageRecord;
                    (long newTimestamp, var releaser) = await storageRecord.GetTimestampAndLockAsync(writeLock, null);
                    _holdLocks.Add(new HoldLockInfo(tkey, storageRecord, writeLock, releaser));
                    conflict |= e.Value.Timestamp != newTimestamp;
                    logger.Trace("[new] add lock. table:{table} key:{key} oldtimestamp:{old} newtimestamp:{new} conflict:{conflict}",
                        TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, newTimestamp, conflict);
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
                    bool writeLock = e.Value.Dirty;
                    var storageRecord = e.Value.StorageRecord;

                    // 如果 holdLocks 全部被对比完毕，直接锁定它
                    if (index >= n)
                    {
                        // lock it!
                        (long newTimestamp, var locker) = await storageRecord.GetTimestampAndLockAsync(writeLock, null);
                        _holdLocks.Add(new HoldLockInfo(tkey, storageRecord, writeLock, locker));
                        conflict |= e.Value.Timestamp != newTimestamp;
                        logger.Trace("[new] add lock. table:{table} key:{key} oldtimestamp:{old} newtimestamp:{new} conflict:{conflict}",
                            TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, newTimestamp, conflict);
                        continue;
                    }

                    HoldLockInfo curLock = _holdLocks[index];
                    int c = curLock.Key.CompareTo(tkey);

                    // holdlocks a  b  ...
                    // needlocks a  b  ...
                    if (c == 0)
                    {
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
                                TxnTable.GetTable(tkey.TableId).Name, tkey.ObjectKey, e.Value.Timestamp, conflict);
                        }
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
                        for (; unlockEndIndex < n && _holdLocks[unlockEndIndex].Key.CompareTo(tkey) < 0; ++unlockEndIndex)
                        {
                            var toUnlockLocker = _holdLocks[unlockEndIndex];
                            toUnlockLocker.StorageRecord.ReleaseLock(toUnlockLocker.Locker);
                            logger.Trace("[remove] unlock not need lock. table:{table} key:{key} ",
                                TxnTable.GetTable(toUnlockLocker.Key.TableId).Name, toUnlockLocker.Key.ObjectKey);
                        }

                        _holdLocks.RemoveRange(index, unlockEndIndex - index);
                        n = _holdLocks.Count;
                    }
                    else
                    {
                        // holdlocks a  c  ...
                        // needlocks a  b  ...
                        // 为了不违背锁序，释放从当前锁开始的所有锁
                        for (int i = index; i < n; ++i)
                        {
                            var toUnlockLocker = _holdLocks[i];
                            toUnlockLocker.StorageRecord.ReleaseLock(toUnlockLocker.Locker);
                            logger.Trace("[remove] unlock for not violate lock order. table:{table} key:{key} ",
                                TxnTable.GetTable(toUnlockLocker.Key.TableId).Name, toUnlockLocker.Key.ObjectKey);
                        }
                        _holdLocks.RemoveRange(index, n - index);
                        n = _holdLocks.Count;
                    }
                }
            }
            logger.Trace("LockAndCheckConflict. conflict:{conflict}", conflict);
            return !conflict;
        }

        private void ProcessLog(FieldLogger log)
        {
            log.Commit();
            TKey tkey = log.GetRoot();
            Debug.Assert(tkey != null);
            var record = _cacheRecords[tkey];
            // CHANGE by WALON
            record.FieldLoggers.Add(log);
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

        internal async Task CommitAsync()
        {
            logger.Trace("commit begin");

            foreach (var log in _fieldLoggers.Values)
            {
                ProcessLog(log);
            }

            foreach (var log in _collectionLoggers.Values)
            {
                ProcessLog(log);
            }
            logger.Trace("holdlocks count:{count}", _holdLocks.Count);

            // TODO 
            // 需要限制等待commit的事务数量
            // 避免Storage发生故障时，造成大量事务积压
            // 

            // if any changes
            // 有可能出现fieldLoggers.count == 0 而 beanRootLogger.count > 0 的情况
            // 还有可能 put 新记录，这时 fieldLogger和beanRootLogger都为空
            if (_cacheRecords.Values.Any(r => r.Dirty))
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
        }


        internal void Rollback()
        {
            try
            {
                foreach (var task in _txnTasks)
                {
                    try
                    {
                        task.Rollback();
                        logger.Trace("txn task:{task} rollback", task);
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "rollback");
                    }
                }

                foreach (var task in _txnRollbackTasks)
                {
                    try
                    {
                        task();
                        logger.Trace("txn task:{task} rollback", task);
                    }
                    catch (Exception e)
                    {
                        logger.Error(e, "commit txn task");
                    }
                }
            }
            finally
            {
                _cacheRecords.Clear();
                _collectionLoggers.Clear();
                _fieldLoggers.Clear();
                _txnTasks.Clear();
                _txnCommitTasks.Clear();
                _txnRollbackTasks.Clear();
            }
        }

        internal void End()
        {
            _cacheRecords.Clear();
            _collectionLoggers.Clear();
            _fieldLoggers.Clear();
            _txnTasks.Clear();
            _txnCommitTasks.Clear();
            _txnRollbackTasks.Clear();


            if (_holdLocks.Count > 0)
            {
                foreach (var holdLock in _holdLocks)
                {
                    var storageRecord = holdLock.StorageRecord;
                    storageRecord.ReleaseLock(holdLock.Locker);
                }
                _holdLocks.Clear();
            }
        }

        public void AddCommitTask(Action action)
        {
            _txnCommitTasks.Add(action);
        }

        public void AddRollbackTask(Action action)
        {
            _txnRollbackTasks.Add(action);
        }

        internal bool GetOrigin(TKey key, out Bean data)
        {
            if (_cacheRecords.TryGetValue(key, out var value))
            {
                data = value.Data;
                return true;
            }
            else
            {
                data = null;
                return false;
            }
        }

        internal void PutOrigin(TKey key, Bean value, long latestSnapshotTimestamp, long timestamp, AbstractRecord storageRecord)
        {
            _cacheRecords.Add(key, new RecordInfo(key, value, latestSnapshotTimestamp, timestamp, storageRecord));
        }

        internal void PutRecord(TKey key, ReplaceRecordLogger data)
        {
            var rec = _cacheRecords[key];
            rec.ChangeLogger = data;
            rec.Dirty = true;
        }

        internal bool GetCacheRecord(TKey key, out Bean value)
        {
            if (_cacheRecords.TryGetValue(key, out var record))
            {
                value = record.ChangeLogger != null ? record.ChangeLogger.Value.Data : record.Data;
                return true;
            }
            else
            {
                value = null;
                return false;
            }
        }

        public Log GetField(long key)
        {
            return logs.TryGetValue(key, out var log) ? log : null;
        }

        public Log GetField(PCollection key)
        {
            return collectionLogs.TryGetValue(key, out var log) ? log: null;
        }

        public void PutField(long key, Log log)
        {
            logs[key] = log;
        }

        public void PutField(PCollection key, Log log)
        {
            collectionLogs[key] = log;
        }
    }
}
