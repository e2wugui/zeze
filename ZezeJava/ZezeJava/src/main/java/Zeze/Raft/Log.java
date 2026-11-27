package Zeze.Raft;

import Zeze.Net.Binary;
import org.jspecify.annotations.Nullable;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * Raft 应用日志基类：状态机操作的单位，由 apply 应用到 StateMachine。
 */
public abstract class Log implements Serializable {
	/**
	 * 日志类型标识（仅使用低 32 位），在同一 StateMachine 内唯一且不参与自身序列化。
	 * 默认基于类名生成；如需避免类名变更影响兼容性，可重载返回固定编号。
	 */
	@Override
	public abstract long typeId();

	// 关联的唯一请求标识，用于去重（>0 为应用生成，0 保留内部使用）。
	private UniqueRequestId unique = new UniqueRequestId();
	private long createTime;
	private Binary rpcResult = Binary.Empty;

	public Log(@Nullable IRaftRpc req) {
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

	/** 将本条日志应用到状态机。 */
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
