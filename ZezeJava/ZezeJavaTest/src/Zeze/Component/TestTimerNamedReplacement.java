package Zeze.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Fast
@Isolated
public class TestTimerNamedReplacement {
	private static final AtomicInteger OldCalls = new AtomicInteger();
	private static volatile CountDownLatch replacementFired;

	public static class OldHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			Transaction.whileCommit(OldCalls::incrementAndGet);
		}
	}

	public static class ReplacementHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			Transaction.whileCommit(replacementFired::countDown);
		}
	}

	@Test
	public void replacingNamedTimerPreservesReplacementAfterQueuedSimpleFire() throws Exception {
		assertReplacementFires(false);
	}

	@Test
	public void replacingNamedTimerPreservesReplacementAfterQueuedCronFire() throws Exception {
		assertReplacementFires(true);
	}

	private static void assertReplacementFires(boolean cron) throws Exception {
		OldCalls.set(0);
		replacementFired = new CountDownLatch(1);
		try (var env = new TimerTestEnv("TestTimerNamedReplacement");
			 var queue = new TimerTestEnv.QueueBlock("named-replacement-" + env.app.getConfig().getServerId())) {
			var timerId = "named-replacement";
			TimerSpec original = cron
					? TimerSpec.ofCron("* * * * * ?").times(1).oneByOneKey(queue.key)
					: TimerSpec.ofDelay(0).times(1).oneByOneKey(queue.key);
			env.transaction(() -> {
				Assertions.assertTrue(env.timer.scheduleNamed(timerId, original, OldHandle.class));
				return 0L;
			});
			TimerTestEnv.awaitDispatch(env.timer.timerFutures.get(timerId));

			env.transaction(() -> {
				Assertions.assertTrue(env.timer.scheduleNamed(timerId,
						TimerSpec.ofDelay(2_000).times(1).oneByOneKey(queue.key), ReplacementHandle.class));
				return 0L;
			});
			queue.drain();

			Assertions.assertTrue(replacementFired.await(5, TimeUnit.SECONDS),
					"replacement must fire even after the old queued fire is discarded");
			Assertions.assertEquals(0, OldCalls.get(), "replaced timer must not call its old handle");
		}
	}
}
