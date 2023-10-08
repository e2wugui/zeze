package UnitTest.Zeze.Util;

import Zeze.Util.TimeCounter;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeCounter {
	@Test
	public void testTimeCounter() {
		var tc = new TimeCounter(2, false);
		tc.increment(1);
		tc.increment(1);
		Assert.assertEquals(2, tc.count());
		tc.increment(2);
		tc.increment(2);
		Assert.assertEquals(4, tc.count());
		tc.discard(4);
		Assert.assertEquals(2, tc.count());
		tc.discard(5);
		Assert.assertEquals(0, tc.count());
	}
}
