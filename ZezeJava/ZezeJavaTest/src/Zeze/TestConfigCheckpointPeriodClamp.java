package Zeze;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Config.setCheckpointPeriod 无下界校验：程序化配置 0/负值后检查点线程
 * cond.await(非正时长) 立即返回，主循环无 sleep 忙转（CPU 100%）；
 * XML 路径 parse 对同样输入显式抛错，setter 侧静默放过。
 *
 * 修复：setter 对齐下界语义，非法值（&lt;1000ms）warn 并夹紧到 1000ms。
 */
@Fast
public class TestConfigCheckpointPeriodClamp {

	@Test
	public void testIllegalValuesClamped() {
		var conf = new Config();
		conf.setCheckpointPeriod(0);
		Assertions.assertEquals(1000, conf.getCheckpointPeriod(), "0应夹紧到1000ms，避免忙转");
		conf.setCheckpointPeriod(-5);
		Assertions.assertEquals(1000, conf.getCheckpointPeriod(), "负值应夹紧到1000ms");
		conf.setCheckpointPeriod(999);
		Assertions.assertEquals(1000, conf.getCheckpointPeriod(), "低于下界应夹紧到1000ms");
	}

	@Test
	public void testValidValuesUnchanged() {
		var conf = new Config();
		conf.setCheckpointPeriod(1000);
		Assertions.assertEquals(1000, conf.getCheckpointPeriod());
		conf.setCheckpointPeriod(60000);
		Assertions.assertEquals(60000, conf.getCheckpointPeriod(), "合法值不受夹紧影响");
	}
}
