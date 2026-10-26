package Zeze.Util;

import java.util.Map;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 快照的Map按调用约定只读；后续采集和计数器重置不得改变已经发布的结果。
 */
@Fast
public class TestCounterSnapshotStability {

	@Test
	public void laterCollectionDoesNotChangeEarlierSnapshot() {
		var perf = new PerfCounter();
		var procedure = perf.allocProcedureCounter("SnapshotPeriod");
		var table = perf.tableCounter(0x7e5e7e5fL, ZezeCounter.TableMetric.CACHE_GET);
		var tableName = String.valueOf(0x7e5e7e5fL);
		procedure.end(0, 1000);
		table.inc(3);
		var first = perf.snapshotAndReset();
		assertSame(first, perf.getLastSnapshot());

		procedure.end(1, 2000);
		table.inc(5);
		perf.allocProcedureCounter("SnapshotNextPeriod").end(0, 1000);
		perf.tableCounter(0x7e5e7e60L, ZezeCounter.TableMetric.CACHE_GET).increment();
		assertSame(first, perf.getLastSnapshot());
		var second = perf.snapshotAndReset();

		assertEquals(Map.of(0L, 1L), first.procedureResults().get("SnapshotPeriod"));
		assertEquals(3L, first.tableResults().get(tableName).get("cacheGet"));
		assertFalse(first.procedureResults().containsKey("SnapshotNextPeriod"));
		assertFalse(first.tableResults().containsKey(String.valueOf(0x7e5e7e60L)));
		assertEquals(Map.of(1L, 1L), second.procedureResults().get("SnapshotPeriod"));
		assertEquals(5L, second.tableResults().get(tableName).get("cacheGet"));
		assertSame(second, perf.getLastSnapshot());
	}

	@Test
	public void resetDoesNotChangePublishedSnapshot() {
		var perf = new PerfCounter();
		perf.allocProcedureCounter("SnapshotBeforeReset").end(0, 1000);
		perf.tableCounter(0x7e5e7e5fL, ZezeCounter.TableMetric.CACHE_GET).inc(7);
		var tableName = String.valueOf(0x7e5e7e5fL);
		var snapshot = perf.snapshotAndReset();

		perf.resetCounter();
		assertSame(snapshot, perf.getLastSnapshot());
		var empty = perf.snapshotAndReset();

		assertTrue(empty.procedureResults().isEmpty());
		assertEquals(0L, empty.tableResults().get(tableName).get("cacheGet"));
		assertEquals(Map.of(0L, 1L), snapshot.procedureResults().get("SnapshotBeforeReset"));
		assertEquals(7L, snapshot.tableResults().get(tableName).get("cacheGet"));
	}
}
