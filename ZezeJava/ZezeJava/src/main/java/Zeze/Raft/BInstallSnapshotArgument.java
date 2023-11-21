package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

final class BInstallSnapshotArgument extends Bean {
	private long term;
	private String leaderId; // Ip:Port
	private long lastIncludedIndex;
	private long lastIncludedTerm;
	private long offset;
	private Binary data;
	private boolean done;

	// 当Done为true时，把LastIncludedLog放到这里，Follower需要至少一个日志。
	private Binary lastIncludedLog = Binary.Empty;

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public String getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(String value) {
		leaderId = value;
	}

	public long getLastIncludedIndex() {
		return lastIncludedIndex;
	}

	public void setLastIncludedIndex(long value) {
		lastIncludedIndex = value;
	}

	public long getLastIncludedTerm() {
		return lastIncludedTerm;
	}

	public void setLastIncludedTerm(long value) {
		lastIncludedTerm = value;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long value) {
		offset = value;
	}

	public Binary getData() {
		return data;
	}

	public void setData(Binary value) {
		data = value;
	}

	public boolean getDone() {
		return done;
	}

	public void setDone(boolean value) {
		done = value;
	}

	public Binary getLastIncludedLog() {
		return lastIncludedLog;
	}

	public void setLastIncludedLog(Binary value) {
		lastIncludedLog = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteString(leaderId);
		bb.WriteLong(lastIncludedIndex);
		bb.WriteLong(lastIncludedTerm);

		bb.WriteLong(offset);
		bb.WriteBinary(data);
		bb.WriteBool(done);

		bb.WriteBinary(lastIncludedLog);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		leaderId = bb.ReadString();
		lastIncludedIndex = bb.ReadLong();
		lastIncludedTerm = bb.ReadLong();

		offset = bb.ReadLong();
		data = bb.ReadBinary();
		done = bb.ReadBool();

		lastIncludedLog = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d LeaderId=%s LastIncludedIndex=%d LastIncludedTerm=%d Offset=%d Done=%b)",
				term, leaderId, lastIncludedIndex, lastIncludedTerm, offset, done);
	}
}
