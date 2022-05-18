package GlobalRaft;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerWithRaft;
import Zeze.Services.ServiceManagerServer;
import Zeze.Transaction.Procedure;
import Zeze.Util.Action0;
import Zeze.Util.LongHashMap;
import Zeze.Util.Random;
import Zeze.Util.Task;
import demo.App;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Raft.StateMachine;

public class TestGlobalCacheMgrWithRaft {
	private static final Logger logger = LogManager.getLogger(TestGlobalCacheMgrWithRaft.class);

	private final ConcurrentHashMap<String, TestGlobalRaft> GlobalRafts = new ConcurrentHashMap<>();

	private final AtomicLong ExpectCount = new AtomicLong();

	private static String ConfigFileName = "global.raft.xml";

	private final App App1 = new App();
	private final App App2 = new App();

	private final LongHashMap<Long> Errors = new LongHashMap<>();
	private final ArrayList<FailAction> FailActions = new ArrayList<>();
	private boolean Running = true;

	public void Run(String[] args) throws InterruptedException {
		try {
			_Run(args);
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

	private void _Run(String[] args) throws Throwable {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				ConfigFileName = args[++i];
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

		Task.tryInitThreadPool(null, null, null);
//		Task.initThreadPool((ThreadPoolExecutor)Executors.newFixedThreadPool(10, new ThreadFactoryWithName("globalRaftTest")),
//				(ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(3, new ThreadFactoryWithName("globalRaftTest-sch")));

		String ip = null;
		int port = 5001;

		InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;

		var config = new Zeze.Config().AddCustomize(new ServiceManagerServer.Conf()).LoadAndParse();
		new ServiceManagerServer(address, port, config);

		GlobalRaft.RaftConfig globalRaftConfig = GlobalRaft.RaftConfig.Load(ConfigFileName);
		for (var node : globalRaftConfig.getNodes().values()) {
			GlobalRafts.computeIfAbsent(node.getName(), (nodeName) -> new TestGlobalRaft(nodeName, ConfigFileName));
		}

		for (var globalRaft : GlobalRafts.values())
			globalRaft.Start();

		System.out.println("Started GlobalCacheManagerWithRaft");

		var conf1 = Config.Load("zeze.xml");
		var conf2 = Config.Load("zeze.xml");
		conf2.setServerId(conf1.getServerId() + 1);

		App1.Start(conf1);
		App2.Start(conf2);

		ClearCurrentCount();

		try {
			RunTrace();
		} finally {
			App1.Stop();
			App2.Stop();
			for (var globalRaft : GlobalRafts.values())
				globalRaft.Stop();
		}
	}

	public void RunTrace() throws Throwable {
		// 并发测试
		System.out.println("Global Raft Test Start!");
		logger.debug("并发测试");
		TestConcurrent("TestConcurrent", 100);

		TestGlobalRaft[] globalRaftsArr = GlobalRafts.values().toArray(new TestGlobalRaft[GlobalRafts.size()]);

		logger.debug("普通节点重启网络一");
		if (globalRaftsArr.length > 0) {
			globalRaftsArr[0].RestartNet();
		}
		TestConcurrent("TestGlobalRaftRestartNet1", 1);

		logger.debug("普通节点重启网络二");
		if (globalRaftsArr.length > 1) {
			globalRaftsArr[0].RestartNet();
			globalRaftsArr[1].RestartNet();
		}
		TestConcurrent("TestGlobalRaftRestartNet2", 1);

		logger.debug("普通节点重启网络三");
		if (globalRaftsArr.length > 2) {
			globalRaftsArr[0].RestartNet();
			globalRaftsArr[1].RestartNet();
			globalRaftsArr[2].RestartNet();
		}
		TestConcurrent("TestGlobalRaftRestartNet3", 1);

		logger.debug("普通节点重启一");
		if (globalRaftsArr.length > 0) {
			globalRaftsArr[0].Stop();
			globalRaftsArr[0].Start();
		}
		TestConcurrent("TestGlobalRaftRestart1", 1);

		logger.debug("普通节点重启二");
		if (globalRaftsArr.length > 1) {
			globalRaftsArr[0].Stop();
			globalRaftsArr[1].Stop();
			globalRaftsArr[0].Start();
			globalRaftsArr[1].Start();
		}
		TestConcurrent("TestGlobalRaftRestart2", 1);

		logger.debug("普通节点重启三");
		if (globalRaftsArr.length > 2) {
			globalRaftsArr[0].Stop();
			globalRaftsArr[1].Stop();
			globalRaftsArr[2].Stop();
			globalRaftsArr[0].Start();
			globalRaftsArr[1].Start();
			globalRaftsArr[2].Start();
		}
		TestConcurrent("TestGlobalRaftRestart3", 1);

		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		logger.fatal(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

		FailActions.add(new FailAction("RestartNet1", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].RestartNet();
		}));

		FailActions.add(new FailAction("RestartNet2", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].RestartNet();
			globalRafts[1].RestartNet();
		}));

		FailActions.add(new FailAction("RestartNet3", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].RestartNet();
			globalRafts[1].RestartNet();
			globalRafts[2].RestartNet();
		}));

		FailActions.add(new FailAction("RestartGlobalRaft1", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].Stop();
			globalRafts[0].Start();
		}));

		FailActions.add(new FailAction("RestartGlobalRaft2", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].Stop();
			globalRafts[1].Stop();
			globalRafts[0].Start();
			globalRafts[1].Start();
		}));

		FailActions.add(new FailAction("RestartGlobalRaft3", () ->
		{
			TestGlobalRaft[] globalRafts = ShuffleGlobalRaft();
			globalRafts[0].Stop();
			globalRafts[1].Stop();
			globalRafts[2].Stop();
			globalRafts[0].Start();
			globalRafts[1].Start();
			globalRafts[2].Start();
		}));

		Task.run(this::RandomTriggerFailActions, "RandomTriggerFailActions");

		var testName = "RealConcurrentDoRequest";
		var lastExpectCount = ExpectCount.get();
		while (true) {
			ExpectCount.addAndGet(ConcurrentAddCount(testName, 10));
			if (ExpectCount.get() - lastExpectCount > 10 * 2 * 5) {
				lastExpectCount = ExpectCount.get();
				if (!Check(testName))
					break;
			}
		}

		Running = false;
		CheckCurrentCount("GlobalRaft Final Check!!!");
	}

	private boolean Check(String testName) throws Throwable {
		int tryCount = 3;
		for (int i = 0; i < tryCount; i++) {
			boolean check = CheckCurrentCount(testName, false);
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("Check={} Index={} ExpectCount={} Errors={}", check, i, ExpectCount.get(), GetErrorsString());
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			if (check)
				return true;
			if (i < tryCount - 1)
				Thread.sleep(10000);
		}
		return false;
	}

	private long ErrorsSum() {
		long sum = 0;
		for (var iter = Errors.iterator(); iter.moveToNext(); ) {
			sum += iter.value();
		}
		return sum;
	}

	private String GetErrorsString() {
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, Errors);
		return sb.toString();
	}

	private void TestConcurrent(String testName, int count) throws Throwable {
		ExpectCount.addAndGet(ConcurrentAddCount(testName, count));
		CheckCurrentCount(testName);
	}

	private long ConcurrentAddCount(String testName, int count) throws Throwable {
		Future<?>[] task2 = new Future[2];

		AtomicInteger finalCount1 = new AtomicInteger();
		AtomicInteger finalCount2 = new AtomicInteger();

		task2[0] = Zeze.Util.Task.run(App1.Zeze.NewProcedure(() -> {
					finalCount1.set(TestConcurrency(App1, count, 1));
					return Procedure.Success;
				}, testName), null, null);

		task2[1] = Zeze.Util.Task.run(App2.Zeze.NewProcedure(() -> {
				finalCount2.set(TestConcurrency(App2, count, 2));
				return Procedure.Success;
			}, testName), null, null);

		try {
			task2[0].get();
			task2[1].get();
			System.out.printf("ConcurrentAddCount task1 count %s task2 count %s%n", finalCount1.get(), finalCount2.get());
		} catch (Exception e) {
			var currentCount = GetCurrentCount();
			var expectCount = this.ExpectCount.get();
			logger.error("ConcurrentAddCount count {} currentCount {} expectCount {}", count, currentCount, expectCount);
			e.printStackTrace();
		}

		return finalCount1.get() + finalCount2.get();
	}

	private int TestConcurrency(App app, int count, int appId) {
		Future<?>[] tasks = new Future[count];
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = Zeze.Util.Task.run(app.Zeze.NewProcedure(() -> {
				var v = app.demo_Module1.getTable1().getOrAdd(99L);
				v.setInt1(v.getInt1() + 1);

				System.out.printf("appId %d value %d timestamp %s%n", appId, v.getInt1(), System.currentTimeMillis());
				return Procedure.Success;
			}, "doConcurrency" + appId), null, null);

//			app.Zeze.NewProcedure(() -> {
//				var v = app.demo_Module1.getTable1().getOrAdd(99L);
//				v.setInt1(v.getInt1() + 1);
//				System.out.println(String.format("appId %d value %d timestamp %s", appId, v.getInt1(), System.currentTimeMillis()));
//				return Procedure.Success;
//			}, "doConcurrency" + appId).Call();
		}

		int finalCount = count;
		for (int i = 0; i < tasks.length; i++) {
			try {
				var result = tasks[i].get();
				if (result.equals(Procedure.AbortException))
					finalCount--;

				if (!result.equals(Procedure.Success))
					System.out.printf("TestConcurrency exception %s%n", result);
			} catch (Exception e) {
				logger.warn("TestConcurrency count {} appId {} index {} fail.", count, appId, i);
				e.printStackTrace();
			}
		}
		System.out.printf("TestConcurrency finalCount %s%n", finalCount);
		return finalCount;
	}

	private long GetCurrentCount() throws Throwable {
		AtomicLong count = new AtomicLong();
		int tryCount = 30;
		for (int i = 0; i < tryCount; i++) {
			var result = App1.Zeze.NewProcedure(() -> {
				if (0 == App1.demo_Module1.getTable1().getOrAdd(99L).getInt1() && 0 != this.ExpectCount.get())
					return Procedure.LogicError;
				count.set(App1.demo_Module1.getTable1().getOrAdd(99L).getInt1());
				return Procedure.Success;
			}, "GetCurrentCount").Call();
			if (result == Procedure.Success)
				break;
		}
		return count.get();
	}

	private void ClearCurrentCount() {
		try {
			App1.Zeze.NewProcedure(() -> {
				var val = App1.demo_Module1.getTable1().get(99L);
				if (val != null) {
					App1.demo_Module1.getTable1().remove(99L);
				}
				return Procedure.Success;
			}, "ClearData").Call();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private boolean CheckCurrentCount(String testName) throws Throwable {
		return CheckCurrentCount(testName, true);
	}

	private boolean CheckCurrentCount(String testName, boolean resetFlag) throws Throwable {
		var currentCount = GetCurrentCount();
		var expectCount = this.ExpectCount.get();
		if (currentCount != expectCount) {
			var report = new StringBuilder();
			Level level = expectCount != currentCount + ErrorsSum() ? Level.FATAL : Level.INFO;
			report.append(String.format("%n---------------------------------------"));
			report.append(String.format("%n%s, Expect=%d,Now=%d,Errors=%s", testName, expectCount, currentCount, GetErrorsString()));
			report.append(String.format("%n---------------------------------------"));
			logger.log(level, report.toString());
			if (resetFlag) {
				ExpectCount.getAndSet(currentCount);
				ClearCurrentCount();
				Errors.clear();
			}
			return level == Level.INFO;
		}
		return true;
	}

	private TestGlobalRaft[] ShuffleGlobalRaft() {
		return Random.Shuffle(GlobalRafts.values().toArray(new TestGlobalRaft[GlobalRafts.size()]));
	}

	private void RandomTriggerFailActions() throws InterruptedException {
		while (Running) {
			FailAction fa = FailActions.get(Random.getInstance().nextInt(FailActions.size()));
			logger.fatal("___________________________ {} _____________________________", fa.Name);
			try {
				fa.Action.run();
				fa.Count++;
			} catch (Throwable e) {
				logger.error("FailAction " + fa.Name, e);
				System.out.println("___________________________________________");
				System.out.println("___________________________________________");
				System.out.println("___________________________________________");
				System.out.println("Press [y] Enter To Exit.");
				System.out.println("___________________________________________");
				System.out.println("___________________________________________");
				System.out.println("___________________________________________");

				LogManager.shutdown();
				System.exit(-1);
			}
			// 等待失败的节点恢复正常
			WaitExceptCountGrow(120);
		}
		var sb = new StringBuilder();
		ByteBuffer.BuildString(sb, FailActions);
		logger.fatal("{}", sb.toString());
	}

	private void WaitExceptCountGrow(@SuppressWarnings("SameParameterValue") long growCount) throws InterruptedException {
		long oldExpectCount = this.ExpectCount.get();
		while (true) {
			Thread.sleep(10);
			if (this.ExpectCount.get() - oldExpectCount > growCount)
				break;
		}
	}

	public static class TestGlobalRaft extends StateMachine {
		private Zeze.Services.GlobalCacheManagerWithRaft GlobalCacheManagerWithRaft;
		private final String GlobalRaftConfigFileName;
		private final String RaftName;

		public TestGlobalRaft(String raftName, String configFileName) {
			RaftName = raftName;
			GlobalRaftConfigFileName = configFileName;
		}

		public void Start() throws Throwable {
			synchronized (this) {
				logger.debug("GlobalCacheManagerWithRaft {} Start ...", RaftName);
				var GlobalRaftConfig = Zeze.Raft.RaftConfig.Load(GlobalRaftConfigFileName);
				GlobalCacheManagerWithRaft = new GlobalCacheManagerWithRaft(RaftName, GlobalRaftConfig);
			}
		}

		public void Stop() throws IOException {
			synchronized (this) {
				logger.debug("GlobalCacheManagerWithRaft {} Stop ...", RaftName);
				if (GlobalCacheManagerWithRaft != null) {
					GlobalCacheManagerWithRaft.close();
					GlobalCacheManagerWithRaft = null;
				}
			}
		}

		public void RestartNet() throws Throwable {
			logger.debug("GlobalRaft {} RestartNet", RaftName);
			try {
				if (GlobalCacheManagerWithRaft != null) {
//					getRaft().getServer().Stop();
					GlobalCacheManagerWithRaft.getRocks().getRaft().getServer().Stop();
				}

				if (GlobalCacheManagerWithRaft != null) {
					for (int i = 0; ; ) {
						try {
//							getRaft().getServer().Start();
							GlobalCacheManagerWithRaft.getRocks().getRaft().getServer().Start();
							break;
						}  catch (BindException | RuntimeException be) {
							if (!(be instanceof BindException) && !(be.getCause() instanceof BindException) || ++i > 30)
								throw be;
							Thread.sleep(100);
						}
					}
				}
			} catch (Throwable e) {
				logger.error("GlobalRaft RestartNet error", e);
				throw e;
			}
		}

		@Override
		public SnapshotResult Snapshot(String path) throws Throwable {
			return null;
		}

		@Override
		public void LoadSnapshot(String path) throws Throwable {
			System.out.println("1111111111111111111111");
		}
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

	public static void main(String[] args) throws Throwable {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-Config"))
				ConfigFileName = args[++i];
		}
		new TestGlobalCacheMgrWithRaft().Run(args);
	}

}
