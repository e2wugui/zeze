package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-47 回归（策略级）：BinLogger写失败恢复的halt判据。FND4-68把恢复循环
 * 从"无限重试"改为"3次×100ms即halt(543543)"——Windows备份/防毒短暂锁定当天
 * 日志文件、NAS抖动等可自愈瞬态故障的自愈周期常超过300ms，被一刀切halt
 * （连同ShutdownHook跳过）。修复：恢复失败改按观察窗判死——退避100ms翻倍
 * （封顶5s）持续重开，累计观察60s仍失败才halt。
 * halt本身不可在测试JVM内行为验证（会杀worker），本用例锁策略契约：
 * 观察窗常量存在且≥30s；退避按指数增长且封顶5s；按退避序列累计达到观察窗
 * 的时点落在[30s,120s]（既非300ms一刀切，也不无限滞留）。修复前常量/方法
 * 不存在，反射失败即红。
 * 行为面：单次写失败后恢复（不触halt）由既有TestBinLoggerWriteFail覆盖。
 */
@Fast
public class TestBinLoggerRecoverHaltWindow {

	@Test
	public void testHaltWindowPolicy() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;

		long haltWindowMs;
		try {
			var f = serviceClass.getDeclaredField("WRITE_RECOVER_HALT_WINDOW_MS");
			f.setAccessible(true);
			haltWindowMs = f.getLong(null);
		} catch (NoSuchFieldException e) {
			Assertions.fail("halt判据必须为观察窗常量WRITE_RECOVER_HALT_WINDOW_MS（FND5-47），实际不存在: " + e);
			return;
		}
		Assertions.assertTrue(haltWindowMs >= 30_000, "观察窗必须≥30s（瞬态故障自愈周期），实际=" + haltWindowMs);
		Assertions.assertTrue(haltWindowMs <= 120_000, "观察窗必须有限（持久故障仍终止），实际=" + haltWindowMs);

		Method backoff;
		try {
			backoff = serviceClass.getDeclaredMethod("recoverBackoffMs", int.class);
			backoff.setAccessible(true);
		} catch (NoSuchMethodException e) {
			Assertions.fail("退避计算必须独立为recoverBackoffMs（FND5-47），实际不存在: " + e);
			return;
		}
		// 指数增长：100,200,400,800,1600,3200；封顶5s。
		Assertions.assertEquals(100L, backoff.invoke(null, 0), "首次退避100ms");
		Assertions.assertEquals(200L, backoff.invoke(null, 1));
		Assertions.assertEquals(800L, backoff.invoke(null, 3));
		Assertions.assertEquals(3200L, backoff.invoke(null, 5));
		Assertions.assertEquals(5000L, backoff.invoke(null, 6), "退避封顶5s");
		Assertions.assertEquals(5000L, backoff.invoke(null, 100), "退避封顶5s");

		// 按退避序列累计达到观察窗的时点：应落在[30s,120s]——修复前为300ms。
		long cumulative = 0;
		int exp = 0;
		while (cumulative < haltWindowMs)
			cumulative += (long)backoff.invoke(null, exp++);
		Assertions.assertTrue(cumulative >= 30_000 && cumulative <= 120_000,
				"累计判死时点必须落在[30s,120s]，实际=" + cumulative + "ms（" + exp + "次重试）");
	}
}
