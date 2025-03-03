﻿using System;
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
		public Bean Belong { get; set; }
		public long LogKey => Belong.ObjectId + VariableId;

		public virtual void Collect(Changes changes, Bean recent, Log vlog)
		{
			// LogBean LogCollection 需要实现这个方法收集日志.
		}

		internal abstract void EndSavepoint(Savepoint currentsp);

		internal abstract Log BeginSavepoint();

		// 会被系列化，实际上由LogBean管理。
		private readonly int _TypeId;
		public virtual int TypeId => _TypeId;
		public int VariableId { get; set; }

		public Log()
        {
			_TypeId = Util.FixedHash.Hash32(Util.Reflect.GetStableName(GetType()));
        }

		public abstract void Encode(ByteBuffer bb);
		public abstract void Decode(ByteBuffer bb);

		public static ConcurrentDictionary<int, Func<Log>> Factorys { get; } = new ConcurrentDictionary<int, Func<Log>>();

		public static Log Create(int typeId)
        {
			if (Factorys.TryGetValue(typeId, out var factory))
				return factory();
			throw new Exception($"unknown log typeId={typeId}");
		}

		public static void Register<T>() where T : Log, new()
		{
            Factorys.TryAdd(new T().TypeId, () => new T());
        }
    }

    // 简单类型日志辅助。
    public class Log<T> : Log
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

		internal override void EndSavepoint(Savepoint currentsp)
		{
			currentsp.Logs[LogKey] = this;
		}

		internal override Log BeginSavepoint()
		{
			return this;
		}

        public override string ToString()
        {
            return $"Value={Value}";
        }
    }

}
