package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Transaction.*;
import Zeze.Util.*;
import Zeze.*;
import java.io.*;
import java.nio.file.*;

/** 
 Raft Core
*/
public final class Raft {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public String getName() {
		return getRaftConfig().getName();
	}
	private String LeaderId;
	public String getLeaderId() {
		return LeaderId;
	}
	public void setLeaderId(String value) {
		LeaderId = value;
	}
	private RaftConfig RaftConfig;
	public RaftConfig getRaftConfig() {
		return RaftConfig;
	}
	private LogSequence LogSequence;
	public LogSequence getLogSequence() {
		return LogSequence;
	}
	public boolean isLeader() {
		return this.getState() == RaftState.Leader;
	}
	public boolean getHasLeader() {
		return false == tangible.StringHelper.isNullOrEmpty(getLeaderId());
	}
	private Server Server;
	public Server getServer() {
		return Server;
	}

	private SimpleThreadPool ImportantThreadPool;
	public SimpleThreadPool getImportantThreadPool() {
		return ImportantThreadPool;
	}

	private StateMachine StateMachine;
	public StateMachine getStateMachine() {
		return StateMachine;
	}


	public void AppendLog(Log log) {
		AppendLog(log, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void AppendLog(Log log, bool ApplySync = true)
	public void AppendLog(Log log, boolean ApplySync) {
		getLogSequence().AppendLog(log, ApplySync);
	}

	//public bool IsShutdown { get; private set; }

	public void Shutdown() {
		//IsShutdown = true;

		// 0 clear pending task if is leader
		if (isLeader()) {
			getServer().getTaskOneByOne().Shutdown();
		}
		else {
			// 如果是 Leader，那么 Shutdown 用户请求任务队列 Server.TaskOneByOne 即可。
			// 用户请求处理依赖 ImportantThreadPool。
			// 如果是 Follower，那么安全关闭 ImportantThreadPool，
			// 但是Follower的请求是来自 Leader，需要考虑一下拒绝方式：
			// 目前考虑是ImportantThreadPool.Shutdown后，直接丢掉来自 Leader的请求。
			// 此时认为 Follower 不再能响应了。
			getImportantThreadPool().Shutdown();
		}

		// 1. close network.
		getServer().Stop();

		synchronized (this) {
			// see WaitLeaderReady.
			// 可以避免状态设置不对的问题。关闭时转换成Follower也是对的。
			ConvertStateTo(RaftState.Follower);
			// Cancel Follower TimerTask
			if (LeaderLostTimerTask != null) {
				LeaderLostTimerTask.Cancel();
			}
			LeaderLostTimerTask = null;
		}

		// 3. close LogSequence (rocksdb)
		getLogSequence().Close();
	}

	private void ProcessExit(Object sender, tangible.EventArgs e) {
		Shutdown();
	}


	public Raft(StateMachine sm, String RaftName, RaftConfig raftconf, Zeze.Config config) {
		this(sm, RaftName, raftconf, config, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName, RaftConfig raftconf) {
		this(sm, RaftName, raftconf, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm, String RaftName) {
		this(sm, RaftName, null, null, "Zeze.Raft.Server");
	}

	public Raft(StateMachine sm) {
		this(sm, null, null, null, "Zeze.Raft.Server");
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Raft(StateMachine sm, string RaftName = null, RaftConfig raftconf = null, Zeze.Config config = null, string name = "Zeze.Raft.Server")
	public Raft(StateMachine sm, String RaftName, RaftConfig raftconf, Config config, String name) {
		if (null == raftconf) {
			raftconf = RaftConfig.Load();
		}
		raftconf.Verify();

		RaftConfig = raftconf;
		sm.setRaft(this);
		StateMachine = sm;

		if (false == tangible.StringHelper.isNullOrEmpty(RaftName)) {
			raftconf.setName(RaftName);
		}

		if (null == config) {
			config = Config.Load(null);
		}

		Server = new Server(this, name, config);
		if (getServer().getConfig().AcceptorCount() != 0) {
			throw new RuntimeException("Acceptor Found!");
		}
		if (getServer().getConfig().ConnectorCount() != 0) {
			throw new RuntimeException("Connector Found!");
		}
		if (getRaftConfig().getNodes().size() < 3) {
			throw new RuntimeException("Startup Nodes.Count Must >= 3.");
		}

		ImportantThreadPool = new SimpleThreadPool(5, String.format("Raft.%1$s", getName()));
		Server.CreateAcceptor(getServer(), raftconf);
		Server.CreateConnector(getServer(), raftconf);

		LogSequence = new LogSequence(this);

		RegisterInternalRpc();
		StartLeaderLostTimerTask();
		getLogSequence().StartSnapshotPerDayTimer();
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
		AppDomain.CurrentDomain.ProcessExit += ProcessExit;
	}

	private int ProcessAppendEntries(Protocol p) {
		var r = p instanceof AppendEntries ? (AppendEntries)p : null;
		synchronized (this) {
			getLogSequence().TrySetTerm(r.getArgument().getTerm());
			// 【注意】只有Leader会发送AppendEntries，总是转到Follower，不管当前状态。
			// raft.pdf 文档描述仅在 Candidate 才转。
			if (getState() != RaftState.Follower) {
				ConvertStateTo(RaftState.Follower);
			}
			setLeaderId(r.getArgument().getLeaderId()); // always replace
			return getLogSequence().FollowerOnAppendEntries(r);
		}
	}

//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
	private java.util.concurrent.ConcurrentHashMap<Long, FileStream> ReceiveSnapshotting = new java.util.concurrent.ConcurrentHashMap<Long, FileStream>();

	private int ProcessInstallSnapshot(Protocol p) {
		var r = p instanceof InstallSnapshot ? (InstallSnapshot)p : null;
		synchronized (this) {
			if (getLogSequence().TrySetTerm(r.getArgument().getTerm())) {
				setLeaderId(r.getArgument().getLeaderId());
				// new term found.
				ConvertStateTo(RaftState.Follower);
			}
		}
		r.getResult().setTerm(getLogSequence().getTerm());
		if (r.getArgument().getTerm() < getLogSequence().getTerm()) {
			// 1. Reply immediately if term < currentTerm
			r.SendResultCode(InstallSnapshot.ResultCodeTermError);
			return Procedure.LogicError;
		}

		// 2. Create new snapshot file if first chunk(offset is 0)
		// 把 LastIncludedIndex 放到文件名中，
		// 新的InstallSnapshot不覆盖原来进行中或中断的。
		var path = Paths.get(getRaftConfig().getDbHome()).resolve(String.format("%1$s.%2$s", LogSequence.SnapshotFileName, r.getArgument().getLastIncludedIndex())).toString();

//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
		FileStream outputFileStream = null;
		if (r.getArgument().getOffset() == 0) {
			// GetOrAdd 允许重新开始。
//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
			outputFileStream = ReceiveSnapshotting.putIfAbsent(r.getArgument().getLastIncludedIndex(), (_) -> new FileStream(path, FileMode.OpenOrCreate));
			outputFileStream.Seek(0, SeekOrigin.End);
		}
		else {
			// ignore return of TryGetValue here.
			tangible.OutObject<FileStream> tempOut_outputFileStream = new tangible.OutObject<FileStream>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
			ReceiveSnapshotting.TryGetValue(r.getArgument().getLastIncludedIndex(), tempOut_outputFileStream);
		outputFileStream = tempOut_outputFileStream.outArgValue;
		}

		if (null == outputFileStream) {
			// 肯定是旧的被丢弃的安装，Discard And Ignore。
			r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
			return Procedure.Success;
		}

		r.getResult().setOffset(-1); // 默认让Leader继续传输，不用重新定位。
		if (r.getArgument().getOffset() > outputFileStream.Length) {
			// 数据块超出当前已经接收到的数据。
			// 填写当前长度，让Leader从该位置开始重新传输。
			r.getResult().setOffset(outputFileStream.Length);
			r.SendResultCode(InstallSnapshot.ResultCodeNewOffset);
			return Procedure.Success;
		}

		if (r.getArgument().getOffset() == outputFileStream.Length) {
			// 正常的Append流程，直接写入。
			// 3. Write data into snapshot file at given offset
			outputFileStream.Write(r.getArgument().getData().getBytes(), r.getArgument().getData().getOffset(), r.getArgument().getData().getCount());
		}
		else {
			// 数据块开始位置小于当前长度。
			var newEndPosition = r.getArgument().getOffset() + r.getArgument().getData().getCount();
			if (newEndPosition > outputFileStream.Length) {
				// 有新的数据需要写入文件。
				outputFileStream.Seek(r.getArgument().getOffset(), SeekOrigin.Begin);
				outputFileStream.Write(r.getArgument().getData().getBytes(), r.getArgument().getData().getOffset(), r.getArgument().getData().getCount());
			}
			r.getResult().setOffset(outputFileStream.Length);
		}

		// 4. Reply and wait for more data chunks if done is false
		if (r.getArgument().getDone()) {
			// 5. Save snapshot file, discard any existing or partial snapshot with a smaller index
			outputFileStream.Close();
			for (var e : ReceiveSnapshotting) {
				if (e.Key < r.getArgument().getLastIncludedIndex()) {
					e.Value.Close();
					var pathDelete = Paths.get(getRaftConfig().getDbHome()).resolve(String.format("%1$s.%2$s", LogSequence.SnapshotFileName, e.Key)).toString();
					(new File(path)).delete();
					TValue _;
					tangible.OutObject<FileStream> tempOut__ = new tangible.OutObject<FileStream>();
//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.FileStream is input or output:
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					ReceiveSnapshotting.TryRemove(e.Key, tempOut__);
				_ = tempOut__.outArgValue;
				}
			}
			// 剩下的处理流程在下面的函数里面。
			getLogSequence().EndReceiveInstallSnapshot(outputFileStream, r);
		}
		r.SendResultCode(0);
		return Procedure.Success;
	}

	public enum RaftState {
		Follower,
		Candidate,
		Leader;

		public static final int SIZE = java.lang.Integer.SIZE;

		public int getValue() {
			return this.ordinal();
		}

		public static RaftState forValue(int value) {
			return values()[value];
		}
	}

	private RaftState State = RaftState.Follower;
	public RaftState getState() {
		return State;
	}
	private void setState(RaftState value) {
		State = value;
	}

	// Candidate
	private SchedulerTask StartRequestVoteDelayTask;
	private SchedulerTask WaitMajorityVoteTimoutTask;
	private java.util.concurrent.ConcurrentHashMap<String, Connector> VoteSuccess = new java.util.concurrent.ConcurrentHashMap<String, Connector>();
	// Leader
	private SchedulerTask HearbeatTimerTask;
	private ManualResetEvent LeaderReadyEvent = new ManualResetEvent(false);
	public ManualResetEvent getLeaderReadyEvent() {
		return LeaderReadyEvent;
	}
	// Follower
	private SchedulerTask LeaderLostTimerTask;

	/** 
	 true，IsLeader && LeaderReady;
	 false, !IsLeader
	 
	 @return 
	*/
	public boolean WaitLeaderReady() {
		synchronized (this) {
			while (isLeader()) {
				if (getLeaderReadyEvent().WaitOne(0)) {
					return true;
				}
				Monitor.Wait(this);
			}
			return false;
		}
	}

	public void ResetLeaderReadyAfterChangeState() {
		synchronized (this) {
			getLeaderReadyEvent().Reset();
			Monitor.PulseAll(this);
		}
	}

	public void SetLeaderReady() {
		if (isLeader()) {
			getLeaderReadyEvent().Set();
			Monitor.PulseAll(this);

			getServer().Foreach((allsocket) -> {
						// 本来这个通告发给Agent(client)即可，
						// 但是现在没有区分是来自Raft的连接还是来自Agent，
						// 全部发送。
						// 另外Raft之间有两个连接，会收到多次，Raft不处理这个通告。
						// 由于Raft数量不多，不会造成大的浪费，不做处理了。
						var r = new LeaderIs();
						r.getArgument().setTerm(getLogSequence().getTerm());
						r.getArgument().setLeaderId(getLeaderId());
						r.Send(allsocket); // skip response.
			});
		}
	}

	private boolean IsLastLogUpToDate(long lastTerm, long lastIndex) {
		var last = getLogSequence().LastRaftLog();
		if (lastTerm > last.getTerm()) {
			return true;
		}
		if (lastTerm < last.getTerm()) {
			return false;
		}
		return lastIndex >= last.getIndex();
	}

	private int ProcessRequestVote(Protocol p) {
		synchronized (this) {
			var r = p instanceof RequestVote ? (RequestVote)p : null;
			if (getLogSequence().TrySetTerm(r.getArgument().getTerm())) {
				// new term found.
				ConvertStateTo(RaftState.Follower);
			}
			// else continue process

			r.getResult().setTerm(getLogSequence().getTerm());
			// RequestVote RPC
			// Receiver implementation:
			// 1.Reply false if term < currentTerm(§5.1)
			// 2.If votedFor is null or candidateId, and candidate's log is at
			// least as up - to - date as receiver's log, grant vote(§5.2, §5.4)
			r.getResult().setVoteGranted((r.getArgument().getTerm() >= getLogSequence().getTerm()) && getLogSequence().CanVoteFor(r.getArgument().getCandidateId()) && IsLastLogUpToDate(r.getArgument().getLastLogTerm(), r.getArgument().getLastLogIndex()));
			if (r.getResult().getVoteGranted()) {
				getLogSequence().SetVoteFor(r.getArgument().getCandidateId());
			}
			logger.Debug("{0}: VoteFor={1} Rpc={2}", getName(), getLogSequence().getVoteFor(), r);
			r.SendResultCode(0);

			return Procedure.Success;
		}
	}

	private int ProcessLeaderIs(Protocol p) {
		var r = p instanceof LeaderIs ? (LeaderIs)p : null;

		// 这个协议是发送给Agent(Client)的，
		// 为了简单，不做区分。
		// Raft也会收到，忽略。
		r.SendResultCode(0);

		return Procedure.Success;
	}

	private int ProcessRequestVoteResult(RequestVote rpc, Connector c) {
		synchronized (this) {
			if (getLogSequence().TrySetTerm(rpc.getResult().getTerm())) {
				// new term found
				ConvertStateTo(RaftState.Follower);
				return Procedure.Success;
			}
		}

//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (rpc.getResult().getVoteGranted() && VoteSuccess.TryAdd(c.getName(), c)) {
			synchronized (this) {
				if (getState() == RaftState.Candidate && VoteSuccess.size() >= getRaftConfig().getHalfCount()) {
					ConvertStateTo(RaftState.Leader);
				}
			}
		}
		return Procedure.Success;
	}

	private void SendRequestVote(SchedulerTask ThisTask) {
		synchronized (this) {
			VoteSuccess.clear(); // 每次选举开始清除。

			setLeaderId("");
			getLogSequence().SetVoteFor(getName()); // Vote Self First.
			getLogSequence().TrySetTerm(getLogSequence().getTerm() + 1);
			if (WaitMajorityVoteTimoutTask != null) {
				WaitMajorityVoteTimoutTask.Cancel();
			}
			WaitMajorityVoteTimoutTask = null;

			var arg = new RequestVoteArgument();
			arg.setTerm(getLogSequence().getTerm());
			arg.setCandidateId(getName());
			var log = getLogSequence().LastRaftLog();
			arg.setLastLogIndex(log.getIndex());
			arg.setLastLogTerm(log.getTerm());

			getServer().getConfig().ForEachConnector((c) -> {
						if (false == c.IsHandshakeDone) {
							return;
						}
						var rpc = new RequestVote();
						rpc.setArgument(arg);
						rpc.Send(c.Socket, (p) -> ProcessRequestVoteResult(rpc, c));
						logger.Debug("{0}: SendRequestVote {1}", getName(), rpc);
			});

			// 定时，如果超时选举还未完成，再次发起选举。
			WaitMajorityVoteTimoutTask = Scheduler.getInstance().Schedule((ThisTask) -> {
						synchronized (this) {
							StartRequestVoteDelayTask = null;
							ConvertStateTo(RaftState.Candidate);
						}
			}, getRaftConfig().getAppendEntriesTimeout() + 1000, -1);
		}
	}

	private void ConvertStateFromFollwerTo(RaftState newState) {
		switch (newState) {
			case Follower:
				logger.Info(String.format("RaftState %1$s: Follower->Follower", getName()));
				return;

			case Candidate:
				logger.Info(String.format("RaftState %1$s: Follower->Candidate", getName()));
				setState(RaftState.Candidate);
				if (LeaderLostTimerTask != null) {
					LeaderLostTimerTask.Cancel();
				}
				LeaderLostTimerTask = null;
				getLogSequence().SetVoteFor(""); // 先清除，在真正自荐前可以给别人投票。
				StartRequestVote();
				return;

			case Leader:
				// 并发的RequestVote的结果如果没有判断当前状态，可能会到达这里。
				// 不是什么大问题。see ProcessRequestVoteResult
				logger.Info(String.format("RaftState %1$s Impossible! Follower->Leader", getName()));
				return;
		}
	}

	private void StartLeaderLostTimerTask() {
		// 每次LeaderActive启动一个Timer会很精确，但需要创建很多Task。
		// 下面这种定时检测的方法在精度方面也可以。只是需要定时check。
		LeaderLostTimerTask = Scheduler.getInstance().Schedule((ThisTask) -> {
					var elapse = Time.getNowUnixMillis() - getLogSequence().getLeaderActiveTime();
					if (elapse > getRaftConfig().getLeaderLostTimeout()) {
						ConvertStateTo(RaftState.Candidate);
					}
		}, Util.Random.getInstance().nextInt(1000), 1000);
	}

	private void ConvertStateFromCandidateTo(RaftState newState) {
		switch (newState) {
			case Follower:
				logger.Info(String.format("RaftState %1$s: Candidate->Follower", getName()));
				setState(RaftState.Follower);
				VoteSuccess.clear(); // 选举结束清除。

				getLogSequence().SetVoteFor("");
				if (StartRequestVoteDelayTask != null) {
					StartRequestVoteDelayTask.Cancel();
				}
				StartRequestVoteDelayTask = null;
				if (WaitMajorityVoteTimoutTask != null) {
					WaitMajorityVoteTimoutTask.Cancel();
				}
				WaitMajorityVoteTimoutTask = null;
				StartLeaderLostTimerTask();
				return;

			case Candidate:
				logger.Info(String.format("RaftState %1$s: Candidate->Candidate", getName()));
				getLogSequence().SetVoteFor(""); // 先清除，在真正自荐前可以给别人投票。
				StartRequestVote();
				return;

			case Leader:
				if (StartRequestVoteDelayTask != null) {
					StartRequestVoteDelayTask.Cancel();
				}
				StartRequestVoteDelayTask = null;
				if (WaitMajorityVoteTimoutTask != null) {
					WaitMajorityVoteTimoutTask.Cancel();
				}
				WaitMajorityVoteTimoutTask = null;
				VoteSuccess.clear(); // 选举结束清除。

				logger.Info(String.format("RaftState %1$s: Candidate->Leader", getName()));
				setState(RaftState.Leader);
				getLogSequence().SetVoteFor("");
				setLeaderId(getName()); // set to self

				// (Reinitialized after election)
				var nextIndex = getLogSequence().getLastIndex() + 1;
				getServer().getConfig().ForEachConnector((c) -> {
							var cex = c instanceof Server.ConnectorEx ? (getServer().ConnectorEx)c : null;
							cex.setNextIndex(nextIndex);
							cex.setMatchIndex(0);
				});

				// Upon election:
				// send initial empty AppendEntries RPCs
				// (heartbeat)to each server; repeat during
				// idle periods to prevent election timeouts(§5.2)
				getLogSequence().AppendLog(new HeartbeatLog(HeartbeatLog.SetLeaderReadyEvent), false);
				HearbeatTimerTask = Scheduler.getInstance().Schedule((ThisTask) -> {
							var elapse = Util.Time.getNowUnixMillis() - getLogSequence().getAppendLogActiveTime();
							if (elapse < getRaftConfig().getLeaderHeartbeatTimer()) {
								getLogSequence().AppendLog(new HeartbeatLog(), false);
							}
				}, 1000, 1000);
				return;
		}
	}

	private void ConvertStateFromLeaderTo(RaftState newState) {
		switch (newState) {
			case Follower:
				logger.Info(String.format("RaftState %1$s: Leader->Follower", getName()));
				setState(RaftState.Follower);
				ResetLeaderReadyAfterChangeState();
				Monitor.PulseAll(this);

				if (HearbeatTimerTask != null) {
					HearbeatTimerTask.Cancel();
				}
				HearbeatTimerTask = null;

				StartLeaderLostTimerTask();
				return;

			case Candidate:
				logger.Error(String.format("RaftState %1$s Impossible! Leader->Candidate", getName()));
				return;

			case Leader:
				logger.Error(String.format("RaftState %1$s Impossible! Leader->Leader", getName()));
				return;
		}
	}

	public void ConvertStateTo(RaftState newState) {
		// 按真值表处理所有情况。
		switch (getState()) {
			case Follower:
				ConvertStateFromFollwerTo(newState);
				return;

			case Candidate:
				ConvertStateFromCandidateTo(newState);
				return;

			case Leader:
				ConvertStateFromLeaderTo(newState);
				return;
		}
	}

	private void StartRequestVote() {
		if (null != StartRequestVoteDelayTask) {
			return;
		}

		StartRequestVoteDelayTask = Scheduler.getInstance().Schedule(::SendRequestVote, Util.Random.getInstance().nextInt(getRaftConfig().getAppendEntriesTimeout() + 1000), -1);
	}

	private void RegisterInternalRpc() {
		getServer().AddFactoryHandle((new RequestVote()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new RequestVote(), Handle = ProcessRequestVote});

		getServer().AddFactoryHandle((new AppendEntries()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new AppendEntries(), Handle = ProcessAppendEntries});

		getServer().AddFactoryHandle((new InstallSnapshot()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new InstallSnapshot(), Handle = ProcessInstallSnapshot});

		getServer().AddFactoryHandle((new LeaderIs()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new LeaderIs(), Handle = ProcessLeaderIs});
	}

}