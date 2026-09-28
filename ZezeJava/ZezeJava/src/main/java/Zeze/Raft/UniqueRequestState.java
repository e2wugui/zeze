package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * 唯一请求存根状态：日志位置、是否已应用与已编码应答，用于重复请求检测与 RaftApplied 回放。
 */
class UniqueRequestState implements Serializable {
	public static final UniqueRequestState NOT_FOUND = new UniqueRequestState();

	private long logIndex;
	private boolean isApplied;
	private Binary rpcResult;

	public UniqueRequestState() {
	}

	public UniqueRequestState(RaftLog raftLog, boolean isApplied) {
		logIndex = raftLog.getIndex();
		this.isApplied = isApplied;
		rpcResult = raftLog.getLog().getRpcResult();
	}

	public boolean isApplied() {
		return isApplied;
	}

	public Binary getRpcResult() {
		return rpcResult;
	}

	@Override
	public final void encode(ByteBuffer bb) {
		bb.WriteLong(logIndex);
		bb.WriteBool(isApplied);
		bb.WriteBinary(rpcResult);
	}

	@Override
	public final void decode(IByteBuffer bb) {
		logIndex = bb.ReadLong();
		isApplied = bb.ReadBool();
		rpcResult = bb.ReadBinary();
	}
}
