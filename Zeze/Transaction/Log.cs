
using System;
using System.Collections.Concurrent;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    /// <summary>
    /// 操作日志。
    /// 主要用于 bean.variable 的修改。
    /// 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
    /// </summary>
    public abstract class Log : Serializable
    {
        public abstract void Commit();
        //public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

        public long LogKey => Belong.ObjectId + VariableId;
        public Bean Belong { get; set; }

        // 会被系列化，实际上由LogBean管理。
        private readonly int _TypeId;
        public virtual int TypeId => _TypeId; public int VariableId { get; set; }

        public Log()
        {
            _TypeId = Zeze.Transaction.Bean.Hash32(Util.Reflect.GetStableName(GetType()));
        }

        public virtual void Collect(Changes changes, Bean recent, Log vlog)
        {
            // LogBean LogCollection 需要实现这个方法收集日志.
        }

        internal virtual void EndSavepoint(Savepoint currentsp)
        {
            throw new NotImplementedException();
        }

        internal virtual Log BeginSavepoint()
        {
            throw new NotImplementedException();
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
    }

    public abstract class Log<TSelf, TValue> : Log where TSelf : Bean
    { 
        public TValue Value { get; set; }

        public Log(TSelf self, TValue value)
        { 
        }
        public TSelf BeanTyped => (TSelf)Belong;
        public virtual long LogKey { get; }
        public override void Decode(ByteBuffer bb)
        {
        }
        public override void Encode(ByteBuffer bb)
        {
        }
    }

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
            return $"{Value}";
        }
    }
}