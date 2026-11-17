package Zeze.Component;

import Zeze.Builtin.Game.Online.BLink;
import Zeze.Builtin.Timer.BTransmitCronTimer;
import Zeze.Builtin.Timer.BTransmitSimpleTimer;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestTransmittedTimerNameReservation {
	public static class Handle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
		}
	}

	@Test
	public void simpleTransmitPreservesOccupiedGlobalTimer() throws Exception {
		checkTransmit(false, true);
	}

	@Test
	public void cronTransmitPreservesOccupiedGlobalTimer() throws Exception {
		checkTransmit(true, true);
	}

	@Test
	public void simpleTransmitAcceptsUnusedAutomaticId() throws Exception {
		checkTransmit(false, false);
	}

	@Test
	public void cronTransmitAcceptsUnusedAutomaticId() throws Exception {
		checkTransmit(true, false);
	}

	private static void checkTransmit(boolean cron, boolean occupied) throws Exception {
		try (var env = new TimerTestEnv("TransmittedTimerNameReservation", true)) {
			var online = java.util.Objects.requireNonNull(env.online);
			var roleTimer = online.getTimerRole();
			long roleId = 1;
			var timerId = occupied ? "reserved-global" : "@transmitted-auto";
			env.transaction(() -> {
				online.getOrAddOnlineShared(roleId).setLoginVersion(1);
				online.getOrAddOnlineShared(roleId).setLink(new BLink("test", 1, Zeze.Game.Online.eLogined));
				online.getTLocal().getOrAdd(roleId).setLoginVersion(1);
				if (occupied)
					Assertions.assertTrue(env.timer.scheduleNamed(timerId, TimerSpec.ofDelay(60_000), Handle.class));
				return 0;
			});
			var originalFuture = env.timer.timerFutures.get(timerId);
			env.transaction(() -> {
				if (cron) {
					var p = new BTransmitCronTimer();
					p.setTimerId(timerId);
					p.setHandleClass(Handle.class.getName());
					p.setLoginVersion(1);
					p.setCronTimer(TimerSpec.ofDay(0, 0, 0).build());
					roleTimer.onTransmitCronTimer(roleId, new Binary(ByteBuffer.encode(p)));
				} else {
					var p = new BTransmitSimpleTimer();
					p.setTimerId(timerId);
					p.setHandleClass(Handle.class.getName());
					p.setLoginVersion(1);
					p.setSimpleTimer(TimerSpec.ofDelay(60_000).build());
					roleTimer.onTransmitSimpleTimer(roleId, new Binary(ByteBuffer.encode(p)));
				}
				Assertions.assertEquals(!occupied, roleTimer.getOnlineTimer(timerId) != null);
				Assertions.assertEquals(occupied, env.timer.getTimer(timerId) != null);
				return 0;
			});
			if (occupied) {
				Assertions.assertSame(originalFuture, env.timer.timerFutures.get(timerId));
				Assertions.assertFalse(java.util.Objects.requireNonNull(originalFuture).isCancelled());
			} else
				Assertions.assertNotNull(env.timer.timerFutures.get(timerId));
		}
	}
}
