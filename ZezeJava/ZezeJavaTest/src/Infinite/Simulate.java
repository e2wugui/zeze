package Infinite;

import java.util.ArrayList;
import Zeze.Util.Random;
import demo.Module1.BValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("NewClassNamingConvention")
public final class Simulate {
	static {
		System.getProperties().putIfAbsent("log4j2.contextSelector",
				"org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
	}

	static final Logger logger = LogManager.getLogger(Simulate.class);

	public final static int AppCount = 10;
	public final static int BatchTaskCount = 50000;
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

	@Before
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

	@After
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

	@Test
	public void testMain() throws Exception {
		logger.fatal("Prepare");
		try {
			Tasks.prepare();
			do {
				++BatchNumber;
				logger.fatal("Run {}", BatchNumber);
				for (int i = 0; i < BatchTaskCount; i++)
					Tasks.randCreateTask().Run();
				logger.fatal("Wait {}", BatchNumber);
				for (var app : Apps) {
					app.WaitAllRunningTasksAndClear();
					logger.fatal("Finish {}-{}", BatchNumber, app.getServerId());
				}
				logger.fatal("Verify {}", BatchNumber);
				//noinspection BusyWait
				Thread.sleep(4000);
				Tasks.verify();
			} while (Infinite);
		} catch (Exception ex) {
			logger.error("", ex);
			throw ex;
		}
		logger.fatal("Done!!!!!!");
	}

	public static void main(String[] args) throws Exception {
		var simulate = new Simulate();
		simulate.Infinite = true;
		simulate.Before();
		try {
			simulate.testMain();
		} catch (Throwable e) { // print stacktrace. rethrow
			logger.fatal("main exception:", e);
			throw e;
		} finally {
			simulate.After();
		}
	}
}
