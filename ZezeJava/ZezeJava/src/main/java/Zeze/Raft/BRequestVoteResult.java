package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

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
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		voteGranted = bb.ReadBool();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d VoteGranted=%b)", term, voteGranted);
	}
}
