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

		// 收集记录的修改,以后需要系列化传输.
		public Dictionary<TableKey, LogBean> Records { get; } = new Dictionary<TableKey, LogBean>();

		public void Collect(Bean parent, Log log)
		{
			if (null == parent)
			{
				Records.TryAdd(log.Bean.TableKey, (LogBean)log);
			}
			else
			{
				if (false == Beans.TryGetValue(parent.ObjectId, out LogBean logbean))
				{
					logbean = parent.CreateLogBean();
					Beans.Add(parent.ObjectId, logbean);
				}
				logbean.Collect(this, log);
			}
		}

		public void Decode(ByteBuffer bb)
		{
			Records.Clear();
			for (int i = bb.ReadInt(); i >= 0; i--)
			{
				var tkey = new TableKey();
				var blog = new RecordLog();
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
	}
}
