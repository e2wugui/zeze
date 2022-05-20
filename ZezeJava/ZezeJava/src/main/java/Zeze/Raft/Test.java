package Zeze.Raft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Record;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;
import Zeze.Util.Random;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Test {
	private static final Logger logger = LogManager.getLogger(Test.class);

	private String RaftConfigFileName = "raft.xml";
	private final ConcurrentHashMap<String, TestRaft> Rafts = new ConcurrentHashMap<>();
	private Agent Agent;
	private final AtomicLong ExpectCount = new AtomicLong();
	private final LongHashMap<Long> Errors = new LongHashMap<>();
	private Future<?> SnapshotTimer;
	private final ArrayList<FailAction> FailActions = new ArrayList<>();
	private boolean Running = true;

	private void LogDump(String db) throws IOException, RocksDBException {
		RocksDB.loadLibrary();
		var options = new Options().setCreateIfMissing(true);
		try (var r1 = RocksDB.open(options, Paths.get(db, "logs").toString())) {
			try (var it1 = r1.newIterator()) {
				it1.seekToFirst();
				var StateMachine = new TestStateMachine();
				var snapshot = Paths.get(db, LogSequence.SnapshotFileName).toString();
				if (new File(snapshot).isFile())
					StateMachine._LoadSnapshot(snapshot);
				try (var dumpFile = new FileOutputStream(db + ".txt")) {
					dumpFile.write(String.format("SnapshotCount = %d\n", StateMachine.getCount()).getBytes(StandardCharsets.UTF_8));
					while (it1.isValid()) {
						var l1 = RaftLog.Decode(new Binary(it1.value()), StateMachine::LogFactory);
						dumpFile.write(l1.toString().getBytes(StandardCharsets.UTF_8));
						dumpFile.write('\n');
						it1.next();
					}
				}
			}
		}
	}

	public void Run(String command, String[] args) throws InterruptedException {
		System.out.println(command);
		try {
			_Run(command, args);
		} catch (Throwable ex) {
			logger.error("Run", ex);
		}
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("Press [Ctrl+c] Enter To Exit.");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		System.out.println("___________________________________________");
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}

	private void _Run(String command, String[] args) throws Throwable {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				RaftConfigFileName = args[++i];
		}

		logger.debug("Start.");

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				logger.error("uncaught fatal exception for thread: " + t.getName(), e);
			} catch (Throwable ex) {
				ex.printStackTrace();
			} finally {
				e.printStackTrace();
			}
		});

		Task.initThreadPool(Task.newFixedThreadPool(10, "test"),
				Executors.newScheduledThreadPool(3, new ThreadFactoryWithName("test-sch")));
		var raftConfigStart = RaftConfig.Load(RaftConfigFileName);

		if (command.equals("RaftDump")) {
			for (var node : raftConfigStart.getNodes().values())
				LogDump(String.format("%s_%d", node.getHost(), node.getPort()));
			return;
		}

		for (var node : raftConfigStart.getNodes().values()) {
			// every node need a private config-file.
			var confPath = Files.createTempFile("", ".xml");
			Files.copy(Paths.get(raftConfigStart.getXmlFileName()), confPath, StandardCopyOption.REPLACE_EXISTING);
			Rafts.computeIfAbsent(node.getName(), nodeName-> new TestRaft(nodeName, confPath.toString()));
		}

		for (var raft : Rafts.values())
			raft.Raft.getServer().Start();

		Agent = new Agent("Zeze.Raft.Agent.Test", raftConfigStart);
		Zeze.Raft.Agent.NetClient client = Agent.getClient();
		client.AddFactoryHandle(AddCount.TypeId_, new Service.ProtocolFactoryHandle<>(AddCount::new));
		client.AddFactoryHandle(GetCount.TypeId_, new Service.ProtocolFactoryHandle<>(GetCount::new));
		client.Start();

		Task.run(() -> {
			//noinspection InfiniteLoopStatement
			for (byte[] tmp = new byte[100]; ; ) {
				try {
					//noinspection ResultOfMethodCallIgnored
					System.in.read(tmp);
					var sb = new StringBuilder();
					sb.append(String.format("----------------------------------%s-----------------------------------\n",
							raftConfigStart.getXmlFileName()));
					for (var r : Rafts.values()) {
						var l = r.Raft != null ? r.Raft.getLogSequence() : null;
						sb.append(String.format("%s CommitIndex=%s LastApplied=%s LastIndex=%s Count=%s", r.RaftName,
								l != null ? l.getCommitIndex() : null,
								l != null ? l.getLastApplied() : null,
								l != null ? l.getLastIndex() : null,
								r.StateMachine != null ? r.StateMachine.Count : null)).append("\n");
					}
					for (var f : FailActions)
						sb.append(String.format("%s TestCount=%s", f.Name, f.Count)).append("\n");
					sb.append(String.format("----------------------------------%s-----------------------------------\n",
							raftConfigStart.getXmlFileName()));
					System.out.println(sb);
				} catch (Exception ex) {
					logger.error("Test._Run", ex);
				}
			}
		}, "DumpWorker");
		try {
			RunTrace();
		} finally {
			client.Stop();
			if (SnapshotTimer != null)
				SnapshotTimer.cancel(false);
			for (var raft : Rafts.values())
				raft.StopRaft();
		}
	}

	private long GetCurrentCount() {
		while (true) {
			try {
				var r = new GetCount();
				Agent.SendForWait(r).await();
				if (!r.isTimeout() && r.getResultCode() == 0)
					return r.Result.Count;
			} catch (Exception ignored) {
			}
		}
	}

	private void ErrorsAdd(long resultCode) {
		if (resultCode == 0)
			return;
		if (resultCode == Procedure.RaftApplied)
			return;
		if (Errors.containsKey(resultCode))
			Errors.put(resultCode, Errors.get(resultCode) + 1);
		else
			Errors.put(resultCode, 1L);
	}

	private long ErrorsSum() {
		long sum = 0;
		for (var it = Errors.iterator(); it.moveToNext(); )
			sum += it.value();
		return sum;
	}

	private String GetErrorsString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, Errors);
		return sb.toString();
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean CheckCurrentCount(String stepName) {
		return CheckCurrentCount(stepName, true);
	}

	private boolean CheckCurrentCount(String stepName, boolean resetExpectCount) {
		var CurrentCount = GetCurrentCount();
		var expectCount = ExpectCount.get();
		if (CurrentCount != expectCount) {
			var report = new StringBuilder();
			var level = expectCount != CurrentCount + ErrorsSum() ? Level.FATAL : Level.INFO;
			report.append(String.format("%n-------------------------------------------"));
			report.append(String.format("%n%s,Expect=%d,Now=%d,Errors=%s",
					stepName, expectCount, CurrentCount, GetErrorsString()));
			report.append(String.format("%n-------------------------------------------"));
			logger.log(level, report.toString());

			if (resetExpectCount) {
				ExpectCount.getAndSet(CurrentCount); // 下一个测试重新开始。
				Errors.clear();
			}
			return level == Level.INFO;
		}
		return true;
	}

	private int ConcurrentAddCount(String stepName, int concurrent) {
		var requests = new ArrayList<AddCount>();
		var tasks = new ArrayList<TaskCompletionSource<?>>();
		for (int i = 0; i < concurrent; ++i) {
			try {
				var req = new AddCount();
				tasks.add(Agent.SendForWait(req));
				// logger.debug("+++++++ {} new AddCount {}", i, req.getUnique().getRequestId());
				requests.add(req);
			} catch (RuntimeException e) {
				//发送错误不统计。ErrorsAdd(Procedure.ErrorSendFail);
			}
			//logger.Debug("+++++++++ REQUEST {0} {1}", stepName, requests[i]);
		}
		// int i = 0;
		for (TaskCompletionSource<?> task : tasks) {
			// logger.debug("+++++++ {} wait", i++);
			task.await();
		}
		// logger.debug("+++++++ finish");
		for (var request : requests) {
			logger.debug("--------- RESPONSE {} {}", stepName, request);
			if (request.isTimeout())
				ErrorsAdd(Procedure.Timeout);
			else
				ErrorsAdd(request.getResultCode());
		}
		return tasks.size();
	}

	@SuppressWarnings("EmptyMethod")
	private void SetLogLevel(@SuppressWarnings("unused") Level level) {
		// LogManager.GlobalThreshold = level;
		/*
		foreach (var rule in NLog.LogManager.Configuration.LoggingRules)
		{
		    //Console.WriteLine($"================ SetLoggingLevels {rule.RuleName}");
		    rule.DisableLoggingForLevels(NLog.LogLevel.Trace, NLog.LogLevel.Fatal);
		    rule.EnableLoggingForLevels(level, NLog.LogLevel.Fatal);
		    //rule.SetLoggingLevels(level, NLog.LogLevel.Fatal);
		}
		*/
	}

	private void TestConcurrent(String testName, int count) {
		ExpectCount.addAndGet(ConcurrentAddCount(testName, count));
		CheckCurrentCount(testName);
	}

	private void RandomSnapshotTimer() throws Throwable {
		var randIndex = Random.getInstance().nextInt(Rafts.size());
		var index = 0;
		for (var test : Rafts.values()) {
			if (index++ == randIndex) {
				if (test.Raft != null)
					test.Raft.getLogSequence().Snapshot(false);
				return;
			}
		}
	}

	public void RunTrace() throws Throwable {
		// 基本测试
		logger.debug("基本测试");
		Agent.SendForWait(new AddCount()).await();
		ExpectCount.incrementAndGet();
		CheckCurrentCount("TestAddCount");

		// 基本并发请求
		logger.debug("基本并发请求");
		SetLogLevel(Level.INFO);
		TestConcurrent("TestConcurrent", 200);
		SetLogLevel(Level.TRACE);

		// 普通节点重启网络一。
		logger.debug("普通节点重启网络一");
		var NotLeaders = GetNodeNotLeaders();
		if (!NotLeaders.isEmpty())
			NotLeaders.get(0).RestartNet();
		TestConcurrent("TestNormalNodeRestartNet1", 1);

		// 普通节点重启网络二。
		logger.debug("普通节点重启网络二");
		if (NotLeaders.size() > 1) {
			NotLeaders.get(0).RestartNet();
			NotLeaders.get(1).RestartNet();
		}
		TestConcurrent("TestNormalNodeRestartNet2", 1);

		// Leader节点重启网络。
		logger.debug("Leader节点重启网络");
		GetLeader().RestartNet();
		TestConcurrent("TestLeaderNodeRestartNet", 1);

		// Leader节点重启网络，【选举】。
		logger.debug("Leader节点重启网络，【选举】");
		{
			var leader = GetLeader();
			leader.Raft.getServer().Stop();
			Task.schedule(leader.Raft.getRaftConfig().getElectionTimeoutMax(), leader.Raft.getServer()::Start);
		}
		TestConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

		// 普通节点重启一。
		logger.debug("普通节点重启一");
		NotLeaders = GetNodeNotLeaders();
		if (!NotLeaders.isEmpty()) {
			NotLeaders.get(0).StopRaft();
			NotLeaders.get(0).StartRaft();
		}
		TestConcurrent("TestNormalNodeRestartRaft1", 1);

		// 普通节点重启二。
		logger.debug("普通节点重启二");
		if (NotLeaders.size() > 1) {
			NotLeaders.get(0).StopRaft();
			NotLeaders.get(1).StopRaft();
			NotLeaders.get(0).StartRaft();
			NotLeaders.get(1).StartRaft();
		}
		TestConcurrent("TestNormalNodeRestartRaft2", 1);

		// Leader节点重启。
		logger.debug("Leader节点重启");
		{
			var leader = GetLeader();
			var StartDelay = leader.Raft.getRaftConfig().getElectionTimeoutMax();
			leader.StopRaft();
			Task.schedule(StartDelay, () -> leader.StartRaft());
		}
		TestConcurrent("TestLeaderNodeRestartRaft", 1);

		// Snapshot & Load
		{
			var leader = GetLeader();
			leader.Raft.getLogSequence().Snapshot();
			leader.StopRaft();
			leader.StartRaft();
		}

		// InstallSnapshot;

		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		SnapshotTimer = Task.schedule(60 * 1000, 60 * 1000, this::RandomSnapshotTimer);

		SetLogLevel(Level.INFO);

		FailActions.add(new FailAction("RestartNet1", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].RestartNet();
		}));
		FailActions.add(new FailAction("RestartNet2", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].RestartNet();
			rafts[1].RestartNet();
		}));
		FailActions.add(new FailAction("RestartNet3", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].RestartNet();
			rafts[1].RestartNet();
			rafts[2].RestartNet();
		}));
		FailActions.add(new FailAction("RestartLeaderNetForVote", () ->
		{
			var leader = GetLeader();
			leader.Raft.getServer().Stop();
			// delay for vote
			Thread.sleep(leader.Raft.getRaftConfig().getElectionTimeoutMax());
			leader.Raft.getServer().Start();
		}));
		FailActions.add(new FailAction("RestartRaft1", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].StopRaft();
			rafts[0].StartRaft();
		}));
		FailActions.add(new FailAction("RestartRaft2", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].StopRaft();
			rafts[1].StopRaft();
			rafts[0].StartRaft();
			rafts[1].StartRaft();
		}));
		FailActions.add(new FailAction("RestartRaft3", () ->
		{
			var rafts = ShuffleRafts();
			rafts[0].StopRaft();
			rafts[1].StopRaft();
			rafts[2].StopRaft();
			rafts[0].StartRaft();
			rafts[1].StartRaft();
			rafts[2].StartRaft();
		}));
		FailActions.add(new FailAction("RestartLeaderRaftForVote", () ->
		{
			var leader = GetLeader();
			var startVoteDelay = leader.Raft.getRaftConfig().getElectionTimeoutMax();
			leader.StopRaft();
			// delay for vote
			Thread.sleep(startVoteDelay);
			leader.StartRaft();
		}));
		var InstallSnapshotCleanNode = Rafts.values().iterator().next(); // 记住，否则Release版本，每次返回值可能会变。
		FailActions.add(new FailAction("InstallSnapshot Clean One Node Data", () ->
		{
			for (var test : Rafts.values()) {
				test.StopRaft(); // 先停止，这样才能强制启动安装。
				test.StartRaft(test == InstallSnapshotCleanNode);
			}
		}));
		// Start Background FailActions
		Task.run(this::RandomTriggerFailActions, "RandomTriggerFailActions");
		var testName = "RealConcurrentDoRequest";
		var lastExpectCount = ExpectCount.get();
		while (true) {
			ExpectCount.addAndGet(ConcurrentAddCount(testName, 20));
			if (ExpectCount.get() - lastExpectCount > 20 * 5) {
				lastExpectCount = ExpectCount.get();
				if (!Check(testName))
					break;
			}
		}
		Running = false;
		SetLogLevel(Level.DEBUG);
		CheckCurrentCount("Final Check!!!");
	}

	private boolean Check(String testName) throws InterruptedException {
		int tryCount = 2;
		for (int i = 0; i < tryCount; ++i) {
			var check = CheckCurrentCount(testName, false);
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("Check={} Step={} ExpectCount={} Errors={}", check, i, ExpectCount.get(), GetErrorsString());
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			if (check)
				return true;
			if (i < tryCount - 1)
				Thread.sleep(10000);
		}
		return false;
	}

	private static class FailAction {
		final String Name;
		final Action0 Action;
		long Count;

		FailAction(String name, Action0 action) {
			Name = name;
			Action = action;
		}

		@Override
		public String toString() {
			return Name + "=" + Count;
		}
	}

	private void WaitExpectCountGrow(@SuppressWarnings("SameParameterValue") long growing) throws InterruptedException {
		long oldTaskCount = ExpectCount.get();
		//noinspection ConditionalBreakInInfiniteLoop
		while (true) {
			//noinspection BusyWait
			Thread.sleep(10);
			if (ExpectCount.get() - oldTaskCount > growing)
				break;
		}
	}

	private void RandomTriggerFailActions() throws InterruptedException {
		while (Running) {
			var fa = FailActions.get(Random.getInstance().nextInt(FailActions.size()));
			// for (var fa : FailActions)
			{
				logger.fatal("___________________________ {} _____________________________", fa.Name);
				try {
					fa.Action.run();
					fa.Count++;
				} catch (Throwable ex) {
					logger.error("FailAction " + fa.Name, ex);
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("Press [y] Enter To Exit.");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					for (var raft : Rafts.values()) {
						if (raft.Raft != null && raft.Raft.getLogSequence() != null) {
							raft.Raft.IsShutdown = true;
							raft.Raft.getLogSequence().Close();
						}
					}
					LogManager.shutdown();
					System.exit(-1);
					/*
					for (var raft : Rafts.Values)
					    raft.StartRaft(); // error recover
					// */
				}
				// 等待失败的节点恢复正常并且服务了一些请求。
				// 由于一个follower失败时，请求处理是能持续进行的，这个等待可能不够。
				WaitExpectCountGrow(110);
			}
		}
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, FailActions);
		logger.fatal("{}", sb.toString());
	}

	private TestRaft GetLeader() throws InterruptedException {
		while (true) {
			for (var raft : Rafts.values()) {
				if (raft.Raft != null && raft.Raft.isLeader())
					return raft;
			}
			//noinspection BusyWait
			Thread.sleep(1000);
		}
	}

	private ArrayList<TestRaft> GetNodeNotLeaders() {
		var NotLeader = new ArrayList<TestRaft>();
		for (var raft : Rafts.values()) {
			if (!raft.Raft.isLeader())
				NotLeader.add(raft);
		}
		return NotLeader;
	}

	private TestRaft[] ShuffleRafts() {
		return Random.Shuffle(Rafts.values().toArray(new TestRaft[Rafts.size()]));
	}

	public static final class AddCount extends RaftRpc<EmptyBean, CountResult> {
		public static final int ProtocolId_ = Bean.Hash32(AddCount.class.getName());
		public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

		public AddCount() {
			Argument = new EmptyBean();
			Result = new CountResult();
		}

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public static final class CountResult extends Bean {
		private long Count;

		public long getCount() {
			return Count;
		}

		public void setCount(long value) {
			Count = value;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteLong(Count);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			Count = bb.ReadLong();
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "Count=" + Count;
		}
	}

	public static final class GetCount extends RaftRpc<EmptyBean, CountResult> {
		public static final int ProtocolId_ = Bean.Hash32(GetCount.class.getName());
		public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

		public GetCount() {
			Argument = new EmptyBean();
			Result = new CountResult();
		}

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public static class TestStateMachine extends StateMachine {
		private long Count;

		public TestStateMachine() {
			AddFactory(new AddCount(null).getTypeId(), () -> new AddCount(null));
		}

		public long getCount() {
			return Count;
		}

		public void setCount(long value) {
			Count = value;
		}

		public void AddCountAndWait(Test.AddCount req) {
			req.Result.Count = Count;
			getRaft().AppendLog(new AddCount(req), req.Result);
		}

		public static final class AddCount extends Log {
			public AddCount(IRaftRpc req) {
				super(req);
			}

			@Override
			public void Apply(RaftLog holder, StateMachine stateMachine) {
				TestStateMachine tsm = (TestStateMachine)stateMachine;
				tsm.setCount(tsm.getCount() + 1);
				// logger.info("--- Apply {}:{} to {}", stateMachine.getRaft().getName(), getUnique().getRequestId(), tsm.getCount());
			}

			@Override
			public void Encode(ByteBuffer bb) {
				super.Encode(bb);
			}

			@Override
			public void Decode(ByteBuffer bb) {
				super.Decode(bb);
			}
		}

		public void _LoadSnapshot(String path) throws IOException {
			try (var file = new FileInputStream(path)) {
				var bytes = new byte[1024];
				int rc = file.read(bytes);
				Count = ByteBuffer.Wrap(bytes, 0, rc).ReadLong();
			}
		}

		@Override
		public void LoadSnapshot(String path) throws IOException {
			synchronized (getRaft()) {
				_LoadSnapshot(path);
				logger.info("{} LoadSnapshot Count={}", getRaft().getName(), Count);
			}
		}

		// 这里没有处理重入，调用者需要保证。
		@Override
		public SnapshotResult Snapshot(String path) throws IOException, RocksDBException {
			SnapshotResult result = new SnapshotResult();
			synchronized (getRaft()) {
				if (getRaft().getLogSequence() != null) {
					var lastAppliedLog = getRaft().getLogSequence().LastAppliedLogTermIndex();
					if (lastAppliedLog != null) {
						result.LastIncludedIndex = lastAppliedLog.getIndex();
						result.LastIncludedTerm = lastAppliedLog.getTerm();
						var bb = ByteBuffer.Allocate();
						logger.info("{} Snapshot Count={}", getRaft().getName(), Count);
						bb.WriteLong(Count);
						try (var file = new FileOutputStream(path)) {
							file.write(bb.Bytes, bb.ReadIndex, bb.Size());
						}
						getRaft().getLogSequence().CommitSnapshot(path, result.LastIncludedIndex);
						result.success = true;
					}
				}
			}
			return result;
		}
	}

	public static class TestRaft {
		private Raft Raft;
		private TestStateMachine StateMachine;
		private final String RaftConfigFileName;
		private final String RaftName;

		public TestRaft(String raftName, String raftConfigFileName) {
			RaftName = raftName;
			RaftConfigFileName = raftConfigFileName;
			try {
				StartRaft(true);
			} catch (Throwable e) {
				logger.error("TestRaft.TestRaft", e);
			}
		}

		public Raft getRaft() {
			return Raft;
		}

		public TestStateMachine getStateMachine() {
			return StateMachine;
		}

		public String getRaftConfigFileName() {
			return RaftConfigFileName;
		}

		public String getRaftName() {
			return RaftName;
		}

		public void RestartNet() throws Throwable {
			logger.debug("Raft.Net {} Restart ...", RaftName);
			try {
				if (Raft != null)
					Raft.getServer().Stop();
				if (Raft != null) {
					for (int i = 0; ; ) {
						try {
							Raft.getServer().Start();
							break;
						} catch (BindException | RuntimeException be) {
							if (!(be instanceof BindException) && !(be.getCause() instanceof BindException) || ++i > 30)
								throw be;
							//noinspection BusyWait
							Thread.sleep(100); // 稍等一小会,上个ServerSocket还没真正关闭
						}
					}
				}
			} catch (Throwable e) {
				logger.error("RestartNet exception", e);
				throw e;
			}
		}

		public void StopRaft() throws Throwable {
			synchronized (this) {
				logger.debug("Raft {} Stop ...", RaftName);
				// 在同一个进程中，没法模拟进程退出，
				// 此时RocksDb应该需要关闭，否则重启会失败吧。
				if (Raft != null) {
					Raft.Shutdown();
					Raft = null;
				}
			}
		}

		public void StartRaft() throws Throwable {
			StartRaft(false);
		}

		public void StartRaft(boolean resetLog) throws Throwable {
			synchronized (this) {
				if (Raft != null) {
					Raft.getServer().Start();
					return;
				}
				logger.debug("Raft {} Start ...", RaftName);
				StateMachine = new TestStateMachine();

				var raftConfig = RaftConfig.Load(RaftConfigFileName);
				raftConfig.setSnapshotMinLogCount(10);
				raftConfig.setUniqueRequestExpiredDays(1);
				raftConfig.setDbHome(Paths.get(RaftName.replace(':', '_')).toString());
				if (resetLog) {
					logger.warn("------------------------------------------------");
					logger.warn("- Reset Log {} -", raftConfig.getDbHome());
					logger.warn("------------------------------------------------");
					// 只删除日志相关数据库。保留重复请求数据库。
					var logsDir = Paths.get(raftConfig.getDbHome(), "logs").toString();
					if (new File(logsDir).isDirectory())
						LogSequence.deleteDirectory(new File(logsDir));
					var raftsDir = Paths.get(raftConfig.getDbHome(), "rafts").toString();
					if (new File(raftsDir).isDirectory())
						LogSequence.deleteDirectory(new File(raftsDir));
					var snapshotFile = Paths.get(raftConfig.getDbHome(), "snapshot.dat").toString();
					if ((new File(snapshotFile)).isFile())
						//noinspection ResultOfMethodCallIgnored
						(new File(snapshotFile)).delete();
				}
				Files.createDirectories(Paths.get(raftConfig.getDbHome()));

				Raft = new Raft(StateMachine, RaftName, raftConfig);
				Raft.getLogSequence().getWriteOptions().setSync(false);
				Raft.getServer().AddFactoryHandle(AddCount.TypeId_,
						new Service.ProtocolFactoryHandle<>(AddCount::new, this::ProcessAddCount));
				Raft.getServer().AddFactoryHandle(GetCount.TypeId_,
						new Service.ProtocolFactoryHandle<>(GetCount::new, this::ProcessGetCount));
				Raft.getServer().Start();
			}
		}

		private long ProcessAddCount(AddCount r) {
			if (!Raft.isLeader())
				return Procedure.RaftRetry; // fast fail

			TestStateMachine sm = StateMachine;
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (sm) {
				sm.AddCountAndWait(r);
				r.SendResultCode(0);
			}
			return Procedure.Success;
		}

		@SuppressWarnings("SameReturnValue")
		private long ProcessGetCount(GetCount r) {
			//noinspection SynchronizeOnNonFinalField
			synchronized (StateMachine) {
				r.Result.Count = StateMachine.Count;
				r.SendResult();
			}
			return Procedure.Success;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		String command = "";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-c"))
				command = args[++i];
		}
		switch (command) {
		case "RaftTest":
		case "RaftDump":
			new Test().Run(command, args);
			break;
		default:
			System.out.println("java Zeze.Raft.Test -c RaftTest -Config raft.xml");
			System.out.println("java Zeze.Raft.Test -c RaftDump -Config raft.xml");
		}
	}
}
