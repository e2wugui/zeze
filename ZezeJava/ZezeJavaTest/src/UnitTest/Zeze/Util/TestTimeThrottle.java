package UnitTest.Zeze.Util;

import Zeze.Util.TimeThrottleQueue;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeThrottle {
	@Test
	public void testTimeThrottle() throws InterruptedException {
		var throttle = new TimeThrottleQueue(1, 3, 1000);
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertFalse(throttle.checkNow(1));
		Thread.sleep(2100);
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertTrue(throttle.checkNow(1));
		Assert.assertFalse(throttle.checkNow(1));
	}
}
