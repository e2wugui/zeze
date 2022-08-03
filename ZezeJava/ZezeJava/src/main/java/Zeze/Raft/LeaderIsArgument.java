package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

/**
 * 下面是非标准的Raft-Rpc，辅助Agent用的。
 */
final class LeaderIsArgument extends Bean {
	private long Term;
	private String LeaderId;
	private boolean IsLeader;

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

	public boolean isLeader() {
		return IsLeader;
	}

	public void setLeader(boolean value) {
		IsLeader = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteString(LeaderId);
		bb.WriteBool(IsLeader);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		LeaderId = bb.ReadString();
		IsLeader = bb.ReadBool();
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
	public int hashCode() {
		final int _prime_ = 31;
		return Long.hashCode(Term) * _prime_ + LeaderId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof LeaderIsArgument))
			return false;
		LeaderIsArgument other = (LeaderIsArgument)obj;
		return Term == other.Term && LeaderId.equals(other.LeaderId);
	}

	@Override
	public String toString() {
		return String.format("(Term=%d LeaderId=%s IsLeader=%b)", Term, LeaderId, IsLeader);
	}
}
