package Zeze.Util;

import java.util.Map;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 统计快照是值对象，深不可变：只冻结外层Map时，持有快照者可改内层计数
 * 污染getLast的后续读取（实测改snapshot.tableResults内层cacheGet为999后
 * getLast同样读到999）；构造输入的别名同理。两层逐层Map.copyOf，
 * 内层修改抛UnsupportedOperationException（值对象契约的显式行为变化）。
 */
@Fast
public class TestCounterSnapshotDeepImmutable {

	@Test
	public void snapshotInnerMapsAreImmutableAndAliasedInputIsCopied() {
		var inner = new java.util.HashMap<String, Long>();
		inner.put("cacheGet", 7L);
		var tableResults = new java.util.HashMap<String, Map<String, Long>>();
		tableResults.put("t1", inner);
		var snapshot = new ZezeCounter.Snapshot(Map.of(), tableResults, "log");

		// 构造后修改构造输入不得影响快照（别名切断）
		inner.put("cacheGet", 999L);
		assertEquals(7L, snapshot.tableResults().get("t1").get("cacheGet"));

		// 内层不可变：修改抛UOE而不是污染
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.tableResults().get("t1").put("cacheGet", 999L));
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.tableResults().put("t2", Map.of()));
	}

	@Test
	public void perCounterCollectProducesDeepImmutableSnapshot() {
		Assertions.assertTrue(ZezeCounter.instance instanceof PerfCounter, "需要默认PerfCounter实现");
		var perf = (PerfCounter)ZezeCounter.instance;
		var handle = perf.tableCounter(0x7e5e7e5fL, ZezeCounter.TableMetric.CACHE_GET);
		handle.inc(3);
		var snapshot = perf.collectAndReset();
		assertEquals(3L, snapshot.tableResults().get(String.valueOf(0x7e5e7e5fL)).get("cacheGet"));
		assertThrows(UnsupportedOperationException.class,
				() -> snapshot.tableResults().get(String.valueOf(0x7e5e7e5fL)).put("cacheGet", 999L));
	}
}
