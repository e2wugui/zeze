package Zeze.Component;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Fast
@Isolated
public class TestSimpleTimerEndTime {
	private static volatile long endTime;
	private static volatile long nextAfterFirst;
	private static volatile @Nullable CountDownLatch firstFired;
	private static volatile @Nullable CountDownLatch afterEndFired;

	public static class RecordingHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			if (context.happenTimes == 1) {
				var next = context.nextExpectedTimeMills;
				Transaction.whileCommit(() -> {
					nextAfterFirst = next;
					requireNonNull(firstFired).countDown();
				});
			}
			if (context.expectedTimeMills > endTime)
				Transaction.whileCommit(requireNonNull(afterEndFired)::countDown);
		}
	}

	public static class PostponePastEndHandle implements TimerHandle {
		private static volatile @Nullable CountDownLatch fired;

		@Override
		public void onTimer(TimerContext context) {
			context.nextExpectedTimeMills = Long.MAX_VALUE;
			Transaction.whileCommit(requireNonNull(fired)::countDown);
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

			Assertions.assertTrue(requireNonNull(firstFired).await(5, TimeUnit.SECONDS), "the initial tick must execute");
			Assertions.assertEquals(0L, nextAfterFirst, "no next tick may be planned after endTime");
			Assertions.assertFalse(requireNonNull(afterEndFired).await(2_500, TimeUnit.MILLISECONDS),
					"a callback whose expected time is after endTime must not execute");
		}
	}

	@Test
	public void callbackCannotLeaveATimerScheduledPastEndTime() throws Exception {
		var fired = new CountDownLatch(1);
		PostponePastEndHandle.fired = fired;
		try (var env = new TimerTestEnv("TestSimpleTimerCallbackEndTime")) {
			var timerId = "callback-postponed-past-end";
			env.transaction(() -> {
				var spec = TimerSpec.ofDelay(0).period(100).endTime(System.currentTimeMillis() + 10_000);
				Assertions.assertTrue(env.timer.scheduleNamed(timerId, spec, PostponePastEndHandle.class, null));
				return 0L;
			});
			Assertions.assertTrue(fired.await(5, TimeUnit.SECONDS));
			env.transaction(() -> {
				Assertions.assertNull(env.timer.getTimerIndex(timerId), "the out-of-range continuation must be cancelled");
				return 0L;
			});
		} finally {
			PostponePastEndHandle.fired = null;
		}
	}
}
