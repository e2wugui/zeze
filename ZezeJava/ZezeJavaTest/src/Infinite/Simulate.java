package Infinite;

import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Util.PerfCounter;
import Zeze.Util.Random;
import Zeze.Util.Task;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NewClassNamingConvention")
public final class Simulate {
	static {
		System.getProperties().putIfAbsent("log4j2.contextSelector",
				"org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
	}

	static final Logger logger = LogManager.getLogger(Simulate.class);

	public final static int AppCount = 5;
	public final static int BatchTaskCount = 20000;
	public final static int CacheCapacity = 1000;
	public final static int AccessKeyBound = (int)(CacheCapacity * 1.2f);
	private static Simulate instance;

	public static Simulate getInstance() {
		return instance;
	}

	private final ArrayList<App> Apps = new ArrayList<>();
	private long BatchNumber;
	public boolean Infinite; // 当使用本目录的Main独立启动时，可以设置为true。

	public Simulate() {
		instance = this;
	}

	public App randApp() {
		return randApp(Apps.size());
	}

	public App randApp(int max) {
		return Apps.get(Random.getInstance().nextInt(Math.min(max, Apps.size())));
	}

	@BeforeEach
	public void Before() throws Exception {
		After();
		for (int serverId = 10; serverId < AppCount + 10; serverId++)
			Apps.add(new App(serverId));

		for (var app : Apps)
			app.Start();

		var allTFlush = new ArrayList<Zeze.Transaction.TableX<Long, BValue>>();
		var allTable1 = new ArrayList<Zeze.Transaction.TableX<Long, BValue>>();
		for (var app : Apps) {
			allTFlush.add(app.app.demo_Module1.getTflush());
			allTable1.add(app.app.demo_Module1.getTable1());
		}
		for (var app : Apps) {
			app.app.demo_Module1.getTflush().getSimulateTables = () -> allTFlush;
			app.app.demo_Module1.getTable1().getSimulateTables = () -> allTable1;
		}
	}

	@AfterEach
	public void After() throws Exception {
		if (Apps.isEmpty())
			return;
		logger.fatal("After");
		for (var app : Apps) {
			app.app.demo_Module1.getTflush().getSimulateTables = null;
			app.app.demo_Module1.getTable1().getSimulateTables = null;
		}
		for (var app : Apps)
			app.Stop();
		Apps.clear();
	}

	final ArrayList<Future<?>> RunningTasks = new ArrayList<>(Simulate.BatchTaskCount);

	public void WaitAllRunningTasksAndClear() {
		Task.waitAll(RunningTasks);
		RunningTasks.clear();
	}

	@Test
	public void testMain() throws Exception {
		var perfScheduled = PerfCounter.instance().cancelScheduledLog();
		logger.fatal("Prepare");
		try {
			var taskDefTimeout = Task.defaultTimeout;
			Task.defaultTimeout = 86400_000;
			Tasks.prepare();
			++BatchNumber;
			logger.fatal("Run {}", BatchNumber);
			if (Apps.getFirst().app.Zeze.getConfig().isHistory()) {
				Apps.getFirst().clearTables();
				// Takeover租约行是启动期簿记（claim不受History管控）：批间一并清掉，且每个app的缓存都要清，
				// 否则各自的renew会用缓存里的旧行复活存储；renew对缺行会自愈重写（重写会被History记录，verify一致）。
				for (var a : Apps)
					App.clearDbTable((Zeze.Transaction.TableX<?, ?>)a.app.getZeze()
							.getTable("Zeze_Builtin_Takeover_tTakeoverLease"));
			}
			for (var app : Apps) {
				if (!app.app.Zeze.getConfig().isHistory())
					logger.info("app {} history disable.", app.app.Zeze.getConfig().getServerId());
			}
			PerfCounter.instance().resetCounter();
			logger.info("timeNow={}", CoverHistory.timeNow);
			for (int i = 0; i < BatchTaskCount; i++) {
				var app = Tasks.randCreateTask().Run();
				if (((i + 1) % 3) == 0)
					RunningTasks.add(app.coverHistory.submitTasks(i));

				//*
				if (((i + 1) % 100) == 0) {
					WaitAllRunningTasksAndClear();
				}
				// */
			}
			logger.fatal("Wait {}", BatchNumber);
			WaitAllRunningTasksAndClear();
			for (var app : Apps) {
				logger.fatal("Finish {}-{}", BatchNumber, app.getServerId());
			}
			logger.fatal("Verify {}", BatchNumber);
			for (var app : Apps)
				app.app.Zeze.checkpointRun();
			if (Apps.getFirst().app.Zeze.getConfig().isHistory())
				Zeze.History.Verify.run(Apps.getFirst().app.Zeze); // 只需要验证一个App，History只有一份。
			//Thread.sleep(4000);
			Tasks.verify();
			Task.defaultTimeout = taskDefTimeout;
			logger.fatal("Done!!!!!!");
		} catch (Exception ex) {
			logger.error("", ex);
			throw ex;
		} finally {
			if (perfScheduled)
				PerfCounter.instance().tryStartScheduledLog();
		}
	}

	public static void main(String[] args) throws Exception {
		var simulate = new Simulate();
		simulate.Infinite = !"false".equalsIgnoreCase(System.getProperty("Infinite"));
		do {
			simulate.Before();
			try {
				simulate.testMain();
			} catch (Throwable e) { // print stacktrace. rethrow
				logger.fatal("main exception:", e);
				throw e;
			} finally {
				simulate.After();
				Tasks.clearAllCounters();
				DatabaseMemory.clear();
			}
		} while (simulate.Infinite);
	}
}
