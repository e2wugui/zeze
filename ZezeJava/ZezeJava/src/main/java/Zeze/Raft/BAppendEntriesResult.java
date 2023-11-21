package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

final class BAppendEntriesResult extends Bean {
	private long term;
	private boolean success;
	private long nextIndex; // for fast locate when mismatch

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean value) {
		success = value;
	}

	public long getNextIndex() {
		return nextIndex;
	}

	public void setNextIndex(long value) {
		nextIndex = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteBool(success);
		bb.WriteLong(nextIndex);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		success = bb.ReadBool();
		nextIndex = bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d Success=%b NextIndex=%d)", term, success, nextIndex);
	}
}
