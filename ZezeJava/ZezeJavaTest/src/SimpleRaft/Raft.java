package SimpleRaft;

import java.util.Arrays;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Raft {
	public static class Log {
		final long key; // log的唯一key. 防止重复. 由客户端填写
		long index; // 每条log的索引. 在log序列中顺序排列. 由leader填写
		long term; // 每条log的term值. 由leader填写

		public Log(long key) {
			this.key = key;
		}

		public long getKey() {
			return key;
		}

		public final long getIndex() {
			return index;
		}

		protected void execute() {
		}

		@Override
		public @NotNull String toString() {
			return "Log{" + "index=" + index + ", term=" + term + '}';
		}
	}

	public interface Env {
		long getCurTime();

		int random(int max); // return [0, max)

		@Nullable
		Object recvEvent(); // return null if no event

		void sendEvent(int id, @NotNull Object obj);

		void sendClientEvent(@NotNull Object obj);

		long getLogCount();

		Log getLog(long index); // return null if not exist

		@NotNull
		Log @NotNull [] getLogs(long indexBegin, long indexEnd); // [indexBegin, indexEnd)

		@Nullable
		Log getLogByKey(long key); // return null if not exist

		boolean appendLog(@NotNull Log log); // return false if this log(key) is already appended

		void appendLogs(@NotNull Log @NotNull [] logs); // ignore already appended log(key)s

		void truncateLogs(long logCount);

		void traceInfo(@NotNull String format);
	}

	private static final class RequestVote {
		final long term;
		final int candidateId;
		final long lastLogIndex;
		final long lastLogTerm;

		RequestVote(long term, int candidateId, long lastLogIndex, long lastLogTerm) {
			this.term = term;
			this.candidateId = candidateId;
			this.lastLogIndex = lastLogIndex;
			this.lastLogTerm = lastLogTerm;
		}

		@Override
		public @NotNull String toString() {
			return "RequestVote{" + "term=" + term + ", candidateId=" + candidateId + ", lastLogIndex=" + lastLogIndex
					+ ", lastLogTerm=" + lastLogTerm + '}';
		}
	}

	private static final class RequestVoteRe {
		final long term;
		final boolean voteGranted;

		RequestVoteRe(long term, boolean voteGranted) {
			this.term = term;
			this.voteGranted = voteGranted;
		}

		@Override
		public @NotNull String toString() {
			return "RequestVoteRe{" + "term=" + term + ", voteGranted=" + voteGranted + '}';
		}
	}

	private static final class AppendEntries {
		final long term;
		final int leaderId;
		final long prevLogIndex;
		final long prevLogTerm;
		final @NotNull Log @NotNull [] entries;
		final long leaderCommit;

		AppendEntries(long term, int leaderId, long prevLogIndex, long prevLogTerm, Log[] entries, long leaderCommit) {
			this.term = term;
			this.leaderId = leaderId;
			this.prevLogIndex = prevLogIndex;
			this.prevLogTerm = prevLogTerm;
			this.entries = entries;
			this.leaderCommit = leaderCommit;
		}

		@Override
		public @NotNull String toString() {
			return "AppendEntries{" + "term=" + term + ", leaderId=" + leaderId + ", prevLogIndex=" + prevLogIndex
					+ ", prevLogTerm=" + prevLogTerm + ", entries=" + Arrays.toString(entries) + ", leaderCommit="
					+ leaderCommit + '}';
		}
	}

	private static final class AppendEntriesRe {
		final long term;
		final boolean success;
		final int followerId;
		final long nextLogIndex;

		AppendEntriesRe(long term, boolean success, int followerId, long nextLogIndex) {
			this.term = term;
			this.followerId = followerId;
			this.success = success;
			this.nextLogIndex = nextLogIndex;
		}

		@Override
		public @NotNull String toString() {
			return "AppendEntriesRe{" + "term=" + term + ", success=" + success + ", followerId=" + followerId
					+ ", nextLogIndex=" + nextLogIndex + '}';
		}
	}

	public static final class AddLog { // 客户端发起AddLog请求. 客户端通过Env.sendEvent发送,服务器通过Env.recvEvent接收处理
		final long serial; // 请求的序列号,每次请求应该都不同,便于与AddLogRe对应
		final @NotNull Log log; // 准备增加的Log,可以是子类对象,不能为null

		AddLog(long serial, @NotNull Log log) {
			this.serial = serial;
			this.log = log;
		}

		@Override
		public @NotNull String toString() {
			return "AddLog{" + "serial=" + serial + ", log=" + log + '}';
		}
	}

	public static final class AddLogRe { // 服务器回复客户端的AddLog. 服务器通过Env.sendClientEvent发送,客户端通过Env.recvClientEvent接收
		final long serial; // 同AddLog请求的serial
		final int result; // 0表示新增成功,1表示已经成功过; <0表示失败,但跟超时一样不确定是否增加,可重试AddLog
		final int leaderId; // 当前的leader. result<0时需要检查leader,下次请求发到该leader. 如果<0则表示尚未选出leader

		AddLogRe(long serial, int result, int leaderId) {
			this.serial = serial;
			this.result = result;
			this.leaderId = leaderId;
		}

		@Override
		public @NotNull String toString() {
			return "AddLogRe{" + "serial=" + serial + ", result=" + result + ", leaderId=" + leaderId + '}';
		}
	}

	private static final boolean ENABLE_TRACE = true;
	private static final int VOTE_TIMEOUT = 1000;
	private static final int APPEND_TIMEOUT = 500;
	private static final int RANDOM_TIMEOUT_MAX = 200;
	private static final int APPEND_LOG_MAX = 100;
	private static final Log[] NO_LOG = new Log[0];

	private final @NotNull Env env;
	private final byte raftCount; // 所有raft节点数量. 有效值:[2,127]
	private final byte selfId; // 自身节点ID. 有效值>=0
	private byte leaderId = -1; // 当前的leader节点ID. -1表示自己是follower, -2-n表示自己是candidate(已有n个投票)
	private byte votedFor = -1; // 为currentTerm投票的节点ID. -1表示没有
	private long timeout; // 下次投票的超时时间戳(毫秒). `<=当前时间戳`表示应该触发超时
	private long currentTerm = -1; // 已知的最新term. 实际有效值一定>=0
	private long commitIndex = -1; // 当前最大的已经执行的log索引
	private final long @NotNull [] nextIndexes; // [仅leader用] 各节点下次需要复制的log索引
	private final long @NotNull [] appendingTimeouts; // [仅leader用] 各节点AppendEntries请求超时的时间戳(毫秒)
	private final HashMap<Long, Long> appendingLogMap = new HashMap<>(); // <index, serial>

	public Raft(final @NotNull Env env, final int raftCount, final int selfId, final long curTime) {
		if (raftCount < 2 || raftCount > 127)
			throw new IllegalArgumentException("raftCount=" + raftCount + " is not in [2,127]");
		if (selfId < 0 || selfId >= raftCount)
			throw new IllegalArgumentException("selfId=" + selfId + " is not in [0," + raftCount + ')');
		this.env = env;
		this.raftCount = (byte)raftCount;
		this.selfId = (byte)selfId;
		timeout = curTime + env.random(RANDOM_TIMEOUT_MAX);
		nextIndexes = new long[raftCount];
		appendingTimeouts = new long[raftCount];
		if (ENABLE_TRACE)
			env.traceInfo("started: timeout=" + timeout);
	}

	public int getLeaderId() {
		return leaderId;
	}

	public long getTerm() {
		return currentTerm;
	}

	public long getCommitIndex() {
		return commitIndex;
	}

	public void run() {
		run(-1);
	}

	public void run(int maxEvents) {
		final long curTime = env.getCurTime();
		if (curTime < 0)
			throw new IllegalArgumentException("curTime=" + curTime + " < 0");
		for (Object event; (maxEvents < 0 || --maxEvents >= 0) && (event = env.recvEvent()) != null; ) {
			if (event instanceof RequestVote)
				onProcess((RequestVote)event, curTime);
			else if (event instanceof RequestVoteRe)
				onProcess((RequestVoteRe)event, curTime);
			else if (event instanceof AppendEntries)
				onProcess((AppendEntries)event, curTime);
			else if (event instanceof AppendEntriesRe)
				onProcess((AppendEntriesRe)event, curTime);
			else if (event instanceof AddLog)
				onProcess((AddLog)event);
			else
				throw new UnsupportedOperationException("unknown event type: " + event.getClass().getName());
		}
		if (selfId == leaderId) { // 如果是leader的话,判断appendingTimeouts是否有超时,超时则发心跳
			for (int i = 0; i < raftCount; i++)
				if (i != selfId && appendingTimeouts[i] < curTime) {
					final long prevIndex = nextIndexes[i] - 1;
					final long prevTerm = prevIndex >= 0 ? env.getLog(prevIndex).term : -1;
					appendingTimeouts[i] = curTime + APPEND_TIMEOUT;
					env.sendEvent(i, new AppendEntries(currentTerm, selfId, prevIndex, prevTerm, NO_LOG, commitIndex));
				}
		} else if (timeout <= curTime) { // 如果不是leader的话,判断timeout是否超时,超时则开始选举
			leaderId = -2; // candidate begin
			votedFor = -1; // 先不给自己投票, 效率略高一点
			timeout = curTime + VOTE_TIMEOUT + env.random(RANDOM_TIMEOUT_MAX);
			final long term = ++currentTerm;
			final long lastIndex = env.getLogCount() - 1;
			final long lastTerm = lastIndex >= 0 ? env.getLog(lastIndex).term : -1;
			if (ENABLE_TRACE)
				env.traceInfo("candidate begin: lastIndex=" + lastIndex + ", lastTerm=" + lastTerm);
			for (int i = 0; i < raftCount; i++)
				if (i != selfId)
					env.sendEvent(i, new RequestVote(term, selfId, lastIndex, lastTerm));
		}
	}

	private void releaseLeader(int leaderId) {
		this.leaderId = (byte)leaderId;
		if (!appendingLogMap.isEmpty()) {
			for (final Long serial : appendingLogMap.values()) // 切换leader时需要把客户端未完成的请求一一回复失败
				env.sendClientEvent(new AddLogRe(serial, -3, leaderId));
			appendingLogMap.clear();
		}
	}

	private void onProcess(final @NotNull RequestVote req, final long curTime) {
		if (currentTerm < req.term) {
			currentTerm = req.term;
			votedFor = -1;
			timeout = curTime + VOTE_TIMEOUT + env.random(RANDOM_TIMEOUT_MAX);
			releaseLeader(-1);
			if (ENABLE_TRACE)
				env.traceInfo("update currentTerm for RequestVote.candidateId=" + req.candidateId);
		}
		boolean voteGranted = false;
		if (currentTerm == req.term && (votedFor == -1 || votedFor == req.candidateId)) {
			final long lastIndex = env.getLogCount() - 1;
			final long lastTerm = lastIndex >= 0 ? env.getLog(lastIndex).term : -1;
			if (lastTerm < req.lastLogTerm || lastTerm == req.lastLogTerm && lastIndex <= req.lastLogIndex) {
				votedFor = (byte)req.candidateId;
				voteGranted = true;
			} else if (leaderId == -1) // 如果发现对方版本比自己低,自己还没发起投票,那么立即开始成为candidate发起新一轮投票
				timeout = curTime;
			if (ENABLE_TRACE)
				env.traceInfo("vote " + voteGranted + "(leaderId=" + leaderId + ") for " + req);
		} else if (ENABLE_TRACE)
			env.traceInfo("vote false(voteFor=" + votedFor + ") for " + req);
		env.sendEvent(req.candidateId, new RequestVoteRe(currentTerm, voteGranted));
	}

	private void onProcess(final @NotNull RequestVoteRe res, final long curTime) {
		if (currentTerm < res.term) {
			currentTerm = res.term;
			votedFor = -1;
			timeout = curTime + VOTE_TIMEOUT + env.random(RANDOM_TIMEOUT_MAX);
			releaseLeader(-1);
			if (ENABLE_TRACE)
				env.traceInfo("update currentTerm for RequestVoteRe");
			return;
		}
		if (currentTerm != res.term) { // 忽略term不一致
			if (ENABLE_TRACE)
				env.traceInfo("ignore(term) " + res);
			return;
		}
		if (leaderId >= -1) { // 忽略非candidate状态
			if (ENABLE_TRACE)
				env.traceInfo("ignore(leaderId=" + leaderId + ") " + res);
			return;
		}
		if (!res.voteGranted)
			return;
		final int curVoteCount = -2 - --leaderId + (votedFor >>> 31);
		if (ENABLE_TRACE)
			env.traceInfo("add vote to " + curVoteCount);
		if (curVoteCount > (raftCount >> 1)) { // 过半投票成功
			leaderId = selfId;
			final long prevIndex = env.getLogCount() - 1;
			final long prevTerm = prevIndex >= 0 ? env.getLog(prevIndex).term : -1;
			final long appendTimeout = curTime + APPEND_TIMEOUT;
			if (ENABLE_TRACE) {
				env.traceInfo("leader begin: prevIndex=" + prevIndex + ", prevTerm=" + prevTerm
						+ ", commitIndex=" + commitIndex);
			}
			for (int i = 0; i < raftCount; i++) {
				if (i != selfId) {
					appendingTimeouts[i] = appendTimeout;
					env.sendEvent(i, new AppendEntries(currentTerm, selfId, prevIndex, prevTerm, NO_LOG, commitIndex));
				}
			}
		}
	}

	private void onProcess(final @NotNull AppendEntries req, final long curTime) {
		if (currentTerm > req.term) {
			if (ENABLE_TRACE)
				env.traceInfo("ignore(term) " + req);
			return;
		}
		if (currentTerm < req.term) {
			currentTerm = req.term;
			votedFor = -1;
			if (ENABLE_TRACE)
				env.traceInfo("update currentTerm for AppendEntries.leaderId=" + req.leaderId);
		}
		if (leaderId != req.leaderId) {
			releaseLeader(req.leaderId);
			if (ENABLE_TRACE)
				env.traceInfo("change leader to " + req);
		}
		timeout = curTime + VOTE_TIMEOUT + env.random(RANDOM_TIMEOUT_MAX);
		if (req.prevLogIndex >= 0) {
			final Log log = env.getLog(req.prevLogIndex);
			if (log == null || log.term != req.prevLogTerm) {
				if (ENABLE_TRACE)
					env.traceInfo("unmatched prevLog(term=" + (log != null ? log.term : -1) + "): " + req);
				env.sendEvent(req.leaderId, new AppendEntriesRe(currentTerm, false, selfId, env.getLogCount()));
				return;
			}
		}
		final int newEntryCount = req.entries.length;
		long nextIndex = req.prevLogIndex + 1;
		final long logCount = env.getLogCount();
		if (nextIndex < logCount) {
			if (nextIndex <= commitIndex)
				throw new IllegalStateException("nextIndex <= commitIndex(" + commitIndex + "): " + req);
			env.truncateLogs(nextIndex);
			if (ENABLE_TRACE)
				env.traceInfo("truncate logs: " + logCount + "=>" + nextIndex);
		}
		if (newEntryCount > 0) {
			env.appendLogs(req.entries);
			nextIndex += newEntryCount;
			if (ENABLE_TRACE)
				env.traceInfo("append logs: [" + (nextIndex - newEntryCount) + ',' + nextIndex + ')');
		}
		long lastApplied = commitIndex;
		commitIndex = Math.min(req.leaderCommit, nextIndex - 1);
		if (ENABLE_TRACE && lastApplied != commitIndex)
			env.traceInfo("follower: commitIndex=" + lastApplied + "=>" + commitIndex + ", logCount=" + nextIndex);
		while (lastApplied < commitIndex)
			env.getLog(++lastApplied).execute();
		env.sendEvent(req.leaderId, new AppendEntriesRe(currentTerm, true, selfId, nextIndex));
	}

	private void onProcess(final @NotNull AppendEntriesRe res, final long curTime) {
		if (currentTerm != res.term) { // 忽略term不一致
			if (ENABLE_TRACE)
				env.traceInfo("ignore(term) " + res);
			return;
		}
		if (selfId != leaderId) { // 不是leader的话就不处理了
			if (ENABLE_TRACE)
				env.traceInfo("ignore(leaderId=" + leaderId + ") " + res);
			return;
		}
		final int followerId = res.followerId;
		final long logCount = env.getLogCount(), nextIndex;
		appendingTimeouts[followerId] = curTime + APPEND_TIMEOUT;
		if (res.success) {
			nextIndexes[followerId] = nextIndex = res.nextLogIndex;
			nextIndexes[selfId] = logCount;
			final long[] tmpNextIndexes = nextIndexes.clone();
			Arrays.sort(tmpNextIndexes);
			long lastApplied = commitIndex;
			commitIndex = Math.max(lastApplied, tmpNextIndexes[(tmpNextIndexes.length - 1) >>> 1] - 1);
			if (ENABLE_TRACE && lastApplied != commitIndex)
				env.traceInfo("leader: commitIndex=" + lastApplied + "=>" + commitIndex + ", logCount=" + logCount);
			while (lastApplied < commitIndex) {
				env.getLog(++lastApplied).execute();
				final Long serial = appendingLogMap.remove(lastApplied);
				if (serial != null)
					env.sendClientEvent(new AddLogRe(serial, 0, leaderId));
			}
			if (nextIndex == logCount) // 已经完成同步
				return;
		} else // 退到上一版试试
			nextIndexes[followerId] = nextIndex = Math.max(Math.min(nextIndexes[followerId] - 1, res.nextLogIndex), 0);
		final long prevTerm = nextIndex > 0 ? env.getLog(nextIndex - 1).term : -1;
		final long nextEndIndex = Math.min(nextIndex + APPEND_LOG_MAX, logCount);
		if (ENABLE_TRACE && res.success)
			env.traceInfo("continue appending to " + followerId + ": [" + nextIndex + ',' + nextEndIndex + ')');
		env.sendEvent(followerId, new AppendEntries(currentTerm, selfId, nextIndex - 1, prevTerm, res.success ?
				env.getLogs(nextIndex, nextEndIndex) : NO_LOG, commitIndex));
	}

	private void onProcess(final @NotNull AddLog req) {
		if (selfId != leaderId) {
			env.sendClientEvent(new AddLogRe(req.serial, -1, leaderId));
			return;
		}
		final Log log = req.log;
		final long nextIndex = env.getLogCount();
		final long prevIndex = nextIndex - 1;
		log.index = nextIndex;
		log.term = currentTerm;
		if (env.appendLog(log)) {
			final Long oldSerial = appendingLogMap.put(log.index, req.serial);
			if (oldSerial != null)
				throw new IllegalStateException("duplicate log index: " + req);
			Log[] logs = null;
			long prevTerm = -2;
			if (ENABLE_TRACE)
				env.traceInfo(req.toString());
			for (int i = 0; i < raftCount; i++) {
				if (i != selfId && nextIndexes[i] == nextIndex) { // 只对完成同步的立即复制
					if (logs == null) {
						logs = new Log[]{log};
						prevTerm = prevIndex >= 0 ? env.getLog(prevIndex).term : -1;
					}
					if (ENABLE_TRACE)
						env.traceInfo("append to " + i + ": index=" + nextIndex + ", prevTerm=" + prevTerm);
					env.sendEvent(i, new AppendEntries(currentTerm, selfId, prevIndex, prevTerm, logs, commitIndex));
				}
			}
		} else {
			final Log oldLog = env.getLogByKey(log.key);
			if (oldLog != null) {
				if (oldLog.index <= commitIndex)
					env.sendClientEvent(new AddLogRe(req.serial, 1, leaderId));
				else {
					final Long oldSerial = appendingLogMap.put(oldLog.index, req.serial);
					if (oldSerial != null && oldSerial != req.serial)
						env.sendClientEvent(new AddLogRe(oldSerial, -2, leaderId));
				}
			} else
				throw new IllegalStateException("can not append: " + req);
		}
	}

	@Override
	public @NotNull String toString() {
		return "Raft{" + "raftCount=" + raftCount + ", selfId=" + selfId + ", leaderId=" + leaderId + ", votedFor="
				+ votedFor + ", timeout=" + timeout + ", currentTerm=" + currentTerm + ", commitIndex=" + commitIndex
				+ ", nextIndexes=" + Arrays.toString(nextIndexes) + ", appendingTimeouts="
				+ Arrays.toString(appendingTimeouts) + ", appendingLogMap=" + appendingLogMap + '}';
	}
}
