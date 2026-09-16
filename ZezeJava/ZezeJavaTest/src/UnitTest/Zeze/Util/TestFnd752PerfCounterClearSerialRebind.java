package UnitTest.Zeze.Util;

import Zeze.Util.PerfCounter;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-52回归：PerfCounter.clearSerial在resetCounter锁内唯一写、observer闭包与
 * PerfProcedureCounter.info锁外多读，字段非volatile——JMM下无happens-before，
 * 普通字段读可被JIT提升出外层循环，闭包长期绑定reset前已从map移除的统计对象，
 * 自增落在无人收集的对象上，统计静默丢失（同族resultMap/resultMapLast/bound
 * 已因同类问题改volatile）。
 * 修复：clearSerial声明为volatile。
 * 说明：JMM可见性缺陷无法从外部确定性复现（同TestFnd728口径，x86强内存模型下
 * 观察不到stale读）；此测试钉住可观察契约——resetCounter之后缓存的observer/
 * procedureCounter必须重绑到新代际统计对象并继续计数（丢失即红）。
 */
@Fast
public class TestFnd752PerfCounterClearSerialRebind {

	@Test
	public void testRunTimeObserverRebindsAfterReset() {
		var pc = new PerfCounter();
		var obs = pc.getRunTimeObserver("TestFnd752.run");
		obs.observe(100);
		obs.observe(200);
		pc.resetCounter(); // clearSerial代际推进并清空runInfoMap
		obs.observe(300); // 必须重绑新代际RunInfo，统计不丢
		obs.observe(400);
		var log = pc.getLogAndReset();
		// 新代际条目：2次、700ns（旧代际计数已被reset清零，若闭包仍绑旧对象则条目缺失）
		Assertions.assertTrue(log.contains("TestFnd752.run: 0ms = 2 * 350ns"),
				"observer must rebind after resetCounter, log:\n" + log);
	}

	@Test
	public void testProcedureCounterRebindsAfterReset() {
		var pc = new PerfCounter();
		var counter = pc.allocProcedureCounter("TestFnd752.proc");
		counter.end(0, 1);
		pc.resetCounter(); // 清空procedureInfoMap，代际推进
		counter.end(0, 2); // PerfProcedureCounter.info按serial重绑
		counter.end(0, 3);
		var log = pc.getLogAndReset();
		// 新代际条目：结果码0计2次、成功率100%
		Assertions.assertTrue(log.contains("TestFnd752.proc:100%, 0:2"),
				"procedure counter must rebind after resetCounter, log:\n" + log);
	}
}
