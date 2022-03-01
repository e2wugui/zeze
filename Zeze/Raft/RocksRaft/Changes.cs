using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class Changes : Serializable
	{
		// 收集日志时,记录所有Bean修改.
		public Dictionary<long, LogBean> Beans { get; } = new Dictionary<long, LogBean>();

		public class Record
        {
			public const int Remove = 0;
			public const int Put = 1;
			public const int Edit = 2;

			public int State { get; set; }
			public Bean PutValue { get; set; }
			public Util.IdentityHashMap<Bean, LogBean> LogBeans { get; } = new Util.IdentityHashMap<Bean, LogBean>();
			public ISet<LogBean> LogBean { get; } = new HashSet<LogBean>();

			public void Collect(Transaction.RecordAccessed ar)
            {
				if (null != ar.PutValueLog) // put or remove
				{
					PutValue = ar.PutValueLog.Value;
					if (PutValue == null)
					{
						State = Remove;
					}
					else
					{
						State = Put;
						if (LogBeans.TryGetValue(PutValue, out var logbean))
							LogBean.Add(logbean); // put and edit
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
						bb.Encode(LogBean);
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
						// TODO bean factory
						PutValue.Decode(bb);
						bb.Decode<LogBean>(LogBean);
						break;

					case Edit:
						bb.Decode<LogBean>(LogBean);
						break;
                }
            }
		}

		// 收集记录的修改,以后需要系列化传输.
		public Dictionary<TableKey, Record> Records { get; } = new Dictionary<TableKey, Record>();

		public void Collect(Log log)
		{
			if (null == log.Owner.Parent)
			{
				// 记录可能存在多个修改日志树。收集的时候全部保留，后面会去掉不需要的。see Transaction._final_commit_
				if (false == Records.TryGetValue(log.Owner.TableKey, out var r))
                {
					r = new Record();
					Records.Add(log.Owner.TableKey, r);
				}
				if (false == r.LogBeans.TryAdd(log.Owner, (LogBean)log))
					throw new Exception("bug!");
			}
			else
			{
				if (false == Beans.TryGetValue(log.Owner.Parent.ObjectId, out LogBean logbean))
				{
					logbean = log.Owner.Parent.CreateLogBean();
					Beans.Add(log.Owner.Parent.ObjectId, logbean);
				}
				logbean.Collect(this, log);
			}
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

		public void Decode(ByteBuffer bb)
		{
			Records.Clear();
			for (int i = bb.ReadInt(); i >= 0; i--)
			{
				var tkey = new TableKey();
				var blog = new Record();
				tkey.Decode(bb);
				blog.Decode(bb);
				Records.Add(tkey, blog);
			}
		}

		public void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Records.Count);
			foreach (var r in Records)
			{
				r.Key.Encode(bb);
				r.Value.Encode(bb);
			}
		}

        public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Records);
            return sb.ToString();
        }
    }
}
