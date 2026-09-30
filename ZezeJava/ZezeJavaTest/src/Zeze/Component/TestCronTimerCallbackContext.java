package Zeze.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
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
public class TestCronTimerCallbackContext {
	private record Snapshot(long happenTimes, long expectedTime) {
	}

	private static final ConcurrentLinkedQueue<Snapshot> Callbacks = new ConcurrentLinkedQueue<>();
	private static volatile CountDownLatch fired;

	public static class RecordingHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			var snapshot = new Snapshot(context.happenTimes, context.expectedTimeMills);
			Transaction.whileCommit(() -> {
				Callbacks.add(snapshot);
				fired.countDown();
			});
		}
	}

	@Test
	public void oneShotCronCallbackReportsItsFirstOccurrenceAndExpectedTime() throws Exception {
		assertCallbackContext(1);
	}

	@Test
	public void finalCronCallbackReportsItsOwnOccurrenceAndExpectedTime() throws Exception {
		assertCallbackContext(2);
	}

	private static void assertCallbackContext(int times) throws Exception {
		Callbacks.clear();
		fired = new CountDownLatch(times);
		try (var env = new TimerTestEnv("TestCronTimerCallbackContext")) {
			env.transaction(() -> {
				env.timer.schedule(TimerSpec.ofCron("* * * * * ?").times(times), RecordingHandle.class);
				return 0L;
			});
			Assertions.assertTrue(fired.await(5, TimeUnit.SECONDS), "all configured callbacks must execute");
			var callbacks = Callbacks.toArray(Snapshot[]::new);
			Assertions.assertEquals(times, callbacks.length);
			for (int i = 0; i < callbacks.length; ++i) {
				Assertions.assertEquals(i + 1L, callbacks[i].happenTimes(),
						"each callback, including the final one, must report its current occurrence");
				Assertions.assertTrue(callbacks[i].expectedTime() > 0, "each callback must have an expected time");
				if (i > 0)
					Assertions.assertTrue(callbacks[i].expectedTime() > callbacks[i - 1].expectedTime(),
							"the final callback must report its own scheduled time");
			}
		}
	}
}
