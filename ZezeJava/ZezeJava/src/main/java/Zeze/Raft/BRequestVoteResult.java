package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BRequestVoteResult extends Bean {
	private long Term;
	private boolean VoteGranted;

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public boolean getVoteGranted() {
		return VoteGranted;
	}

	public void setVoteGranted(boolean value) {
		VoteGranted = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteBool(VoteGranted);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		VoteGranted = bb.ReadBool();
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
		return String.format("(Term=%d VoteGranted=%b)", Term, VoteGranted);
	}
}
