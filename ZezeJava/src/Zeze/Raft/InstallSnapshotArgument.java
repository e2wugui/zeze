package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class InstallSnapshotArgument extends Bean {
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
	private long LastIncludedIndex;
	public long getLastIncludedIndex() {
		return LastIncludedIndex;
	}
	public void setLastIncludedIndex(long value) {
		LastIncludedIndex = value;
	}
	private long LastIncludedTerm;
	public long getLastIncludedTerm() {
		return LastIncludedTerm;
	}
	public void setLastIncludedTerm(long value) {
		LastIncludedTerm = value;
	}
	private long Offset;
	public long getOffset() {
		return Offset;
	}
	public void setOffset(long value) {
		Offset = value;
	}
	private Binary Data;
	public Binary getData() {
		return Data;
	}
	public void setData(Binary value) {
		Data = value;
	}
	private boolean Done;
	public boolean getDone() {
		return Done;
	}
	public void setDone(boolean value) {
		Done = value;
	}

	// 当Done为true时，把LastIncludedLog放到这里，Follower需要至少一个日志。
	private Binary LastIncludedLog = Binary.Empty;
	public Binary getLastIncludedLog() {
		return LastIncludedLog;
	}
	public void setLastIncludedLog(Binary value) {
		LastIncludedLog = value;
	}

	@Override
	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setLeaderId(bb.ReadString());
		setLastIncludedIndex(bb.ReadLong());
		setLastIncludedTerm(bb.ReadLong());

		setOffset(bb.ReadLong());
		setData(bb.ReadBinary());
		setDone(bb.ReadBool());

		setLastIncludedLog(bb.ReadBinary());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteString(getLeaderId());
		bb.WriteLong(getLastIncludedIndex());
		bb.WriteLong(getLastIncludedTerm());

		bb.WriteLong(getOffset());
		bb.WriteBinary(getData());
		bb.WriteBool(getDone());

		bb.WriteBinary(getLastIncludedLog());
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return String.format("(Term=%1$s LeaderId=%2$s LastIncludedIndex=%3$s LastIncludedTerm=%4$s Offset=%5$s Done=%6$s)", getTerm(), getLeaderId(), getLastIncludedIndex(), getLastIncludedTerm(), getOffset(), getDone());
	}
}