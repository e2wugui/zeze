using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class Log : Serializable
	{
		// 事务运行时属性，不会被系列化。当 Decode，Bean为null。
		public Bean Bean { get; set; }
		public long LogKey => Bean.ObjectId + VariableId;

		// 会被系列化，实际上由LogBean管理。
		public int VariableId { get; set; }

		public abstract void Apply(Bean holder);
		public abstract void Encode(ByteBuffer bb);
		public abstract void Decode(ByteBuffer bb);
	}

	// 简单类型日志辅助。
	public abstract class Log<T> : Log
	{
		public T Value { get; set; }

		public override void Encode(ByteBuffer bb)
		{
			SerializeHelper<T>.Encode(bb, Value);
		}

		public override void Decode(ByteBuffer bb)
		{
			Value = SerializeHelper<T>.Decode(bb);
		}
	}

}
