package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTimeThrottleBandwidth {
	private static void checkLargeWindow(TimeThrottle throttle) {
		// Four 1 GiB accounting events wrap an int to zero without allocating any payload.
		for (int i = 0; i < 4; i++)
			assertFalse(throttle.checkNow(1 << 30), "over-budget event " + i);
		assertFalse(throttle.checkNow(1), "the same window must remain over budget");
	}

	@Test
	void queueDoesNotForgetBandwidthAfterFourGiB() {
		try (var throttle = new TimeThrottleQueue(3600, 1, 1)) {
			checkLargeWindow(throttle);
		}
	}

	@Test
	void counterDoesNotForgetBandwidthAfterFourGiB() {
		Task.tryInitThreadPool();
		try (var throttle = new TimeThrottleCounter(3600, 1, 1)) {
			checkLargeWindow(throttle);
		}
	}

	@Test
	void ordinaryBandwidthBoundaryMatchesBothImplementations() {
		Task.tryInitThreadPool();
		try (var queue = new TimeThrottleQueue(3600, 1, 1);
			 var counter = new TimeThrottleCounter(3600, 1, 1)) {
			for (var throttle : new TimeThrottle[]{queue, counter}) {
				assertTrue(throttle.checkNow(3599));
				assertFalse(throttle.checkNow(1));
				assertFalse(throttle.checkNow(0));
			}
		}
	}
}
