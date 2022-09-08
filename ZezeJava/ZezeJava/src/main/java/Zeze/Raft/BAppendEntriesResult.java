package Zeze.Raft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class BAppendEntriesResult extends Bean {
	private long Term;
	private boolean Success;
	private long NextIndex; // for fast locate when mismatch

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public boolean getSuccess() {
		return Success;
	}

	public void setSuccess(boolean value) {
		Success = value;
	}

	public long getNextIndex() {
		return NextIndex;
	}

	public void setNextIndex(long value) {
		NextIndex = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteBool(Success);
		bb.WriteLong(NextIndex);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		Success = bb.ReadBool();
		NextIndex = bb.ReadLong();
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
		return String.format("(Term=%d Success=%b NextIndex=%d)", Term, Success, NextIndex);
	}
}
