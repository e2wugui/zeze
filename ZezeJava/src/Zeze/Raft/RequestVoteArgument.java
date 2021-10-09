package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

}

public final class RequestVoteArgument extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	private String CandidateId;
	public String getCandidateId() {
		return CandidateId;
	}
	public void setCandidateId(String value) {
		CandidateId = value;
	}
	private long LastLogIndex;
	public long getLastLogIndex() {
		return LastLogIndex;
	}
	public void setLastLogIndex(long value) {
		LastLogIndex = value;
	}
	private long LastLogTerm;
	public long getLastLogTerm() {
		return LastLogTerm;
	}
	public void setLastLogTerm(long value) {
		LastLogTerm = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setCandidateId(bb.ReadString());
		setLastLogIndex(bb.ReadLong());
		setLastLogTerm(bb.ReadLong());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteString(getCandidateId());
		bb.WriteLong(getLastLogIndex());
		bb.WriteLong(getLastLogTerm());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s CandidateId=%2$s LastLogIndex=%3$s LastLogTerm=%4$s)", getTerm(), getCandidateId(), getLastLogIndex(), getLastLogTerm());
	}
}