package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class RequestVoteArgument extends Bean {
	private long Term;
	private String CandidateId;
	private long LastLogIndex;
	private long LastLogTerm;
	private boolean NodeReady;

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public String getCandidateId() {
		return CandidateId;
	}

	public void setCandidateId(String value) {
		CandidateId = value;
	}

	public long getLastLogIndex() {
		return LastLogIndex;
	}

	public void setLastLogIndex(long value) {
		LastLogIndex = value;
	}

	public long getLastLogTerm() {
		return LastLogTerm;
	}

	public void setLastLogTerm(long value) {
		LastLogTerm = value;
	}

	public boolean getNodeReady() {
		return NodeReady;
	}

	public void setNodeReady(boolean value) {
		NodeReady = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteString(CandidateId);
		bb.WriteLong(LastLogIndex);
		bb.WriteLong(LastLogTerm);
		bb.WriteBool(NodeReady);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		CandidateId = bb.ReadString();
		LastLogIndex = bb.ReadLong();
		LastLogTerm = bb.ReadLong();
		NodeReady = bb.ReadBool();
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d CandidateId=%s LastLogIndex=%d LastLogTerm=%d NodeReady=%b)",
				Term, CandidateId, LastLogIndex, LastLogTerm, NodeReady);
	}
}
