package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;

class UniqueRequestState implements Serializable {
	public static final UniqueRequestState NOT_FOUND = new UniqueRequestState();

	private long LogIndex;
	private boolean IsApplied;
	private Binary RpcResult;

	public UniqueRequestState() {
	}

	public UniqueRequestState(RaftLog raftLog, boolean isApplied) {
		LogIndex = raftLog.getIndex();
		IsApplied = isApplied;
		RpcResult = raftLog.getLog().getRpcResult();
	}

	public boolean isApplied() {
		return IsApplied;
	}

	public Binary getRpcResult() {
		return RpcResult;
	}

	@Override
	public final void Encode(ByteBuffer bb) {
		bb.WriteLong(LogIndex);
		bb.WriteBool(IsApplied);
		bb.WriteBinary(RpcResult);
	}

	@Override
	public final void Decode(ByteBuffer bb) {
		LogIndex = bb.ReadLong();
		IsApplied = bb.ReadBool();
		RpcResult = bb.ReadBinary();
	}
}
