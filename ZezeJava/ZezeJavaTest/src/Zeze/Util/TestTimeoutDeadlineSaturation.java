package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 上界校验即溢出防线：超界输入公开入口拒绝；满值（365天）入未来桶不回绕。 */
@Fast
public class TestTimeoutDeadlineSaturation {
	@Test
	public void maximumDelayIsBoundedAndFuture() {
		var manager = new TimeoutManager();
		manager.start();
		try {
			// 溢出量级输入被公开入口拒绝——上界校验（365天，与 Long.MAX 有 292× 余量）就是防线，
			// 内部计算不再为"按契约不可能"的输入做饱和防御（删守卫裁定哲学）。
			assertThrows(IllegalArgumentException.class,
					() -> manager.schedule(Long.MAX_VALUE, () -> { }));
			// 满值（365天）入未来桶：桶序大于刚建的近桶（若乘法溢出回绕成负桶，此处即红）。
			var year = manager.schedule(365L * 24 * 3600 * 1000, () -> { });
			var near = manager.schedule(1, () -> { });
			assertTrue(year.getTimeoutSet().compareTo(near.getTimeoutSet()) > 0,
					"满值必须落在未来桶（未溢出回绕）");
			year.close();
			near.close();
		} finally {
			manager.stop();
		}
	}
}
