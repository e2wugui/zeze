package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Util.ZezeCounter;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 过程速率watch的差分必须用单调累计源：周期快照（getLast().procedureResults()）
 * 每轮换新且只含上一周期计数，(total-last)在稳态恒0——动态降级回调永不触发；
 * 收集边界恰好落入检查间隔时又把整周期计数当30s速率（约3.3倍高估）。
 * 修复后按名称的单调累计计数做差分，35次/30s（≈1.17/s）对阈值1必须触发，
 * 且无新增执行不得重复触发。
 */
@Fast
public class TestProcedureStatisticsRateWatch {

	@Test
	public void rateThresholdTriggersOnMonotonicDelta() {
		// 默认ZezeCounter实现即PerfCounter（提供单调累计源），见类注释的知情边界。
		assertTrue(ZezeCounter.instance instanceof Zeze.Util.PerfCounter,
				"本测试需要默认PerfCounter实现");

		var name = "TestProcedureStatisticsRateWatch";
		var handleRan = new AtomicInteger();
		var counter = ZezeCounter.instance.allocProcedureCounter(name);
		var watcher = new ProcedureStatistics.Watcher(name, 1, handleRan::incrementAndGet);

		watcher.check(); // 基线：构造以来的增量为0，不得触发
		assertEquals(0, handleRan.get(), "无执行不得触发");

		for (int i = 0; i < 35; i++) // 35次/30s ≈ 1.17/s，达到阈值1
			counter.end(0, 1000);

		watcher.check();
		assertEquals(1, handleRan.get(), "达到阈值的执行速率必须触发回调（周期快照差分恒0永不触发）");

		watcher.check(); // 无新增执行：单调差分回到0，不得重复触发
		assertEquals(1, handleRan.get(), "无新增执行不得重复触发");
	}
}
