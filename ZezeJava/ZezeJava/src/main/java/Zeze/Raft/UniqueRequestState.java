package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

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
