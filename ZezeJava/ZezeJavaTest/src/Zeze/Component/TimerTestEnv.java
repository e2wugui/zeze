package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.junit.jupiter.api.Assertions;

/** Real Timer assembly with an independent server id and Memory database. */
final class TimerTestEnv implements AutoCloseable {
	final Application app;
	final Timer timer;
	final TakeoverTestEnv.TestAppBase appBase;

	TimerTestEnv(String name) throws Exception {
		Task.tryInitThreadPool();
		var conf = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		app = new Application(name + conf.getServerId(), conf);
		new ProviderApp(app);
		appBase = new TakeoverTestEnv.TestAppBase(app);
		app.initialize(appBase);
		app.start();
		timer = app.getTimer();
		timer.start();
	}

	void transaction(FuncLong action) {
		Assertions.assertEquals(0L, app.newProcedure(action, "TimerTestEnv.transaction").call());
	}

	@Override
	public void close() throws Exception {
		app.stop();
	}

	/** Holds the public one-by-one queue until a scheduled fire has been dispatched into it. */
	static final class QueueBlock implements AutoCloseable {
		final String key;
		private final CountDownLatch release = new CountDownLatch(1);

		QueueBlock(String key) throws InterruptedException {
			this.key = key;
			var entered = new CountDownLatch(1);
			TaskSpec.ofAction(() -> {
				entered.countDown();
				Assertions.assertTrue(release.await(30, TimeUnit.SECONDS), "queue blocker must be released");
			}).executeOneByOne(key);
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS), "queue blocker must enter");
		}

		void drain() throws InterruptedException {
			var drained = new CountDownLatch(1);
			TaskSpec.ofAction(drained::countDown).executeOneByOne(key);
			release.countDown();
			Assertions.assertTrue(drained.await(5, TimeUnit.SECONDS), "queued fire must finish");
		}

		@Override
		public void close() {
			release.countDown();
		}
	}

	static void awaitDispatch(Future<?> scheduled) throws InterruptedException {
		Assertions.assertNotNull(scheduled, "timer must have been scheduled");
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!scheduled.isDone() && System.nanoTime() < deadline)
			Thread.sleep(5);
		// The future is inspected only to arrange the queued-fire race; outcomes use callback behavior.
		Assertions.assertTrue(scheduled.isDone(), "scheduled wrapper must dispatch before replacement");
	}
}
