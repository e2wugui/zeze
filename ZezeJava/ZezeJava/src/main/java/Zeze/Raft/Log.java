package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * 用户接口。
 */
public abstract class Log implements Serializable {
	/**
	 * 用于序列化，外部使用，实现类不用 decode、encode 这个变量。
	 * 只读，常量即可。
	 * 在一个StateMachine实现中唯一。
	 * 不冲突的时候使用默认实现即可。
	 * 【注意】
	 * 如果实现类的FullName发生了改变，需要更新所有的Raft-Node。
	 * 如果不想跟名字相关，重载并提供一个编号。
	 * Log的typeId只使用低32位
	 */
	@Override
	public abstract long typeId();

	// 当前这个Log是哪个应用的Rpc请求引起的。
	// 【Raft用来检测重复的请求】。
	// RaftConfig里面配置AutoKeyLocalStep开启这个功能。
	// 启用这个功能要求应用的RpcSessionId持久化，并且全局唯一，对每个AutoKeyLocalStep递增。
	// 【注意】应用生成的Id必须大于0；0保留给内部；小于0未使用。
	private UniqueRequestId unique = new UniqueRequestId();
	private long createTime;
	private Binary rpcResult = Binary.Empty;

	public Log(IRaftRpc req) {
		if (req != null) {
			unique = req.getUnique();
			setCreateTime(req.getCreateTime());
		}
	}

	public UniqueRequestId getUnique() {
		return unique;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long value) {
		createTime = value;
	}

	public Binary getRpcResult() {
		return rpcResult;
	}

	public void setRpcResult(Binary value) {
		rpcResult = value;
	}

	/**
	 * 最主要的实现接口。
	 */
	public abstract void apply(RaftLog holder, StateMachine stateMachine) throws Exception;

	@Override
	public void encode(ByteBuffer bb) {
		unique.encode(bb);
		bb.WriteLong(createTime);
		bb.WriteBinary(rpcResult);
	}

	@Override
	public void decode(IByteBuffer bb) {
		unique.decode(bb);
		createTime = bb.ReadLong();
		rpcResult = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return String.format("(%s RequestId=%d Create=%d)",
				getClass().getSimpleName(), unique.getRequestId(), createTime);
	}
}
