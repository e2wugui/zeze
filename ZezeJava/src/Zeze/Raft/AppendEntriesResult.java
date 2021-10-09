package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class AppendEntriesResult extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	private boolean Success;
	public boolean getSuccess() {
		return Success;
	}
	public void setSuccess(boolean value) {
		Success = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setSuccess(bb.ReadBool());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteBool(getSuccess());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s Success=%2$s)", getTerm(), getSuccess());
	}
}