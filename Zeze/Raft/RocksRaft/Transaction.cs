﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;
using System.Threading;

namespace Zeze.Raft.RocksRaft
{
	public sealed class Transaction
	{
		public static Transaction Current => threadlocal.Value;

		public bool TryGetLog(long logKey, out Log log)
		{
            log = GetLog(logKey);
            return null != log;
		}

        public Log GetLog(long logKey)
        {
            return Savepoints.Count > 0
                ? Savepoints[Savepoints.Count - 1].GetLog(logKey)
                : null;
        }

		public void PutLog(Log log)
		{
            Savepoints[Savepoints.Count - 1].PutLog(log);
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
                if (null != PutValueLog)
                    return PutValueLog.Value;
                return Origin.Value;
            }

            public PutLog PutValueLog { get; private set; }

            public class PutLog : Log<Bean>
            {
                public override void FollowerApply(Bean parent)
                {
                }

                public override void LeaderApply()
                {
                }
            }

            public RecordAccessed(Record origin)
            {
                Origin = origin;
                Timestamp = origin.Timestamp;
            }

            public void Put(Transaction current, Bean value)
            {
                PutValueLog = new PutLog() { Parent = this, Value = value };
                current.PutLog(PutValueLog);
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
            }

            public override void Encode(ByteBuffer bb)
            {
            }
        }

        internal SortedDictionary<TableKey, RecordAccessed> AccessedRecords { get; }
            = new SortedDictionary<TableKey, RecordAccessed>();

        private readonly List<Savepoint> Savepoints = new List<Savepoint>();

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
        
        private static ThreadLocal<Transaction> threadlocal = new ThreadLocal<Transaction>();

		public static Transaction Create()
        {
			if (null == threadlocal.Value)
				threadlocal.Value = new Transaction();
			return threadlocal.Value;
        }

		public static void Destory()
        {
			threadlocal.Value = null;
        }

		public void Begin()
        {
			Savepoint sp = Savepoints.Count > 0
                ? Savepoints[Savepoints.Count - 1].BeginSavepoint()
                : new Savepoint();

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
				Savepoints[Savepoints.Count - 1].CommitTo(last);
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
		}

		internal long Perform(Procedure proc)
        {
            try
            {
				var result = proc.Call();
				if (0 == result)
				{
                    if (_lock_and_check_(Zeze.Transaction.TransactionLevel.Serializable))
                    {
                        _final_commit_(proc);
                        return 0;
                    }
                    // else redo future
                }
				_final_rollback_(proc);
				return result;
			}
			catch (Exception ex)
            {
                if (ex.GetType().Name == "AssertFailedException")
                {
                    _final_rollback_(proc);
                    throw;
                }

                if (_lock_and_check_(Zeze.Transaction.TransactionLevel.Serializable))
                {
                    _final_rollback_(proc);
                    return Zeze.Transaction.Procedure.Exception;
                }
                return Zeze.Transaction.Procedure.Exception;
            }
		}

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Changes Changes { get; private set; } = new Changes();

        private readonly List<Action> CommitActions = new List<Action>();

        public void RunWhileCommit(Action action)
        {
            CommitActions.Add(action);
        }

        private void _trigger_commit_actions_(Procedure procedure)
        {
            foreach (Action action in CommitActions)
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
            CommitActions.Clear();
        }

        private bool _lock_and_check_(Zeze.Transaction.TransactionLevel level)
        {
            bool allRead = true;
            if (Savepoints.Count > 0)
            {
                foreach (var log in Savepoints[Savepoints.Count - 1].Logs.Values)
                {
                    // 特殊日志。不是 bean 的修改日志，当然也不会修改 Record。
                    // 现在不会有这种情况，保留给未来扩展需要。
                    if (log.Parent == null)
                        continue;

                    TableKey tkey = log.Parent.TableKey;
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

        private void _final_commit_(Procedure procedure)
        {
            // Collect Changes
            Savepoint sp = Savepoints[Savepoints.Count - 1];
            foreach (Log log in sp.Logs.Values)
            {
                // 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
                if (log.Parent == null)
                    continue;

                // 当changes.Collect在日志往上一级传递时调用，
                // 第一个参数Owner为null，表示bean属于record，到达root了。
                Changes.Collect(log.Parent, log);
            }
            foreach (var ar in AccessedRecords.Values)
            {
                Changes.CollectRecord(ar);
            }

            // Raft
            // procedure.Rocks.Raft.AppendLog(null, procedure.Rpc?.Result);

            // Apply
            foreach (Log log in sp.Logs.Values)
            {
                log.LeaderApply();
            }

            foreach (var e in AccessedRecords)
            {
                if (e.Value.Dirty)
                {
                    e.Value.Origin.LeaderApply(e.Value);
                }
            }

            // Flush To Database
            procedure.Rocks.Flush(this);

            procedure.RequestProtocol?.SendResultCode(procedure.RequestProtocol.ResultCode);

            // Trigger Application Action
            _trigger_commit_actions_(procedure);
        }
        
        private void _final_rollback_(Procedure procedure)
        {

        }

	}

}
