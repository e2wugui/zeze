package Zeze.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BSimpleTimer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTimerSpecTerminalTime {
	@Test
	public void aFiniteCronRecordsItsFinalTickAndStopsWithoutThrowing() throws Exception {
		var expression = "0 0 0 1 1 ? 2020";
		var beforeLast = ZonedDateTime.of(2019, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli();
		var last = CronTimerSpec.cronNextTime(expression, beforeLast);
		var timer = new BCronTimer();
		timer.setCronExpression(expression);
		timer.setRemainTimes(-1);
		timer.setNextExpectedTime(last);
		assertFalse(CronTimerSpec.nextCronTimer(timer, false));
		assertEquals(last, timer.getExpectedTime());
		assertEquals(1, timer.getHappenTimes());
		assertEquals(0, timer.getNextExpectedTime());
	}

	@Test
	public void buildersRejectAnInitialTriggerBeyondTheDeadline() {
		var end = System.currentTimeMillis() + 60_000;
		assertThrows(IllegalArgumentException.class, () -> TimerSpec.ofDelay(3_600_000).endTime(end).build());
		var nextYear = ZonedDateTime.now().getYear() + 1;
		assertThrows(IllegalArgumentException.class,
				() -> TimerSpec.ofCron("0 0 0 1 1 ? " + nextYear).endTime(end).build());
		assertDoesNotThrow(() -> TimerSpec.ofDelay(0).endTime(end).build());
	}

	@Test
	public void lateSimpleTimerCannotOverflowTheCurrentTimeAnchor() {
		for (var policy : new int[] {Timer.eMissfirePolicyNothing, Timer.eMissfirePolicyRunOnce,
				Timer.eMissfirePolicyRunOnceOldNext}) {
			for (boolean missfire : new boolean[] {false, true}) {
				var timer = new BSimpleTimer();
				timer.setNextExpectedTime(1);
				timer.setPeriod(Long.MAX_VALUE - 2);
				timer.setRemainTimes(-1);
				timer.setMissfirePolicy(policy);
				SimpleTimerSpec.beforeCallSimpleTimer(timer, missfire);
				assertEquals(1, timer.getExpectedTime());
				assertEquals(1, timer.getHappenTimes());
				assertEquals(missfire && policy == Timer.eMissfirePolicyRunOnce ? 0 : Long.MAX_VALUE - 1,
						timer.getNextExpectedTime());
			}
		}
	}
}
