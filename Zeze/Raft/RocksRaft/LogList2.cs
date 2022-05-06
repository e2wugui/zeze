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
		}

		public Dictionary<LogBean, OutInt> Changed { get; } = new(); // changed V logs. using in collect.

		public override void Encode(ByteBuffer bb)
		{
			var curList = ((CollList2<E>)This)._list;
			foreach (var e in Changed)
			{
				var logBean = e.Key;
				//noinspection SuspiciousMethodCalls
				var idxExist = curList.IndexOf((E)logBean.This);
				if (idxExist < 0)
					Changed.Remove(logBean);
				else
					e.Value.Value = idxExist;
			}
			bb.WriteUInt(Changed.Count);
			foreach (var e in Changed)
			{
				e.Key.Encode(bb);
				bb.WriteUInt(e.Value.Value);
			}

			bb.WriteUInt(OpLogs.Count);
			foreach (var opLog in OpLogs)
			{
				bb.WriteUInt(opLog.op);
				if (opLog.op < OpLog.OP_CLEAR)
				{
					bb.WriteUInt(opLog.index);
					if (opLog.op < OpLog.OP_REMOVE)
						opLog.value.Encode(bb);
				}
			}
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

			OpLogs.Clear();
			for (var logSize = bb.ReadUInt(); --logSize >= 0;)
			{
				int op = bb.ReadUInt();
				int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
				E value = null;
				if (op < OpLog.OP_REMOVE)
				{
					value = new E();
					value.Decode(bb);
				}
				OpLogs.Add(new OpLog(op, index, value));
			}
		}

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			if (Changed.TryAdd((LogBean)vlog, new OutInt()))
				changes.Collect(recent, this);
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append(" opLogs:");
			ByteBuffer.BuildString(sb, OpLogs);
			sb.Append(" Changed:");
			ByteBuffer.BuildString(sb, Changed);
			return sb.ToString();
		}
	}
}
