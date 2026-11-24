package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestTimeCounterDiscardAfterClockRollback {
	@Test
	public void discardsExpiredBucketEvenWhenAnEarlierSlotIsStillCurrent() {
		try (var counter = new TimeCounter(2, false)) {
			counter.increment(10);
			counter.increment(7);

			counter.discard(10);
			Assertions.assertEquals(1, counter.count());
		}
	}
}
