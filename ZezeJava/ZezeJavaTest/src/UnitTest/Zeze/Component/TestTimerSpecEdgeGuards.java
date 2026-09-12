package UnitTest.Zeze.Component;

import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Component.AbstractTimer;
import Zeze.Component.CronTimerSpec;
import Zeze.Component.SimpleTimerSpec;
import Zeze.Component.TimerSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-44：SimpleTimerSpec.beforeCallSimpleTimer 的周期推进无溢出防护——period 使推进回绕为负后
 * <=now 恒真，fireSimple 的 delay=max(负-now,1) 恒 1ms，定时器无限重触发（每轮含事务与DB写）。
 * FND4-46：CronExpression.getNextValidTimeAfter 对无可行后续时间（过期年份）返回 null，
 * 原直接 getTime() 深处 NPE——入口显式拒绝为 IllegalArgumentException。
 */
@Fast
public class TestTimerSpecEdgeGuards {

	@Test
	public void testPeriodOverflowTerminates() {
		for (var policy : new int[]{AbstractTimer.eMissfirePolicyNothing,
				AbstractTimer.eMissfirePolicyRunOnce, AbstractTimer.eMissfirePolicyRunOnceOldNext}) {
			var t = new BSimpleTimer();
			t.setPeriod(Long.MAX_VALUE - 100); // 极端period：任意正基点相加必回绕为负
			t.setRemainTimes(-1); // 无限次
			t.setEndTime(0);
			t.setNextExpectedTime(1_000L); // 过去的触发点
			t.setMissfirePolicy(policy);
			SimpleTimerSpec.beforeCallSimpleTimer(t, false);
			Assertions.assertEquals(0L, t.getNextExpectedTime(),
					"period溢出必须终止调度（nextExpectedTime=0），policy=" + policy
							+ " got " + t.getNextExpectedTime());
		}

		// 回归：正常period定点推进不受影响
		var t = new BSimpleTimer();
		t.setPeriod(10_000);
		t.setRemainTimes(-1);
		t.setEndTime(0);
		t.setNextExpectedTime(System.currentTimeMillis() - 1);
		t.setMissfirePolicy(AbstractTimer.eMissfirePolicyNothing);
		SimpleTimerSpec.beforeCallSimpleTimer(t, false);
		Assertions.assertTrue(t.getNextExpectedTime() > 0);
	}

	@Test
	public void testExpiredCronRejectedExplicitly() throws Exception {
		// 过期年份表达式：Quartz系getNextValidTimeAfter返回null（文档行为）
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> TimerSpec.ofCron("0 0 0 1 1 ? 2020").build(),
				"无可行后续时间的cron必须在入口显式拒绝（原深处NPE）");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> CronTimerSpec.cronNextTime("0 0 0 1 1 ? 2020", System.currentTimeMillis()));

		// 回归：正常表达式不受影响
		Assertions.assertTrue(CronTimerSpec.cronNextTime("* * * * * ?", System.currentTimeMillis()) > 0);
	}
}
