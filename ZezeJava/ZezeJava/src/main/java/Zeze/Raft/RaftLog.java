package Zeze.Raft;

import java.util.function.IntFunction;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.TaskCompletionSource;

public final class RaftLog implements Serializable {
	private long Term;
	private long Index;
	private Log Log;

	private IntFunction<Log> LogFactory; // 不会被序列化。Local Only.
	private TaskCompletionSource<Integer> LeaderFuture;

	public RaftLog(long term, long index, Log log) {
		Term = term;
		Index = index;
		Log = log;
	}

	public RaftLog(IntFunction<Log> logFactory) {
		LogFactory = logFactory;
	}

	public long getTerm() {
		return Term;
	}

	public long getIndex() {
		return Index;
	}

	public Log getLog() {
		return Log;
	}

	public IntFunction<Log> getLogFactory() {
		return LogFactory;
	}

	public TaskCompletionSource<Integer> getLeaderFuture() {
		return LeaderFuture;
	}

	public void setLeaderFuture(TaskCompletionSource<Integer> leaderFuture) {
		LeaderFuture = leaderFuture;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Term);
		bb.WriteLong(Index);
		bb.WriteInt4(Log.getTypeId());
		Log.Encode(bb);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Term = bb.ReadLong();
		Index = bb.ReadLong();
		Log = LogFactory.apply(bb.ReadInt4());
		Log.Decode(bb);
	}

	public ByteBuffer Encode() {
		ByteBuffer bb = ByteBuffer.Allocate(64);
		Encode(bb);
		return bb;
	}

	public static RaftLog Decode(Binary data, IntFunction<Log> logFactory) {
		RaftLog raftLog = new RaftLog(logFactory);
		data.Decode(raftLog);
		return raftLog;
	}

	public static RaftLog DecodeTermIndex(byte[] bytes) {
		var bb = ByteBuffer.Wrap(bytes);
		var term = bb.ReadLong(); // term
		var index = bb.ReadLong(); // index
		return new RaftLog(term, index, null);
	}

	@Override
	public String toString() {
		return String.format("(Term=%d Index=%d Log=%s)", Term, Index, Log);
	}
}
