package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Fast
@Isolated
public class TestTimerFailedFireNamedReplacement {
	private static volatile CountDownLatch replacementFired;

	public static class BrokenHandle implements TimerHandle {
		public BrokenHandle() {
			throw new IllegalStateException("Handler construction failed after registration");
		}

		@Override
		public void onTimer(@NotNull TimerContext context) {
			Assertions.fail("Broken handler must not execute");
		}
	}

	public static class ReplacementHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			Transaction.whileCommit(replacementFired::countDown);
		}
	}

	/** Pauses the separate cleanup at its cancellation entry, after the failed fire has ended. */
	private static final class CleanupGateTimer extends Timer {
		final CountDownLatch cleanupEntered = new CountDownLatch(1);
		final CountDownLatch releaseCleanup = new CountDownLatch(1);

		CleanupGateTimer(Application app) {
			super(new TakeoverTestEnv.TestAppBase(app));
		}

		@Override
		public boolean cancel(@Nullable String timerId) {
			var stack = Transaction.getCurrent().getProcedureStack();
			if ("Timer.cancelTimer".equals(stack.getLast().getActionName())) {
				cleanupEntered.countDown();
				try {
					Assertions.assertTrue(releaseCleanup.await(10, TimeUnit.SECONDS),
							"cleanup must be released after replacement registration");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
			}
			return super.cancel(timerId);
		}
	}

	@Test
	public void replacementSurvivesCleanupOfFailedSimpleFire() throws Exception {
		assertReplacementSurvives(false);
	}

	@Test
	public void replacementSurvivesCleanupOfFailedCronFire() throws Exception {
		assertReplacementSurvives(true);
	}

	private static void assertReplacementSurvives(boolean cron) throws Exception {
		Task.tryInitThreadPool();
		replacementFired = new CountDownLatch(1);
		var conf = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		var app = new Application("TestTimerFailedFireNamedReplacement" + conf.getServerId(), conf);
		var timer = new CleanupGateTimer(app);
		try {
			app.start();
			timer.loadCustomClassAnd();
			timer.start();
			var timerId = "failed-fire-replacement";
			var queueKey = "failed-fire-replacement-" + conf.getServerId();
			TimerSpec original = cron
					? TimerSpec.ofCron("* * * * * ?").times(1).oneByOneKey(queueKey)
					: TimerSpec.ofDelay(0).times(1).oneByOneKey(queueKey);
			transaction(app, () -> {
				// Registration verifies that the default constructor exists without instantiating it.
				Assertions.assertTrue(timer.scheduleNamed(timerId, original, BrokenHandle.class));
				return 0L;
			});
			Assertions.assertTrue(timer.cleanupEntered.await(5, TimeUnit.SECONDS),
					"failed fire must reach its separate cleanup transaction");

			transaction(app, () -> {
				Assertions.assertTrue(timer.scheduleNamed(timerId,
						TimerSpec.ofDelay(0).period(60_000).times(2).oneByOneKey(queueKey),
						ReplacementHandle.class));
				return 0L;
			});
			var oldFireFinished = new CountDownLatch(1);
			TaskSpec.ofAction(oldFireFinished::countDown).executeOneByOne(queueKey);
			timer.releaseCleanup.countDown();
			Assertions.assertTrue(oldFireFinished.await(5, TimeUnit.SECONDS), "old cleanup must finish");

			transaction(app, () -> {
				Assertions.assertNotNull(timer.getTimerIndex(timerId),
						"cleanup of the failed registration must preserve the replacement");
				Assertions.assertEquals(ReplacementHandle.class.getName(), timer.getTimer(timerId).getHandleName());
				return 0L;
			});
			Assertions.assertTrue(replacementFired.await(5, TimeUnit.SECONDS),
					"replacement must execute its own callback");
		} finally {
			timer.releaseCleanup.countDown();
			try {
				timer.stop();
			} finally {
				app.stop();
			}
		}
	}

	private static void transaction(Application app, FuncLong action) {
		Assertions.assertEquals(Procedure.Success,
				app.newProcedure(action, "TestTimerFailedFireNamedReplacement.transaction").call());
	}
}
