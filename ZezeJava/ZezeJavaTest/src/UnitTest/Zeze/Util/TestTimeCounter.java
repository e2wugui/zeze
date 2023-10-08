package UnitTest.Zeze.Util;

import Zeze.Util.TimeCounter;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeCounter {
	@Test
	public void testTimeCounter() {
		var tc = new TimeCounter(2, false);
		tc.increment(1000);
		tc.increment(1001);
		Assert.assertEquals(2, tc.count());
		tc.increment(2000);
		tc.increment(2001);
		Assert.assertEquals(4, tc.count());
		tc.discard(4000);
		Assert.assertEquals(2, tc.count());
		tc.discard(5000);
		Assert.assertEquals(0, tc.count());
	}
}
