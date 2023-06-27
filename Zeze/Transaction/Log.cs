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
#if !USE_CONFCS
        public abstract void Commit();
        //public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

        public virtual long LogKey => Belong.ObjectId + VariableId;
        public Bean Belong;

        public virtual void Collect(Changes changes, Bean recent, Log vlog)
        {
            // LogBean LogCollection 需要实现这个方法收集日志.
        }

        internal abstract void EndSavepoint(Savepoint currentsp);

        internal abstract Log BeginSavepoint();

#endif
        // 会被系列化，实际上由LogBean管理。
        public abstract int TypeId { get; }
        public int VariableId;

        public abstract void Encode(ByteBuffer bb);
        public abstract void Decode(ByteBuffer bb);

        public static readonly ConcurrentDictionary<int, Func<Log>> Factories = new ConcurrentDictionary<int, Func<Log>>();

        public static Log Create(int typeId)
        {
            if (Factories.TryGetValue(typeId, out var factory))
                return factory();
            throw new Exception($"unknown log typeId={typeId}");
        }

        public static void Register<T>() where T : Log, new()
        {
            Factories.TryAdd(new T().TypeId, () => new T());
        }
    }

    public class Log<T> : Log
    {
        public static readonly string StableName = Util.Reflect.GetStableName(typeof(Log<T>));
        public static readonly int TypeId_ = Util.FixedHash.Hash32(StableName);

        public T Value;

        public override int TypeId => TypeId_;

        public override void Encode(ByteBuffer bb)
        {
            SerializeHelper<T>.Encode(bb, Value);
        }

        public override void Decode(ByteBuffer bb)
        {
            Value = SerializeHelper<T>.Decode(bb);
        }

        public override string ToString()
        {
            return $"{Value}";
        }

#if !USE_CONFCS
        internal override void EndSavepoint(Savepoint currentsp)
        {
            currentsp.Logs[LogKey] = this;
        }

        internal override Log BeginSavepoint()
        {
            return this;
        }

        public override void Commit()
        {
            throw new NotImplementedException();
        }
#endif
    }
}
