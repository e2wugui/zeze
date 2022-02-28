using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class Log : Serializable
	{
		// 事务运行时属性，不会被系列化。
		// 当 Decode，Bean为null。
		// Apply通过参数得到日志应用需要的Bean。
		public Bean Bean { get; set; }
		public long LogKey => Bean.ObjectId + VariableId;
		public virtual void Collect(Changes changes, Log vlog)
		{
			// LogBean LogCollection 需要实现这个方法收集日志.
		}

		// 会被系列化，实际上由LogBean管理。
		private readonly int _TypeId;
		public virtual int TypeId => _TypeId;
		public int VariableId { get; set; }

		public Log()
        {
			_TypeId = Zeze.Transaction.Bean.Hash32(GetType().FullName);
        }

		public abstract void Apply(Bean holder);
		public abstract void Encode(ByteBuffer bb);
		public abstract void Decode(ByteBuffer bb);

		public static ConcurrentDictionary<int, Func<Log>> Factorys { get; } = new ConcurrentDictionary<int, Func<Log>>();

		public static Log Create(int typeId)
        {
			if (Factorys.TryGetValue(typeId, out var factory))
				return factory();
			throw new Exception($"unkown log typeid={typeId}");
		}
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
