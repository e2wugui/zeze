package Zeze.Raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskCanceledException;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
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

	private volatile String leaderId;
	private final RaftConfig raftConfig;
	private final LogSequence logSequence;
	private final Server server;
	private final TaskOneByOneByKey taskOneByOne;
	private final String userTaskOneByOneKey;

	private final StateMachine stateMachine;
	public volatile boolean isShutdown = false;
	// follower侧接收中的安装登记表：条目=对.installing文件的所有权
	// token（文件唯一命名、finalizing收尾占位、gc/清理不变式见ReceiveSnapshotting）。
	// 锁序：raft→本表（收尾侧持raft锁调get/removeIdentity，与gc/snapshot既有方向一致）。
	final ReceiveSnapshotting receiveSnapshotting = new ReceiveSnapshotting(this);
	private volatile RaftState state = RaftState.Follower;
	private Future<?> timerTask;
	private long lowPrecisionTimer;
	private final Lock atFatalKillsLock = new ReentrantLock();
	private final ArrayList<Action0> atFatalKills = new ArrayList<>();

	// Candidate
	private final ConcurrentHashSet<RequestVote> requestVotes = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<PreVote> preVotes = new ConcurrentHashSet<>();
	private boolean preVoting; // 处于预投票阶段（term还没有增加）。
	private long nextVoteTime; // 等待当前轮选举结果超时；用来启动下一次选举。

	// Leader
	private long leaderWaitReadyTerm;
	private long leaderWaitReadyIndex;
	private volatile TaskCompletionSource<Boolean> leaderReadyFuture = new TaskCompletionSource<>();

	// Follower
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
			Thread.currentThread().interrupt();
			throw Task.forceThrow(e);
		}
	}

	public boolean await(long time) {
		try {
			return condition.await(time, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw Task.forceThrow(e);
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
		TaskSpec.ofAction(task).executeOneByOne(userTaskOneByOneKey, taskOneByOne);
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

	// 测试钩子：注入后 fatalKill 只置 isShutdown 并调用该动作即返回，不执行真实的
	// atFatalKills/logSequence.close/LogManager.shutdown/halt（会杀死或破坏测试 JVM）。
	private volatile Runnable fatalKillHookForTest;

	void setFatalKillHookForTest(Runnable hook) {
		fatalKillHookForTest = hook;
	}

	public void fatalKill() {
		isShutdown = true;
		var hook = fatalKillHookForTest;
		if (hook != null) {
			hook.run();
			return;
		}
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
		} catch (InterruptedException ie) { // 先恢复中断标志，再按重试语义包装
			Thread.currentThread().interrupt();
			throw new RaftRetryException("Interrupted", ie);
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
		} catch (InterruptedException ie) { // 先恢复中断标志，再按重试语义包装
			Thread.currentThread().interrupt();
			throw new RaftRetryException("Interrupted", ie);
		} catch (Throwable ex) { // rethrow RaftRetryException
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	// 是否有InstallSnapshot正在接收中（follower侧，含finalizing收尾占位）。本地snapshot需要避开。
	public boolean isReceivingSnapshot() {
		return !receiveSnapshotting.isEmpty();
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
			logSequence.getSendSnapshotting().cancelAll();
			receiveSnapshotting.cancelAll();

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
		this(sm, RaftName, null, raftConf, config, name, serverFactory, taskOneByOne);
	}

	public Raft(StateMachine sm, String RaftName, RocksDatabase database, RaftConfig raftConf, Config config, String name,
	            Func3<Raft, String, Config, Server> serverFactory, @NotNull TaskOneByOneByKey taskOneByOne) throws Exception {

		if (raftConf == null)
			raftConf = RaftConfig.load();
		raftConf.verify();

		this.taskOneByOne = taskOneByOne;
		// userTaskOneByOneKey沿用改名前的配置名（历史语义，不动）。
		userTaskOneByOneKey = "Zeze.Raft.UserTaskOneByOneKey." + raftConf.getName();
		sm.setRaft(this);
		stateMachine = sm;

		if (RaftName != null && !RaftName.isEmpty()) {
			// 【FND8-42】改名/联动DbHome在私有副本上执行，不变异调用方传入的配置对象：
			// 共享同一RaftConfig的多Raft会互相污染Name/DbHome（快照路径错位、跨实例覆盖）。
			raftConf = raftConf.derive(RaftName);
		}
		raftConfig = raftConf;

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

		logSequence = new LogSequence(this, database);

		registerInternalRpc();

		var snapshot = logSequence.getCommittedSnapshotFile();
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

		timerTask = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(20, 20);
	}

	private long processAppendEntries(AppendEntries r) throws Exception {
		lock();
		try {
			return logSequence.followerOnAppendEntries(r);
		} finally {
			unlock();
		}
	}

	long processInstallSnapshot(InstallSnapshot r) throws Exception { // package-private：测试直调合成rpc
		lock();
		try {
			r.Result.setTerm(logSequence.getTerm());
			if (r.Argument.getTerm() < logSequence.getTerm() || r.Argument.getTerm() > LogSequence.TERM_MAX) {
				// 1. Reply immediately if term < currentTerm
				// FND6-08：超过TERM_MAX的term非法（trySetTerm拒绝采纳），同样按term错误
				// 提前返回，不得落穿后续处理接受非法Leader的快照。
				r.SendResultCode(InstallSnapshot.ResultCodeTermError);
				return 0;
			}

			var setTermResult = logSequence.trySetTerm(r.Argument.getTerm());
			if (setTermResult == LogSequence.SetTermResult.Newer) {
				r.Result.setTerm(logSequence.getTerm());
				// new term found.
				convertStateTo(RaftState.Follower);
			} else if (setTermResult == LogSequence.SetTermResult.Same) {
				// 与 followerOnAppendEntries 的同term处理对齐。
				switch (getState()) {
				case Candidate:
					// 同term已经存在合法Leader，让位。
					convertStateTo(RaftState.Follower);
					break;
				case Leader:
					logger.fatal("Receive InstallSnapshot from another leader={} with same term={}, there must be a bug. this={}",
							r.Argument.getLeaderId(), logSequence.getTerm(), getLeaderId(), new Exception());
					fatalKill();
					return 0;
				}
			}
			leaderId = r.Argument.getLeaderId();
			logSequence.setLeaderActiveTime(System.currentTimeMillis());

			// 本地快照进行中（重阶段在锁外写 backupDir）时拒绝新安装的首块：
			// 接收完成后的状态机重置会与本地快照并发操作同一 backupDir，失败不可自愈。
			// 应答冲突码让 leader 中断本次安装（InstallSnapshotState.processResult 对非
			// Success/非 NewOffset 码即 SendSnapshotting.end），下个心跳自动重试。
			// 传输中途才开始本地快照的情形由 endReceiveInstallSnapshot 内的兜底检查拦截。
			if (r.Argument.getOffset() == 0 && logSequence.getSnapshotting()) {
				r.SendResultCode(InstallSnapshot.ResultCodeSnapshottingConflict);
				return 0;
			}
		} finally {
			unlock();
		}

		// 2. 接收数据块（表锁内完成所有非done应答）；done块返回finalizing条目，
		// 剩下的处理流程在endReceiveInstallSnapshot里面。
		var doneEntry = receiveSnapshotting.process(r);
		if (doneEntry != null)
			r.SendResultCode(logSequence.endReceiveInstallSnapshot(doneEntry, r));
		return Procedure.Success;
	}

	// 清理残留的接收条目（follower 侧）：周期驱动，具体不变式见
	// ReceiveSnapshotting.gc。
	void gcReceiveSnapshotting(long now) {
		long term;
		String leaderId;
		lock(); // term/leaderId 的写点都在 raft 锁内；锁顺序 raft→receiveSnapshotting，
		try {   // 与 processInstallSnapshot 一致（无反向嵌套路径）。
			term = logSequence.getTerm();
			leaderId = getLeaderId();
		} finally {
			unlock();
		}
		receiveSnapshotting.gc(now, term, leaderId, receiveSnapshottingTimeout());
	}

	long receiveSnapshottingTimeout() { // package-private：测试按公式合成超时，不硬编码
		// 合法安装的单 chunk 往返必然 <= AppendEntriesTimeout（超时即中断，中断后下个
		// 心跳重来），最坏合法空闲 ≈ AppendEntriesTimeout + LeaderHeartbeatTimer；
		// 取 4/2 倍留足余量，避免误杀慢速但合法的安装。
		return raftConfig.getAppendEntriesTimeout() * 4L + raftConfig.getLeaderHeartbeatTimer() * 2L;
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
				var electionTimeout = logSequence.getElectionTimeout();
				if (now - logSequence.getLeaderActiveTime() > electionTimeout) {
					logger.warn("LeaderLostTimeout: {} > {}", now - logSequence.getLeaderActiveTime(), electionTimeout);
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
				// 重连复查必须在Raft锁内：shutdown的isShutdown置位在同锁内，锁内复查
				// 彻底关死"检查过后才shutdown"的TOCTOU窗口——锁外重连会为已停Raft重建连接，
				// epoch一致使重连引擎永久运转。Raft→Service→Connector为既定单向锁序，start()
				// 构造链非阻塞，锁内调用安全。
				if (!isShutdown)
					server.getConfig().forEachConnector(Connector::start);
			}
		} finally {
			unlock();
			//timerTask = Task.scheduleNow(10, this::onTimer);
		}
	}

	private void onLowPrecisionTimer() throws Exception {
		// Connector重连在onTimer的Raft锁内复查isShutdown后执行（见上）；本方法仅LogSequence清理。
		logSequence.removeExpiredUniqueRequestSet();
		gcReceiveSnapshotting(System.currentTimeMillis()); // 残留接收条目周期清理
		logSequence.drainPendingDeleteGenFiles(); // gen清扫重试名单周期冲刷
	}

	/**
	 * true，IsLeader && LeaderReady;
	 * false, !IsLeader，或等待超时（仍是not-ready的Leader，见内部注释）。
	 */
	boolean waitLeaderReady() {
		// 等待预算为一个完整的选举周期（与Agent.waitForLeader的默认超时同公式：
		// PreVote引入后选举最坏情况多一轮）。
		long waitMs = raftConfig.getLeaderHeartbeatTimer()
				+ raftConfig.getElectionTimeoutMax() * 2L
				+ raftConfig.getAppendEntriesTimeout();
		var deadline = System.nanoTime() + waitMs * 1_000_000L;
		lock();
		try {
			var volatileTmp = leaderReadyFuture; // 每次只等待一轮的选举，不考虑中间Leader发生变化。
			while (isLeader()) {
				if (volatileTmp.isDone())
					return volatileTmp.get();
				// 多数派失联的分区场景下，SetLeaderReadyEvent永远无法提交，
				// 且没有更高term的消息到达（不会有signalAll唤醒），无期限的await()会让
				// 派发进来的请求任务无限堆积（每请求占一个unique串行桶+一个线程）。
				// 等待超出一个选举周期后放弃并返回false：processRequest会走
				// trySendLeaderIs路径，请求最终由客户端rpc超时/Agent重发闭环处理。
				var remain = deadline - System.nanoTime();
				if (remain <= 0)
					break;
				await(remain / 1_000_000L);
			}
		} finally {
			unlock();
		}
		return false;
	}

	public boolean isReadyLeader() {
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

	@SuppressWarnings("SameReturnValue")
	private long processPreVote(PreVote r) throws Exception {
		lock();
		try {
			// PreVote 接收者实现（raft 博士论文 §4.2.3）：
			// 不修改 term/voteFor，不重置任何选举计时。
			// 仅当候选者的"下一个term"更大、日志够新，
			// 且本节点近期没有听得到有效Leader时授予。
			r.Result.setTerm(logSequence.getTerm());
			r.Result.setVoteGranted(r.Argument.getTerm() > logSequence.getTerm()
					&& isLeaderSilent() && isLastLogUpToDate(r.Argument));
			logger.info("{}: PreVote Granted={} Rpc={}", getName(), r.Result.getVoteGranted(), r);
			r.SendResultCode(0);

			return Procedure.Success;
		} finally {
			unlock();
		}
	}

	private boolean isLeaderSilent() {
		return switch (getState()) {
			case Leader -> false; // 自己就是Leader。
			case Candidate -> true; // 自己也在选举中，说明Leader已经失联。
			default -> // Follower：在最小选举超时内收到过Leader消息，视为Leader仍然活着。
					System.currentTimeMillis() - logSequence.getLeaderActiveTime()
							>= raftConfig.getLeaderHeartbeatTimer() + 100;
		};
	}

	private long processPreVoteResult(PreVote rpc, @SuppressWarnings("unused") Connector c) throws Exception {
		if (rpc.isTimeout() || rpc.getResultCode() != 0)
			return 0; // skip error. re-vote later.

		lock();
		try {
			if (!preVoting || getState() != RaftState.Candidate) {
				// 结果回来时，上下文已经发生变化，忽略这个结果。
				return 0;
			}

			// 与 processRequestVoteResult 对齐（raft §5.1 Rules for Servers）：应答里带回
			// 更高 term 时更新term并立即退回Follower。PreVote候选者没有自增term，
			// 多数派其他节点可能已推进到更高term并选出了新leader：不消费Result.term
			// 会多走一轮携带过期term的无效prevote循环（仍被拒，直到新leader的
			// AppendEntries/RequestVote直达本节点）。
			if (logSequence.trySetTerm(rpc.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				convertStateTo(RaftState.Follower);
				return Procedure.Success;
			}

			if (preVotes.contains(rpc) && rpc.Result.getVoteGranted()) {
				int granteds = 0;
				for (var vote : preVotes) {
					if (vote.Result.getVoteGranted())
						++granteds;
				}

				if (getState() == RaftState.Candidate && granteds >= raftConfig.getHalfCount()) {
					// 预投票达成多数派（加上自己），开始真正的选举（此时才增加term）。
					preVoting = false;
					sendRequestVote();
				}
			}
		} finally {
			unlock();
		}
		return Procedure.Success;
	}

	private void startVote() throws RocksDBException {
		if (raftConfig.isPreVote())
			sendPreVote();
		else {
			preVoting = false;
			sendRequestVote();
		}
	}

	/**
	 * FND6-08：term 达到上界时拒绝发起选举。term+1 溢出回绕为负值会被 trySetTerm 判 Older，
	 * 选举永久冻结；且预投票携带的回绕term会传染。仅在库被旧版本投毒后可达，
	 * 需人工清理 rocks rafts 表的 term 后才能恢复。
	 */
	private boolean checkTermCanElect() {
		if (logSequence.getTerm() < LogSequence.TERM_MAX)
			return true;
		logger.fatal("{} term({}) reached TERM_MAX({}), refuse election to avoid term+1 overflow wrap."
						+ " manual intervention required: reset term in rocks rafts table.",
				getName(), logSequence.getTerm(), LogSequence.TERM_MAX);
		return false;
	}

	private void sendPreVote() throws RocksDBException {
		// FND6-08补：拒绝选举也要推进nextVoteTime——原拒绝路径在设置nextVoteTime之前return，
		// onTimer的Candidate分支(now>nextVoteTime恒真)每tick重进，20ms一条fatal刷日志，
		// 恰是本修复在trySetTerm里防的洪泛在自家拒绝路径上的翻版。
		nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();
		if (!checkTermCanElect())
			return;
		preVotes.clear(); // 每次预投票开始清除。
		preVoting = true;

		var arg = new BRequestVoteArgument();
		arg.setTerm(logSequence.getTerm() + 1); // 预投票携带"下一个term"，不修改自身term。
		arg.setCandidateId(getName());
		var log = logSequence.lastRaftLogTermIndex();
		arg.setLastLogIndex(log.getIndex());
		arg.setLastLogTerm(log.getTerm());
		arg.setNodeReady(logSequence.getNodeReady());

		server.getConfig().forEachConnector(c -> {
			var rpc = new PreVote();
			rpc.Argument = arg;
			preVotes.add(rpc);
			var sendResult = rpc.Send(c.TryGetReadySocket(),
					p -> processPreVoteResult(rpc, c), raftConfig.getAppendEntriesTimeout() - 100);
			logger.info("{}:{}: SendPreVote {}", getName(), sendResult, rpc);
		});
	}

	private void sendRequestVote() throws RocksDBException {
		// 同sendPreVote——拒绝选举也推进nextVoteTime防onTimer每tick重进刷fatal。
		nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();
		if (!checkTermCanElect())
			return;
		requestVotes.clear(); // 每次选举开始清除。
		logSequence.trySetTerm(logSequence.getTerm() + 1);
		logSequence.setVoteFor(getName()); // 先投给自己。

		var arg = new BRequestVoteArgument();
		arg.setTerm(logSequence.getTerm());
		arg.setCandidateId(getName());
		var log = logSequence.lastRaftLogTermIndex();
		arg.setLastLogIndex(log.getIndex());
		arg.setLastLogTerm(log.getTerm());
		arg.setNodeReady(logSequence.getNodeReady());

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
			return;
		case Candidate:
			logger.info("RaftState {}: Follower->Candidate", getName());
			state = RaftState.Candidate;
			startVote();
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
			state = RaftState.Follower;
			requestVotes.clear();
			preVotes.clear();
			preVoting = false;
			return;
		case Candidate:
			logger.info("RaftState {}: Candidate->Candidate", getName());
			startVote();
			return;
		case Leader:
			requestVotes.clear();
			preVotes.clear();
			preVoting = false;
			receiveSnapshotting.cancelAll();

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
			// 【R3-F3】先于appendLog登记等待条件：appendLog（RocksDB写）失败时state已是Leader，
			// 事后登记永远不会执行，setLeaderReady的唯一匹配条件（index/term）永不命中，产生
			// 永不ready的活Leader且无自愈（心跳照发压制重选举，磁盘恢复也不能自愈，FND10 raft-01）。
			// Raft锁内nextIndex==lastIndex+1即appendLog将写入的index，term即logSequence.getTerm()，
			// 预登记与事后登记的值完全一致。
			leaderWaitReadyIndex = nextIndex;
			leaderWaitReadyTerm = logSequence.getTerm();
			try {
				logSequence.appendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent), null);
			} catch (Throwable ex) {
				// appendLog同步写失败（磁盘满/IO故障等）时state已是Leader且只初始化一半，退位路径
				// 会再次触碰半初始化状态；按"宁死勿僵尸"（fatalKill判例，FND3-21）整进程终止，
				// 多数派仍在时集群只是少一节点，由外部拉起重启自愈。
				logger.error("append SetLeaderReadyEvent fail on become leader, fatalKill.", ex);
				fatalKill();
			}
		}
	}

	private void convertStateFromLeaderTo(RaftState newState) throws Exception {
		// 本来 Leader -> Follower 需要，为了健壮性，全部改变都重置。
		resetLeaderReadyAfterChangeState();
		logSequence.getSendSnapshotting().cancelAll();
		logSequence.cancelPendingAppendLogFutures();

		switch (newState) {
		case Follower:
			logger.info("RaftState {}: Leader->Follower", getName());
			state = RaftState.Follower;
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
		server.AddFactoryHandle(PreVote.TypeId_, new Service.ProtocolFactoryHandle<>(
				PreVote::new, this::processPreVote, TransactionLevel.Serializable, DispatchMode.Normal));
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
		server.getConfig().forEachConnector(Connector::start);
		r.SendResult();
		return 0;
	}

	private long processStopServer(StopServerConnector r) {
		server.getConfig().forEachConnector(Connector::stop);
		r.SendResult();
		return 0;
	}
}
