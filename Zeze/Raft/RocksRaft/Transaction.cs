using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class Transaction
	{
		public static Transaction Current => threadLocal.Value;

		public bool LogTryGet(long logKey, out Log log)
		{
			log = null;
			return false;
		}

        public Log LogGet(long logKey)
        {
            if (LogTryGet(logKey, out var log))
                return log;
            return null;
        }

		public void LogPut(Log log)
		{

		}

		public Log LogGetOrAdd(long logKey, Func<Log> logFactory)
		{
			return logFactory();
		}

        public class RecordAccessed : Bean
        {
            public Record OriginRecord { get; }
            public long Timestamp { get; }
            public bool Dirty { get; set; }

            public Bean NewestValue()
            {
                PutLog log = (PutLog)Current.LogGet(ObjectId);
                if (null != log)
                    return log.Value;
                return OriginRecord.Value;
            }

            // Record 修改日志先提交到这里(Savepoint.Commit里面调用）。处理完Savepoint后再处理 Dirty 记录。
            public PutLog CommittedPutLog { get; private set; }

            public class PutLog : Log<Bean>
            {
                public PutLog(RecordAccessed bean, Bean putValue)
                {
                    base.Bean = bean;
                    base.Value = putValue;
                }

                public override void Apply(Bean holder)
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
                current.LogPut(new PutLog(this, putValue));
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
        
        private static System.Threading.ThreadLocal<Transaction> threadLocal
            = new System.Threading.ThreadLocal<Transaction>();

		public static Transaction Create()
        {
			if (null == threadLocal.Value)
				threadLocal.Value = new Transaction();
			return threadLocal.Value;
        }

		public static void Destory()
        {
			threadLocal.Value = null;
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
					_final_commit();
					return 0;
				}
				_final_rollback();
				return result;
			}
			catch (Exception _)
            {
				return Zeze.Transaction.Procedure.Exception;
            }
		}

		private void _final_commit()
        {

        }

		private void _final_rollback()
        {

        }

	}

}
