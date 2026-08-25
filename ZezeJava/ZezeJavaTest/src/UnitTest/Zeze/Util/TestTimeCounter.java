package UnitTest.Zeze.Util;

import Zeze.Util.TimeCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTimeCounter {
	@Test
	public void testTimeCounter() {
		var tc = new TimeCounter(2, false);
		tc.increment(1);
		tc.increment(1);
		Assertions.assertEquals(2, tc.count());
		tc.increment(2);
		tc.increment(2);
		Assertions.assertEquals(4, tc.count());
		tc.discard(4);
		Assertions.assertEquals(2, tc.count());
		tc.discard(5);
		Assertions.assertEquals(0, tc.count());
	}
}
