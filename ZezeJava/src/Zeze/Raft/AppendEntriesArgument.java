package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class AppendEntriesArgument extends Bean {
	private long Term;
	public long getTerm() {
		return Term;
	}
	public void setTerm(long value) {
		Term = value;
	}
	private String LeaderId;
	public String getLeaderId() {
		return LeaderId;
	}
	public void setLeaderId(String value) {
		LeaderId = value;
	}
	private long PrevLogIndex;
	public long getPrevLogIndex() {
		return PrevLogIndex;
	}
	public void setPrevLogIndex(long value) {
		PrevLogIndex = value;
	}
	private long PrevLogTerm;
	public long getPrevLogTerm() {
		return PrevLogTerm;
	}
	public void setPrevLogTerm(long value) {
		PrevLogTerm = value;
	}
	private ArrayList<Binary> Entries = new ArrayList<Binary> ();
	public ArrayList<Binary> getEntries() {
		return Entries;
	}
	private long LeaderCommit;
	public long getLeaderCommit() {
		return LeaderCommit;
	}
	public void setLeaderCommit(long value) {
		LeaderCommit = value;
	}

	// Leader发送AppendEntries时，从这里快速得到Entries的最后一个日志的Index
	// 不会系列化。
	private long LastEntryIndex;
	public long getLastEntryIndex() {
		return LastEntryIndex;
	}
	public void setLastEntryIndex(long value) {
		LastEntryIndex = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setLeaderId(bb.ReadString());
		setPrevLogIndex(bb.ReadLong());
		setPrevLogTerm(bb.ReadLong());

		getEntries().clear();
		for (int c = bb.ReadInt(); c > 0; --c) {
			getEntries().add(bb.ReadBinary());
		}

		setLeaderCommit(bb.ReadLong());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteString(getLeaderId());
		bb.WriteLong(getPrevLogIndex());
		bb.WriteLong(getPrevLogTerm());

		bb.WriteInt(getEntries().size());
		for (var e : getEntries()) {
			bb.WriteBinary(e);
		}

		bb.WriteLong(getLeaderCommit());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s LeaderId=%2$s PrevLogIndex=%3$s PrevLogTerm=%4$s LeaderCommit=%5$s)", getTerm(), getLeaderId(), getPrevLogIndex(), getPrevLogTerm(), getLeaderCommit());
	}
}