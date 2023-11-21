package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

/**
 * 下面是非标准的Raft-Rpc，辅助Agent用的。
 */
final class BLeaderIsArgument extends Bean {
	private long term;
	private String leaderId;
	private boolean isLeader;

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

	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean value) {
		isLeader = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteString(leaderId);
		bb.WriteBool(isLeader);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		leaderId = bb.ReadString();
		isLeader = bb.ReadBool();
	}

	@Override
	public int hashCode() {
		final int _prime_ = 31;
		return Long.hashCode(term) * _prime_ + leaderId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof BLeaderIsArgument))
			return false;
		BLeaderIsArgument other = (BLeaderIsArgument)obj;
		return term == other.term && leaderId.equals(other.leaderId);
	}

	@Override
	public String toString() {
		return String.format("(Term=%d LeaderId=%s IsLeader=%b)", term, leaderId, isLeader);
	}
}
