package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

/**
 * 用户接口。
 */
public abstract class Log implements Serializable {
	/**
	 * 用于序列化，外部使用，实现类不用 Decode、Encode 这个变量。
	 * 只读，常量即可。
	 * 在一个StateMachine实现中唯一。
	 * 不冲突的时候使用默认实现即可。
	 * 【注意】
	 * 如果实现类的FullName发生了改变，需要更新所有的Raft-Node。
	 * 如果不想跟名字相关，重载并提供一个编号。
	 */
	private final int _TypeId;

	// 当前这个Log是哪个应用的Rpc请求引起的。
	// 【Raft用来检测重复的请求】。
	// RaftConfig里面配置AutoKeyLocalStep开启这个功能。
	// 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
	// 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
	private UniqueRequestId Unique = new UniqueRequestId();
	private long CreateTime;
	private Binary RpcResult = Binary.Empty;

	public Log(IRaftRpc req) {
		if (req != null) {
			Unique = req.getUnique();
			setCreateTime(req.getCreateTime());
		}
		_TypeId = Bean.hash32(getClass().getName());
	}

	public int getTypeId() {
		return _TypeId;
	}

	public UniqueRequestId getUnique() {
		return Unique;
	}

	public long getCreateTime() {
		return CreateTime;
	}

	public void setCreateTime(long value) {
		CreateTime = value;
	}

	public Binary getRpcResult() {
		return RpcResult;
	}

	public void setRpcResult(Binary value) {
		RpcResult = value;
	}

	/**
	 * 最主要的实现接口。
	 */
	public abstract void Apply(RaftLog holder, StateMachine stateMachine) throws Throwable;

	@Override
	public void Encode(ByteBuffer bb) {
		Unique.Encode(bb);
		bb.WriteLong(CreateTime);
		bb.WriteBinary(RpcResult);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Unique.Decode(bb);
		CreateTime = bb.ReadLong();
		RpcResult = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return String.format("(%s RequestId=%d Create=%d)",
				getClass().getSimpleName(), Unique.getRequestId(), CreateTime);
	}
}
