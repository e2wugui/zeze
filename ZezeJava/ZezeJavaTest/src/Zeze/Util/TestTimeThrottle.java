package Zeze.Util;

import java.util.concurrent.atomic.AtomicLong;
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

	@Test
	public void testMonotonicWindowExpiryBoundary() {
		long base = 10_000_000_000L;
		var clockBeforeExpiry = new AtomicLong(base);
		try (var throttle = new TimeThrottleQueue(1, 2, 100, clockBeforeExpiry::get)) {
			Assertions.assertTrue(throttle.checkNow(60));
			clockBeforeExpiry.set(base + 999_999_999L);
			// 距首个包仅差 1ns 过期，60+40=100 达到 bandwidthLimit 应拒绝
			Assertions.assertFalse(throttle.checkNow(40));
		}

		var clockAtExpiry = new AtomicLong(base);
		try (var throttle = new TimeThrottleQueue(1, 2, 100, clockAtExpiry::get)) {
			Assertions.assertTrue(throttle.checkNow(60));
			clockAtExpiry.set(base + 1_000_000_000L);
			// 恰好经过 expireNs，首个包已淘汰，仅计本次 40 < 100 应放行
			Assertions.assertTrue(throttle.checkNow(40));
		}
	}

	@Test
	public void testMonotonicClockWrapAroundLongMaxValue() {
		long nearMax = Long.MAX_VALUE - 500_000_000L;
		var clockBeforeExpiry = new AtomicLong(nearMax);
		try (var throttle = new TimeThrottleQueue(1, 2, 100, clockBeforeExpiry::get)) {
			Assertions.assertTrue(throttle.checkNow(60));
			// 跨 Long.MAX_VALUE 回绕到负数，但已流逝 999_999_999ns < 1s，仍未过期
			clockBeforeExpiry.set(nearMax + 999_999_999L);
			Assertions.assertTrue(clockBeforeExpiry.get() < 0);
			Assertions.assertFalse(throttle.checkNow(40));
		}

		var clockAtExpiry = new AtomicLong(nearMax);
		try (var throttle = new TimeThrottleQueue(1, 2, 100, clockAtExpiry::get)) {
			Assertions.assertTrue(throttle.checkNow(60));
			// 跨 Long.MAX_VALUE 回绕到负数且恰好流逝 1s，旧包必须正常淘汰
			clockAtExpiry.set(nearMax + 1_000_000_000L);
			Assertions.assertTrue(clockAtExpiry.get() < 0);
			Assertions.assertTrue(throttle.checkNow(40));
			// 继续在负区间前进 1s 后仍正常滑动淘汰
			clockAtExpiry.set(clockAtExpiry.get() + 1_000_000_000L);
			Assertions.assertTrue(throttle.checkNow(99));
		}
	}
}
