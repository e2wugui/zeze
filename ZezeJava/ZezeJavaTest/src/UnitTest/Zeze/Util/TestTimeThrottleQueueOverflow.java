package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.TimeThrottleQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 构造参数乘法溢出校验（FND2-U0-5）：
 * seconds*1000 / limit*seconds / bandwidthLimit*seconds 原先按 int 静默回绕——
 * expire 为负会让 checkNow 的过期清理循环立即 break（marks 永不淘汰，限流器退化
 * 为恒 return false 的全拒绝），阈值同样静默失真。修复后乘积溢出构造期抛 IAE。
 */
@Fast
public class TestTimeThrottleQueueOverflow {

	@Test
	public void testConstructorOverflow() {
		// seconds*1000 溢出（seconds > MAX/1000 ≈ 24.8天窗口）
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new TimeThrottleQueue(Integer.MAX_VALUE / 1000 + 1, 1, 1));
		// limit*seconds 溢出
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new TimeThrottleQueue(2, Integer.MAX_VALUE / 2 + 1, 1));
		// bandwidthLimit*seconds 溢出
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new TimeThrottleQueue(2, 1, Integer.MAX_VALUE / 2 + 1));
		// 三个乘积都恰好不溢出的边界值应放行（只构造，不跑限流逻辑）
		Assertions.assertDoesNotThrow(() -> new TimeThrottleQueue(Integer.MAX_VALUE / 1000, 0, 0));
		Assertions.assertDoesNotThrow(() -> new TimeThrottleQueue(2, Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2));
		// 常规参数
		Assertions.assertDoesNotThrow(() -> new TimeThrottleQueue(1, 3, 1000));
	}
}
