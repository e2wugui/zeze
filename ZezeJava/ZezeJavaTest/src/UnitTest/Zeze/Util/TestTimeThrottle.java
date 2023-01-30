package UnitTest.Zeze.Util;

import Zeze.Util.TimeThrottleQueue;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeThrottle {
	@Test
	public void testTimeThrottle() throws InterruptedException {
		var throttle = new TimeThrottleQueue(2, 3);
		Assert.assertTrue(throttle.markNow());
		Assert.assertTrue(throttle.markNow());
		Assert.assertTrue(throttle.markNow());
		Assert.assertFalse(throttle.markNow());
		Thread.sleep(2100);
		Assert.assertTrue(throttle.markNow());
		Assert.assertTrue(throttle.markNow());
		Assert.assertTrue(throttle.markNow());
		Assert.assertFalse(throttle.markNow());
	}
}
