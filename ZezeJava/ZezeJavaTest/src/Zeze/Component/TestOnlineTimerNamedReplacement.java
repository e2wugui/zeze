package Zeze.Component;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.Transaction;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Fast
@Isolated
public class TestOnlineTimerNamedReplacement {
	private static final AtomicInteger OldCalls = new AtomicInteger();
	private static volatile @Nullable CountDownLatch replacementFired;

	public static class OldHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			Transaction.whileCommit(OldCalls::incrementAndGet);
		}
	}

	public static class ReplacementHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
			Transaction.whileCommit(requireNonNull(replacementFired)::countDown);
		}
	}

	@Test
	public void recreatedOnlineTimerDiscardsQueuedSimpleFire() throws Exception {
		assertReplacementFires(false);
	}

	@Test
	public void recreatedOnlineTimerDiscardsQueuedCronFire() throws Exception {
		assertReplacementFires(true);
	}

	private static void assertReplacementFires(boolean cron) throws Exception {
		OldCalls.set(0);
		replacementFired = new CountDownLatch(1);
		try (var env = new TimerTestEnv("TestOnlineTimerNamedReplacement", true);
			 var queue = new TimerTestEnv.QueueBlock("online-replacement-" + env.app.getConfig().getServerId())) {
			long roleId = 1;
			env.transaction(() -> {
				requireNonNull(env.online).getOrAddOnlineShared(roleId).setLoginVersion(1);
				env.online.getOrAddOnlineShared(roleId).setLink(
						new Zeze.Builtin.Game.Online.BLink("test", 1, Zeze.Game.Online.eLogined));
				env.online.getTLocal().getOrAdd(roleId).setLoginVersion(1);
				return 0L;
			});
			var roleTimer = requireNonNull(env.online).getTimerRole();
			var timerId = "online-named-replacement";
			TimerSpec original = cron
					? TimerSpec.ofCron("* * * * * ?").times(1).oneByOneKey(queue.key)
					: TimerSpec.ofDelay(0).times(1).oneByOneKey(queue.key);
			env.transaction(() -> {
				Assertions.assertTrue(roleTimer.scheduleOnlineNamed(roleId, timerId, original, OldHandle.class, null));
				return 0L;
			});
			TimerTestEnv.awaitDispatch(env.timer.timerFutures.get(timerId));

			env.transaction(() -> {
				Assertions.assertTrue(roleTimer.cancelOnline(timerId, roleId));
				Assertions.assertTrue(roleTimer.scheduleOnlineNamed(roleId, timerId,
						TimerSpec.ofDelay(2_000).times(1).oneByOneKey(queue.key), ReplacementHandle.class, null));
				return 0L;
			});
			queue.drain();

			Assertions.assertEquals(0, OldCalls.get(), "canceled registration must not consume the replacement");
			Assertions.assertTrue(requireNonNull(replacementFired).await(5, TimeUnit.SECONDS),
					"recreated online timer must execute its own handle");
		}
	}
}
