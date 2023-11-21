package Zeze.Raft;

import java.util.ArrayList;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

final class BAppendEntriesArgument extends Bean {
	private long term;
	private String leaderId;
	private long prevLogIndex;
	private long prevLogTerm;
	private final ArrayList<Binary> entries = new ArrayList<>();
	private long leaderCommit;

	// Leader发送AppendEntries时，从这里快速得到Entries的最后一个日志的Index
	// 不会序列化。
	private long lastEntryIndex;

	public long getTerm() {
		return term;
	}

	public void setTerm(long value) {
		term = value;
	}

	public String getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(String value) {
		leaderId = value;
	}

	public long getPrevLogIndex() {
		return prevLogIndex;
	}

	public void setPrevLogIndex(long value) {
		prevLogIndex = value;
	}

	public long getPrevLogTerm() {
		return prevLogTerm;
	}

	public void setPrevLogTerm(long value) {
		prevLogTerm = value;
	}

	public ArrayList<Binary> getEntries() {
		return entries;
	}

	public long getLeaderCommit() {
		return leaderCommit;
	}

	public void setLeaderCommit(long value) {
		leaderCommit = value;
	}

	public long getLastEntryIndex() {
		return lastEntryIndex;
	}

	public void setLastEntryIndex(long value) {
		lastEntryIndex = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteString(leaderId);
		bb.WriteLong(prevLogIndex);
		bb.WriteLong(prevLogTerm);

		bb.WriteUInt(entries.size());
		for (Binary e : entries)
			bb.WriteBinary(e);

		bb.WriteLong(leaderCommit);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		leaderId = bb.ReadString();
		prevLogIndex = bb.ReadLong();
		prevLogTerm = bb.ReadLong();

		entries.clear();
		for (int c = bb.ReadUInt(); c > 0; c--)
			entries.add(bb.ReadBinary());

		leaderCommit = bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.format("(Term=%d LeaderId=%s PrevLogIndex=%d PrevLogTerm=%d LeaderCommit=%d)",
				term, leaderId, prevLogIndex, prevLogTerm, leaderCommit);
	}
}
