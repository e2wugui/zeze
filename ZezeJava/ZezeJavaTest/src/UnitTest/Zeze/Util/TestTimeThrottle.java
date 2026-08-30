package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.TimeThrottleQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestTimeThrottle {
	@Test
	public void testTimeThrottle() throws InterruptedException {
		try (var throttle = new TimeThrottleQueue(1, 3, 1000)) {
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertFalse(throttle.checkNow(1));
			// checkNow 是惰性过期（调用时现场清理 expire 之前的 mark，无后台定时器），睡过 expire+余量即可
			Thread.sleep(1200);
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertTrue(throttle.checkNow(1));
			Assertions.assertFalse(throttle.checkNow(1));
		}
	}
}
