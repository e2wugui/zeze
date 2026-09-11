package UnitTest.Zeze.Component;

import harness.Fast;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Component.AbstractTimer;
import Zeze.Component.CronTimerSpec;
import Zeze.Component.SimpleTimerSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * missfire 策略的运行期生效（FND3-30）：推进一跳后仍在过去（迟到超过一个整周期）时，
 * Nothing/RunOnce 以当前时间+period 重设、OldNext 跳到未来最近定点——终止追赶式连发。
 * 判据避开毫秒级抖动：跳后仍在未来则维持固定速率推进。
 * 装载期（missfire=true）既有行为作为回归护栏。
 */
@Fast
public class TestTimerMissfireRuntime {
	private static final long PERIOD = 10_000;
	private static final String CRON = "0/10 * * * * ?"; // 每10秒一个触发点

	private static BSimpleTimer newSimple(long nextExpectedTime, int missfirePolicy) {
		var t = new BSimpleTimer();
		t.setPeriod(PERIOD);
		t.setRemainTimes(-1); // 无限次
		t.setEndTime(0); // 无限制
		t.setNextExpectedTime(nextExpectedTime);
		t.setMissfirePolicy(missfirePolicy);
		return t;
	}

	private static BCronTimer newCron(long nextExpectedTime, int missfirePolicy) {
		var t = new BCronTimer();
		t.setCronExpression(CRON);
		t.setRemainTimes(-1);
		t.setEndTime(0);
		t.setNextExpectedTime(nextExpectedTime);
		t.setMissfirePolicy(missfirePolicy);
		return t;
	}

	// 运行期（missfire=false）迟到多个周期：修复前 nextExpectedTime 仍落在过去——追赶式连发的根源
	@Test
	public void testSimpleRuntimeLate() {
		for (var policy : new int[]{AbstractTimer.eMissfirePolicyNothing,
				AbstractTimer.eMissfirePolicyRunOnce}) {
			var t0 = System.currentTimeMillis();
			var timer = newSimple(t0 - 10 * PERIOD, policy);
			SimpleTimerSpec.beforeCallSimpleTimer(timer, false);
			var t1 = System.currentTimeMillis();
			// Nothing/RunOnce：以当前时间+period 重设（放行在途这一次，终止连发）
			Assertions.assertTrue(timer.getNextExpectedTime() >= t0 + PERIOD
							&& timer.getNextExpectedTime() <= t1 + PERIOD,
					"policy=" + policy + " next=" + timer.getNextExpectedTime());
		}

		// OldNext：跳到未来最近定点并保持对齐
		var base = 1_000_000L; // 任意固定基准
		var t0 = System.currentTimeMillis();
		var timer = newSimple(base, AbstractTimer.eMissfirePolicyRunOnceOldNext);
		SimpleTimerSpec.beforeCallSimpleTimer(timer, false);
		var t1 = System.currentTimeMillis();
		var next = timer.getNextExpectedTime();
		Assertions.assertTrue(next > t0 && next <= t1 + PERIOD, "next=" + next);
		Assertions.assertEquals(0, (next - base) % PERIOD, "OldNext must stay aligned");
	}

	// 毫秒级抖动（推进一跳后仍在未来）：维持固定速率推进，不重设——任何策略
	@Test
	public void testSimpleJitterKeepsRate() {
		for (var policy : new int[]{AbstractTimer.eMissfirePolicyNothing,
				AbstractTimer.eMissfirePolicyRunOnce, AbstractTimer.eMissfirePolicyRunOnceOldNext}) {
			var now = System.currentTimeMillis();
			var next = now - 5; // 迟到5ms，远小于一个周期
			var timer = newSimple(next, policy);
			SimpleTimerSpec.beforeCallSimpleTimer(timer, false);
			Assertions.assertEquals(next + PERIOD, timer.getNextExpectedTime(), "policy=" + policy);
		}
	}

	// 装载期（missfire=true）回归护栏：RunOnce 任意迟到以 now+period 重设；OldNext 迟到跳点
	@Test
	public void testSimpleLoadFlag() {
		var t0 = System.currentTimeMillis();
		var timer = newSimple(t0 - 5, AbstractTimer.eMissfirePolicyRunOnce);
		SimpleTimerSpec.beforeCallSimpleTimer(timer, true);
		var t1 = System.currentTimeMillis();
		Assertions.assertTrue(timer.getNextExpectedTime() >= t0 + PERIOD
				&& timer.getNextExpectedTime() <= t1 + PERIOD);

		timer = newSimple(t0 - 10 * PERIOD, AbstractTimer.eMissfirePolicyRunOnceOldNext);
		SimpleTimerSpec.beforeCallSimpleTimer(timer, true);
		var next = timer.getNextExpectedTime();
		Assertions.assertTrue(next > t0 && next <= t1 + PERIOD, "next=" + next);
		Assertions.assertEquals(0, (next - (t0 - 10 * PERIOD + PERIOD)) % PERIOD, "must stay aligned");
	}

	// cron 运行期迟到：修复前按旧触发点序列只前进一个槽（仍在过去）；修复后从 now 重算
	@Test
	public void testCronRuntimeLate() throws Exception {
		for (var policy : new int[]{AbstractTimer.eMissfirePolicyNothing,
				AbstractTimer.eMissfirePolicyRunOnce, AbstractTimer.eMissfirePolicyRunOnceOldNext}) {
			var t0 = System.currentTimeMillis();
			var slot = t0 / 10_000 * 10_000 - 60_000; // 60秒前的触发点
			var timer = newCron(slot, policy);
			var hasNext = CronTimerSpec.nextCronTimer(timer, false);
			var t1 = System.currentTimeMillis();
			Assertions.assertTrue(hasNext, "policy=" + policy);
			var next = timer.getNextExpectedTime();
			Assertions.assertTrue(next > t0 && next <= t1 + 10_000, "policy=" + policy + " next=" + next);
			Assertions.assertEquals(0, next % 10_000, "must stay on cron slot");
		}
	}

	// cron 毫秒级抖动（本触发点内醒来）：推进到下一个触发点，不重设
	@Test
	public void testCronJitterKeepsRate() throws Exception {
		var now = System.currentTimeMillis();
		var slot = now / 10_000 * 10_000; // 本触发点（最多迟到10秒以内）
		var timer = newCron(slot, AbstractTimer.eMissfirePolicyNothing);
		Assertions.assertTrue(CronTimerSpec.nextCronTimer(timer, false));
		Assertions.assertEquals(slot + 10_000, timer.getNextExpectedTime());
	}

	// cron 装载期（missfire=true）护栏：RunOnce 以当前时间为基准重设
	@Test
	public void testCronLoadFlag() throws Exception {
		var t0 = System.currentTimeMillis();
		var slot = t0 / 10_000 * 10_000 - 60_000;
		var timer = newCron(slot, AbstractTimer.eMissfirePolicyRunOnce);
		Assertions.assertTrue(CronTimerSpec.nextCronTimer(timer, true));
		var next = timer.getNextExpectedTime();
		Assertions.assertTrue(next > t0 && next <= t0 + 10_000, "next=" + next);
	}
}
