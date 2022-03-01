using System;
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
					_final_commit_();
					return 0;
				}
				_final_rollback_();
				return result;
			}
			catch (Exception)
            {
                _final_rollback_();
                return Zeze.Transaction.Procedure.Exception;
            }
		}

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private void _final_commit_()
        {
            var changes = new Changes();
            Savepoint sp = Savepoints[Savepoints.Count - 1];
            foreach (Log log in sp.Logs.Values)
            {
                // 这里都是修改操作的日志，没有Owner的日志是特殊测试目的加入的，简单忽略即可。
                if (log.Parent == null)
                    continue;

                // 当changes.Collect在日志往上一级传递时调用，
                // 第一个参数Owner为null，表示bean属于record，到达root了。
                changes.Collect(log.Parent, log);
            }
            foreach (var ar in AccessedRecords.Values)
            {
                changes.CollectRecord(ar);
            }
            var sb = new StringBuilder();
            ByteBuffer.BuildString(sb, changes.Records);
            Console.WriteLine(sb.ToString());
        }
        
        private void _final_rollback_()
        {

        }

	}

}
