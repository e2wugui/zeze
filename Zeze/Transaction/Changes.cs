using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Transaction.Collections;
using Zeze.Util;

namespace Zeze.Transaction
{
	public class Changes
	{        
		// 收集日志时,记录所有Bean修改.
        // key is Bean.ObjectId
        public Dictionary<long, LogBean> Beans { get; } = new();
		public Transaction Transaction { get; }

		public class Record
        {
			public const int Remove = 0;
			public const int Put = 1;
			public const int Edit = 2;

			public int State { get; set; }
			public Bean Value { get; set; }
			public ISet<LogBean> LogBean { get; } = new HashSet<LogBean>();

			public LogBean GetLogBean()
			{
				using var it = LogBean.GetEnumerator();
				return it.MoveNext() ? it.Current : null;
			}

			public Log GetVariableLog(int variableId)
            {
				Log log = null;
				GetLogBean()?.Variables.TryGetValue(variableId, out log);
				return log;
            }

			// 所有的日志修改树，key is Record.Value。中间变量，不需要系列化。
			public IdentityHashMap<Bean, LogBean> LogBeans { get; } = new IdentityHashMap<Bean, LogBean>();
			public Table Table { get; }

			public Record(Table table)
            {
				Table = table;
            }

			public void Collect(Transaction.RecordAccessed ar)
            {
				if (null != ar.CommittedPutLog) // put or remove
				{
					var put = ar.CommittedPutLog.Value;
					if (null != put)
					{
						Value = put;
						State = Put;
					}
					else
					{
						Value = ar.Origin.Value;
						State = Remove;
					}
					return;
				}

				State = Edit;
				if (LogBeans.TryGetValue(ar.Origin.Value, out var logBean))
                {
					Value = ar.Origin.Value;
					LogBean.Add(logBean); // edit
				}
			}

			public void Encode(ByteBuffer bb)
            {
				bb.WriteUInt(State);
				switch (State)
				{
					case Remove:
						break;

					case Put:
						Value.Encode(bb);
						break;

					case Edit:
						bb.Encode(LogBean);
						break;
				}
			}

			public void Decode(ByteBuffer bb)
            {
				State = bb.ReadUInt();
				switch (State)
                {
					case Remove:
						break;

					case Put:
						Value = Table.NewBeanValue();
						Value.Decode(bb);
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
				sb.Append(" PutValue=").Append(Value);
				sb.Append("\nLog=");
				ByteBuffer.BuildString(sb, LogBean);
				sb.Append("\nAllLog=");
				ByteBuffer.BuildString(sb, LogBeans.Values);

				return sb.ToString();
            }
        }

		// 收集记录的修改,以后需要系列化传输.
		// 如果表没有监听者，则不会收集该表的改变。
		public Dictionary<TableKey, Record> Records { get; } = new();
		public IdentityHashMap<Table, IReadOnlySet<ChangeListener>> Listeners { get; } = new();

		public Changes(Transaction trans)
		{
			Transaction = trans;

			// 建立脏记录的表的监听者的快照，以后收集日志和通知监听者都使用这个快照，避免由于监听者发生变化造成收集和通知不一致。
			foreach (var ar in trans.AccessedRecords.Values)
			{
				if (ar.Dirty)
				{
					var tmp = ar.Origin.Table.ChangeListenerMap.GetListeners();
					if (tmp.Count > 0)
						Listeners.TryAdd(ar.Origin.Table, tmp);
				}
			}
		}

		public void Collect(Bean recent, Log log)
		{
			// is table has listener
			if (false == Listeners.TryGetValue(recent.RootInfo.Record.Table, out _))
				return;

			if (null == log.Belong)
            {
				// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
				if (false == Records.TryGetValue(recent.TableKey, out var r))
				{
					r = new Record(recent.RootInfo.Record.Table);
					Records.Add(recent.TableKey, r);
				}
				r.LogBeans.TryAdd(recent, (LogBean)log);
				return; // root
			}

			if (false == Beans.TryGetValue(log.Belong.ObjectId, out LogBean logBean))
			{
				if (log.Belong is Collection) 
				{
					// 容器使用共享的日志。需要先去查询，没有的话才创建。
					logBean = (LogBean)Transaction.Current.GetLog(log.Belong.Parent.ObjectId + log.Belong.VariableId);
				}
				logBean ??= log.Belong.CreateLogBean();
				Beans.Add(log.Belong.ObjectId, logBean);
			}
			logBean.Collect(this, log.Belong, log);
		}

		public void CollectRecord(Transaction.RecordAccessed ar)
        {
			// is table has listener
			if (false == Listeners.TryGetValue(ar.Origin.Table, out _))
				return;

			var tKey = ar.TableKey;

			if (false == Records.TryGetValue(tKey, out var r))
            {
				// put record only
				r = new Record(ar.Origin.Table);
				Records.Add(tKey, r);
			}

			r.Collect(ar);
		}

        public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Records);
            return sb.ToString();
        }

		private static readonly ILogger logger = LogManager.GetLogger(typeof(Changes));

		public void NotifyListener()
		{
			foreach (var e in Records)
			{
				if (Listeners.TryGetValue(e.Value.Table, out var listeners))
				{
					foreach (var listener in listeners)
					{
						try
                        {
							listener.OnChanged(e.Key.Key, e.Value);
						}
						catch (Exception ex)
                        {
							logger.Error(ex);
                        }
					}
				}
				else
				{
					logger.Error("Impossible! Record Log Exist But No Listener");
				}
			}
		}
	}
}
