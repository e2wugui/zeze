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




}
