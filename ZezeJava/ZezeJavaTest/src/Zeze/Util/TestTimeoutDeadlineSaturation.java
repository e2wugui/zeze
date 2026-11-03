package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestTimeoutDeadlineSaturation {
	@Test
	public void maximumDelayStaysInFutureBucket() {
		var manager = new TimeoutManager();
		var timeout = new TimeoutManager.Concurrent().schedule(manager.new Worker(),
				Long.MAX_VALUE, () -> { }, Timeout::new);
		assertEquals(0, timeout.getTimeoutSet().compareTo(
				new TimeoutManager.TimeoutSet(Long.MAX_VALUE / 1000 * 1000)));
		timeout.close();
	}
}
