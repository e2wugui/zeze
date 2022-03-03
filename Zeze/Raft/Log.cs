using Zeze.Serialize;
using Zeze.Transaction;
using System;
using Zeze.Net;
using System.Threading.Tasks;

namespace Zeze.Raft
{
    /// <summary>
    /// 用户接口。
    /// </summary>
    public abstract class Log : Serializable
    {
        /// <summary>
        /// 用于系列化，外部使用，实现类不用 Decode、Encode 这个变量。
        /// 只读，常量即可。
        /// 在一个StateMachine实现中唯一。
        /// 不冲突的时候使用默认实现即可。
        /// 【注意】
        /// 如果实现类的FullName发生了改变，需要更新所有的Raft-Node。
        /// 如果不想跟名字相关，重载并提供一个编号。
        /// </summary>
        private readonly int _TypeId;
        public virtual int TypeId => _TypeId;

        // 当前这个Log是哪个应用的Rpc请求引起的。
        // 【Raft用来检测重复的请求】。
        // RaftConfig里面配置AutoKeyLocalStep开启这个功能。
        // 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
        // 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
        public UniqueRequestId Unique { get; } = new UniqueRequestId();
        public long CreateTime { get; set; }
        public Binary RpcResult { get; set; } = Binary.Empty;

        public Log(IRaftRpc req)
        {
            if (null != req)
            {
                Unique = req.Unique;
                CreateTime = req.CreateTime;
            }
            _TypeId = (int)Bean.Hash32(GetType().FullName);
        }

        /// <summary>
        /// 最主要的实现接口。
        /// </summary>
        /// <param name="stateMachine"></param>
        public abstract void Apply(RaftLog holder, StateMachine stateMachine);

        public virtual void Decode(ByteBuffer bb)
        {
            Unique.Decode(bb);
            CreateTime = bb.ReadLong();
            RpcResult = bb.ReadBinary();
        }

        public virtual void Encode(ByteBuffer bb)
        {
            Unique.Encode(bb);
            bb.WriteLong(CreateTime);
            bb.WriteBinary(RpcResult);
        }

        public override string ToString()
        {
            return $"{GetType().Name} RequestId={Unique.RequestId} Create={CreateTime}";
        }
    }
}
