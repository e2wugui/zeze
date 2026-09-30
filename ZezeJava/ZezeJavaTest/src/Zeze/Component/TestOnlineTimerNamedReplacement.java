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
public class TestOnlineTimerNamedReplacement {
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
	public void recreatedOnlineTimerDiscardsQueuedSimpleFire() throws Exception {
		assertReplacementFires(false, false);
	}

	@Test
	public void recreatedOnlineTimerDiscardsQueuedHotSimpleFire() throws Exception {
		assertReplacementFires(false, true);
	}

	@Test
	public void recreatedOnlineTimerDiscardsQueuedCronFire() throws Exception {
		assertReplacementFires(true, false);
	}

	@Test
	public void recreatedOnlineTimerDiscardsQueuedHotCronFire() throws Exception {
		assertReplacementFires(true, true);
	}

	private static void assertReplacementFires(boolean cron, boolean hot) throws Exception {
		OldCalls.set(0);
		replacementFired = new CountDownLatch(1);
		try (var env = new TimerTestEnv("TestOnlineTimerNamedReplacement", true);
			 var queue = new TimerTestEnv.QueueBlock("online-replacement-" + env.app.getConfig().getServerId())) {
			long roleId = 1;
			env.transaction(() -> {
				env.online.getOrAddOnlineShared(roleId).setLoginVersion(1);
				env.online.getOrAddOnlineShared(roleId).setLink(
						new Zeze.Builtin.Game.Online.BLink("test", 1, Zeze.Game.Online.eLogined));
				env.online.getTLocal().getOrAdd(roleId).setLoginVersion(1);
				return 0L;
			});
			var roleTimer = env.online.getTimerRole();
			var timerId = "online-named-replacement";
			TimerSpec original = cron
					? TimerSpec.ofCron("* * * * * ?").times(1).oneByOneKey(queue.key)
					: TimerSpec.ofDelay(0).times(1).oneByOneKey(queue.key);
			env.transaction(() -> {
				Assertions.assertTrue(schedule(roleTimer, roleId, timerId, original, OldHandle.class, hot));
				return 0L;
			});
			TimerTestEnv.awaitDispatch(env.timer.timerFutures.get(timerId));

			env.transaction(() -> {
				Assertions.assertTrue(roleTimer.cancelOnline(timerId, roleId));
				Assertions.assertTrue(schedule(roleTimer, roleId, timerId,
						TimerSpec.ofDelay(2_000).times(1).oneByOneKey(queue.key), ReplacementHandle.class, hot));
				return 0L;
			});
			queue.drain();

			Assertions.assertEquals(0, OldCalls.get(), "canceled registration must not consume the replacement");
			Assertions.assertTrue(replacementFired.await(5, TimeUnit.SECONDS),
					"recreated online timer must execute its own handle");
		}
	}

	private static boolean schedule(TimerRole timer, long roleId, String timerId, TimerSpec spec,
			Class<? extends TimerHandle> handle, boolean hot) {
		return hot ? timer.scheduleOnlineNamedHot(roleId, timerId, spec, handle, null)
				: timer.scheduleOnlineNamed(roleId, timerId, spec, handle, null);
	}
}
