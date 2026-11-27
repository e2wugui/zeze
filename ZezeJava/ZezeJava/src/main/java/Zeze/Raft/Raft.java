package Zeze.Raft;

import java.util.Objects;

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
import org.rocksdb.RocksDBException;
import org.jspecify.annotations.Nullable;

/**
 * Raft Core：单节点 Raft 协议核心，维护 Follower/Candidate/Leader 状态机与选举、计时器。
 */
public final class Raft {
	private static final Logger logger = LogManager.getLogger(Raft.class);

	private volatile @Nullable String leaderId;
	private final RaftConfig raftConfig;
	private final LogSequence logSequence;
	private final Server server;
	private final TaskOneByOneByKey taskOneByOne;
	private final String userTaskOneByOneKey;

	private final StateMachine stateMachine;
	public volatile boolean isShutdown = false;
	// follower 侧快照接收登记表（锁序：raft -> receiveSnapshotting）。
	final ReceiveSnapshotting receiveSnapshotting = new ReceiveSnapshotting(this);
	private volatile RaftState state = RaftState.Follower;
	private @Nullable Future<?> timerTask;
	private long lowPrecisionTimer;
	private final Lock atFatalKillsLock = new ReentrantLock();
	private final ArrayList<Action0> atFatalKills = new ArrayList<>();

	// Candidate
	private final ConcurrentHashSet<RequestVote> requestVotes = new ConcurrentHashSet<>();
	private final ConcurrentHashSet<PreVote> preVotes = new ConcurrentHashSet<>();
	private boolean preVoting; // 处于预投票阶段（term 尚未递增）
	private long nextVoteTime; // 当前轮选举超时时间，超时后重启选举

	// Leader
	private long leaderWaitReadyTerm;
	private long leaderWaitReadyIndex;
	private volatile TaskCompletionSource<Boolean> leaderReadyFuture = new TaskCompletionSource<>();

	private final Lock mutex = new ReentrantLock();
	private final Condition condition = mutex.newCondition();

	private @Nullable Action0 onLeaderReady; // Leader 就绪回调
	@Nullable Action0 onFollowerReceiveKeepAlive;

	public void setOnLeaderReady(@Nullable Action0 action) {
		onLeaderReady = action;
	}

	public void setOnFollowerReceiveKeepAlive(@Nullable Action0 action) {
		onFollowerReceiveKeepAlive = action;
	}

	public String getName() {
		return Objects.requireNonNull(raftConfig.getName());
	}

	public @Nullable String getLeaderId() {
		return leaderId;
	}

	void setLeaderId(@Nullable String value) {
		leaderId = value;
	}

	public RaftConfig getRaftConfig() {
		return raftConfig;
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

	public static void executeImportantTask(Runnable task) {
		//noinspection DataFlowIssue
		Task.getCriticalThreadPool().execute(task);
	}

	public void executeUserTask(Action0 task) {
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

	public void fatalKill() {
		isShutdown = true;
		atFatalKillsLock.lock();
		try {
			for (Action0 action : atFatalKills) {
				try {
					action.run();
				} catch (Throwable e) { // 致命终止前兜底捕获所有异常
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

	public void appendLog(Log log, @Nullable Serializable result, Action2<RaftLog, Boolean> callback) {
		if (result != null)
			log.setRpcResult(new Binary(ByteBuffer.encode(result)));
		try {
			logSequence.appendLog(log, callback);
		} catch (RaftRetryException | TaskCanceledException er) {
			throw er;
		} catch (InterruptedException ie) { // 先恢复中断标志，再按重试语义包装
			Thread.currentThread().interrupt();
			throw new RaftRetryException("Interrupted", ie);
		} catch (Throwable ex) {
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	public void appendLog(Log log) {
		appendLog(log, (Serializable)null);
	}

	public void appendLog(Log log, @Nullable Serializable result) {
		appendLog(log, result, -1);
	}

	/** expectedTerm为负时不限定任期；非负时在实际追加日志的锁内校验。 */
	public void appendLog(Log log, @Nullable Serializable result, long expectedTerm) {
		if (result != null)
			log.setRpcResult(new Binary(ByteBuffer.encode(result)));
		try {
			logSequence.appendLog(log, expectedTerm);
		} catch (RaftRetryException | TaskCanceledException er) {
			throw er;
		} catch (InterruptedException ie) { // 先恢复中断标志，再按重试语义包装
			Thread.currentThread().interrupt();
			throw new RaftRetryException("Interrupted", ie);
		} catch (Throwable ex) {
			throw new RaftRetryException("Inner Exception", ex);
		}
	}

	// 是否有正在接收或收尾中的 InstallSnapshot，本地 snapshot 需避开。
	public boolean isReceivingSnapshot() {
		return !receiveSnapshotting.isEmpty();
	}

	public void shutdown() throws Exception {
		boolean firstCaller;
		lock();
		try {
			// 拆库段仅由首调用者执行，但所有并发调用者均须等待本地快照及后台任务排空后再返回。
			firstCaller = !isShutdown;
			isShutdown = true;
		} finally {
			unlock();
		}
		if (firstCaller) {
			ShutdownHook.remove(this);
			server.stop();
		}

		// 不可中断地排空本地快照（避免提前关闭状态机），随后再等待日志裁剪与后台 apply 完成。
		lock();
		try {
			while (logSequence.getSnapshotting())
				condition.awaitUninterruptibly();
		} finally {
			unlock();
		}

		var removeLogBeforeFuture = logSequence.removeLogBeforeFuture;
		if (removeLogBeforeFuture != null)
			removeLogBeforeFuture.await();
		var applyFuture = logSequence.applyFuture;
		if (applyFuture != null)
			applyFuture.await();

		if (!firstCaller)
			return; // 排空与等待已完成，拆库段由首调用者独占

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

	public Raft(StateMachine sm, @Nullable String RaftName) throws Exception {
		this(sm, RaftName, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, @Nullable String RaftName, @Nullable RaftConfig raftConf) throws Exception {
		this(sm, RaftName, raftConf, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, @Nullable String RaftName, @Nullable RaftConfig raftConf, @Nullable Config config) throws Exception {
		this(sm, RaftName, raftConf, config, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, @Nullable String RaftName, @Nullable RaftConfig raftConf, @Nullable Config config, String name)
			throws Exception {
		this(sm, RaftName, raftConf, config, name, Server::new, new TaskOneByOneByKey());
	}

	public Raft(StateMachine sm, @Nullable String RaftName, @Nullable RaftConfig raftConf, @Nullable Config config, String name,
	            Func3<Raft, String, Config, Server> serverFactory, TaskOneByOneByKey taskOneByOne) throws Exception {
		this(sm, RaftName, null, raftConf, config, name, serverFactory, taskOneByOne);
	}

	public Raft(StateMachine sm, @Nullable String RaftName, @Nullable RocksDatabase database, @Nullable RaftConfig raftConf, @Nullable Config config, String name,
	            Func3<Raft, String, Config, Server> serverFactory, TaskOneByOneByKey taskOneByOne) throws Exception {

		if (raftConf == null)
			raftConf = RaftConfig.load();
		raftConf.verify();

		this.taskOneByOne = taskOneByOne;
		// userTaskOneByOneKey 沿用原始配置名。
		userTaskOneByOneKey = "Zeze.Raft.UserTaskOneByOneKey." + raftConf.getName();
		sm.setRaft(this);
		stateMachine = sm;

		if (RaftName != null && !RaftName.isEmpty()) {
			// 派生私有副本改名并联动 DbHome，避免修改调用方传入的配置对象。
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

		Files.createDirectories(Paths.get(Objects.requireNonNull(raftConfig.getDbHome())));

		logSequence = new LogSequence(this, database);

		registerInternalRpc();

		var snapshot = logSequence.getCommittedSnapshotFile();
		// 首次快照前也可从原子应用水位恢复；默认状态机无快照仍reset。
		long t = System.nanoTime();
		sm.recover(new File(snapshot).isFile() ? snapshot : null);
		logger.info("Raft {} recover time={}ms", getName(), (System.nanoTime() - t) / 1_000_000);

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
			if (isShutdown)
				return 0;
			return logSequence.followerOnAppendEntries(r);
		} finally {
			unlock();
		}
	}

	long processInstallSnapshot(InstallSnapshot r) throws Exception {
		ReceiveSnapshotting.@Nullable Entry doneEntry;
		lock();
		try {
			if (isShutdown)
				return 0;
			r.Result.setTerm(logSequence.getTerm());
			if (r.Argument.getTerm() < logSequence.getTerm() || r.Argument.getTerm() > LogSequence.TERM_MAX) {
				// term 过旧或超出 TERM_MAX 上界时直接拒绝。
				r.SendResultCode(InstallSnapshot.ResultCodeTermError);
				return 0;
			}

			var setTermResult = logSequence.trySetTerm(r.Argument.getTerm());
			if (setTermResult == LogSequence.SetTermResult.Newer) {
				r.Result.setTerm(logSequence.getTerm());
				convertStateTo(RaftState.Follower);
			} else if (setTermResult == LogSequence.SetTermResult.Same) {
				// 同 term 处理与 followerOnAppendEntries 对齐。
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

			// 已提交或已应用前缀内的旧快照直接确认（等于上界仍允许重装）。
			if (logSequence.isSnapshotStale(r.Argument.getLastIncludedIndex())) {
				receiveSnapshotting.discard(r.Argument.getLastIncludedIndex());
				r.SendResultCode(0);
				return 0;
			}

			// 本地快照进行中拒绝新安装首块，避免并发操作状态机备份目录（传输中途的冲突由收尾阶段兜底拦截）。
			if (r.Argument.getOffset() == 0 && logSequence.getSnapshotting()) {
				r.SendResultCode(InstallSnapshot.ResultCodeSnapshottingConflict);
				return 0;
			}
			// 准入校验与写块须同持 Raft 锁，避免过期请求在锁外误删新任期快照文件。
			doneEntry = receiveSnapshotting.process(r);
		} finally {
			unlock();
		}

		// 收尾可能等待后台裁剪，须在释放 Raft 锁后执行（内部重新复核归属）。
		if (doneEntry != null)
			r.SendResultCode(logSequence.endReceiveInstallSnapshot(doneEntry, r));
		return Procedure.Success;
	}

	// 周期清理残留接收条目（锁序：raft -> receiveSnapshotting），确保归属读取与删除原子一致。
	void gcReceiveSnapshotting(long now) {
		lock();
		try {
			receiveSnapshotting.gc(now, logSequence.getTerm(), getLeaderId(), receiveSnapshottingTimeout());
		} finally {
			unlock();
		}
	}

	long receiveSnapshottingTimeout() {
		// 留足单块超时与心跳重试余量，避免误杀慢速合法安装。
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
	 * 各状态共用这一个Timer，不分别维护各自的Timer。
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
					convertStateTo(RaftState.Candidate); // 选举超时，重新发起
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
				// 在 Raft 锁内复查 isShutdown 后再触发重连（锁序 Raft -> Service -> Connector），避免已停节点重建连接。
				if (!isShutdown)
					server.getConfig().forEachConnector(Connector::start);
			}
		} finally {
			unlock();
		}
	}

	private void onLowPrecisionTimer() throws Exception {
		logSequence.removeExpiredUniqueRequestSet();
		gcReceiveSnapshotting(System.currentTimeMillis());
		logSequence.drainPendingDeleteGenFiles();
	}

	/** 等待当前 Leader 就绪；非 Leader 或等待超时时返回 false。 */
	boolean waitLeaderReady() {
		// 等待预算为一个完整选举周期（含 PreVote 轮次）。
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
				// 多数派失联时 SetLeaderReadyEvent 无法提交，限时等待避免请求任务无限堆积。
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
			var volatileTmp = leaderReadyFuture;
			return isLeader() && volatileTmp.isDone() && volatileTmp.get();
		} finally {
			unlock();
		}
	}

	void resetLeaderReadyAfterChangeState() {
		leaderReadyFuture.setResult(false);
		leaderReadyFuture = new TaskCompletionSource<>(); // 为下一任 Leader 重置
		signalAll(); // 调用方已持有 Raft 锁
	}

	void setLeaderReady(RaftLog heart) throws Exception {
		if (isLeader()) {
			// 过滤历史重放的旧心跳，仅匹配当前任期等待就绪的索引。
			if (heart.getTerm() != leaderWaitReadyTerm || heart.getIndex() != leaderWaitReadyIndex)
				return;

			leaderWaitReadyIndex = 0;
			leaderWaitReadyTerm = 0;

			logger.info("{} {} LastIndex={} Count={}", getName(), raftConfig.getDbHome(),
					logSequence.getLastIndex(), logSequence.getTestStateMachineCount());

			leaderReadyFuture.setResult(true);
			signalAll(); // 调用方已持有 Raft 锁

			server.foreach(allSocket -> {
				// 向全部已握手连接广播 LeaderIs（Raft 节点间连接收到后忽略）。
				if (allSocket.isHandshakeDone()) {
					var r = new LeaderIs();
					r.Argument.setTerm(logSequence.getTerm());
					r.Argument.setLeaderId(Objects.requireNonNull(leaderId));
					r.Argument.setLeader(isLeader());
					r.Send(allSocket);
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
				// 集群首次启动允许未就绪节点互投产生初始 Leader；运行期新增空节点占少数派无法当选。
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
			if (isShutdown)
				return 0;
			if (logSequence.trySetTerm(r.Argument.getTerm()) == LogSequence.SetTermResult.Newer)
				convertStateTo(RaftState.Follower);

			// Raft RequestVote 接收规则：term 匹配、未投票或已投给该候选者且日志不旧于本地时授票。
			r.Result.setTerm(logSequence.getTerm());
			r.Result.setVoteGranted(r.Argument.getTerm() == logSequence.getTerm() &&
					logSequence.canVoteFor(r.Argument.getCandidateId()) && isLastLogUpToDate(r.Argument));

			if (r.Result.getVoteGranted()) {
				logSequence.setVoteFor(Objects.requireNonNull(r.Argument.getCandidateId()));
				// 授票后给候选者完整的选举窗口；拒绝的请求不能压制本地选举。
				long now = System.currentTimeMillis();
				logSequence.setLeaderActiveTime(now);
				nextVoteTime = now + logSequence.getElectionTimeout();
			}
			logger.info("{}: VoteFor={} Rpc={}", getName(), logSequence.getVoteFor(), r);
			r.SendResultCode(0);

			return Procedure.Success;
		} finally {
			unlock();
		}
	}

	@SuppressWarnings("SameReturnValue")
	private static long processLeaderIs(LeaderIs r) {
		// 仅 Agent 处理 LeaderIs，Raft 节点收到直接忽略。
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private long processRequestVoteResult(RequestVote rpc, @SuppressWarnings("unused") Connector c) throws Exception {
		if (rpc.isTimeout() || rpc.getResultCode() != 0)
			return 0; // 忽略失败应答，等待下轮重选

		lock();
		try {
			if (isShutdown)
				return 0;
			// 旧选举的真实应答仍能报告更高任期，须先退位再过滤投票归属。
			if (logSequence.trySetTerm(rpc.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				convertStateTo(RaftState.Follower);
				return Procedure.Success;
			}

			if (logSequence.getTerm() != rpc.Argument.getTerm() || getState() != RaftState.Candidate) {
				// 结果回来时，上下文已经发生变化，忽略这个结果。
				logger.info("{} NotOwner={} NotCandidate={}", getName(),
						logSequence.getTerm() != rpc.Argument.getTerm(), getState() != RaftState.Candidate);
				return 0;
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
			if (isShutdown)
				return 0;
			// PreVote 不修改 term/voteFor 或选举计时；仅在候选者任期更新、日志足够新且近期未收到有效 Leader 消息时授票。
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
			case Leader -> false;
			case Candidate -> true;
			default -> // 最小沉默窗口与选举超时下界一致（heartbeat + ElectionTimeoutBase）。
					System.currentTimeMillis() - logSequence.getLeaderActiveTime()
							>= raftConfig.getLeaderHeartbeatTimer() + RaftConfig.ElectionTimeoutBase;
		};
	}

	private long processPreVoteResult(PreVote rpc, @SuppressWarnings("unused") Connector c) throws Exception {
		if (rpc.isTimeout() || rpc.getResultCode() != 0)
			return 0; // 忽略失败应答，等待下轮重选

		lock();
		try {
			if (isShutdown)
				return 0;
			// 旧选举的真实应答仍能报告更高任期，须先退位再过滤投票归属。
			if (logSequence.trySetTerm(rpc.Result.getTerm()) == LogSequence.SetTermResult.Newer) {
				convertStateTo(RaftState.Follower);
				return Procedure.Success;
			}

			if (!preVoting || getState() != RaftState.Candidate) {
				// 结果回来时，上下文已经发生变化，忽略这个结果。
				return 0;
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

	/** term 达到上界时拒绝发起选举，避免 term+1 溢出回绕；需人工重置 rafts 表中的 term 后恢复。 */
	private boolean checkTermCanElect() {
		if (logSequence.getTerm() < LogSequence.TERM_MAX)
			return true;
		logger.fatal("{} term({}) reached TERM_MAX({}), refuse election to avoid term+1 overflow wrap."
						+ " manual intervention required: reset term in rocks rafts table.",
				getName(), logSequence.getTerm(), LogSequence.TERM_MAX);
		return false;
	}

	private void sendPreVote() throws RocksDBException {
		// 拒绝选举时同样推进 nextVoteTime，避免 onTimer 每 tick 重复触发 fatal 日志。
		nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();
		if (!checkTermCanElect())
			return;
		preVotes.clear();
		preVoting = true;

		var arg = new BRequestVoteArgument();
		arg.setTerm(logSequence.getTerm() + 1); // 预投票携带下一任期，不修改自身 term
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
		// 拒绝选举时同样推进 nextVoteTime，避免 onTimer 每 tick 重复触发 fatal 日志。
		nextVoteTime = System.currentTimeMillis() + raftConfig.getElectionTimeout();
		if (!checkTermCanElect())
			return;
		requestVotes.clear();
		logSequence.trySetTerm(logSequence.getTerm() + 1);
		logSequence.setVoteFor(getName()); // 先投给自己

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
			// 纵深防御：清掉上次leadership可能残留的发送会话。
			logSequence.getSendSnapshotting().cancelAll();

			logger.info("RaftState {}: Candidate->Leader", getName());
			state = RaftState.Leader;
			leaderId = getName();
			resetTimerTime();

			// 选举后重置各 follower 复制进度
			var nextIndex = logSequence.getLastIndex() + 1;

			server.getConfig().forEachConnector(c -> {
				var cex = (Server.ConnectorEx)c;
				cex.start();
				cex.setNextIndex(nextIndex);
				cex.setMatchIndex(0);
				cex.setPending(null); // 清理旧任期残留单飞槽位，防止阻塞 SetLeaderReadyEvent 与心跳
			});

			// 先预登记 leaderWaitReadyIndex/Term，再追加心跳日志作为就绪见证；
			// 避免 appendLog 失败导致已切 Leader 但匹配条件未登记而永久卡在非 ready 状态。
			leaderWaitReadyIndex = nextIndex;
			leaderWaitReadyTerm = logSequence.getTerm();
			try {
				logSequence.appendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent), null);
			} catch (Throwable ex) {
				// 当选后首条心跳日志落盘失败时节点处于半初始化 Leader 态，直接致命退出由外部拉起恢复。
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
		server.getConfig().forEachConnector(c -> ((Server.ConnectorEx)c).setPending(null));
		// Leader 态期间不更新 leaderActiveTime，退位时必须刷新，避免下一个 20ms tick 立即误超时选举。
		resetTimerTime();

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
		// 不无条件重置 leaderActiveTime：仅 Leader->Follower 与 Candidate->Leader 重置；
		// Follower/Candidate 收到更高任期但被拒的 RequestVote 时不得刷新选举计时，
		// 有效 Leader 消息（AppendEntries/InstallSnapshot）与授票路径已显式刷新 leaderActiveTime。
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
		String leaderId = getLeaderId();
		if (leaderId == null || leaderId.isEmpty())
			return Procedure.Unknown;

		if (getName().equals(leaderId) && !isLeader())
			return Procedure.Unknown;

		r.Result.setTerm(getLogSequence().getTerm());
		r.Result.setLeaderId(Objects.requireNonNull(leaderId));
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
