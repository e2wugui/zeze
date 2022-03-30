package Infinite;

import java.util.ArrayList;
import Zeze.Util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("NewClassNamingConvention")
public final class Simulate {
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
	public void Before() throws Throwable {
		After();
		for (int serverId = 0; serverId < AppCount; serverId++)
			Apps.add(new App(serverId));
		for (var app : Apps)
			app.Start();
	}

	@After
	public void After() throws Throwable {
		for (var app : Apps)
			app.Stop();
		Apps.clear();
	}

	@Test
	public void testMain() throws Throwable {
		logger.fatal("Prepare");
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
			Tasks.verify();
		} while (Infinite);
		logger.fatal("Done!!!!!!");
	}

	public static void main(String[] args) throws Throwable {
		var simulate = new Simulate();
		simulate.Infinite = args.length > 0 && args[0].equalsIgnoreCase("infinite"); // 一直执行。
		simulate.Before();
		try {
			simulate.testMain();
		} finally {
			simulate.After();
		}
	}
}
