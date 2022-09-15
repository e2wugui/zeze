package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BInstallSnapshotArgument extends Bean {
	private long Term;
	private String LeaderId; // Ip:Port
	private long LastIncludedIndex;
	private long LastIncludedTerm;
	private long Offset;
	private Binary Data;
	private boolean Done;

	// 当Done为true时，把LastIncludedLog放到这里，Follower需要至少一个日志。
	private Binary LastIncludedLog = Binary.Empty;

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public String getLeaderId() {
		return LeaderId;
	}

	public void setLeaderId(String value) {
		LeaderId = value;
	}

	public long getLastIncludedIndex() {
		return LastIncludedIndex;
	}

	public void setLastIncludedIndex(long value) {
		LastIncludedIndex = value;
	}

	public long getLastIncludedTerm() {
		return LastIncludedTerm;
	}

	public void setLastIncludedTerm(long value) {
		LastIncludedTerm = value;
	}

	public long getOffset() {
		return Offset;
	}

	public void setOffset(long value) {
		Offset = value;
	}

	public Binary getData() {
		return Data;
	}

	public void setData(Binary value) {
		Data = value;
	}

	public boolean getDone() {
		return Done;
	}

	public void setDone(boolean value) {
		Done = value;
	}

	public Binary getLastIncludedLog() {
		return LastIncludedLog;
	}

	public void setLastIncludedLog(Binary value) {
		LastIncludedLog = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteString(LeaderId);
		bb.WriteLong(LastIncludedIndex);
		bb.WriteLong(LastIncludedTerm);

		bb.WriteLong(Offset);
		bb.WriteBinary(Data);
		bb.WriteBool(Done);

		bb.WriteBinary(LastIncludedLog);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		LeaderId = bb.ReadString();
		LastIncludedIndex = bb.ReadLong();
		LastIncludedTerm = bb.ReadLong();

		Offset = bb.ReadLong();
		Data = bb.ReadBinary();
		Done = bb.ReadBool();

		LastIncludedLog = bb.ReadBinary();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d LeaderId=%s LastIncludedIndex=%d LastIncludedTerm=%d Offset=%d Done=%b)",
				Term, LeaderId, LastIncludedIndex, LastIncludedTerm, Offset, Done);
	}
}
