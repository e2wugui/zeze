package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Task;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

/**
 * Raft Core
 */
public final class Raft {
	private static final Logger logger = LogManager.getLogger(Raft.class);
	private static final AtomicReference<ArrayList<Raft>> processExits = new AtomicReference<>();

	private String LeaderId;
	private final RaftConfig RaftConfig;
	private final LogSequence _LogSequence;
	private final Server Server;
	private final ExecutorService ImportantThreadPool;
	private final StateMachine StateMachine;
	public volatile boolean IsShutdown = false;
	private final Lock ReceiveSnapshottingLock = new ReentrantLock();
	private final HashMap<Long, RandomAccessFile> ReceiveSnapshotting = new HashMap<>();
	private volatile RaftState _State = RaftState.Follower;
	private Future<?> TimerTask;
	private long LowPrecisionTimer;
	private final Lock AtFatalKillsLock = new ReentrantLock();
	private final ArrayList<Action0> AtFatalKills = new ArrayList<>();

	// Candidate
	private final ConcurrentHashSet<RequestVote> RequestVotes = new ConcurrentHashSet<>();
	private long NextVoteTime; // 等待当前轮选举结果超时；用来启动下一次选举。

	// Leader
	private long LeaderWaitReadyTerm;
	private long LeaderWaitReadyIndex;
	private volatile TaskCompletionSource<Boolean> LeaderReadyFuture = new TaskCompletionSource<>();

	// Follower
	private long LeaderLostTimeout;

	private final Lock mutex = new ReentrantLock();
	private final Condition condition = mutex.newCondition();

	public String getName() {
		return RaftConfig.getName();
	}

	public String getLeaderId() {
		return LeaderId;
	}

	void setLeaderId(String value) {
		LeaderId = value;
	}

	public RaftConfig getRaftConfig() {
		return RaftConfig;
	}

	public void lock() {
		mutex.lock();
	}

	public void unlock() {
		mutex.unlock();
	}

	public boolean tryLock() {
		return mutex.tryLock();
	}

	public void await() {
		try {
			condition.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void await(long time) {
		try {
			condition.await(time, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void signal() {
		condition.signal();
	}

	public void signalAll() {
		condition.signalAll();
	}

	public LogSequence getLogSequence() {
		return _LogSequence;
	}

	public boolean isLeader() {
		return _State == RaftState.Leader;
	}

	public Server getServer() {
		return Server;
	}

	// 不能加锁
	public boolean isWorkingLeader() {
		return isLeader() && !IsShutdown;
	}

	public ExecutorService getImportantThreadPool() {
		return ImportantThreadPool;
	}

	public StateMachine getStateMachine() {
		return StateMachine;
	}

	public void addAtFatalKill(Action0 action) {
		AtFatalKillsLock.lock();
		try {
			AtFatalKills.add(action);
		} finally {
			AtFatalKillsLock.unlock();
		}
	}

	public void FatalKill() {
		IsShutdown = true;
		AtFatalKillsLock.lock();
		try {
			for (Action0 action : AtFatalKills) {
				try {
					action.run();
				} catch (Throwable e) {
					logger.error("FatalKill", e);
				}
			}
		} finally {
			AtFatalKillsLock.unlock();
		}
		_LogSequence.Close();
		LogManager.shutdown();
		System.exit(-1);
	}

	public void AppendLog(Log log) {
		AppendLog(log, null);
	}

	public void AppendLog(Log log, Bean result) {
		if (result != null)
			log.setRpcResult(new Binary(ByteBuffer.Encode(result)));
		try {
			_LogSequence.AppendLog(log, true);
		} catch (RaftRetryException | TaskCanceledException er) {
			throw er;
		} catch (Throwable ex) {
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	private void CancelAllReceiveSnapshotting() {
		ReceiveSnapshottingLock.lock();
		try {
			ReceiveSnapshotting.values().forEach(file -> {
				try {
					file.close();
				} catch (IOException e) {
					logger.warn("CancelAllReceiveSnapshotting close Exception", e); // 文件关闭异常还是不向上抛了
				}
			});
			ReceiveSnapshotting.clear();
		} finally {
			ReceiveSnapshottingLock.unlock();
		}
	}

	public void Shutdown() throws Throwable {
		lock();
		try {
			// shutdown 只做一次。
			if (IsShutdown)
				return;

			ArrayList<Raft> exits = processExits.get();
			if (exits != null) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (exits) {
					exits.remove(this);
				}
			}
			IsShutdown = true;
		} finally {
			unlock();
		}
		Server.Stop();

		var removeLogBeforeFuture = _LogSequence.RemoveLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		var applyFuture = _LogSequence.ApplyFuture;
		if (applyFuture != null)
			applyFuture.await();

		lock();
		try {
			_LogSequence.CancelAllInstallSnapshot();
			CancelAllReceiveSnapshotting();

			if (TimerTask != null) {
				TimerTask.cancel(false);
				TimerTask = null;
			}
			ConvertStateTo(RaftState.Follower);
			_LogSequence.Close();
		} finally {
			unlock();
		}
		ImportantThreadPool.shutdown(); // 需要停止线程。
	}

	public Raft(StateMachine sm) throws Throwable {
		this(sm, null, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName) throws Throwable {
		this(sm, RaftName, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf) throws Throwable {
		this(sm, RaftName, raftConf, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf, Zeze.Config config) throws Throwable {
		this(sm, RaftName, raftConf, config, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf, Zeze.Config config, String name)
			throws Throwable {
		if (raftConf == null)
			raftConf = Zeze.Raft.RaftConfig.Load();
		raftConf.Verify();

		RaftConfig = raftConf;
		sm.setRaft(this);
		StateMachine = sm;

		if (RaftName != null && !RaftName.isEmpty()) {
			// 如果 DbHome 和 Name 相关，一般表示没有特别配置。
			// 此处特别设置 Raft.Name 时，需要一起更新。
			if (raftConf.getDbHome().equals(raftConf.getName().replace(':', '_')))
				raftConf.setDbHome(RaftName.replace(':', '_'));
			raftConf.setName(RaftName);
		}

		if (config == null)
			config = Zeze.Config.Load();

		Server = new Server(this, name, config);
		if (Server.getConfig().AcceptorCount() != 0)
			throw new IllegalStateException("Acceptor Found!");
		if (Server.getConfig().ConnectorCount() != 0)
			throw new IllegalStateException("Connector Found!");
		if (RaftConfig.getNodes().size() < 3)
			throw new IllegalStateException("Startup Nodes.Count Must >= 3.");

		ImportantThreadPool = Task.newFixedThreadPool(5, "Raft");
		Zeze.Raft.Server.CreateAcceptor(Server, raftConf);
		Zeze.Raft.Server.CreateConnector(Server, raftConf);

		Files.createDirectories(Paths.get(RaftConfig.getDbHome()));

		_LogSequence = new LogSequence(this);

		RegisterInternalRpc();

		var snapshot = _LogSequence.getSnapshotFullName();
		if ((new File(snapshot)).isFile())
			sm.LoadSnapshot(snapshot);

		ArrayList<Raft> exits = processExits.get();
		if (exits == null) {
			if (processExits.compareAndSet(null, exits = new ArrayList<>())) {
				Runtime.getRuntime().addShutdownHook(new Thread("RaftShutdown") {
					@Override
					public void run() {
						ArrayList<Raft> exits = processExits.get();
						//noinspection SynchronizationOnLocalVariableOrMethodParameter
						// TODO LOCK
						synchronized (exits) {
							for (Raft raft : exits.toArray(new Raft[exits.size()])) {
								try {
									raft.Shutdown();
								} catch (Throwable e) {
									logger.error("ShutdownHook", e);
								}
							}
						}
						LogManager.shutdown();
					}
				});
			} else
				exits = processExits.get();
		}
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (exits) {
			exits.add(this);
		}

		TimerTask = Task.schedule(10, this::OnTimer);
	}

	private long ProcessAppendEntries(AppendEntries r) throws Throwable {
		lock();
		try {
			return _LogSequence.FollowerOnAppendEntries(r);
		} finally {
			unlock();
		}
	}

	private long ProcessInstallSnapshot(InstallSnapshot r) throws Throwable {
		lock();
		try {
			r.Result.setTerm(_LogSequence.getTerm());
			if (r.Argument.getTerm() < _LogSequence.getTerm()) {
				// 1. Reply immediately if term < currentTerm
				r.SendResultCode(InstallSnapshot.ResultCodeTermError);
				return 0;
			}

			if (_LogSequence.TrySetTerm(r.Argument.getTerm()) == LogSequence.SetTermResult.Newer) {
				r.Result.setTerm(_LogSequence.getTerm());
				// new term found.
				ConvertStateTo(RaftState.Follower);
			}
			LeaderId = r.Argument.getLeaderId();
			_LogSequence.setLeaderActiveTime(System.currentTimeMillis());
		} finally {
			unlock();
		}

		// 2. Create new snapshot file if first chunk(offset is 0)
		// 把 LastIncludedIndex 放到文件名中，
		// 新的InstallSnapshot不覆盖原来进行中或中断的。
		String path = Paths.get(RaftConfig.getDbHome(),
				LogSequence.SnapshotFileName + ".installing." + r.Argument.getLastIncludedIndex()).toString();

		ReceiveSnapshottingLock.lock();
		try {
			RandomAccessFile outputFileStream = ReceiveSnapshotting.get(r.Argument.getLastIncludedIndex());
			if (outputFileStream == null) {
				if (r.Argument.getOffset() != 0) {
					// 肯定是旧的被丢弃的安装，Discard And Ignore。
					r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
					return Procedure.Success;
				}
				ReceiveSnapshotting.put(r.Argument.getLastIncludedIndex(),
						outputFileStream = new RandomAccessFile(path, "rw"));
			}
			if (r.Argument.getOffset() == 0)
				outputFileStream.seek(0);

			r.Result.setOffset(-1); // 默认让Leader继续传输，不用重新定位。
			long fileLength = outputFileStream.length();
			if (r.Argument.getOffset() > fileLength) {
				// 数据块超出当前已经接收到的数据。
				// 填写当前长度，让Leader从该位置开始重新传输。
				r.Result.setOffset(fileLength);
				r.SendResultCode(InstallSnapshot.ResultCodeNewOffset);
				return Procedure.Success;
			}

			if (r.Argument.getOffset() == fileLength) {
				// 正常的Append流程，直接写入。
				// 3. Write data into snapshot file at given offset
				r.Argument.getData().writeToFile(outputFileStream);
			} else {
				// 数据块开始位置小于当前长度。
				var newEndPosition = r.Argument.getOffset() + r.Argument.getData().size();
				if (newEndPosition > fileLength) {
					// 有新的数据需要写入文件。
					outputFileStream.seek(r.Argument.getOffset());
					r.Argument.getData().writeToFile(outputFileStream);
				}
				r.Result.setOffset(outputFileStream.length());
			}

			// 4. Reply and wait for more data chunks if done is false
			if (r.Argument.getDone()) {
				// 5. Save snapshot file, discard any existing or partial snapshot with a smaller index
				ReceiveSnapshotting.remove(r.Argument.getLastIncludedIndex());
				try {
					outputFileStream.close();
				} catch (IOException e) {
					logger.warn("ProcessInstallSnapshot close(1) Exception", e); // 文件关闭异常还是不向上抛了
				}
				for (var it = ReceiveSnapshotting.entrySet().iterator(); it.hasNext(); ) {
					var e = it.next();
					if (e.getKey() < r.Argument.getLastIncludedIndex()) {
						it.remove();
						try {
							e.getValue().close();
						} catch (IOException ex) {
							logger.warn("ProcessInstallSnapshot close(2) Exception", ex); // 文件关闭异常还是不向上抛了
						}
						var pathDelete = Paths.get(RaftConfig.getDbHome(),
								LogSequence.SnapshotFileName + ".installing." + e.getKey()).toString();
						//noinspection ResultOfMethodCallIgnored
						new File(pathDelete).delete();
					}
				}
			}
		} finally {
			ReceiveSnapshottingLock.unlock();
		}
		if (r.Argument.getDone()) {
			// 剩下的处理流程在下面的函数里面。
			_LogSequence.EndReceiveInstallSnapshot(path, r);
		}
		r.SendResultCode(0);
		return Procedure.Success;
	}

	public enum RaftState {
		Follower,
		Candidate,
		Leader
	}

	public RaftState getState() {
		return _State;
	}

	// 重置 OnTimer 需要的所有时间。
	private void ResetTimerTime() throws Throwable {
		var now = System.currentTimeMillis();
		_LogSequence.setLeaderActiveTime(now);
		Server.getConfig().ForEachConnector(c -> ((Server.ConnectorEx)c).setAppendLogActiveTime(now));
	}

	/**
	 * 每个Raft使用一个固定Timer，根据不同的状态执行相应操作。
	 * 【简化】不同状态下不管维护管理不同的Timer了。
	 */
	private void OnTimer() throws Throwable {
		lock();
		try {
			if (IsShutdown)
				return;
			long now = System.currentTimeMillis();
			switch (getState()) {
			case Follower:
				if (now - _LogSequence.getLeaderActiveTime() > LeaderLostTimeout)
					ConvertStateTo(RaftState.Candidate);
				break;
			case Candidate:
				if (now > NextVoteTime)
					ConvertStateTo(RaftState.Candidate); // vote timeout. restart
				break;
			case Leader:
				Server.getConfig().ForEachConnector(c -> {
					var cex = (Server.ConnectorEx)c;
					if (now - cex.getAppendLogActiveTime() > RaftConfig.getLeaderHeartbeatTimer()) {
						_LogSequence.SendHeartbeatTo(cex);
					}
				});
				break;
			}
			if (++LowPrecisionTimer > 1000) { // 10s
				LowPrecisionTimer = 0;
				OnLowPrecisionTimer();
			}
		} finally {
			unlock();
			TimerTask = Task.schedule(10, this::OnTimer);
		}
	}

	private void OnLowPrecisionTimer() throws Throwable {
		Server.getConfig().ForEachConnector(Connector::Start); // Connector Reconnect Bug?
		_LogSequence.RemoveExpiredUniqueRequestSet();
	}

	/**
	 * true，IsLeader && LeaderReady;
	 * false, !IsLeader
	 */
	boolean WaitLeaderReady() throws Exception {
		lock();
		try {
			var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
			while (isLeader()) {
				if (volatileTmp.isDone())
					return volatileTmp.get();
				await();
			}
		} finally {
			unlock();
		}
		return false;
	}

	public boolean isReadyLeader() throws Exception {
		lock();
		try {
			var volatileTmp = LeaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
			return isLeader() && volatileTmp.isDone() && volatileTmp.get();
		} finally {
			unlock();
		}
	}

	void ResetLeaderReadyAfterChangeState() {
		LeaderReadyFuture.SetResult(false);
		LeaderReadyFuture = new TaskCompletionSource<>(); // prepare for next leader
		signalAll(); // has under lock(this)
	}

	void SetLeaderReady(RaftLog heart) throws Throwable {
		if (isLeader()) {
			// 是否过期First-Heartbeat。
			// 使用 LeaderReadyFuture 可以更加精确的识别。
			// 但是，由于RaftLog不是常驻内存的，保存不了进程级别的变量。
			if (heart.getTerm() != LeaderWaitReadyTerm || heart.getIndex() != LeaderWaitReadyIndex)
				return;

			LeaderWaitReadyIndex = 0;
			LeaderWaitReadyTerm = 0;

			logger.info("{} {} LastIndex={} Count={}", getName(), RaftConfig.getDbHome(),
					_LogSequence.getLastIndex(), _LogSequence.GetTestStateMachineCount());

			LeaderReadyFuture.SetResult(true);
			signalAll(); // has under lock(this)

			Server.Foreach(allSocket -> {
				// 本来这个通告发给Agent(client)即可，
				// 但是现在没有区分是来自Raft的连接还是来自Agent，
				// 全部发送。
				// 另外Raft之间有两个连接，会收到多次，Raft不处理这个通告。
				// 由于Raft数量不多，不会造成大的浪费，不做处理了。
				if (allSocket.isHandshakeDone()) {
					var r = new LeaderIs();
					r.Argument.setTerm(_LogSequence.getTerm());
					r.Argument.setLeaderId(LeaderId);
					r.Argument.setLeader(isLeader());
					r.Send(allSocket); // skip response.
				}
			});
		}
	}

	private boolean IsLastLogUpToDate(RequestVoteArgument candidate) throws RocksDBException {
		// NodeReady local candidate
		//           false false       IsLastLogUpToDate
		//           false true        false
		//           true  false       false
		//           true  true        IsLastLogUpToDate
		var last = _LogSequence.LastRaftLogTermIndex();
		if (!_LogSequence.getNodeReady()) {
			if (!candidate.getNodeReady()) {
				// 整个Raft集群第一次启动时，允许给初始节点投票。此时所有的初始节点形成多数派。任何一个当选都是可以的。
				// 以后由于机器更换再次启动而处于初始状态的节点肯定是少数派，即使它们之间互相投票，也不能成功。
				// 如果违背了这点，意味着违背了Raft的可用原则，已经不在Raft的处理范围内了。
				return IsLastLogUpToDate(last, candidate);
			}

			// 拒绝投票直到发现达成多数派。
			return false;
		}
		return candidate.getNodeReady() && IsLastLogUpToDate(last, candidate);
	}

	private boolean IsLastLogUpToDate(RaftLog last, RequestVoteArgument candidate) {
		if (candidate.getLastLogTerm() > last.getTerm())
			return true;
		if (candidate.getLastLogTerm() < last.getTerm())
			return false;
		return candidate.getLastLogIndex() >= last.getIndex();
	}

	@SuppressWarnings("SameReturnValue")
	private long ProcessRequestVote(RequestVote r) throws Throwable {
		lock();
		try {
			// 不管任何状态重置下一次时间，使得每个node从大概一个时刻开始。
			NextVoteTime = System.currentTimeMillis() + RaftConfig.getElectionTimeout();

			if (_LogSequence.TrySetTerm(r.Argument.getTerm()) == LogSequence.SetTermResult.Newer)
				ConvertStateTo(RaftState.Follower); // new term found.
			// else continue process

			// RequestVote RPC
			// Receiver implementation:
			// 1.Reply false if term < currentTerm(§5.1)
			// 2.If votedFor is null or candidateId, and candidate's log is at
			// least as up - to - date as receiver's log, grant vote(§5.2, §5.4)

			r.Result.setTerm(_LogSequence.getTerm());
			r.Result.setVoteGranted(r.Argument.getTerm() == _LogSequence.getTerm() &&
					_LogSequence.CanVoteFor(r.Argument.getCandidateId()) && IsLastLogUpToDate(r.Argument));

			if (r.Result.getVoteGranted())
				_LogSequence.SetVoteFor(r.Argument.getCandidateId());
			logger.info("{}: VoteFor={} Rpc={}", getName(), _LogSequence.getVoteFor(), r);
			r.SendResultCode(0);

			return Procedure.Success;
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private long ProcessLeaderIs(LeaderIs r) {
		// 这个协议是发送给Agent(Client)的，
		// 为了简单，不做区分。
		// Raft也会收到，忽略。
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private long ProcessRequestVoteResult(RequestVote rpc, @SuppressWarnings("unused") Connector c) throws Throwable {
		if (rpc.isTimeout() || rpc.getResultCode() != 0)
			return 0; // skip error. re-vote later.

		lock();
		try {
			if (_LogSequence.getTerm() != rpc.Argument.getTerm() || getState() != RaftState.Candidate) {
				// 结果回来时，上下文已经发生变化，忽略这个结果。
				logger.info("{} NotOwner={} NotCandidate={}", getName(),
						_LogSequence.getTerm() != rpc.Argument.getTerm(), getState() != RaftState.Candidate);
				return 0;
			}

			if (_LogSequence.TrySetTerm(rpc.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				// new term found
				ConvertStateTo(RaftState.Follower);
				return Procedure.Success;
			}

			if (RequestVotes.contains(rpc) && rpc.Result.getVoteGranted()) {
				int granteds = 0;
				for (var vote : RequestVotes) {
					if (vote.Result.getVoteGranted())
						++granteds;
				}

				if (getState() == RaftState.Candidate // 确保当前状态是选举中。没有判断这个，后面 ConvertStateTo 也会忽略不正确的状态转换。
						&& granteds >= RaftConfig.getHalfCount() // 加上自己就是多数派了。
						&& _LogSequence.CanVoteFor(getName())) {
					_LogSequence.SetVoteFor(getName());
					ConvertStateTo(RaftState.Leader);
				}
			}
		} finally {
			unlock();
		}
		return Procedure.Success;
	}

	private void SendRequestVote() throws Throwable {
		RequestVotes.clear(); // 每次选举开始清除。
		// LogSequence.SetVoteFor(Name); // 先收集结果，达到 RaftConfig.HalfCount 才判断是否给自己投票。
		_LogSequence.TrySetTerm(_LogSequence.getTerm() + 1);

		var arg = new RequestVoteArgument();
		arg.setTerm(_LogSequence.getTerm());
		arg.setCandidateId(getName());
		var log = _LogSequence.LastRaftLogTermIndex();
		arg.setLastLogIndex(log.getIndex());
		arg.setLastLogTerm(log.getTerm());
		arg.setNodeReady(_LogSequence.getNodeReady());

		NextVoteTime = System.currentTimeMillis() + RaftConfig.getElectionTimeout();
		Server.getConfig().ForEachConnector(c -> {
			var rpc = new RequestVote();
			rpc.Argument = arg;
			RequestVotes.add(rpc);
			var sendResult = rpc.Send(c.TryGetReadySocket(),
					p -> ProcessRequestVoteResult(rpc, c), RaftConfig.getAppendEntriesTimeout() - 100);
			logger.info("{}:{}: SendRequestVote {}", getName(), sendResult, rpc);
		});
	}

	private void ConvertStateFromFollowerTo(RaftState newState) throws Throwable {
		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Follower->Follower", getName());
			LeaderLostTimeout = RaftConfig.getElectionTimeout();
			return;
		case Candidate:
			logger.info("RaftState {}: Follower->Candidate", getName());
			_State = RaftState.Candidate;
			SendRequestVote();
			return;
		case Leader:
			// 并发的RequestVote的结果如果没有判断当前状态，可能会到达这里。
			// 不是什么大问题。see ProcessRequestVoteResult
			logger.info("RaftState {} Impossible! Follower->Leader", getName());
		}
	}

	private void ConvertStateFromCandidateTo(RaftState newState) throws Throwable {
		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Candidate->Follower", getName());
			LeaderLostTimeout = RaftConfig.getElectionTimeout();
			_State = RaftState.Follower;
			RequestVotes.clear();
			return;
		case Candidate:
			logger.info("RaftState {}: Candidate->Candidate", getName());
			SendRequestVote();
			return;
		case Leader:
			RequestVotes.clear();
			CancelAllReceiveSnapshotting();

			logger.info("RaftState {}: Candidate->Leader", getName());
			_State = RaftState.Leader;
			LeaderId = getName(); // set to self

			// (Reinitialized after election)
			var nextIndex = _LogSequence.getLastIndex() + 1;

			Server.getConfig().ForEachConnector(c -> {
				var cex = (Server.ConnectorEx)c;
				cex.Start(); // 马上尝试连接。
				cex.setNextIndex(nextIndex);
				cex.setMatchIndex(0);
			});

			// Upon election:
			// send initial empty AppendEntries RPCs
			// (heartbeat)to each server; repeat during
			// idle periods to prevent election timeouts(§5.2)
			LogSequence.AppendLogResult result = new LogSequence.AppendLogResult();
			_LogSequence.AppendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent, getName()), false, result);
			LeaderWaitReadyIndex = result.index;
			LeaderWaitReadyTerm = result.term;
		}
	}

	private void ConvertStateFromLeaderTo(RaftState newState) throws Throwable {
		// 本来 Leader -> Follower 需要，为了健壮性，全部改变都重置。
		ResetLeaderReadyAfterChangeState();
		_LogSequence.CancelAllInstallSnapshot();
		_LogSequence.CancelPendingAppendLogFutures();

		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Leader->Follower", getName());
			_State = RaftState.Follower;
			LeaderLostTimeout = RaftConfig.getElectionTimeout();
			return;
		case Candidate:
			logger.error("RaftState {} Impossible! Leader->Candidate", getName());
			return;
		case Leader:
			logger.error("RaftState {} Impossible! Leader->Leader", getName());
		}
	}

	public void ConvertStateTo(RaftState newState) throws Throwable {
		ResetTimerTime();
		// 按真值表处理所有情况。
		switch (getState()) {
		case Follower:
			ConvertStateFromFollowerTo(newState);
			return;
		case Candidate:
			ConvertStateFromCandidateTo(newState);
			return;
		case Leader:
			ConvertStateFromLeaderTo(newState);
		}
	}

	private void RegisterInternalRpc() {
		Server.AddFactoryHandle(RequestVote.TypeId_,
				new Service.ProtocolFactoryHandle<>(RequestVote::new, this::ProcessRequestVote));
		Server.AddFactoryHandle(AppendEntries.TypeId_,
				new Service.ProtocolFactoryHandle<>(AppendEntries::new, this::ProcessAppendEntries));
		Server.AddFactoryHandle(InstallSnapshot.TypeId_,
				new Service.ProtocolFactoryHandle<>(InstallSnapshot::new, this::ProcessInstallSnapshot));
		Server.AddFactoryHandle(LeaderIs.TypeId_,
				new Service.ProtocolFactoryHandle<>(LeaderIs::new, this::ProcessLeaderIs));
	}
}
