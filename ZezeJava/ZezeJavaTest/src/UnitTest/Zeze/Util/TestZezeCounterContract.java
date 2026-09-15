package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.NoopCounter;
import Zeze.Util.PerfCounter;
import Zeze.Util.PrometheusCounter;
import Zeze.Util.ZezeCounter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ZezeCounter 契约测试：PerfCounter / PrometheusCounter / NoopCounter 三个实现跑同一套调用，
 * 保证"实现可插拔"的承诺有机器背书。共同要求是不抛异常，PerfCounter侧额外校验可观察输出。
 */
@Fast
public class TestZezeCounterContract {
	private static final AtomicInteger seq = new AtomicInteger();

	/** Prometheus指标挂在进程级defaultRegistry上，整个JVM只允许创建一个实例。 */
	private static final PrometheusCounter prometheus = new PrometheusCounter();

	private static String uniqueName(String prefix) {
		return prefix + "_contract_" + seq.incrementAndGet();
	}

	/** 三个实现共用的行为契约，返回allocCounter使用的名字。 */
	private static String runContract(ZezeCounter c) {
		var counterName = uniqueName("count");
		var counter = c.allocCounter(counterName);
		counter.increment();
		counter.inc(41);

		var labeled = c.allocLabeledCounterCreator(uniqueName("labeled"), "op");
		labeled.labelValues("a").inc(2);
		labeled.labelValues("b").increment();

		var observer = c.allocRunTimeObserverCreator(uniqueName("observe"), "op");
		observer.labelValues("x").observe(1_000_000);

		c.getRunTimeObserver("ContractRunKey").observe(2_000_000);
		c.addTaskRunTime("ContractTask", 3_000_000);

		c.procedureStart("ContractProc");
		c.procedureEnd("ContractProc", 0, 1_000_000);
		c.procedureEnd("ContractProc", 1, 2_000_000);
		c.procedureRedo("ContractProc");
		c.procedureRedoAndReleaseLock("ContractProc");

		for (var metric : ZezeCounter.TableMetric.values())
			c.tableCounter(0x1234, metric).increment();

		c.addRecvSizeTime(0x100, null, 64, 1_000_000);
		c.addRecvDispatchTime(0x100, 1_000);
		c.addSendSize(0x100, 64);
		for (var rc = 0; rc < 30; rc++) // 30个码，默认封顶20
			c.procedureEnd("ContractCapProc", 1_000 + rc, 1_000_000);
		return counterName;
	}

	@Test
	public void testPerfCounterContract() {
		var c = new PerfCounter();
		var counterName = runContract(c);
		var log = c.getLogAndReset();
		Assertions.assertTrue(log.contains("ContractProc"), "procedure section");
		Assertions.assertTrue(log.contains("ContractTask"), "run section");
		Assertions.assertTrue(log.contains("ContractRunKey"), "run observer");
		Assertions.assertTrue(log.contains("[table: 1]"), "table section");
		Assertions.assertTrue(log.contains(counterName + ": 42"), "count value 1+41");

		// Snapshot：收集即发布，结构化数据与formattedLog一致
		var snap = c.getLast();
		Assertions.assertEquals(Map.of(0L, 1L, 1L, 1L), snap.procedureResults().get("ContractProc"));
		Assertions.assertEquals(1L, snap.tableResults().get("4660").get("cacheGet"), "0x1234=4660");
		Assertions.assertFalse(snap.formattedLog().isEmpty());

		// result_code基数封顶：30个码只保留前20个
		var capped = c.getLast().procedureResults().get("ContractCapProc");
		Assertions.assertEquals(20, capped.size(), "result code capped");

		// 空窗口：条目保留（供名称列表），计数清零
		var empty = c.collectAndReset();
		Assertions.assertTrue(empty.procedureResults().get("ContractProc").isEmpty());
		Assertions.assertNotNull(empty.tableResults().get("4660"));
	}

	@Test
	public void testPrometheusCounterContract() {
		var counterName = runContract(prometheus);
		var names = new StringBuilder();
		for (var snapshot : PrometheusRegistry.defaultRegistry.scrape())
			names.append(snapshot.getMetadata().getName()).append('\n');
		var scrape = names.toString();
		Assertions.assertTrue(scrape.contains(counterName), "custom counter registered");
		Assertions.assertTrue(scrape.contains("procedure_completed"), "procedure metric");
		Assertions.assertTrue(scrape.contains("protocol_recv_bytes"), "protocol metric");
		Assertions.assertTrue(scrape.contains("database_table_operation"), "table metric");
		Assertions.assertTrue(scrape.contains("task_duration_seconds"), "task metric");
		Assertions.assertTrue(scrape.contains("protocol_dispatch_seconds"), "dispatch metric");

		// result_code基数封顶：procedure_completed的标签值应包含other且不超过20+1个
		var otherSeen = false;
		for (var snapshot : PrometheusRegistry.defaultRegistry.scrape()) {
			if (!snapshot.getMetadata().getName().equals("procedure_completed"))
				continue;
			for (var dp : snapshot.getDataPoints()) {
				if ("other".equals(dp.getLabels().get("result_code")))
					otherSeen = true;
			}
		}
		Assertions.assertTrue(otherSeen, "result code capped to other");
	}

	@Test
	public void testNoopCounterContract() {
		runContract(NoopCounter.instance); // 全部无操作、不抛异常即通过
		Assertions.assertSame(ZezeCounter.Snapshot.EMPTY, NoopCounter.instance.collectAndReset());
		Assertions.assertSame(ZezeCounter.Snapshot.EMPTY, NoopCounter.instance.getLast());
	}

	@Test
	public void testGlobalInstanceNotNull() {
		Assertions.assertNotNull(ZezeCounter.instance);
		// ENABLE随"真实实现是否存在"取值（instance不是NoopCounter时为true）
		Assertions.assertEquals(!(ZezeCounter.instance instanceof NoopCounter), ZezeCounter.ENABLE);
	}
}
