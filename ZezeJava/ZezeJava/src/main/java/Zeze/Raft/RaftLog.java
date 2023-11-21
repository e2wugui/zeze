package Zeze.Raft;

import java.util.function.IntFunction;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action2;

public final class RaftLog implements Serializable {
	private long term;
	private long index;
	private Log log;

	private transient IntFunction<Log> logFactory; // 不会被序列化。Local Only.
	private transient Action2<RaftLog, Boolean> leaderCallback;

	public RaftLog(long term, long index, Log log) {
		this.term = term;
		this.index = index;
		this.log = log;
	}

	public RaftLog(IntFunction<Log> logFactory) {
		this.logFactory = logFactory;
	}

	public long getTerm() {
		return term;
	}

	public long getIndex() {
		return index;
	}

	public Log getLog() {
		return log;
	}

	public IntFunction<Log> getLogFactory() {
		return logFactory;
	}

	public void invokeCallback() throws Exception {
		if (null != leaderCallback)
			leaderCallback.run(this, true);
	}

	public void cancelCallback() throws Exception {
		if (null != leaderCallback)
			leaderCallback.run(this, false);
	}

	public boolean isLeaderRequest() {
		return null != leaderCallback;
	}

	public void setLeaderCallback(Action2<RaftLog, Boolean> leaderCallback) {
		this.leaderCallback = leaderCallback;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteLong(index);
		bb.WriteInt4((int)log.typeId());
		log.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		index = bb.ReadLong();
		log = logFactory.apply(bb.ReadInt4());
		log.decode(bb);
	}

	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.Allocate(64);
		this.encode(bb);
		return bb;
	}

	public static RaftLog decode(Binary data, IntFunction<Log> logFactory) {
		RaftLog raftLog = new RaftLog(logFactory);
		data.decode(raftLog);
		return raftLog;
	}

	public static RaftLog decodeTermIndex(byte[] bytes) {
		var bb = ByteBuffer.Wrap(bytes);
		var term = bb.ReadLong(); // term
		var index = bb.ReadLong(); // index
		return new RaftLog(term, index, null);
	}

	@Override
	public String toString() {
		return String.format("(Term=%d Index=%d Log=%s)", term, index, log);
	}
}
