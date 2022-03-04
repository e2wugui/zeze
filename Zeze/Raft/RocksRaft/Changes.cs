using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class Changes : Zeze.Raft.Log
	{
		// 收集日志时,记录所有Bean修改.
		// key is Bean.ObjectId
		public Dictionary<long, LogBean> Beans { get; } = new Dictionary<long, LogBean>();
		public Procedure Procedure { get; }
		public Transaction Transaction { get; }

		public Changes(Procedure p, Transaction trans)
			: base((IRaftRpc)p.Rpc)
        {
			Procedure = p;
			Transaction = trans;
		}

		public class Record
        {
			public const int Remove = 0;
			public const int Put = 1;
			public const int Edit = 2;

			public int State { get; set; }
			public Bean PutValue { get; set; }
			public ISet<LogBean> LogBean { get; } = new HashSet<LogBean>();

			// 所有的日志修改树，key is Record.Value。不会被系列化。
			public Util.IdentityHashMap<Bean, LogBean> LogBeans { get; } = new Util.IdentityHashMap<Bean, LogBean>();

			public Table Table;

			public void SetTableByName(Changes changes, string name)
            {
				if (false == changes.Procedure.Rocks.Tables.TryGetValue(name, out Table))
				{
					Rocks.logger.Error($"table not found {name}");
					changes.Procedure.Rocks.Raft.FatalKill();
				}
			}

			public void Collect(Transaction.RecordAccessed ar)
            {
				if (null != ar.PutLog) // put or remove
				{
					PutValue = ar.PutLog.Value;
					if (PutValue == null)
					{
						State = Remove;
					}
					else
					{
						State = Put;
					}
				}
				else
				{
					State = Edit;
					if (LogBeans.TryGetValue(ar.Origin.Value, out var logbean))
						LogBean.Add(logbean); // edit
				}
			}

			public void Encode(ByteBuffer bb)
            {
				bb.WriteInt(State);
				switch (State)
				{
					case Remove:
						break;

					case Put:
						PutValue.Encode(bb);
						break;

					case Edit:
						bb.Encode(LogBean);
						break;
				}
			}

			public void Decode(ByteBuffer bb)
            {
				State = bb.ReadInt();
				switch (State)
                {
					case Remove:
						break;

					case Put:
						PutValue = Table.NewValue();
						PutValue.Decode(bb);
						break;

					case Edit:
						bb.Decode(LogBean);
						break;
                }
            }

            public override string ToString()
            {
				var sb = new StringBuilder();

				sb.Append("State=").Append(State);
				sb.Append(" PutValue=").Append(PutValue);
				sb.Append("\nLog=");
				ByteBuffer.BuildString(sb, LogBean);
				sb.Append("\nAllLog=");
				ByteBuffer.BuildString(sb, LogBeans.Values);

				return sb.ToString();
            }
        }

		// 收集记录的修改,以后需要系列化传输.
		public Dictionary<TableKey, Record> Records { get; } = new Dictionary<TableKey, Record>();

		public void Collect(Bean recent, Log log)
		{
			if (null == log.Belong)
            {
				// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
				if (false == Records.TryGetValue(recent.TableKey, out var r))
				{
					r = new Record();
					Records.Add(recent.TableKey, r);
				}
				r.LogBeans.TryAdd(recent, (LogBean)log);
				return; // root
			}

			if (false == Beans.TryGetValue(log.Belong.ObjectId, out LogBean logbean))
			{
				if (log.Belong is Collection)
				{
					// 容器使用共享的日志。需要先去查询，没有的话才创建。
					logbean = (LogBean)Transaction.Current.GetLog(log.Belong.Parent.ObjectId + log.Belong.VariableId);
				}
				if (null == logbean)
                {
					logbean = log.Belong.CreateLogBean();
				}
				Beans.Add(log.Belong.ObjectId, logbean);
			}
			logbean.Collect(this, log.Belong, log);
		}

		public void CollectRecord(Transaction.RecordAccessed ar)
        {
			var tkey = ar.TableKey;
			if (false == Records.TryGetValue(tkey, out var r))
			{
				r = new Record();
				Records.Add(tkey, r);
			}
			r.Collect(ar);
        }

		public override void Decode(ByteBuffer bb)
		{
			Records.Clear();
			for (int i = bb.ReadInt(); i > 0; i--)
			{
				var tkey = new TableKey();
				var r = new Record();
				tkey.Name = bb.ReadString();
				r.SetTableByName(this, tkey.Name);
				r.Table.DecodeKey(bb, out tkey.Key);
				r.Decode(bb);
				Records.Add(tkey, r);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Records.Count);
			foreach (var r in Records)
			{
				// encode TableKey
				r.Value.SetTableByName(this, r.Key.Name);
				bb.WriteString(r.Key.Name);
				r.Value.Table.EncodeKey(bb, r.Key.Key);
				// encode record
				r.Value.Encode(bb);
			}
		}

        public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Records);
            return sb.ToString();
        }

        public override void Apply(RaftLog holder, StateMachine stateMachine)
        {
			if (holder.LeaderFuture != null)
			{
				Transaction.LeaderApply(Procedure);
			}
            else
            {
				Procedure.Rocks.FollowerApply(this);
			}
		}
	}
}
