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
public class TestCrossFamilyNamedReplacement {
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
	public void onlineReplacementSurvivesQueuedGlobalSimpleFire() throws Exception {
		assertReplacementFires(true, false);
	}

	@Test
	public void onlineReplacementSurvivesQueuedGlobalCronFire() throws Exception {
		assertReplacementFires(true, true);
	}

	@Test
	public void globalReplacementSurvivesQueuedOnlineSimpleFire() throws Exception {
		assertReplacementFires(false, false);
	}

	@Test
	public void globalReplacementSurvivesQueuedOnlineCronFire() throws Exception {
		assertReplacementFires(false, true);
	}

	private static void assertReplacementFires(boolean globalFirst, boolean cron) throws Exception {
		OldCalls.set(0);
		replacementFired = new CountDownLatch(1);
		try (var env = new TimerTestEnv("TestCrossFamilyNamedReplacement", true);
			 var queue = new TimerTestEnv.QueueBlock("cross-family-replacement-" + env.app.getConfig().getServerId())) {
			long roleId = 1;
			env.transaction(() -> {
				env.online.getOrAddOnlineShared(roleId).setLoginVersion(1);
				env.online.getOrAddOnlineShared(roleId).setLink(
						new Zeze.Builtin.Game.Online.BLink("test", 1, Zeze.Game.Online.eLogined));
				env.online.getTLocal().getOrAdd(roleId).setLoginVersion(1);
				return 0L;
			});
			var roleTimer = env.online.getTimerRole();
			var timerId = "cross-family-named-replacement";
			TimerSpec original = cron
					? TimerSpec.ofCron("* * * * * ?").times(1).oneByOneKey(queue.key)
					: TimerSpec.ofDelay(0).times(1).oneByOneKey(queue.key);
			env.transaction(() -> {
				Assertions.assertTrue(globalFirst
						? env.timer.scheduleNamed(timerId, original, OldHandle.class)
						: roleTimer.scheduleOnlineNamed(roleId, timerId, original, OldHandle.class, null));
				return 0L;
			});
			TimerTestEnv.awaitDispatch(env.timer.timerFutures.get(timerId));

			env.transaction(() -> {
				var replacement = TimerSpec.ofDelay(2_000).times(1).oneByOneKey(queue.key);
				if (globalFirst) {
					env.timer.cancel(timerId);
					Assertions.assertTrue(roleTimer.scheduleOnlineNamed(roleId, timerId, replacement,
							ReplacementHandle.class, null));
				} else {
					Assertions.assertTrue(roleTimer.cancelOnline(timerId, roleId));
					Assertions.assertTrue(env.timer.scheduleNamed(timerId, replacement, ReplacementHandle.class));
				}
				return 0L;
			});
			queue.drain();

			Assertions.assertTrue(replacementFired.await(5, TimeUnit.SECONDS),
					"a replacement in another timer family must still execute its own handle");
			Assertions.assertEquals(0, OldCalls.get(), "canceled timer must not execute its old handle");
		}
	}
}
