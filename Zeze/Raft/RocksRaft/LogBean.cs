using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogBean : Log
	{
		public Dictionary<int, Log> Variables { get; } = new Dictionary<int, Log>();

		public override void Decode(ByteBuffer bb)
		{
			for (int i = bb.ReadInt(); i >= 0; --i)
			{
				var typeId = bb.ReadInt4();
				var log = Log.Create(typeId);

				var varId = bb.ReadInt();
				log.VariableId = varId;
				log.Decode(bb);

				Variables.Add(varId, log);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Variables.Count);
			foreach (var log in Variables.Values)
			{
				bb.WriteInt4(log.TypeId);
				bb.WriteInt(log.VariableId);
				log.Encode(bb);
			}
		}

		// 仅发生在事务执行期间。Decode-Apply不会执行到这里。
		public override void Collect(Changes changes, Log vlog)
        {
			if (Variables.TryAdd(vlog.VariableId, vlog))
            {
				// 向上传递
				changes.Collect(Bean.Parent, this);
			}
		}

		public override void Apply(Bean holder)
		{
			foreach (var vlog in Variables.Values)
			{
				vlog.Apply(holder);
			}
		}
	}

	public class Changes
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
	}
}
