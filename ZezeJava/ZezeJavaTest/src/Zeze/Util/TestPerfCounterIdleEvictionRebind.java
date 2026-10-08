package Zeze.Util;

import Zeze.Util.PerfCounter;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND12 util-02回归：collectAndResetCore的空闲淘汰（连续MAX_IDLE_COUNT=10轮零计数
 * it.remove()）原先不推进clearSerial，而observer闭包/PerfProcedureCounter的重绑只挂
 * serial!=clearSerial——被淘汰条目的终身缓存句柄（RocksDatabase静态observer、
 * Procedure懒解析counter）此后永久写入已脱离map的对象，该统计从周期日志静默消失
 * 直到重启或resetCounter（夜间空闲≥10个收集周期被回收、日间恢复即触发）。
 * 修复：空闲淘汰发生时推进clearSerial；getRunInfoWithSerial命中时重盖serial戳，
 * 使存活句柄重绑一次后重新稳定。本测试直接驱动getLogAndReset()模拟收集周期，
 * 钉住可观察契约——空闲淘汰后恢复的统计必须重新出现。
 */
@Fast
public class TestPerfCounterIdleEvictionRebind {

	@Test
	public void testRunTimeObserverRebindsAfterIdleEviction() {
		var pc = new PerfCounter();
		var obs = pc.getRunTimeObserver("PerfIdleEviction.run");
		obs.observe(100);
		pc.getLogAndReset(); // 收集到1次计数：条目存活、idleCount归零
		for (int i = 0; i < 10; i++)
			pc.getLogAndReset(); // 连续10轮零计数：条目被空闲淘汰
		obs.observe(300); // 低峰恢复：句柄必须重绑新代际条目并继续计数
		obs.observe(400);
		var log = pc.getLogAndReset();
		Assertions.assertTrue(log.contains("TestFnd12Util02.run: 0ms = 2 * 350ns"),
				"observer must rebind after idle eviction, log:\n" + log);
	}

	@Test
	public void testProcedureCounterRebindsAfterIdleEviction() {
		var pc = new PerfCounter();
		var counter = pc.allocProcedureCounter("PerfIdleEviction.proc");
		counter.end(0, 1);
		pc.getLogAndReset();
		for (int i = 0; i < 10; i++)
			pc.getLogAndReset();
		counter.end(0, 2);
		counter.end(0, 3);
		var log = pc.getLogAndReset();
		Assertions.assertTrue(log.contains("TestFnd12Util02.proc:100%, 0:2"),
				"procedure counter must rebind after idle eviction, log:\n" + log);
	}
}
