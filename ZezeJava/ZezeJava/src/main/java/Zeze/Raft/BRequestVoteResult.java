package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BRequestVoteResult extends Bean {
	private long term;
	private boolean voteGranted;

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public boolean getVoteGranted() {
		return voteGranted;
	}

	public void setVoteGranted(boolean value) {
		voteGranted = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteBool(voteGranted);
	}

	@Override
	public void decode(ByteBuffer bb) {
		term = bb.ReadLong();
		voteGranted = bb.ReadBool();
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
		return String.format("(Term=%d VoteGranted=%b)", term, voteGranted);
	}
}
