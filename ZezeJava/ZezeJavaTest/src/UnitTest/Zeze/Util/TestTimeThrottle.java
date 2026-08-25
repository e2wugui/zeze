package UnitTest.Zeze.Util;

import Zeze.Util.TimeThrottleQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTimeThrottle {
	@Test
	public void testTimeThrottle() throws InterruptedException {
		var throttle = new TimeThrottleQueue(1, 3, 1000);
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertFalse(throttle.checkNow(1));
		Thread.sleep(2100);
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertTrue(throttle.checkNow(1));
		Assertions.assertFalse(throttle.checkNow(1));
	}
}
