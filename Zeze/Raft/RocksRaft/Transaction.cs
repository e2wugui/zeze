using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Threading;
using Zeze.Util;

namespace Zeze.Raft.RocksRaft
{
	public sealed class Transaction
	{
		public static Transaction Current => asyncLocal.Value;

        public HashSet<IPessimismLock> PessimismLocks { get; } = new();
        public Procedure Procedure { get; private set; }

        public async Task<T> AddPessimismLockAsync<T>(T plock)
            where T : IPessimismLock
        {
            if (Procedure.Rocks.RocksMode != RocksMode.Pessimism)
                throw new Exception("RocksMode Is Not Pessimism!");

            if (PessimismLocks.Add(plock))
                await plock.LockAsync();
            return plock;
        }

		public bool TryGetLog(long logKey, out Log log)
		{
            log = GetLog(logKey);
            return null != log;
		}

        public Log GetLog(long logKey)
        {
            return Savepoints.Count > 0 ? Savepoints[^1].GetLog(logKey) : null;
        }

		public void PutLog(Log log)
		{
            Savepoints[^1].PutLog(log);
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

        public class RecordAccessed : Bean
        {
            public Record Origin { get; }
            public long Timestamp { get; }
            public bool Dirty { get; set; }

            public Bean NewestValue()
            {
                if (null != PutLog)
                    return PutLog.Value;
                return Origin.Value;
            }

            public override Bean Copy()
            {
                throw new NotImplementedException();
            }

            public Log<Bean> PutLog { get; private set; }

            public RecordAccessed(Record origin)
            {
                Origin = origin;
                Timestamp = origin.Timestamp;
            }

            public void Put(Transaction current, Bean value)
            {
                PutLog = new Log<Bean>() { Belong = this, Value = value };
                current.PutLog(PutLog);
            }

            public void Remove(Transaction current)
            {
                Put(current, null);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
            }

            public override void Decode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            public override void Encode(ByteBuffer bb)
            {
                throw new NotImplementedException();
            }

            public override void FollowerApply(Log log)
            {
                // Follower 不会到达这里。
                throw new NotImplementedException();
            }

            public override void LeaderApplyNoRecursive(Log log)
            {
                // 在处理完 Log 以后，专门处理 PutLog 。see _final_commit_ & Record.LeaderApply
            }
        }

        internal SortedDictionary<TableKey, RecordAccessed> AccessedRecords { get; }
            = new SortedDictionary<TableKey, RecordAccessed>();

        private readonly List<Savepoint> Savepoints = new();

        internal void AddRecordAccessed(Record.RootInfo root, RecordAccessed r)
        {
            r.InitRootInfo(root, null);
            AccessedRecords.Add(root.TableKey, r);
        }

        internal RecordAccessed GetRecordAccessed(TableKey key)
        {
            if (AccessedRecords.TryGetValue(key, out var record))
                return record;

            return null;
        }
        
        private static readonly AsyncLocal<Transaction> asyncLocal = new();

		public static Transaction Create()
        {
			if (null == asyncLocal.Value)
				asyncLocal.Value = new Transaction();
			return asyncLocal.Value;
        }

		public static void Destory()
        {
			asyncLocal.Value = null;
        }

		public void Begin()
        {
			Savepoint sp = Savepoints.Count > 0 ? Savepoints[^1].BeginSavepoint() : new Savepoint();
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
				Savepoints[^1].MergeFrom(last, true);
			}
			/*
            else
            {
                // 最外层存储过程提交在 Perform 中处理
            }
            */
		}

        private List<Action> LastRollbackActions;
        
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

        internal async Task<long> Perform(Procedure procedure)
        {
            Procedure = procedure;

            try
            {
				var rc = await procedure.CallAsync();
                if (LockAndCheck(Zeze.Transaction.TransactionLevel.Serializable))
                {
                    if (0 == rc)
                    {
                        await FinalCommit(procedure);
                    }
                    else
                    {
                        procedure.SetAutoResponseResultCode(rc);
                        FinalRollback(procedure);
                    }
                    return rc;
                }
                procedure.SetAutoResponseResultCode(rc);
                FinalRollback(procedure); // 乐观锁，这里应该redo
                return rc;
			}
            catch (Zeze.Util.ThrowAgainException)
            {
                procedure.SetAutoResponseResultCode(ResultCode.Exception);
                FinalRollback(procedure);
                throw;
            }
            catch (RaftRetryException ex1)
            {
                logger.Debug(ex1);
                procedure.SetAutoResponseResultCode(ResultCode.RaftRetry);
                FinalRollback(procedure);
                return ResultCode.RaftRetry;
            }
            catch (Exception ex)
            {
                procedure.SetAutoResponseResultCode(ResultCode.Exception);
                logger.Error(ex);

                if (ex.GetType().Name == "AssertFailedException")
                {
                    FinalRollback(procedure);
                    throw;
                }

                if (LockAndCheck(Zeze.Transaction.TransactionLevel.Serializable))
                {
                    FinalRollback(procedure);
                    return ResultCode.Exception;
                }
                FinalRollback(procedure); // 乐观锁，这里应该redo
                return ResultCode.Exception;
            }
            finally
            {
                foreach (var plock in PessimismLocks)
                {
                    plock.Dispose();
                }
                PessimismLocks.Clear();
            }
		}

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Changes Changes { get; private set; }


        public void RunWhileCommit(Action action)
        {
            Savepoints[^1].CommitActions.Add(action);
        }

        public void RunWhileRollback(Action action)
        {
            Savepoints[^1].RollbackActions.Add(action);
        }

        private static void TriggerCommitActions(Procedure procedure, Savepoint last)
        {
            foreach (Action action in last.CommitActions)
            {
                try
                {
                    action();
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Commit Procedure {0} Action {1}", procedure, action.Method.Name);
                    if (ex.GetType().Name == "AssertFailedException")
                    {
                        throw;
                    }
                }
            }
            last.CommitActions.Clear();
        }

        private bool LockAndCheck(Zeze.Transaction.TransactionLevel level)
        {
            bool allRead = true;
            if (Savepoints.Count == 1)
            {
                foreach (var log in Savepoints[0].Logs.Values)
                {
                    // 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
                    // 现在不会有这种情况，保留给未来扩展需要。
                    if (log.Belong == null)
                        continue;

                    TableKey tkey = log.Belong.TableKey;
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
            if (allRead && level == Zeze.Transaction.TransactionLevel.AllowDirtyWhenAllRead)
                return true; // 使用一个新的enum表示一下？
            return true;
        }

        private async Task FinalCommit(Procedure procedure)
        {
            // Collect Changes
            Savepoint sp = Savepoints[^1];
            Changes = new Changes(procedure.Rocks, this, procedure.UniqueRequest);
            foreach (Log log in sp.Logs.Values)
            {
                // 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
                if (log.Belong == null)
                    continue;

                // 当changes.Collect在日志往上一级传递时调用，
                // 第一个参数Owner为null，表示bean属于record，到达root了。
                Changes.Collect(log.Belong, log);
            }

            foreach (var ar in AccessedRecords.Values)
            {
                if (ar.Dirty)
                    Changes.CollectRecord(ar);
            }

            if (Changes.Records.Count > 0) // has changes
            {
                await procedure.Rocks.UpdateAtomicLongs(Changes.AtomicLongs);
                await procedure.Rocks.Raft.AppendLog(Changes, procedure.UniqueRequest?.ResultBean);
            }

            TriggerCommitActions(procedure, sp);
            procedure.AutoResponse?.SendResult();
        }

        internal async Task LeaderApply(Changes changes)
        {
            Savepoint sp = Savepoints[^1];
            foreach (Log log in sp.Logs.Values)
            {
                log.Belong?.LeaderApplyNoRecursive(log);
            }
            var rs = new List<Record>();
            foreach (var ar in AccessedRecords.Values)
            {
                if (ar.Dirty)
                {
                    ar.Origin.LeaderApply(ar);
                    rs.Add(ar.Origin);
                }
            }
            await changes.Rocks.Flush(rs, changes);
        }

        private void FinalRollback(Procedure procedure)
        {
            if (null != LastRollbackActions)
            {
                foreach (var act in LastRollbackActions)
                {
                    try
                    {
                        act();
                    }
                    catch (Exception ex)
                    {
                        logger.Error(ex);
                    }
                }
                LastRollbackActions = null;
            }
            procedure.AutoResponse?.SendResult();
        }

    }

}
