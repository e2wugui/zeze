package Zeze.Raft;

import java.util.ArrayList;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

final class AppendEntriesArgument extends Bean {
	private long Term;
	private String LeaderId;
	private long PrevLogIndex;
	private long PrevLogTerm;
	private final ArrayList<Binary> Entries = new ArrayList<>();
	private long LeaderCommit;

	// Leader发送AppendEntries时，从这里快速得到Entries的最后一个日志的Index
	// 不会序列化。
	private long LastEntryIndex;

	public long getTerm() {
		return Term;
	}

	public void setTerm(long value) {
		Term = value;
	}

	public String getLeaderId() {
		return LeaderId;
	}

	public void setLeaderId(String value) {
		LeaderId = value;
	}

	public long getPrevLogIndex() {
		return PrevLogIndex;
	}

	public void setPrevLogIndex(long value) {
		PrevLogIndex = value;
	}

	public long getPrevLogTerm() {
		return PrevLogTerm;
	}

	public void setPrevLogTerm(long value) {
		PrevLogTerm = value;
	}

	public ArrayList<Binary> getEntries() {
		return Entries;
	}

	public long getLeaderCommit() {
		return LeaderCommit;
	}

	public void setLeaderCommit(long value) {
		LeaderCommit = value;
	}

	public long getLastEntryIndex() {
		return LastEntryIndex;
	}

	public void setLastEntryIndex(long value) {
		LastEntryIndex = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteString(LeaderId);
		bb.WriteLong(PrevLogIndex);
		bb.WriteLong(PrevLogTerm);

		bb.WriteUInt(Entries.size());
		for (Binary e : Entries)
			bb.WriteBinary(e);

		bb.WriteLong(LeaderCommit);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		LeaderId = bb.ReadString();
		PrevLogIndex = bb.ReadLong();
		PrevLogTerm = bb.ReadLong();

		Entries.clear();
		for (int c = bb.ReadUInt(); c > 0; c--)
			Entries.add(bb.ReadBinary());

		LeaderCommit = bb.ReadLong();
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
		return String.format("(Term=%d LeaderId=%s PrevLogIndex=%d PrevLogTerm=%d LeaderCommit=%d)",
				Term, LeaderId, PrevLogIndex, PrevLogTerm, LeaderCommit);
	}
}
