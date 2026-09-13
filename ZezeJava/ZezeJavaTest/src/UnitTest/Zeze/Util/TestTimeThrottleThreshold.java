package UnitTest.Zeze.Util;

import Zeze.Util.Task;
import Zeze.Util.TimeThrottleCounter;
import Zeze.Util.TimeThrottleQueue;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND5-10：Counter与Queue两实现对limit的语义必须一致（limit=每窗口放行量）。
 * 曾为counter&lt;limit（第limit个即拒）vs marks.size()&lt;=limit（第limit+1个才拒），
 * 同配置切换实现名断连临界点差1（Service.checkThrottle超限直接断连）。
 */
@Fast
public class TestTimeThrottleThreshold {
	private TimeThrottleCounter counter;
	private TimeThrottleQueue queue;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
		counter = new TimeThrottleCounter(1, 3, Integer.MAX_VALUE / 2);
		queue = new TimeThrottleQueue(1, 3, Integer.MAX_VALUE / 2);
	}

	@AfterEach
	public void after() {
		counter.close();
		queue.close();
	}

	@Test
	public void testSameThresholdSemantics() {
		// 同参数下两实现的放行/拒绝时序必须逐包一致：1..limit放行，limit+1拒绝。
		for (int i = 1; i <= 3; i++)
			Assertions.assertTrue(counter.checkNow(0), "Counter第" + i + "个包（<=limit）应放行");
		Assertions.assertFalse(counter.checkNow(0), "Counter第limit+1个包应拒绝");

		for (int i = 1; i <= 3; i++)
			Assertions.assertTrue(queue.checkNow(0), "Queue第" + i + "个包（<=limit）应放行");
		Assertions.assertFalse(queue.checkNow(0), "Queue第limit+1个包应拒绝");
	}
}
