package UnitTest.Zeze.Util;

import java.util.concurrent.TimeUnit;
import Zeze.Util.GlobalTimer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND8-12回归：GlobalTimer.getCurrentSeconds原用System.nanoTime()/1e9——任意原点的
 * 单调秒（系统启动基准），与epoch毫秒的getCurrentMillis相差可达数年，同名族隐含的
 * getCurrentMillis()/1000 == getCurrentSeconds()契约不成立，与epoch秒作差/比较的
 * 代码会得到荒谬结果。修复：curSec改用currentTimeMillis()/1000（同源epoch），
 * 代价为放弃单调性（NTP回拨期间超时判定延迟、可自愈，可忽略）。
 */
@Fast
public class TestFnd812GlobalTimerEpochClock {

	@BeforeAll
	public static void initPool() {
		// GlobalTimer类初始化注册周期任务，需调度池先就绪
		Task.tryInitThreadPool();
	}

	@Test
	public void testSecondsIsEpochAndSameSourceAsMillis() {
		var epochSec = System.currentTimeMillis() / 1000;
		var sec = GlobalTimer.getCurrentSeconds();
		// 修复前红：nanoTime任意原点，与epoch相差可达数年
		Assertions.assertTrue(Math.abs(sec - epochSec) <= 2,
				"getCurrentSeconds必须是epoch秒，偏差不超过采样陈旧（实际差=" + (sec - epochSec) + "s）");

		var ms = GlobalTimer.getCurrentMillis();
		var sec2 = GlobalTimer.getCurrentSeconds();
		// 同源采样周期1s：两getter相差0~2秒属正常
		Assertions.assertTrue(Math.abs(ms / 1000 - sec2) <= 2,
				"getCurrentMillis()/1000与getCurrentSeconds()同源，相差应在采样周期内");
	}

	@Test
	public void testSecondsAdvancesWithPeriodicRefresh() {
		var start = GlobalTimer.getCurrentSeconds();
		// 轮询等待周期任务刷新（首发延迟1s+周期1s），不用裸sleep赌时序
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (GlobalTimer.getCurrentSeconds() == start) {
			Assertions.assertTrue(System.nanoTime() < deadline, "getCurrentSeconds必须在10秒内被周期任务刷新");
			Thread.onSpinWait();
		}
		// 刷新后仍是epoch
		var epochSec = System.currentTimeMillis() / 1000;
		Assertions.assertTrue(Math.abs(GlobalTimer.getCurrentSeconds() - epochSec) <= 2,
				"刷新后的值必须保持epoch语义");
	}
}
