package UnitTest.Zeze.Arch;

import Zeze.Arch.LoadConfig;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A1-F3回归：digestionDelayExSeconds接受0/负值——timeoutDelaySeconds=0使LoadBase
 * 的慢报累计+=0永不达阈值（负载上报静默停止），且finally resume(0)→scheduleNow(0)
 * 定时链即时自续空转。修复：setter拒绝<=0（与setMaxOnlineNew同构）。
 */
@Fast
public class TestA1F3LoadConfigValidation {

	@Test
	public void testDigestionDelayExSecondsRejectsNonPositive() {
		var conf = new LoadConfig();
		assertEquals(1, conf.getDigestionDelayExSeconds(), "默认值保持1");

		assertThrows(IllegalArgumentException.class, () -> conf.setDigestionDelayExSeconds(0), "0必须被拒绝");
		assertThrows(IllegalArgumentException.class, () -> conf.setDigestionDelayExSeconds(-1), "负值必须被拒绝");
		assertEquals(1, conf.getDigestionDelayExSeconds(), "拒绝后原值不变");

		assertDoesNotThrow(() -> conf.setDigestionDelayExSeconds(2));
		assertEquals(2, conf.getDigestionDelayExSeconds());
	}
}
