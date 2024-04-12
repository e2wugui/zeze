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
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Test {
	private static final Logger logger = LogManager.getLogger(Test.class);

	private String raftConfigFileName = "raft.xml";
	private final ConcurrentHashMap<String, TestRaft> rafts = new ConcurrentHashMap<>();
	private Agent agent;
	private final AtomicLong expectCount = new AtomicLong();
	private final LongHashMap<Long> errors = new LongHashMap<>();
	private Future<?> snapshotTimer;
	private final ArrayList<FailAction> failActions = new ArrayList<>();
	private boolean running = true;

	private static void logDump(String db) throws IOException, RocksDBException {
		RocksDB.loadLibrary();
		try (var r1 = RocksDB.openReadOnly(RocksDatabase.getCommonOptions(), Paths.get(db, "logs").toString())) {
			try (var it1 = r1.newIterator(RocksDatabase.getDefaultReadOptions())) {
				var StateMachine = new TestStateMachine();
				var snapshot = Paths.get(db, LogSequence.snapshotFileName).toString();
				if (new File(snapshot).isFile())
					StateMachine._loadSnapshot(snapshot);
				try (var dumpFile = new FileOutputStream(db + ".txt")) {
					dumpFile.write(String.format("SnapshotCount = %d\n", StateMachine.getCount()).getBytes(StandardCharsets.UTF_8));
					for (it1.seekToFirst(); it1.isValid(); it1.next()) {
						var l1 = RaftLog.decode(new Binary(it1.value()), StateMachine::logFactory);
						dumpFile.write(l1.toString().getBytes(StandardCharsets.UTF_8));
						dumpFile.write('\n');
					}
				}
			}
		}
	}

	public void run(String command, String[] args) throws InterruptedException {
		System.out.println(command);
		try {
			_run(command, args);
		} catch (Exception ex) {
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

	private void _run(String command, String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				raftConfigFileName = args[++i];
		}

		logger.debug("Start.");

		Thread.setDefaultUncaughtExceptionHandler(
				(t, e) -> logger.fatal("uncaught fatal exception for thread: {}", t.getName(), e));

		Task.initThreadPool(Task.newFixedThreadPool(10, "test"),
				Executors.newScheduledThreadPool(3, new ThreadFactoryWithName("test-sch")));
		var raftConfigStart = RaftConfig.load(raftConfigFileName);

		if (command.equals("RaftDump")) {
			for (var node : raftConfigStart.getNodes().values())
				logDump(String.format("%s_%d", node.getHost(), node.getPort()));
			return;
		}

		for (var node : raftConfigStart.getNodes().values()) {
			// every node need a private config-file.
			var confPath = Files.createTempFile("", ".xml");
			Files.copy(Paths.get(raftConfigStart.getXmlFileName()), confPath, StandardCopyOption.REPLACE_EXISTING);
			rafts.computeIfAbsent(node.getName(), nodeName -> new TestRaft(nodeName, confPath.toString()));
		}

		for (var raft : rafts.values())
			raft.raft.getServer().start();

		agent = new Agent("Zeze.Raft.Agent.Test", raftConfigStart);
		Agent.NetClient client = agent.getClient();
		client.AddFactoryHandle(AddCount.TypeId_, new Service.ProtocolFactoryHandle<>(AddCount::new));
		client.AddFactoryHandle(GetCount.TypeId_, new Service.ProtocolFactoryHandle<>(GetCount::new));
		client.start();

		Task.run(() -> {
			//noinspection InfiniteLoopStatement
			for (byte[] tmp = new byte[100]; ; ) {
				try {
					//noinspection ResultOfMethodCallIgnored
					System.in.read(tmp);
					var sb = new StringBuilder();
					sb.append(String.format("----------------------------------%s-----------------------------------\n",
							raftConfigStart.getXmlFileName()));
					for (var r : rafts.values()) {
						var l = r.raft != null ? r.raft.getLogSequence() : null;
						sb.append(String.format("%s CommitIndex=%s LastApplied=%s LastIndex=%s Count=%s", r.raftName,
								l != null ? l.getCommitIndex() : null,
								l != null ? l.getLastApplied() : null,
								l != null ? l.getLastIndex() : null,
								r.stateMachine != null ? r.stateMachine.count : null)).append("\n");
					}
					for (var f : failActions)
						sb.append(String.format("%s TestCount=%s", f.name, f.count)).append("\n");
					sb.append(String.format("----------------------------------%s-----------------------------------\n",
							raftConfigStart.getXmlFileName()));
					System.out.println(sb);
				} catch (Exception ex) {
					logger.error("Test._Run", ex);
				}
			}
		}, "DumpWorker", DispatchMode.Normal);
		try {
			runTrace();
		} finally {
			client.stop();
			if (snapshotTimer != null)
				snapshotTimer.cancel(false);
			for (var raft : rafts.values())
				raft.stopRaft();
		}
	}

	private long getCurrentCount() {
		while (true) {
			try {
				var r = new GetCount();
				agent.sendForWait(r).await();
				if (!r.isTimeout() && r.getResultCode() == 0)
					return r.Result.count;
			} catch (Exception ignored) {
			}
		}
	}

	private void errorsAdd(long resultCode) {
		if (resultCode == 0)
			return;
		if (resultCode == Procedure.RaftApplied)
			return;
		errors.compute(resultCode, (__, n) -> n != null ? n + 1 : 1);
	}

	private long errorsSum() {
		long sum = 0;
		for (var it = errors.iterator(); it.moveToNext(); )
			sum += it.value();
		return sum;
	}

	private String getErrorsString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, errors);
		return sb.toString();
	}

	private boolean checkCurrentCount(String stepName) {
		return checkCurrentCount(stepName, true);
	}

	private boolean checkCurrentCount(String stepName, boolean resetExpectCount) {
		var CurrentCount = getCurrentCount();
		var expectCount = this.expectCount.get();
		if (CurrentCount != expectCount) {
			var report = new StringBuilder();
			var level = expectCount != CurrentCount + errorsSum() ? Level.FATAL : Level.INFO;
			report.append(String.format("%n-------------------------------------------"));
			report.append(String.format("%n%s,Expect=%d,Now=%d,Errors=%s",
					stepName, expectCount, CurrentCount, getErrorsString()));
			report.append(String.format("%n-------------------------------------------"));
			logger.log(level, "{}", report.toString());

			if (resetExpectCount) {
				this.expectCount.getAndSet(CurrentCount); // 下一个测试重新开始。
				errors.clear();
			}
			return level == Level.INFO;
		}
		return true;
	}

	private int concurrentAddCount(String stepName, int concurrent) {
		var requests = new ArrayList<AddCount>();
		var tasks = new ArrayList<TaskCompletionSource<?>>();
		for (int i = 0; i < concurrent; ++i) {
			try {
				var req = new AddCount();
				req.setTimeout(3600_000);
				tasks.add(agent.sendForWait(req));
				// logger.debug("+++++++ {} new AddCount {}", i, req.getUnique().getRequestId());
				requests.add(req);
			} catch (Exception e) {
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
				errorsAdd(Procedure.Timeout);
			else
				errorsAdd(request.getResultCode());
		}
		return tasks.size();
	}

	@SuppressWarnings("EmptyMethod")
	private void setLogLevel(@SuppressWarnings("unused") Level level) {
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

	private void testConcurrent(String testName, int count) {
		expectCount.addAndGet(concurrentAddCount(testName, count));
		checkCurrentCount(testName);
	}

	private void randomSnapshotTimer() throws Exception {
		var randIndex = Random.getInstance().nextInt(rafts.size());
		var index = 0;
		for (var test : rafts.values()) {
			if (index++ == randIndex) {
				if (test.raft != null)
					test.raft.getLogSequence().snapshot();
				return;
			}
		}
	}

	public void runTrace() throws Exception {
		// 基本测试
		logger.debug("基本测试");
		agent.sendForWait(new AddCount()).await();
		expectCount.incrementAndGet();
		checkCurrentCount("TestAddCount");

		// 基本并发请求
		logger.debug("基本并发请求");
		setLogLevel(Level.INFO);
		testConcurrent("TestConcurrent", 200);
		setLogLevel(Level.TRACE);

		// 普通节点重启网络一。
		logger.debug("普通节点重启网络一");
		var NotLeaders = getNodeNotLeaders();
		if (!NotLeaders.isEmpty())
			NotLeaders.get(0).restartNet();
		testConcurrent("TestNormalNodeRestartNet1", 1);

		// 普通节点重启网络二。
		logger.debug("普通节点重启网络二");
		if (NotLeaders.size() > 1) {
			NotLeaders.get(0).restartNet();
			NotLeaders.get(1).restartNet();
		}
		testConcurrent("TestNormalNodeRestartNet2", 1);

		// Leader节点重启网络。
		logger.debug("Leader节点重启网络");
		getLeader().restartNet();
		testConcurrent("TestLeaderNodeRestartNet", 1);

		// Leader节点重启网络，【选举】。
		logger.debug("Leader节点重启网络，【选举】");
		{
			var leader = getLeader();
			leader.raft.getServer().stop();
			Task.schedule(leader.raft.getRaftConfig().getElectionTimeoutMax(), leader.raft.getServer()::start);
		}
		testConcurrent("TestLeaderNodeRestartNet_NewVote", 1);

		// 普通节点重启一。
		logger.debug("普通节点重启一");
		NotLeaders = getNodeNotLeaders();
		if (!NotLeaders.isEmpty()) {
			NotLeaders.get(0).stopRaft();
			NotLeaders.get(0).startRaft();
		}
		testConcurrent("TestNormalNodeRestartRaft1", 1);

		// 普通节点重启二。
		logger.debug("普通节点重启二");
		if (NotLeaders.size() > 1) {
			NotLeaders.get(0).stopRaft();
			NotLeaders.get(1).stopRaft();
			NotLeaders.get(0).startRaft();
			NotLeaders.get(1).startRaft();
		}
		testConcurrent("TestNormalNodeRestartRaft2", 1);

		// Leader节点重启。
		logger.debug("Leader节点重启");
		{
			var leader = getLeader();
			var StartDelay = leader.raft.getRaftConfig().getElectionTimeoutMax();
			leader.stopRaft();
			Task.schedule(StartDelay, leader::startRaft);
		}
		testConcurrent("TestLeaderNodeRestartRaft", 1);

		// Snapshot & Load
		{
			var leader = getLeader();
			leader.raft.getLogSequence().snapshot();
			leader.stopRaft();
			leader.startRaft();
		}

		// InstallSnapshot;

		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		snapshotTimer = Task.scheduleUnsafe(60 * 1000, 60 * 1000, this::randomSnapshotTimer);

		setLogLevel(Level.INFO);

		failActions.add(new FailAction("RestartNet1", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].restartNet();
		}));
		failActions.add(new FailAction("RestartNet2", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].restartNet();
			rafts[1].restartNet();
		}));
		failActions.add(new FailAction("RestartNet3", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].restartNet();
			rafts[1].restartNet();
			rafts[2].restartNet();
		}));
		failActions.add(new FailAction("RestartLeaderNetForVote", () ->
		{
			var leader = getLeader();
			leader.raft.getServer().stop();
			// delay for vote
			Thread.sleep(leader.raft.getRaftConfig().getElectionTimeoutMax());
			leader.raft.getServer().start();
		}));
		failActions.add(new FailAction("RestartRaft1", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].stopRaft();
			rafts[0].startRaft();
		}));
		failActions.add(new FailAction("RestartRaft2", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].stopRaft();
			rafts[1].stopRaft();
			rafts[0].startRaft();
			rafts[1].startRaft();
		}));
		failActions.add(new FailAction("RestartRaft3", () ->
		{
			var rafts = shuffleRafts();
			rafts[0].stopRaft();
			rafts[1].stopRaft();
			rafts[2].stopRaft();
			rafts[0].startRaft();
			rafts[1].startRaft();
			rafts[2].startRaft();
		}));
		failActions.add(new FailAction("RestartLeaderRaftForVote", () ->
		{
			var leader = getLeader();
			var startVoteDelay = leader.raft.getRaftConfig().getElectionTimeoutMax();
			leader.stopRaft();
			// delay for vote
			Thread.sleep(startVoteDelay);
			leader.startRaft();
		}));
		var InstallSnapshotCleanNode = rafts.values().iterator().next(); // 记住，否则Release版本，每次返回值可能会变。
		failActions.add(new FailAction("InstallSnapshot Clean One Node Data", () ->
		{
			for (var test : rafts.values()) {
				test.stopRaft(); // 先停止，这样才能强制启动安装。
				test.startRaft(test == InstallSnapshotCleanNode);
			}
		}));
		// Start Background FailActions
		Task.run(this::randomTriggerFailActions, "RandomTriggerFailActions", DispatchMode.Normal);
		var testName = "RealConcurrentDoRequest";
		var lastExpectCount = expectCount.get();
		while (true) {
			expectCount.addAndGet(concurrentAddCount(testName, 20));
			if (expectCount.get() - lastExpectCount > 20 * 5) {
				lastExpectCount = expectCount.get();
				if (!check(testName))
					break;
			}
		}
		running = false;
		setLogLevel(Level.DEBUG);
		checkCurrentCount("Final Check!!!");
	}

	private boolean check(String testName) throws InterruptedException {
		int tryCount = 2;
		for (int i = 0; i < tryCount; ++i) {
			var check = checkCurrentCount(testName, false);
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("Check={} Step={} ExpectCount={} Errors={}", check, i, expectCount.get(), getErrorsString());
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
		final String name;
		final Action0 action;
		long count;

		FailAction(String name, Action0 action) {
			this.name = name;
			this.action = action;
		}

		@Override
		public String toString() {
			return name + "=" + count;
		}
	}

	private void waitExpectCountGrow(@SuppressWarnings("SameParameterValue") long growing) throws InterruptedException {
		long oldTaskCount = expectCount.get();
		//noinspection ConditionalBreakInInfiniteLoop
		while (true) {
			//noinspection BusyWait
			Thread.sleep(10);
			if (expectCount.get() - oldTaskCount > growing)
				break;
		}
	}

	private void randomTriggerFailActions() throws Exception {
		while (running) {
			var fa = failActions.get(Random.getInstance().nextInt(failActions.size()));
			// for (var fa : FailActions)
			{
				logger.fatal("___________________________ {} _____________________________", fa.name);
				try {
					fa.action.run();
					fa.count++;
				} catch (Exception ex) {
					logger.error("FailAction {}", fa.name, ex);
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("Press [y] Enter To Exit.");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					System.out.println("___________________________________________");
					for (var raft : rafts.values()) {
						if (raft.raft != null && raft.raft.getLogSequence() != null) {
							raft.raft.isShutdown = true;
							raft.raft.getLogSequence().close();
						}
					}
					System.exit(-1);
					/*
					for (var raft : Rafts.Values)
					    raft.StartRaft(); // error recover
					// */
				}
				// 等待失败的节点恢复正常并且服务了一些请求。
				// 由于一个follower失败时，请求处理是能持续进行的，这个等待可能不够。
				waitExpectCountGrow(110);
			}
		}
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, failActions);
		logger.fatal("{}", sb.toString());
	}

	private TestRaft getLeader() throws InterruptedException {
		while (true) {
			for (var raft : rafts.values()) {
				if (raft.raft != null && raft.raft.isLeader())
					return raft;
			}
			//noinspection BusyWait
			Thread.sleep(1000);
		}
	}

	private ArrayList<TestRaft> getNodeNotLeaders() {
		var NotLeader = new ArrayList<TestRaft>();
		for (var raft : rafts.values()) {
			if (!raft.raft.isLeader())
				NotLeader.add(raft);
		}
		return NotLeader;
	}

	private TestRaft[] shuffleRafts() {
		return Random.shuffle(rafts.values().toArray(new TestRaft[rafts.size()]));
	}

	public static final class AddCount extends RaftRpc<EmptyBean, BCountResult> {
		public static final int ProtocolId_ = Bean.hash32(AddCount.class.getName());
		public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

		static {
			register(TypeId_, AddCount.class);
		}

		public AddCount() {
			Argument = EmptyBean.instance;
			Result = new BCountResult();
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

	public static final class BCountResult extends Bean {
		private long count;

		public long getCount() {
			return count;
		}

		public void setCount(long value) {
			count = value;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(count);
		}

		@Override
		public void decode(IByteBuffer bb) {
			count = bb.ReadLong();
		}

		@Override
		public String toString() {
			return "Count=" + count;
		}
	}

	public static final class GetCount extends RaftRpc<EmptyBean, BCountResult> {
		public static final int ProtocolId_ = Bean.hash32(GetCount.class.getName());
		public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

		static {
			register(TypeId_, GetCount.class);
		}

		public GetCount() {
			Argument = EmptyBean.instance;
			Result = new BCountResult();
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
		private long count;

		public TestStateMachine() {
			addFactory(AddCount.TypeId_, () -> new AddCount(null));
		}

		public long getCount() {
			return count;
		}

		public void setCount(long value) {
			count = value;
		}

		public void addCountAndWait(Test.AddCount req) {
			req.Result.count = count;
			getRaft().appendLog(new AddCount(req), req.Result);
		}

		public static final class AddCount extends Log {
			public static final int TypeId_ = Bean.hash32(AddCount.class.getName());

			@Override
			public long typeId() {
				return TypeId_;
			}

			public AddCount(IRaftRpc req) {
				super(req);
			}

			@Override
			public void apply(RaftLog holder, StateMachine stateMachine) {
				TestStateMachine tsm = (TestStateMachine)stateMachine;
				tsm.setCount(tsm.getCount() + 1);
				// logger.info("--- Apply {}:{} to {}", stateMachine.getRaft().getName(), getUnique().getRequestId(), tsm.getCount());
			}

			@Override
			public void encode(ByteBuffer bb) {
				super.encode(bb);
			}

			@Override
			public void decode(IByteBuffer bb) {
				super.decode(bb);
			}
		}

		public void _loadSnapshot(String path) throws IOException {
			try (var file = new FileInputStream(path)) {
				var bytes = new byte[1024];
				int rc = file.read(bytes);
				count = ByteBuffer.Wrap(bytes, rc).ReadLong();
			}
		}

		@Override
		public void loadSnapshot(String path) throws IOException {
			_loadSnapshot(path);
			logger.info("{} LoadSnapshot Count={}", getRaft().getName(), count);
		}

		// 这里没有处理重入，调用者需要保证。
		@Override
		public SnapshotResult snapshot(String path) throws IOException, RocksDBException {
			SnapshotResult result = new SnapshotResult();
			getRaft().lock();
			try {
				if (getRaft().getLogSequence() != null) {
					var lastAppliedLog = getRaft().getLogSequence().lastAppliedLogTermIndex();
					if (lastAppliedLog != null) {
						result.lastIncludedIndex = lastAppliedLog.getIndex();
						result.lastIncludedTerm = lastAppliedLog.getTerm();
						var bb = ByteBuffer.Allocate();
						logger.info("{} Snapshot Count={}", getRaft().getName(), count);
						bb.WriteLong(count);
						try (var file = new FileOutputStream(path)) {
							file.write(bb.Bytes, bb.ReadIndex, bb.size());
						}
						getRaft().getLogSequence().commitSnapshot(path, result.lastIncludedIndex);
						result.success = true;
					}
				}
			} finally {
				getRaft().unlock();
			}
			return result;
		}
	}

	public static class TestRaft extends ReentrantLock {
		private Raft raft;
		private TestStateMachine stateMachine;
		private final String raftConfigFileName;
		private final String raftName;

		public TestRaft(String raftName, String raftConfigFileName) {
			this.raftName = raftName;
			this.raftConfigFileName = raftConfigFileName;
			try {
				startRaft(true);
			} catch (Exception e) {
				logger.error("TestRaft.TestRaft", e);
			}
		}

		public Raft getRaft() {
			return raft;
		}

		public TestStateMachine getStateMachine() {
			return stateMachine;
		}

		public String getRaftConfigFileName() {
			return raftConfigFileName;
		}

		public String getRaftName() {
			return raftName;
		}

		public void restartNet() throws Exception {
			logger.debug("Raft.Net {} Restart ...", raftName);
			try {
				if (raft != null)
					raft.getServer().stop();
				if (raft != null) {
					for (int i = 0; ; ) {
						try {
							raft.getServer().start();
							break;
						} catch (Exception e) {
							if (!(e instanceof BindException) && !(e.getCause() instanceof BindException) || ++i > 30)
								throw e;
							//noinspection BusyWait
							Thread.sleep(100); // 稍等一小会,上个ServerSocket还没真正关闭
						}
					}
				}
			} catch (Exception e) {
				logger.error("RestartNet exception", e);
				throw e;
			}
		}

		public void stopRaft() throws Exception {
			lock();
			try {
				logger.debug("Raft {} Stop ...", raftName);
				// 在同一个进程中，没法模拟进程退出，
				// 此时RocksDb应该需要关闭，否则重启会失败吧。
				if (raft != null) {
					raft.shutdown();
					raft = null;
				}
			} finally {
				unlock();
			}
		}

		public void startRaft() throws Exception {
			startRaft(false);
		}

		public void startRaft(boolean resetLog) throws Exception {
			lock();
			try {
				if (raft != null) {
					raft.getServer().start();
					return;
				}
				logger.debug("Raft {} Start ...", raftName);
				stateMachine = new TestStateMachine();

				var raftConfig = RaftConfig.load(raftConfigFileName);
				raftConfig.setUniqueRequestExpiredDays(1);
				raftConfig.setDbHome(Paths.get(raftName.replace(':', '_')).toString());
				if (resetLog) {
					logger.warn("------------------------------------------------");
					logger.warn("- Reset Log {} -", raftConfig.getDbHome());
					logger.warn("------------------------------------------------");
					// 只删除日志相关数据库。保留重复请求数据库。
					LogSequence.deletedDirectoryAndCheck(new File(raftConfig.getDbHome(), "logs"));
					LogSequence.deletedDirectoryAndCheck(new File(raftConfig.getDbHome(), "rafts"));
					LogSequence.deletedDirectoryAndCheck(new File(raftConfig.getDbHome(), "snapshot.dat"));
				}
				Files.createDirectories(Paths.get(raftConfig.getDbHome()));

				raft = new Raft(stateMachine, raftName, raftConfig);
				raft.getLogSequence().setWriteOptions(RocksDatabase.getDefaultWriteOptions());
				raft.getServer().AddFactoryHandle(AddCount.TypeId_,
						new Service.ProtocolFactoryHandle<>(AddCount::new, this::processAddCount));
				raft.getServer().AddFactoryHandle(GetCount.TypeId_,
						new Service.ProtocolFactoryHandle<>(GetCount::new, this::processGetCount));
				raft.getServer().start();
			} finally {
				unlock();
			}
		}

		private long processAddCount(AddCount r) {
			if (!raft.isLeader())
				return Procedure.RaftRetry; // fast fail

			TestStateMachine sm = stateMachine;
			sm.lock();
			try {
				sm.addCountAndWait(r);
				r.SendResultCode(0);
			} finally {
				sm.unlock();
			}
			return Procedure.Success;
		}

		@SuppressWarnings("SameReturnValue")
		private long processGetCount(GetCount r) {
			stateMachine.lock();
			try {
				r.Result.count = stateMachine.count;
				r.SendResult();
			} finally {
				stateMachine.unlock();
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
			new Test().run(command, args);
			break;
		default:
			System.out.println("java Zeze.Raft.Test -c RaftTest -Config raft.xml");
			System.out.println("java Zeze.Raft.Test -c RaftDump -Config raft.xml");
		}
	}
}
