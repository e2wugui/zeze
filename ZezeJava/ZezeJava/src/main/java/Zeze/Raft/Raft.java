package Zeze.Raft;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Action0;
import Zeze.Util.Action2;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.Func3;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskOneByOneByKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;

/**
 * Raft Core
 */
public final class Raft {
	private static final Logger logger = LogManager.getLogger(Raft.class);
	// private static final AtomicLong threadPoolCounter = new AtomicLong();

	private String leaderId;
	private final RaftConfig raftConfig;
	private final LogSequence logSequence;
	private final Server server;
	private final TaskOneByOneByKey taskOneByOne;
	private final String userTaskOneByOneKey;

	private final StateMachine stateMachine;
	public volatile boolean isShutdown = false;
	private final Lock receiveSnapshottingLock = new ReentrantLock();
	private final HashMap<Long, RandomAccessFile> receiveSnapshotting = new HashMap<>();
	private volatile RaftState state = RaftState.Follower;
	private Future<?> timerTask;
	private long lowPrecisionTimer;
	private final Lock atFatalKillsLock = new ReentrantLock();
	private final ArrayList<Action0> atFatalKills = new ArrayList<>();

	// Candidate
	private final ConcurrentHashSet<RequestVote> requestVotes = new ConcurrentHashSet<>();
	private long nextVoteTime; // 等待当前轮选举结果超时；用来启动下一次选举。

	// Leader
	private long leaderWaitReadyTerm;
	private long leaderWaitReadyIndex;
	private volatile TaskCompletionSource<Boolean> leaderReadyFuture = new TaskCompletionSource<>();

	// Follower
	private long leaderLostTimeout;

	private final Lock mutex = new ReentrantLock();
	private final Condition condition = mutex.newCondition();

	private Action0 onLeaderReady; // isLeader & isReady
	Action0 onFollowerReceiveKeepAlive;

	public void setOnLeaderReady(Action0 action) {
		onLeaderReady = action;
	}

	public void setOnFollowerReceiveKeepAlive(Action0 action) {
		onFollowerReceiveKeepAlive = action;
	}

	public String getName() {
		return raftConfig.getName();
	}

	public String getLeaderId() {
		return leaderId;
	}

	void setLeaderId(String value) {
		leaderId = value;
	}

	public RaftConfig getRaftConfig() {
		return raftConfig;
	}

//	private long lockTime = System.currentTimeMillis();
//	private long unlockTime = System.currentTimeMillis();

	public void lock() {
//		var lockBefore = System.currentTimeMillis();
		mutex.lock();
//		lockTime = System.currentTimeMillis();
//		if (lockTime - lockBefore > 500) {
//			logger.warn("--- wait lock too long: {}, noLockTime: {}", lockTime - lockBefore, lockTime - unlockTime, new Exception());
//		}
	}

	public void unlock() {
//		unlockTime = System.currentTimeMillis();
//		var t = unlockTime - lockTime;
//		if (t > 500) {
//			logger.warn("--- lock time too long: {}", t, new Exception());
//		}
		mutex.unlock();
	}

	public boolean tryLock() {
		return mutex.tryLock();
	}

	public void await() {
		try {
//			var t = System.currentTimeMillis();
//			if (t - lockTime > 500) {
//				logger.warn("--- lock time too long: {}", t - lockTime, new Exception());
//			}
			condition.await();
		} catch (InterruptedException e) {
			Task.forceThrow(e);
		}
	}

	public boolean await(long time) {
		try {
			return condition.await(time, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Task.forceThrow(e);
			return false; // never run here
		}
	}

	public void signal() {
		condition.signal();
	}

	public void signalAll() {
		condition.signalAll();
	}

	public LogSequence getLogSequence() {
		return logSequence;
	}

	public boolean isLeader() {
		return state == RaftState.Leader;
	}

	public Server getServer() {
		return server;
	}

	// 不能加锁
	public boolean isWorkingLeader() {
		return isLeader() && !isShutdown;
	}

	public static void executeImportantTask(@NotNull Runnable task) {
		Task.getCriticalThreadPool().execute(task);
	}

	public void executeUserTask(@NotNull Action0 task) {
		taskOneByOne.Execute(userTaskOneByOneKey, task);
	}

	public StateMachine getStateMachine() {
		return stateMachine;
	}

	public void addAtFatalKill(Action0 action) {
		atFatalKillsLock.lock(); // atFatalKill 不中断
		try {
			atFatalKills.add(action);
		} finally {
			atFatalKillsLock.unlock();
		}
	}

	public void fatalKill() {
		isShutdown = true;
		atFatalKillsLock.lock();
		try {
			for (Action0 action : atFatalKills) {
				try {
					action.run();
				} catch (Throwable e) { // kill self. 必须捕捉所有异常。logger.error
					logger.error("FatalKill", e);
				}
			}
		} finally {
			atFatalKillsLock.unlock();
		}
		try {
			logSequence.close();
		} catch (Exception e) {
			logger.error("", e);
		}
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	public void appendLog(Log log, Action2<RaftLog, Boolean> callback) {
		appendLog(log, null, callback);
	}

	public void appendLog(Log log, Serializable result, Action2<RaftLog, Boolean> callback) {
		if (result != null)
			log.setRpcResult(new Binary(ByteBuffer.encode(result)));
		try {
			logSequence.appendLog(log, callback);
		} catch (RaftRetryException | TaskCanceledException er) {
			throw er;
		} catch (Throwable ex) { // rethrow RaftRetryException
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	public void appendLog(Log log) {
		appendLog(log, (Serializable)null);
	}

	public void appendLog(Log log, Serializable result) {
		if (result != null)
			log.setRpcResult(new Binary(ByteBuffer.encode(result)));
		try {
			logSequence.appendLog(log);
		} catch (RaftRetryException | TaskCanceledException er) {
			throw er;
		} catch (Throwable ex) { // rethrow RaftRetryException
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	private void cancelAllReceiveSnapshotting() {
		receiveSnapshottingLock.lock(); // cancel 不中断
		try {
			receiveSnapshotting.values().forEach(file -> {
				try {
					file.close();
				} catch (IOException e) {
					logger.warn("CancelAllReceiveSnapshotting close Exception", e); // 文件关闭异常还是不向上抛了
				}
			});
			receiveSnapshotting.clear();
		} finally {
			receiveSnapshottingLock.unlock();
		}
	}

	public void shutdown() throws Exception {
		lock();
		try {
			// shutdown 只做一次。
			if (isShutdown)
				return;
			isShutdown = true;
		} finally {
			unlock();
		}
		ShutdownHook.remove(this);
		server.stop();

		var removeLogBeforeFuture = logSequence.removeLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		var applyFuture = logSequence.applyFuture;
		if (applyFuture != null)
			applyFuture.await();

		if (timerTask != null) {
			timerTask.cancel(false);
			timerTask = null;
		}

		lock();
		try {
			logSequence.cancelAllInstallSnapshot();
			cancelAllReceiveSnapshotting();

			convertStateTo(RaftState.Follower);
			logSequence.close();
		} finally {
			unlock();
		}
	}

	public Raft(StateMachine sm) throws Exception {
		this(sm, null, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName) throws Exception {
		this(sm, RaftName, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf) throws Exception {
		this(sm, RaftName, raftConf, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf, Config config) throws Exception {
		this(sm, RaftName, raftConf, config, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf, Config config, String name)
			throws Exception {
		this(sm, RaftName, raftConf, config, name, Server::new, new TaskOneByOneByKey());
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftConf, Config config, String name,
				Func3<Raft, String, Config, Server> serverFactory, @NotNull TaskOneByOneByKey taskOneByOne) throws Exception {

		if (raftConf == null)
			raftConf = RaftConfig.load();
		raftConf.verify();

		this.taskOneByOne = taskOneByOne;
		raftConfig = raftConf;
		userTaskOneByOneKey = "Zeze.Raft.UserTaskOneByOneKey." + raftConfig.getName();
		sm.setRaft(this);
		stateMachine = sm;

		if (RaftName != null && !RaftName.isEmpty()) {
			// 如果 DbHome 和 Name 相关，一般表示没有特别配置。
			// 此处特别设置 Raft.Name 时，需要一起更新。
			if (raftConf.getDbHome().equals(raftConf.getName().replace(':', '_')))
				raftConf.setDbHome(RaftName.replace(':', '_'));
			raftConf.setName(RaftName);
		}

		if (config == null)
			config = Config.load();
		server = serverFactory.call(this, name, config);
		if (server.getConfig().acceptorCount() != 0)
			throw new IllegalStateException("Acceptor Found!");
		if (server.getConfig().connectorCount() != 0)
			throw new IllegalStateException("Connector Found!");
		if (raftConfig.getNodes().size() < 3)
			throw new IllegalStateException("Startup Nodes.Count Must >= 3.");

		Server.createAcceptor(server, raftConf);
		Server.createConnector(server, raftConf);

		Files.createDirectories(Paths.get(raftConfig.getDbHome()));

		logSequence = new LogSequence(this);

		registerInternalRpc();

		var snapshot = logSequence.getSnapshotFullName();
		if (new File(snapshot).isFile()) {
			long t = System.nanoTime();
			sm.loadSnapshot(snapshot);
			logger.info("Raft {} LoadSnapshot time={}ms", getName(), (System.nanoTime() - t) / 1_000_000);
		} else {
			sm.reset();
			logger.info("Raft {} reset state machine.", getName());
		}

		ShutdownHook.add(this, () -> {
			logger.info("Raft {} ShutdownHook begin", getName());
			shutdown();
			logger.info("Raft {} ShutdownHook end", getName());
		});

		timerTask = Task.scheduleUnsafe(20, 20, this::onTimer);
	}

	private long processAppendEntries(AppendEntries r) throws Exception {
		lock();
		try {
			return logSequence.followerOnAppendEntries(r);
		} finally {
			unlock();
		}
	}

	private long processInstallSnapshot(InstallSnapshot r) throws Exception {
		lock();
		try {
			r.Result.setTerm(logSequence.getTerm());
			if (r.Argument.getTerm() < logSequence.getTerm()) {
				// 1. Reply immediately if term < currentTerm
				r.SendResultCode(InstallSnapshot.ResultCodeTermError);
				return 0;
			}

			if (logSequence.trySetTerm(r.Argument.getTerm()) == LogSequence.SetTermResult.Newer) {
				r.Result.setTerm(logSequence.getTerm());
				// new term found.
				convertStateTo(RaftState.Follower);
			}
			leaderId = r.Argument.getLeaderId();
			logSequence.setLeaderActiveTime(System.currentTimeMillis());
		} finally {
			unlock();
		}

		// 2. Create new snapshot file if first chunk(offset is 0)
		// 把 LastIncludedIndex 放到文件名中，
		// 新的InstallSnapshot不覆盖原来进行中或中断的。
		String path = Paths.get(raftConfig.getDbHome(),
				LogSequence.snapshotFileName + ".installing." + r.Argument.getLastIncludedIndex()).toString();

		receiveSnapshottingLock.lock();
		try {
			RandomAccessFile outputFileStream = receiveSnapshotting.get(r.Argument.getLastIncludedIndex());
			if (outputFileStream == null) {
				if (r.Argument.getOffset() != 0) {
					// 肯定是旧的被丢弃的安装，Discard And Ignore。
					r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
					return Procedure.Success;
				}
				receiveSnapshotting.put(r.Argument.getLastIncludedIndex(),
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
				receiveSnapshotting.remove(r.Argument.getLastIncludedIndex());
				try {
					outputFileStream.close();
				} catch (IOException e) {
					logger.warn("ProcessInstallSnapshot close(1) Exception", e); // 文件关闭异常还是不向上抛了
				}
				for (var it = receiveSnapshotting.entrySet().iterator(); it.hasNext(); ) {
					var e = it.next();
					if (e.getKey() < r.Argument.getLastIncludedIndex()) {
						it.remove();
						try {
							e.getValue().close();
						} catch (IOException ex) {
							logger.warn("ProcessInstallSnapshot close(2) Exception", ex); // 文件关闭异常还是不向上抛了
						}
						var pathDelete = Paths.get(raftConfig.getDbHome(),
								LogSequence.snapshotFileName + ".installing." + e.getKey()).toString();
						Files.delete(Path.of(pathDelete));
					}
				}
			}
		} finally {
			receiveSnapshottingLock.unlock();
		}
		if (r.Argument.getDone()) {
			// 剩下的处理流程在下面的函数里面。
			logSequence.endReceiveInstallSnapshot(path, r);
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
		return state;
	}

	// 重置 OnTimer 需要的所有时间。
	private void resetTimerTime() {
		var now = System.currentTimeMillis();
		logSequence.setLeaderActiveTime(now);
		server.getConfig().forEachConnector(c -> ((Server.ConnectorEx)c).setAppendLogActiveTime(now));
	}

	/**
	 * 每个Raft使用一个固定Timer，根据不同的状态执行相应操作。
	 * 【简化】不同状态下不管维护管理不同的Timer了。
	 */
	private void onTimer() throws Exception {
		lock();
		try {
			if (isShutdown)
				return;
			long now = System.currentTimeMillis();
			switch (getState()) {
			case Follower:
				if (now - logSequence.getLeaderActiveTime() > leaderLostTimeout) {
					logger.warn("LeaderLostTimeout: {} > {}", now - logSequence.getLeaderActiveTime(), leaderLostTimeout);
					convertStateTo(RaftState.Candidate);
				}
				break;
			case Candidate:
				if (now > nextVoteTime)
					convertStateTo(RaftState.Candidate); // vote timeout. restart
				break;
			case Leader:
				server.getConfig().forEachConnector(c -> {
					var cex = (Server.ConnectorEx)c;
					if (now - cex.getHeartbeatTime() > raftConfig.getLeaderHeartbeatTimer())
						logSequence.sendHeartbeatTo(cex);
				});
				break;
			}
			if (++lowPrecisionTimer > 1000) {
				lowPrecisionTimer = 0;
				onLowPrecisionTimer();
			}
		} finally {
			unlock();
			//timerTask = Task.scheduleUnsafe(10, this::onTimer);
		}
	}

	private void onLowPrecisionTimer() throws ParseException, RocksDBException {
		server.getConfig().forEachConnector(Connector::start); // Connector Reconnect Bug?
		logSequence.removeExpiredUniqueRequestSet();
	}

	/**
	 * true，IsLeader && LeaderReady;
	 * false, !IsLeader
	 */
	boolean waitLeaderReady() throws Exception {
		lock();
		try {
			var volatileTmp = leaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
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
			var volatileTmp = leaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
			return isLeader() && volatileTmp.isDone() && volatileTmp.get();
		} finally {
			unlock();
		}
	}

	void resetLeaderReadyAfterChangeState() {
		leaderReadyFuture.setResult(false);
		leaderReadyFuture = new TaskCompletionSource<>(); // prepare for next leader
		signalAll(); // has under lock(this)
	}

	void setLeaderReady(RaftLog heart) throws Exception {
		if (isLeader()) {
			// 是否过期First-Heartbeat。
			// 使用 LeaderReadyFuture 可以更加精确的识别。
			// 但是，由于RaftLog不是常驻内存的，保存不了进程级别的变量。
			if (heart.getTerm() != leaderWaitReadyTerm || heart.getIndex() != leaderWaitReadyIndex)
				return;

			leaderWaitReadyIndex = 0;
			leaderWaitReadyTerm = 0;

			logger.info("{} {} LastIndex={} Count={}", getName(), raftConfig.getDbHome(),
					logSequence.getLastIndex(), logSequence.getTestStateMachineCount());

			leaderReadyFuture.setResult(true);
			signalAll(); // has under lock(this)

			server.foreach(allSocket -> {
				// 本来这个通告发给Agent(client)即可，
				// 但是现在没有区分是来自Raft的连接还是来自Agent，
				// 全部发送。
				// 另外Raft之间有两个连接，会收到多次，Raft不处理这个通告。
				// 由于Raft数量不多，不会造成大的浪费，不做处理了。
				if (allSocket.isHandshakeDone()) {
					var r = new LeaderIs();
					r.Argument.setTerm(logSequence.getTerm());
					r.Argument.setLeaderId(leaderId);
					r.Argument.setLeader(isLeader());
					r.Send(allSocket); // skip response.
				}
			});
			if (onLeaderReady != null)
				onLeaderReady.run();
		}
	}

	private boolean isLastLogUpToDate(BRequestVoteArgument candidate) throws RocksDBException {
		// NodeReady local candidate
		//           false false       IsLastLogUpToDate
		//           false true        false
		//           true  false       false
		//           true  true        IsLastLogUpToDate
		var last = logSequence.lastRaftLogTermIndex();
		if (!logSequence.getNodeReady()) {
			if (!candidate.getNodeReady()) {
				// 整个Raft集群第一次启动时，允许给初始节点投票。此时所有的初始节点形成多数派。任何一个当选都是可以的。
				// 以后由于机器更换再次启动而处于初始状态的节点肯定是少数派，即使它们之间互相投票，也不能成功。
				// 如果违背了这点，意味着违背了Raft的可用原则，已经不在Raft的处理范围内了。
				return isLastLogUpToDate(last, candidate);
			}

			// 拒绝投票直到发现达成多数派。
			return false;
		}
		return candidate.getNodeReady() && isLastLogUpToDate(last, candidate);
	}

	private static boolean isLastLogUpToDate(RaftLog last, BRequestVoteArgument candidate) {
		if (candidate.getLastLogTerm() > last.getTerm())
			return true;
		if (candidate.getLastLogTerm() < last.getTerm())
			return false;
		return candidate.getLastLogIndex() >= last.getIndex();
	}

	@SuppressWarnings("SameReturnValue")
	private long processRequestVote(RequestVote r) throws Exception {
		lock();
		try {
			// 不管任何状态重置下一次时间，使得每个node从大概一个时刻开始。
			nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();

			if (logSequence.trySetTerm(r.Argument.getTerm()) == LogSequence.SetTermResult.Newer)
				convertStateTo(RaftState.Follower); // new term found.
			// else continue process

			// RequestVote RPC
			// Receiver implementation:
			// 1.Reply false if term < currentTerm(§5.1)
			// 2.If votedFor is null or candidateId, and candidate's log is at
			// least as up - to - date as receiver's log, grant vote(§5.2, §5.4)

			r.Result.setTerm(logSequence.getTerm());
			r.Result.setVoteGranted(r.Argument.getTerm() == logSequence.getTerm() &&
					logSequence.canVoteFor(r.Argument.getCandidateId()) && isLastLogUpToDate(r.Argument));

			if (r.Result.getVoteGranted())
				logSequence.setVoteFor(r.Argument.getCandidateId());
			logger.info("{}: VoteFor={} Rpc={}", getName(), logSequence.getVoteFor(), r);
			r.SendResultCode(0);

			return Procedure.Success;
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private static long processLeaderIs(LeaderIs r) {
		// 这个协议是发送给Agent(Client)的，
		// 为了简单，不做区分。
		// Raft也会收到，忽略。
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private long processRequestVoteResult(RequestVote rpc, @SuppressWarnings("unused") Connector c) throws Exception {
		if (rpc.isTimeout() || rpc.getResultCode() != 0)
			return 0; // skip error. re-vote later.

		lock();
		try {
			if (logSequence.getTerm() != rpc.Argument.getTerm() || getState() != RaftState.Candidate) {
				// 结果回来时，上下文已经发生变化，忽略这个结果。
				logger.info("{} NotOwner={} NotCandidate={}", getName(),
						logSequence.getTerm() != rpc.Argument.getTerm(), getState() != RaftState.Candidate);
				return 0;
			}

			if (logSequence.trySetTerm(rpc.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				// new term found
				convertStateTo(RaftState.Follower);
				return Procedure.Success;
			}

			if (requestVotes.contains(rpc) && rpc.Result.getVoteGranted()) {
				int granteds = 0;
				for (var vote : requestVotes) {
					if (vote.Result.getVoteGranted())
						++granteds;
				}

				if (getState() == RaftState.Candidate // 确保当前状态是选举中。没有判断这个，后面 ConvertStateTo 也会忽略不正确的状态转换。
						&& granteds >= raftConfig.getHalfCount() // 加上自己就是多数派了。
						&& logSequence.canVoteFor(getName())) {
					logSequence.setVoteFor(getName());
					convertStateTo(RaftState.Leader);
				}
			}
		} finally {
			unlock();
		}
		return Procedure.Success;
	}

	private void sendRequestVote() throws RocksDBException {
		requestVotes.clear(); // 每次选举开始清除。
		// LogSequence.SetVoteFor(Name); // 先收集结果，达到 RaftConfig.HalfCount 才判断是否给自己投票。
		logSequence.trySetTerm(logSequence.getTerm() + 1);

		var arg = new BRequestVoteArgument();
		arg.setTerm(logSequence.getTerm());
		arg.setCandidateId(getName());
		var log = logSequence.lastRaftLogTermIndex();
		arg.setLastLogIndex(log.getIndex());
		arg.setLastLogTerm(log.getTerm());
		arg.setNodeReady(logSequence.getNodeReady());

		nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();
		server.getConfig().forEachConnector(c -> {
			var rpc = new RequestVote();
			rpc.Argument = arg;
			requestVotes.add(rpc);
			var sendResult = rpc.Send(c.TryGetReadySocket(),
					p -> processRequestVoteResult(rpc, c), raftConfig.getAppendEntriesTimeout() - 100);
			logger.info("{}:{}: SendRequestVote {}", getName(), sendResult, rpc);
		});
	}

	private void convertStateFromFollowerTo(RaftState newState) throws RocksDBException {
		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Follower->Follower", getName());
			leaderLostTimeout = raftConfig.getElectionTimeout();
			return;
		case Candidate:
			logger.info("RaftState {}: Follower->Candidate", getName());
			state = RaftState.Candidate;
			sendRequestVote();
			return;
		case Leader:
			// 并发的RequestVote的结果如果没有判断当前状态，可能会到达这里。
			// 不是什么大问题。see ProcessRequestVoteResult
			logger.info("RaftState {} Impossible! Follower->Leader", getName());
		}
	}

	private void convertStateFromCandidateTo(RaftState newState) throws Exception {
		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Candidate->Follower", getName());
			leaderLostTimeout = raftConfig.getElectionTimeout();
			state = RaftState.Follower;
			requestVotes.clear();
			return;
		case Candidate:
			logger.info("RaftState {}: Candidate->Candidate", getName());
			sendRequestVote();
			return;
		case Leader:
			requestVotes.clear();
			cancelAllReceiveSnapshotting();

			logger.info("RaftState {}: Candidate->Leader", getName());
			state = RaftState.Leader;
			leaderId = getName(); // set to self

			// (Reinitialized after election)
			var nextIndex = logSequence.getLastIndex() + 1;

			server.getConfig().forEachConnector(c -> {
				var cex = (Server.ConnectorEx)c;
				cex.start(); // 马上尝试连接。
				cex.setNextIndex(nextIndex);
				cex.setMatchIndex(0);
			});

			// Upon election:
			// send initial empty AppendEntries RPCs
			// (heartbeat)to each server; repeat during
			// idle periods to prevent election timeouts(§5.2)
			var result = logSequence.appendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent), null);
			leaderWaitReadyIndex = result.index;
			leaderWaitReadyTerm = result.term;
		}
	}

	private void convertStateFromLeaderTo(RaftState newState) throws Exception {
		// 本来 Leader -> Follower 需要，为了健壮性，全部改变都重置。
		resetLeaderReadyAfterChangeState();
		logSequence.cancelAllInstallSnapshot();
		logSequence.cancelPendingAppendLogFutures();

		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Leader->Follower", getName());
			state = RaftState.Follower;
			leaderLostTimeout = raftConfig.getElectionTimeout();
			return;
		case Candidate:
			logger.error("RaftState {} Impossible! Leader->Candidate", getName());
			return;
		case Leader:
			logger.error("RaftState {} Impossible! Leader->Leader", getName());
		}
	}

	public void convertStateTo(RaftState newState) throws Exception {
		resetTimerTime();
		// 按真值表处理所有情况。
		switch (getState()) {
		case Follower:
			convertStateFromFollowerTo(newState);
			return;
		case Candidate:
			convertStateFromCandidateTo(newState);
			return;
		case Leader:
			convertStateFromLeaderTo(newState);
		}
	}

	private void registerInternalRpc() {
		server.AddFactoryHandle(RequestVote.TypeId_, new Service.ProtocolFactoryHandle<>(
				RequestVote::new, this::processRequestVote, TransactionLevel.Serializable, DispatchMode.Normal));
		server.AddFactoryHandle(AppendEntries.TypeId_, new Service.ProtocolFactoryHandle<>(
				AppendEntries::new, this::processAppendEntries, TransactionLevel.Serializable, DispatchMode.Normal));
		server.AddFactoryHandle(InstallSnapshot.TypeId_, new Service.ProtocolFactoryHandle<>(
				InstallSnapshot::new, this::processInstallSnapshot, TransactionLevel.Serializable, DispatchMode.Normal));
		server.AddFactoryHandle(LeaderIs.TypeId_, new Service.ProtocolFactoryHandle<>(
				LeaderIs::new, Raft::processLeaderIs, TransactionLevel.Serializable, DispatchMode.Normal));
		server.AddFactoryHandle(GetLeader.TypeId_, new Service.ProtocolFactoryHandle<>(
				GetLeader::new, this::processGetLeader, TransactionLevel.None, DispatchMode.Normal));
		server.AddFactoryHandle(StartServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
				StartServerConnector::new, this::processStartServer, TransactionLevel.None, DispatchMode.Normal));
		server.AddFactoryHandle(StopServerConnector.TypeId_, new Service.ProtocolFactoryHandle<>(
				StopServerConnector::new, this::processStopServer, TransactionLevel.None, DispatchMode.Normal));
	}

	private long processGetLeader(GetLeader r) {
		// see Server::trySendLeaderIs
		String leaderId = getLeaderId();
		if (leaderId == null || leaderId.isEmpty())
			return Procedure.Unknown;

		if (getName().equals(leaderId) && !isLeader())
			return Procedure.Unknown;

		// redirect
		r.Result.setTerm(getLogSequence().getTerm());
		r.Result.setLeaderId(leaderId); // maybe empty
		r.Result.setLeader(isLeader());
		r.trySendResultCode(Procedure.Success);
		return 0;
	}

	private long processStartServer(StartServerConnector r) {
		server.getConfig().forEachConnector(Connector::stop);
		r.SendResult();
		return 0;
	}

	private long processStopServer(StopServerConnector r) {
		server.getConfig().forEachConnector(Connector::start);
		r.SendResult();
		return 0;
	}
}
