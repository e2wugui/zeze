package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Fast
@Isolated
public class TestSimpleTimerEndTime {
	private static volatile long endTime;
	private static volatile long nextAfterFirst;
	private static volatile CountDownLatch firstFired;
	private static volatile CountDownLatch afterEndFired;

	public static class RecordingHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			if (context.happenTimes == 1) {
				var next = context.nextExpectedTimeMills;
				Transaction.whileCommit(() -> {
					nextAfterFirst = next;
					firstFired.countDown();
				});
			}
			if (context.expectedTimeMills > endTime)
				Transaction.whileCommit(afterEndFired::countDown);
		}
	}

	@Test
	public void periodicTimerStopsBeforeItsNextTickWouldExceedEndTime() throws Exception {
		firstFired = new CountDownLatch(1);
		afterEndFired = new CountDownLatch(1);
		nextAfterFirst = -1;
		try (var env = new TimerTestEnv("TestSimpleTimerEndTime")) {
			env.transaction(() -> {
				endTime = System.currentTimeMillis() + 1_000;
				env.timer.schedule(TimerSpec.ofDelay(0).period(2_000).endTime(endTime), RecordingHandle.class);
				return 0L;
			});

			Assertions.assertTrue(firstFired.await(5, TimeUnit.SECONDS), "the initial tick must execute");
			Assertions.assertEquals(0L, nextAfterFirst, "no next tick may be planned after endTime");
			Assertions.assertFalse(afterEndFired.await(2_500, TimeUnit.MILLISECONDS),
					"a callback whose expected time is after endTime must not execute");
		}
	}
}
