package Zeze.Raft;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Action2;

/**
 * Raft 日志条目：term+index 定位，内嵌应用 Log 与 leader 侧回调。
 */
public final class RaftLog implements Serializable {
	private long term;
	private long index;
	private @Nullable Log log;

	private transient @Nullable IntFunction<@Nullable Log> logFactory; // 不会被序列化。Local Only.
	private transient @Nullable Action2<RaftLog, Boolean> leaderCallback;

	public RaftLog(long term, long index, @Nullable Log log) {
		this.term = term;
		this.index = index;
		this.log = log;
	}

	public RaftLog(IntFunction<@Nullable Log> logFactory) {
		this.logFactory = logFactory;
	}

	public long getTerm() {
		return term;
	}

	public long getIndex() {
		return index;
	}

	public @Nullable Log getLog() {
		return log;
	}

	public @Nullable IntFunction<@Nullable Log> getLogFactory() {
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

	public void setLeaderCallback(@Nullable Action2<RaftLog, Boolean> leaderCallback) {
		this.leaderCallback = leaderCallback;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(term);
		bb.WriteLong(index);
		//noinspection DataFlowIssue
		bb.WriteInt4((int)log.typeId());
		log.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		term = bb.ReadLong();
		index = bb.ReadLong();
		log = Objects.requireNonNull(logFactory).apply(bb.ReadInt4());
		//noinspection DataFlowIssue
		log.decode(bb);
	}

	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.Allocate(64);
		this.encode(bb);
		return bb;
	}

	public static RaftLog decode(Binary data, IntFunction<@Nullable Log> logFactory) {
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
