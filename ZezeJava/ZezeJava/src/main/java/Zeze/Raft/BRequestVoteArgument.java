package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

final class BRequestVoteArgument extends Bean {
	private long term;
	private String candidateId;
	private long lastLogIndex;
	private long lastLogTerm;
	private boolean nodeReady;

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public String getCandidateId() {
		return candidateId;
	}

	public void setCandidateId(String value) {
		candidateId = value;
	}

	public long getLastLogIndex() {
		return lastLogIndex;
	}

	public void setLastLogIndex(long value) {
		lastLogIndex = value;
	}

	public long getLastLogTerm() {
		return lastLogTerm;
	}

	public void setLastLogTerm(long value) {
		lastLogTerm = value;
	}

	public boolean getNodeReady() {
		return nodeReady;
	}

	public void setNodeReady(boolean value) {
		nodeReady = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteString(candidateId);
		bb.WriteLong(lastLogIndex);
		bb.WriteLong(lastLogTerm);
		bb.WriteBool(nodeReady);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		candidateId = bb.ReadString();
		lastLogIndex = bb.ReadLong();
		lastLogTerm = bb.ReadLong();
		nodeReady = bb.ReadBool();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d CandidateId=%s LastLogIndex=%d LastLogTerm=%d NodeReady=%b)",
				term, candidateId, lastLogIndex, lastLogTerm, nodeReady);
	}
}
