package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class RequestVoteResult extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	private boolean VoteGranted;
	public boolean getVoteGranted() {
		return VoteGranted;
	}
	public void setVoteGranted(boolean value) {
		VoteGranted = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setVoteGranted(bb.ReadBool());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteBool(getVoteGranted());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s VoteGranted=%2$s)", getTerm(), getVoteGranted());
	}
}