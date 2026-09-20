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
		c.addTaskRunTime(TestZezeCounterContract.class, 4_000_000); // Class键归一化为类名，与字符串键同条目

		var proc = c.allocProcedureCounter("ContractProc");
		proc.start();
		proc.end(0, 1_000_000);
		proc.end(1, 2_000_000);
		proc.redo();
		proc.redoAndReleaseLock();

		for (var metric : ZezeCounter.TableMetric.values())
			c.tableCounter(0x1234, metric).increment();

		c.addRecvSizeTime(0x100, null, 64, 1_000_000);
		c.addRecvDispatchTime(0x100, 1_000);
		c.addSendSize(0x100, 64);
		var capProc = c.allocProcedureCounter("ContractCapProc");
		for (var rc = 0; rc < 30; rc++) // 30个码，默认封顶20
			capProc.end(1_000 + rc, 1_000_000);
		return counterName;
	}

	@Test
	public void testPerfCounterContract() {
		var c = new PerfCounter();
		var counterName = runContract(c);
		var log = c.getLogAndReset();
		Assertions.assertTrue(log.contains("ContractProc"), "procedure section");
		Assertions.assertTrue(log.contains("ContractTask"), "run section");
		Assertions.assertTrue(log.contains("UnitTest.Zeze.Util.TestZezeCounterContract"), "class key normalized");
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

	/**
	 * U4-F3：getRunTimeObserver 的 key 规范化后查重。原实现 map 按原始 name 去重、
	 * 注册名却经 builder 内部规范化，二者非单射——不同 key（如 "Foo.Bar"/"Foo-Bar"）
	 * 注册出同名指标时 register() 抛异常打穿调用方（BinLogger 静态初始化即死）。
	 * 修复：以 sanitizeMetricName+prometheusName 复合规范化后的名字作 map 键，
	 * 碰撞 key 共享同一 observer（Prometheus 侧指标名即身份，共享是唯一优雅降级）。
	 */
	@Test
	public void testGetRunTimeObserverSanitizeDedup() {
		var o1 = prometheus.getRunTimeObserver("Contract.Sanitize.Key");
		var o2 = prometheus.getRunTimeObserver("Contract-Sanitize-Key"); // 规范化后同名
		Assertions.assertSame(o1, o2, "规范化碰撞的 key 必须共享同一 observer（修复前此处抛 IllegalStateException）");
		Assertions.assertSame(o1, prometheus.getRunTimeObserver("Contract.Sanitize.Key"), "同 key 幂等");
		o1.observe(1_000_000);
		o2.observe(2_000_000); // 共享 observer，不抛
	}

	@Test
	public void testGlobalInstanceNotNull() {
		Assertions.assertNotNull(ZezeCounter.instance);
		// ENABLE随"真实实现是否存在"取值（instance不是NoopCounter时为true）
		Assertions.assertEquals(!(ZezeCounter.instance instanceof NoopCounter), ZezeCounter.ENABLE);
	}
}
