package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.Util.TaskCanceledException;
import RocksDbSharp.*;
import Zeze.Net.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

public class LogSequence {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private Raft Raft;
	public final Raft getRaft() {
		return Raft;
	}

	private long Term;
	public final long getTerm() {
		return Term;
	}
	private void setTerm(long value) {
		Term = value;
	}
	private long LastIndex;
	public final long getLastIndex() {
		return LastIndex;
	}
	private void setLastIndex(long value) {
		LastIndex = value;
	}
	// 用来处理NextIndex回溯时限制搜索。snapshot需要修订这个值。
	private long FirstIndex;
	public final long getFirstIndex() {
		return FirstIndex;
	}
	private void setFirstIndex(long value) {
		FirstIndex = value;
	}
	private long CommitIndex;
	public final long getCommitIndex() {
		return CommitIndex;
	}
	private void setCommitIndex(long value) {
		CommitIndex = value;
	}
	private long LastApplied;
	public final long getLastApplied() {
		return LastApplied;
	}
	private void setLastApplied(long value) {
		LastApplied = value;
	}

	// 这个不是日志需要的，因为持久化，所以就定义在这里吧。
	private String VoteFor;
	public final String getVoteFor() {
		return VoteFor;
	}
	public final void setVoteFor(String value) {
		VoteFor = value;
	}

	// 初始化的时候会加入一条日志(Index=0，不需要真正apply)，
	// 以后Snapshot时，会保留LastApplied的。
	// 所以下面方法不会返回空。
	// 除非什么例外发生。那就抛空指针异常吧。
	public final RaftLog LastAppliedLog() {
		return ReadLog(getLastApplied());
	}

	public final long GetAndSetFirstIndex(long newFirstIndex) {
		synchronized (getRaft()) {
			long tmp = getFirstIndex();
			setFirstIndex(newFirstIndex);
			return tmp;
		}
	}

	public final void RemoveLogBeforeLastApplied(long oldFirstIndex) {
		RemoveLogReverse(getLastApplied() - 1, oldFirstIndex);
	}

	private void RemoveLogReverse(long startIndex, long firstIndex) {
		if (startIndex >= getLastApplied()) {
			throw new RuntimeException("Error At Least Retain One Applied Log");
		}

		for (var index = startIndex; index >= firstIndex; --index) {
			var key = ByteBuffer.Allocate();
			key.WriteLong8(index);
			getLogs().Remove(key.getBytes(), key.getSize());
		}
	}

	// Leader
	private long AppendLogActiveTime = Zeze.Util.Time.getNowUnixMillis();
	public final long getAppendLogActiveTime() {
		return AppendLogActiveTime;
	}
	public final void setAppendLogActiveTime(long value) {
		AppendLogActiveTime = value;
	}
	// Follower
	private long LeaderActiveTime = Zeze.Util.Time.getNowUnixMillis();
	public final long getLeaderActiveTime() {
		return LeaderActiveTime;
	}
	private void setLeaderActiveTime(long value) {
		LeaderActiveTime = value;
	}

	private RocksDb Logs;
	private RocksDb getLogs() {
		return Logs;
	}
	private void setLogs(RocksDb value) {
		Logs = value;
	}
	private RocksDb Rafts;
	private RocksDb getRafts() {
		return Rafts;
	}
	private void setRafts(RocksDb value) {
		Rafts = value;
	}

	public final void Close() {
		synchronized (getRaft()) {
			if (getLogs() != null) {
				getLogs().Dispose();
			}
			setLogs(null);
			if (getRafts() != null) {
				getRafts().Dispose();
			}
			setRafts(null);
		}
	}

	public LogSequence(Raft raft) {
		Raft = raft;
		var options = (new DbOptions()).SetCreateIfMissing(true);

		setRafts(RocksDb.Open(options, Paths.get(getRaft().getRaftConfig().getDbHome()).resolve("rafts").toString())); {
			// Read Term
			var termKey = ByteBuffer.Allocate();
			termKey.WriteInt(0);
			RaftsTermKey = termKey.Copy();
			var termValue = getRafts().Get(RaftsTermKey);
			if (null != termValue) {
				var bb = ByteBuffer.Wrap(termValue);
				setTerm(bb.ReadLong());
			}
			else {
				setTerm(0);
			}
			// Read VoteFor
			var voteForKey = ByteBuffer.Allocate();
			voteForKey.WriteInt(1);
			RaftsVoteForKey = voteForKey.Copy();
			var voteForvalue = getRafts().Get(RaftsVoteForKey);
			if (null != voteForvalue) {
				var bb = ByteBuffer.Wrap(voteForvalue);
				setVoteFor(bb.ReadString());
			}
			else {
				setVoteFor("");
			}
		}


		setLogs(RocksDb.Open(options, Paths.get(getRaft().getRaftConfig().getDbHome()).resolve("logs").toString())); {
			// Read Last Log Index
			try (var itLast = getLogs().NewIterator()) {
				itLast.SeekToLast();
				if (itLast.Valid()) {
					setLastIndex(RaftLog.Decode(new Binary(itLast.Value()), getRaft().getStateMachine().LogFactory).getIndex());
				}
				else {
					// empty. add one for prev.
					SaveLog(new RaftLog(getTerm(), 0, new HeartbeatLog()));
					setLastIndex(0);
				}
    
				try (var itFirst = getLogs().NewIterator()) {
					itFirst.SeekToFirst();
					setFirstIndex(RaftLog.Decode(new Binary(itFirst.Value()), getRaft().getStateMachine().LogFactory).getIndex());
					// 【注意】snapshot 以后 FirstIndex 会推进，不再是从0开始。
					setLastApplied(getFirstIndex());
					setCommitIndex(getFirstIndex());
				}
			}
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private readonly byte[] RaftsTermKey;
	private final byte[] RaftsTermKey;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private readonly byte[] RaftsVoteForKey;
	private final byte[] RaftsVoteForKey;

	private void SaveLog(RaftLog log) {
		setLastIndex(log.getIndex()); // 记住最后一个Index，用来下一次生成。

		var key = ByteBuffer.Allocate();
		key.WriteLong8(log.getIndex());
		var value = log.Encode();

		// key,value offset must 0
		getLogs().Put(key.getBytes(), key.getSize(), value.getBytes(), value.getSize(), null, (new WriteOptions()).SetSync(true));
	}

	private RaftLog ReadLog(long index) {
		var key = ByteBuffer.Allocate();
		key.WriteLong8(index);
		var value = getLogs().Get(key.getBytes(), key.getSize());
		if (null == value) {
			return null;
		}
		return RaftLog.Decode(new Binary(value), getRaft().getStateMachine().LogFactory);
	}

	public final boolean TrySetTerm(long term) {
		if (term > getTerm()) {
			setTerm(term);
			var termValue = ByteBuffer.Allocate();
			termValue.WriteLong(term);
			getRafts().Put(RaftsTermKey, RaftsTermKey.length, termValue.getBytes(), termValue.getSize(), null, (new WriteOptions()).SetSync(true));
			return true;
		}
		return false;
	}

	public final boolean CanVoteFor(String voteFor) {
		return tangible.StringHelper.isNullOrEmpty(getVoteFor()) || getVoteFor().equals(voteFor);
	}

	public final void SetVoteFor(String voteFor) {
		if (false == getVoteFor().equals(voteFor)) {
			setVoteFor(voteFor);
			var voteForValue = ByteBuffer.Allocate();
			voteForValue.WriteString(voteFor);
			getRafts().Put(RaftsVoteForKey, RaftsVoteForKey.length, voteForValue.getBytes(), voteForValue.getSize(), null, (new WriteOptions()).SetSync(true));
		}
	}

	/** 
	 从startIndex开始，直到找到一个存在的日志。
	 最多找到结束Index。这是为了能处理Index不连续。
	 虽然算法上不可能，但花几行代码这样处理一下吧。
	 这个方法看起来也有可能返回null，实际上应该不会发生。
	 
	 @param startIndex
	 @return 
	*/
	private RaftLog ReadLogStart(long startIndex) {
		for (long index = startIndex; index <= getLastIndex(); ++index) {
			var raftLog = ReadLog(index);
			if (null != raftLog) {
				return raftLog;
			}
		}
		return null;
	}

	private RaftLog FindMaxMajorityLog(long startIndex) {
		RaftLog lastMajorityLog = null;
		for (long index = startIndex; index <= getLastIndex();) {
			var raftLog = ReadLogStart(index);
			if (null == raftLog) {
				break;
			}
			index = raftLog.getIndex() + 1;
			int MajorityCount = 0;
			getRaft().getServer().getConfig().ForEachConnector((c) -> {
						var cex = c instanceof Server.ConnectorEx ? (Server.ConnectorEx)c : null;
						if (cex.getMatchIndex() >= raftLog.getIndex()) {
							++MajorityCount;
						}
			});
			// 没有达成多数派，中断循环。后面返回上一个majority，仍可能为null。
			// 等于的时候加上自己就是多数派了。
			if (MajorityCount < getRaft().getRaftConfig().getHalfCount()) {
				break;
			}
			lastMajorityLog = raftLog;
		}
		return lastMajorityLog;
	}

	private void TryCommit(AppendEntries rpc, Server.ConnectorEx connector) {
		connector.setNextIndex(rpc.getArgument().getLastEntryIndex() + 1);
		connector.setMatchIndex(rpc.getArgument().getLastEntryIndex());

		// 已经提交的，旧的 AppendEntries 的结果，不用继续处理了。
		// 【注意】这个不是必要的，是一个小优化。
		if (rpc.getArgument().getLastEntryIndex() <= getCommitIndex()) {
			return;
		}

		// Rules for Servers
		// If there exists an N such that N > commitIndex, a majority
		// of matchIndex[i] ≥ N, and log[N].term == currentTerm:
		// set commitIndex = N(§5.3, §5.4).

		// TODO 对于 Leader CommitIndex 初始化问题。
		var raftLog = FindMaxMajorityLog(getCommitIndex() + 1);
		if (null == raftLog) {
			return; // 一个多数派都没有找到。
		}

		if (raftLog.getTerm() != getTerm()) {
			// 如果是上一个 Term 未提交的日志在这一次形成的多数派，
			// 不自动提交。
			// 总是等待当前 Term 推进时，顺便提交它。
			return;
		}
		setCommitIndex(raftLog.getIndex());
		TryApply(raftLog);
	}

	private void TryApply(RaftLog lastApplyableLog) {
		if (null == lastApplyableLog) {
			logger.Error("lastApplyableLog is null.");
			return;
		}
		for (long index = getLastApplied() + 1; index <= lastApplyableLog.getIndex();) {
			var raftLog = ReadLogStart(index);
			if (null == raftLog) {
				logger.Warn("What Happened! index={0} lastApplyable={1} LastApplied={2}", index, lastApplyableLog.getIndex(), getLastApplied());
				return; // end?
			}

			index = raftLog.getIndex() + 1;

			if (raftLog.getLog().UniqueRequestId > 0) {
				// 这是防止请求重复执行用的。
				// 需要对每个Raft.Agent的请求排队处理。
				// see Net.cs Server.DispatchProtocol

				// 这里不需要递增判断：由于请求是按网络传过来的顺序处理的，到达这里肯定是递增的。
				// 如果来自客户端的请求Id不是增长的，在 Net.cs::Server 处理时会拒绝掉。
				getLastAppliedAppRpcUniqueRequestId().put(raftLog.getLog().AppInstance, raftLog.getLog().UniqueRequestId);
			}
			raftLog.getLog().Apply(getRaft().getStateMachine());
			setLastApplied(raftLog.getIndex()); // 循环可能退出，在这里修改。

			TValue future;
			tangible.OutObject<TaskCompletionSource<Integer>> tempOut_future = new tangible.OutObject<TaskCompletionSource<Integer>>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getWaitApplyFutures().TryRemove(raftLog.getIndex(), tempOut_future)) {
			future = tempOut_future.outArgValue;
				future.SetResult(0);
			}
		else {
			future = tempOut_future.outArgValue;
		}
		}
	}

	private java.util.concurrent.ConcurrentHashMap<String, Long> LastAppliedAppRpcUniqueRequestId = new java.util.concurrent.ConcurrentHashMap<String, Long> ();
	public final java.util.concurrent.ConcurrentHashMap<String, Long> getLastAppliedAppRpcUniqueRequestId() {
		return LastAppliedAppRpcUniqueRequestId;
	}

	private java.util.concurrent.ConcurrentHashMap<Long, TaskCompletionSource<Integer>> WaitApplyFutures = new java.util.concurrent.ConcurrentHashMap<Long, TaskCompletionSource<Integer>> ();
	public final java.util.concurrent.ConcurrentHashMap<Long, TaskCompletionSource<Integer>> getWaitApplyFutures() {
		return WaitApplyFutures;
	}


	public final void AppendLog(Log log) {
		AppendLog(log, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void AppendLog(Log log, bool WaitApply = true)
	public final void AppendLog(Log log, boolean WaitApply) {
		if (false == getRaft().isLeader()) {
			throw new TaskCanceledException(); // 快速失败
		}

		TaskCompletionSource<Integer> future = null;
		synchronized (getRaft()) {
			setLastIndex(getLastIndex() + 1);
			var raftLog = new RaftLog(getTerm(), getLastIndex(), log);
			if (WaitApply) {
				future = new TaskCompletionSource<Integer>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				if (false == getWaitApplyFutures().TryAdd(raftLog.getIndex(), future)) {
					throw new RuntimeException("Impossible");
				}
			}
			SaveLog(raftLog);
		}

		// 广播给followers并异步等待多数确认
		getRaft().getServer().getConfig().ForEachConnector((connector) -> TrySendAppendEntries(connector instanceof Server.ConnectorEx ? (Server.ConnectorEx)connector : null, null));

		if (WaitApply) {
			future.Task.Wait();
		}
	}

	/** 
	 see ReadLogStart
	 
	 @param startIndex
	 @return 
	*/
	private RaftLog ReadLogReverse(long startIndex) {
		for (long index = startIndex; index >= getFirstIndex(); --index) {
			var raftLog = ReadLog(index);
			if (null != raftLog) {
				return raftLog;
			}
		}
		logger.Error(String.format("impossible")); // 日志列表肯定不会为空。
		return null;
	}

	// 是否正在创建Snapshot过程中，用来阻止新的创建请求。
	private boolean Snapshotting = false;
	private boolean getSnapshotting() {
		return Snapshotting;
	}
	private void setSnapshotting(boolean value) {
		Snapshotting = value;
	}
	// 是否有安装进程正在进行中，用来阻止新的创建请求。
	private HashMap<String, Server.ConnectorEx> InstallSnapshotting = new HashMap<String, Server.ConnectorEx> ();
	public final HashMap<String, Server.ConnectorEx> getInstallSnapshotting() {
		return InstallSnapshotting;
	}

	public static final String SnapshotFileName = "snapshot";
	private Util.SchedulerTask SnapshotTimer;

	public final void StopSnapshotPerDayTimer() {
		synchronized (getRaft()) {
			if (SnapshotTimer != null) {
				SnapshotTimer.Cancel();
			}
			SnapshotTimer = null;
		}
	}

	public final void StartSnapshotPerDayTimer() {
		synchronized (getRaft()) {
			if (null != SnapshotTimer) {
				return;
			}

			if (getRaft().getRaftConfig().getSnapshotHourOfDay() >= 0 && getRaft().getRaftConfig().getSnapshotHourOfDay() < 24) {
				var now = LocalDateTime.now();
				var firstTime = LocalDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), getRaft().getRaftConfig().getSnapshotHourOfDay(), getRaft().getRaftConfig().getSnapshotMinute(), 0);
				if (firstTime.CompareTo(now) < 0) {
					firstTime = firstTime.plusDays(1);
				}
				var delay = Util.Time.DateTimeToUnixMillis(firstTime) - Util.Time.DateTimeToUnixMillis(now);
				SnapshotTimer = Zeze.Util.Scheduler.getInstance().Schedule((ThisTask) -> StartSnapshot(false), delay, 20 * 3600 * 1000);
			}
		}
	}

//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
	public final void EndReceiveInstallSnapshot(FileStream s, InstallSnapshot r) {
		synchronized (getRaft()) {
			// 6. If existing log entry has same index and term as snapshot's
			// last included entry, retain log entries following it and reply
			var last = ReadLog(r.getArgument().getLastIncludedIndex());
			if (null != last && last.getTerm() == r.getArgument().getLastIncludedTerm()) {
				// 这里全部保留更简单吧，否则如果没有applied，那不就糟了吗？
				// RemoveLogReverse(r.Argument.LastIncludedIndex - 1);
				return;
			}
			// 7. Discard the entire log
			// 整个删除，那么下一次AppendEnties又会找不到prev。不就xxx了吗?
			// 我的想法是，InstallSnapshot 最后一个 trunk 带上 LastIncludedLog，
			// 接收者清除log，并把这条日志插入（这个和系统初始化时插入的Index=0的日志道理差不多）。
			// 【除了快照最后包含的日志，其他都删除。】
			var lastIncludedLog = RaftLog.Decode(r.getArgument().getLastIncludedLog(), getRaft().getStateMachine().LogFactory);
			SaveLog(lastIncludedLog);
			// follower 没有并发请求需要处理，在锁内删除。
			RemoveLogReverse(lastIncludedLog.getIndex() - 1, getFirstIndex());
			RemoveLogAndCancelStart(lastIncludedLog.getIndex() + 1, getLastIndex());
			setLastIndex(lastIncludedLog.getIndex());
			setFirstIndex(lastIncludedLog.getIndex());
			setCommitIndex(getFirstIndex());
			setLastApplied(getFirstIndex());

			// 8. Reset state machine using snapshot contents (and load
			// snapshot's cluster configuration)
			getRaft().getStateMachine().LoadFromSnapshot(s.Name);
			logger.Debug("{0} EndReceiveInstallSnapshot Path={1}", getRaft().getName(), s.Name);
		}
	}


	public final void StartSnapshot() {
		StartSnapshot(false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void StartSnapshot(bool NeedNow = false)
	public final void StartSnapshot(boolean NeedNow) {
		synchronized (getRaft()) {
			if (getSnapshotting() || !getInstallSnapshotting().isEmpty()) {
				return;
			}

			if (getLastApplied() - getFirstIndex() < getRaft().getRaftConfig().getSnapshotMinLogCount() && false == NeedNow) {
				return;
			}

			setSnapshotting(true);
		}
		try {
			long LastIncludedIndex;
			long LastIncludedTerm;
			var path = Paths.get(getRaft().getRaftConfig().getDbHome()).resolve(SnapshotFileName).toString();

			// 忽略Snapshot返回结果。肯定是重复调用导致的。
			// out 结果这里没有使用，定义在参数里面用来表示这个很重要。
			tangible.OutObject<Long> tempOut_LastIncludedIndex = new tangible.OutObject<Long>();
			tangible.OutObject<Long> tempOut_LastIncludedTerm = new tangible.OutObject<Long>();
			getRaft().getStateMachine().Snapshot(path, tempOut_LastIncludedIndex, tempOut_LastIncludedTerm);
		LastIncludedTerm = tempOut_LastIncludedTerm.outArgValue;
		LastIncludedIndex = tempOut_LastIncludedIndex.outArgValue;
			logger.Debug("{0} Snapshot Path={1} LastIndex={2} LastTerm={3}", getRaft().getName(), path, LastIncludedIndex, LastIncludedTerm);
		}
		finally {
			synchronized (getRaft()) {
				setSnapshotting(false);
			}
		}
	}

	private void InstallSnapshot(String path, Server.ConnectorEx connector) {
		// 整个安装成功结束时设置。中间Break(return)不设置。
		// 后面 finally 里面使用这个标志
		boolean InstallSuccess = false;
		logger.Debug("{0} InstallSnapshot Start... Path={1} ToConnector={2}", getRaft().getName(), path, connector.getName());
		try {
			var snapshotFile = new FileInputStream(path);
			long offset = 0;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var buffer = new byte[32 * 1024];
			var buffer = new byte[32 * 1024];
			var FirstLog = ReadLog(getFirstIndex());
			var trunkArg = new InstallSnapshotArgument();
			trunkArg.setTerm(getTerm());
			trunkArg.setLeaderId(getRaft().getLeaderId());
			trunkArg.setLastIncludedIndex(FirstLog.getIndex());
			trunkArg.setLastIncludedTerm(FirstLog.getTerm());

			while (!trunkArg.getDone() && getRaft().isLeader()) {
				int rc = snapshotFile.Read(buffer);
				trunkArg.setOffset(offset);
				trunkArg.setData(new Binary(buffer, 0, rc));
				trunkArg.setDone(rc < buffer.length);
				offset += rc;

				if (trunkArg.getDone()) {
					trunkArg.setLastIncludedLog(new Binary(FirstLog.Encode()));
				}

				while (getRaft().isLeader()) {
					connector.getHandshakeDoneEvent().WaitOne();
					var future = new TaskCompletionSource<Integer>();
					var r = new InstallSnapshot();
					r.setArgument(trunkArg);
					if (!r.Send(connector.getSocket(), (_) -> {
						future.SetResult(0);
						return Procedure.Success;
					})) {
						continue;
					}
					future.Task.Wait();
					if (r.isTimeout()) {
						continue;
					}

					synchronized (getRaft()) {
						if (this.TrySetTerm(r.getResult().getTerm())) {
							// new term found.
							getRaft().ConvertStateTo(getRaft().RaftState.Follower);
							return;
						}
					}

					switch (r.getResultCode()) {
						case global:
							:Zeze.Raft.InstallSnapshot.ResultCodeNewOffset: break;

						default:
							logger.Warn(String.format("InstallSnapshot Break ResultCode=%1$s", r.getResultCode()));
							return;
					}

					if (r.getResult().getOffset() >= 0) {
						if (r.getResult().getOffset() > snapshotFile.Length) {
							logger.Error(String.format("InstallSnapshot.Result.Offset Too Big.%1$s/%2$s", r.getResult().getOffset(), snapshotFile.Length));
							return; // 中断安装。
						}
						offset = r.getResult().getOffset();
						snapshotFile.Seek(offset, SeekOrigin.Begin);
					}
					break;
				}
			}
			InstallSuccess = getRaft().isLeader();
			logger.Debug("{0} InstallSnapshot [SUCCESS] Path={1} ToConnector={2}", getRaft().getName(), path, connector.getName());
		}
		finally {
			synchronized (getRaft()) {
				connector.setInstallSnapshotting(false);
				getInstallSnapshotting().remove(connector.getName());
				if (InstallSuccess) {
					// 安装完成，重新初始化，使得以后的AppendEnties能继续工作。
					// = FirstIndex + 1，防止Index跳着分配，使用ReadLogStart。
					var next = ReadLogStart(getFirstIndex() + 1);
					connector.setNextIndex(next == null ? getFirstIndex() + 1 : next.getIndex());
				}
			}
		}
	}
	private void StartInstallSnapshot(Server.ConnectorEx connector) {
		if (connector.getInstallSnapshotting()) {
			return;
		}
		var path = Paths.get(getRaft().getRaftConfig().getDbHome()).resolve(SnapshotFileName).toString();
		// 如果 Snapshotting，此时不启动安装。
		// 以后重试 AppendEntries 时会重新尝试 Install.
		if ((new File(path)).isFile() && false == getSnapshotting()) {
			connector.setInstallSnapshotting(true);
			getInstallSnapshotting().put(connector.getName(), connector);
			Zeze.Util.Task.Run(() -> InstallSnapshot(path, connector), String.format("InstallSnapshot To '%1$s'", connector.getName()));
		}
		else {
			// 这一般的情况是snapshot文件被删除了。
			// 【注意】这种情况也许报错更好？
			// 内部会判断，不会启动多个snapshot。
			StartSnapshot(true);
		}
	}

	private int ProcessAppendEntriesResult(Server.ConnectorEx connector, Protocol p) {
		// 这个rpc处理流程总是返回 Success，需要统计观察不同的分支的发生情况，再来定义不同的返回值。

		if (false == getRaft().isLeader()) {
			return Procedure.Success; // maybe close.
		}

		var r = p instanceof AppendEntries ? (AppendEntries)p : null;
		if (r.isTimeout()) {
			TrySendAppendEntries(connector, r); //resend
			return Procedure.Success;
		}

		synchronized (getRaft()) {
			if (getRaft().getLogSequence().TrySetTerm(r.getResult().getTerm())) {
				getRaft().setLeaderId(""); // 此时不知道谁是Leader。
											  // new term found.
				getRaft().ConvertStateTo(getRaft().RaftState.Follower);
				// 发现新的 Term，已经不是Leader，不能继续处理了。
				// 直接返回。
				connector.setPending(null);
				return Procedure.Success;
			}

			if (getRaft().getState() != getRaft().RaftState.Leader) {
				connector.setPending(null);
				return Procedure.Success;
			}
		}

		if (r.getResult().getSuccess()) {
			synchronized (getRaft()) {
				TryCommit(r, connector);
			}
			// TryCommit 推进了NextIndex，
			// 可能日志没有复制完或者有新的AppendLog。
			// 尝试继续复制日志。
			// see TrySendAppendEntries 内的
			// "限制一次发送的日志数量”
			TrySendAppendEntries(connector, r);
			return Procedure.Success;
		}

		// 日志同步失败，调整NextIndex，再次尝试。
		synchronized (getRaft()) {
			// TODO raft.pdf 提到一个优化
			connector.setNextIndex(connector.getNextIndex() - 1);
			TrySendAppendEntries(connector, r); //resend. use new NextIndex。
			return Procedure.Success;
		}
	}

	private void TrySendAppendEntries(Server.ConnectorEx connector, AppendEntries pending) {
		synchronized (getRaft()) {
			// 按理说，多个Follower设置一次就够了，这里就不做这个处理了。
			setAppendLogActiveTime(Util.Time.getNowUnixMillis());

			if (connector.getPending() != pending) {
				return;
			}

			// 先清除，下面中断(return)不用每次自己清除。
			connector.setPending(null);
			if (false == connector.isHandshakeDone()) {
				// Hearbeat Will Retry
				return;
			}

			// 【注意】
			// 正在安装Snapshot，此时不复制日志，肯定失败。
			// 不做这个判断也是可以工作的，算是优化。
			if (connector.getInstallSnapshotting()) {
				return;
			}

			if (connector.getNextIndex() > getLastIndex()) {
				return;
			}

			var nextLog = ReadLogReverse(connector.getNextIndex());
			if (nextLog.getIndex() == getFirstIndex()) {
				// 已经到了日志开头，此时不会有prev-log，无法复制日志了。
				// 这一般发生在Leader进行了Snapshot，但是Follower的日志还更老。
				// 新起的Follower也一样。
				StartInstallSnapshot(connector);
				return;
			}

			// 现在Index总是递增，但没有确认步长总是为1，这样能处理不为1的情况。
			connector.setNextIndex(nextLog.getIndex());

			connector.setPending(new AppendEntries());
			connector.getPending().getArgument().setTerm(getTerm());
			connector.getPending().getArgument().setLeaderId(getRaft().getName());
			connector.getPending().getArgument().setLeaderCommit(getCommitIndex());

			// 肯定能找到的。
			var prevLog = ReadLogReverse(nextLog.getIndex() - 1);
			connector.getPending().getArgument().setPrevLogIndex(prevLog.getIndex());
			connector.getPending().getArgument().setPrevLogTerm(prevLog.getTerm());

			// 限制一次发送的日志数量，【注意】这个不是raft要求的。
			int maxCount = getRaft().getRaftConfig().getMaxAppendEntiresCount();
			RaftLog lastCopyLog = nextLog;
			for (var copyLog = nextLog; maxCount > 0 && null != copyLog && copyLog.getIndex() <= getLastIndex(); copyLog = ReadLogStart(copyLog.getIndex() + 1), --maxCount) {
				lastCopyLog = copyLog;
				connector.getPending().getArgument().getEntries().add(new Binary(copyLog.Encode()));
			}
			connector.getPending().getArgument().setLastEntryIndex(lastCopyLog.getIndex());
			if (false == connector.getPending().Send(connector.getSocket(), (p) -> ProcessAppendEntriesResult(connector, p), getRaft().getRaftConfig().getAppendEntriesTimeout())) {
				connector.setPending(null);
				// Hearbeat Will Retry
			}
		}
	}

	public final RaftLog LastRaftLog() {
		return ReadLog(getLastIndex());
	}

	private void RemoveLogAndCancelStart(long startIndex, long endIndex) {
		for (long index = startIndex; index <= endIndex; ++index) {
			TValue future;
			tangible.OutObject<TaskCompletionSource<Integer>> tempOut_future = new tangible.OutObject<TaskCompletionSource<Integer>>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (index > getLastApplied() && getWaitApplyFutures().TryRemove(index, tempOut_future)) {
			future = tempOut_future.outArgValue;
				// 还没有applied的日志被删除，
				// 当发生在重新选举，但是旧的leader上还有一些没有提交的请求时，
				// 需要取消。
				// 其中判断：index > LastApplied 不是必要的。
				// Apply的时候已经TryRemove了，仅会成功一次。
				future.SetCanceled();
			}
		else {
			future = tempOut_future.outArgValue;
		}

			var key = ByteBuffer.Allocate();
			key.WriteLong8(index);
			getLogs().Remove(key.getBytes(), key.getSize());
		}
	}

	public final int FollowerOnAppendEntries(AppendEntries r) {
		setLeaderActiveTime(Zeze.Util.Time.getNowUnixMillis());
		r.getResult().setTerm(getTerm());
		r.getResult().setSuccess(false); // set default false

		if (r.getArgument().getTerm() < getTerm()) {
			// 1. Reply false if term < currentTerm (§5.1)
			r.SendResult();
			logger.Info("this={0} Leader={1} Index={2} term < currentTerm", getRaft().getName(), r.getArgument().getLeaderId(), r.getArgument().getLastEntryIndex());
			return Procedure.Success;
		}

		var prevLog = ReadLog(r.getArgument().getPrevLogIndex());
		if (prevLog == null || prevLog.getTerm() != r.getArgument().getPrevLogTerm()) {
			// 2. Reply false if log doesn't contain an entry
			// at prevLogIndex whose term matches prevLogTerm(§5.3)
			r.SendResult();
			logger.Info("this={0} Leader={1} Index={2} prevLog mismatch", getRaft().getName(), r.getArgument().getLeaderId(), r.getArgument().getLastEntryIndex());
			return Procedure.Success;
		}

		for (var raftLogData : r.getArgument().getEntries()) {
			var copyLog = RaftLog.Decode(raftLogData, getRaft().getStateMachine().LogFactory);
			var conflictCheck = ReadLog(copyLog.getIndex());
			if (null != conflictCheck) {
				if (conflictCheck.getTerm() != copyLog.getTerm()) {
					// 3. If an existing entry conflicts
					// with a new one (same index but different terms),
					// delete the existing entry and all that follow it(§5.3)
					// raft.pdf 5.3
					RemoveLogAndCancelStart(conflictCheck.getIndex(), getLastIndex());
					setLastIndex(prevLog.getIndex());
				}
			}
			else {
				// 4. Append any new entries not already in the log
				SaveLog(copyLog);
			}
			// 复用这个变量。当冲突需要删除时，精确指到前一个日志。
			prevLog = copyLog;
		}
		// 5. If leaderCommit > commitIndex,
		// set commitIndex = min(leaderCommit, index of last new entry)
		if (r.getArgument().getLeaderCommit() > getCommitIndex()) {
			setCommitIndex(Math.min(r.getArgument().getLeaderCommit(), LastRaftLog().getIndex()));
			TryApply(ReadLog(getCommitIndex()));
		}
		r.getResult().setSuccess(true);
		logger.Debug("{0}: {1}", getRaft().getName(), r);
		r.SendResultCode(0);

		// 有Leader，清除一下上一次选举的投票。要不然可能下一次选举无法给别人投票。
		// 这个不是必要的：因为要进行选举的时候，自己肯定也会尝试选自己，会重置，
		// 但是清除一下，可以让选举更快进行。不用等待选举TimerTask。
		SetVoteFor("");
		return Procedure.Success;
	}
}
