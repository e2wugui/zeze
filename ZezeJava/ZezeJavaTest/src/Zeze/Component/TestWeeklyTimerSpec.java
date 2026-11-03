package Zeze.Component;

import java.util.Calendar;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestWeeklyTimerSpec {
	@Test
	public void everyWeekdayBuildsAndFiresAtRequestedTime() {
		for (int weekday = Calendar.SUNDAY; weekday <= Calendar.SATURDAY; weekday++) {
			var spec = TimerSpec.ofWeek(weekday, 8, 30, 15);
			var next = Calendar.getInstance();
			next.setTimeInMillis(spec.build().getNextExpectedTime());
			Assertions.assertEquals(weekday, next.get(Calendar.DAY_OF_WEEK));
			Assertions.assertEquals(8, next.get(Calendar.HOUR_OF_DAY));
			Assertions.assertEquals(30, next.get(Calendar.MINUTE));
			Assertions.assertEquals(15, next.get(Calendar.SECOND));
		}
	}
}
