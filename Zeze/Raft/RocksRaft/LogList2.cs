using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{


	public class LogList2<E> : LogList1<E>
			where E : Bean, new()
	{
		public class OutInt
		{
			public int Value { get; set; }
            public override string ToString()
            {
                return Value.ToString();
            }
        }

		public Dictionary<LogBean, OutInt> Changed { get; } = new(); // changed V logs. using in collect.

		internal override Log BeginSavepoint()
		{
			var dup = new LogList2<E>();
            dup.This = This;
            dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}

		public override void Encode(ByteBuffer bb)
		{
			if (null != Value)
            {
				// follower接收到log时，Value为空，此时不做Changed过滤。
				var miss = new List<LogBean>();
				foreach (var e in Changed)
				{
					var logBean = e.Key;
					var idxExist = Value.IndexOf((E)logBean.This);
					if (idxExist < 0)
						miss.Add(logBean);
					else
						e.Value.Value = idxExist;
				}
				foreach (var logbean in miss)
				{
					Changed.Remove(logbean);
				}
			}

			bb.WriteUInt(Changed.Count);
			foreach (var e in Changed)
			{
				e.Key.Encode(bb);
				bb.WriteUInt(e.Value.Value);
			}
			// encode oplogs
			base.Encode(bb);
		}

		public override void Decode(ByteBuffer bb)
		{
			Changed.Clear();
			for (int i = bb.ReadUInt(); i > 0; i--)
			{
				var value = new LogBean();
				value.Decode(bb);
				var index = bb.ReadUInt();
				Changed[value] = new OutInt() { Value = index };
			}
			// deocde oplogs
			base.Decode(bb);
		}

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			if (Changed.TryAdd((LogBean)vlog, new OutInt()))
				changes.Collect(recent, this);
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append("OpLogs:");
			ByteBuffer.BuildString(sb, OpLogs);
			sb.Append(" Changed:");
			ByteBuffer.BuildString(sb, Changed);
			return sb.ToString();
		}
	}
}
