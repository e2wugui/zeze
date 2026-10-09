package Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * resetCounter必须保留TableInfo与计数句柄身份：tableCounter交出的
 * LongAdderCounter被调用方长期缓存复用（如Lockey的计数路径），
 * 清空tableInfoMap会让旧句柄的增量落在脱离收集的孤儿对象上——
 * reset后旧句柄加的样本静默丢失，直到按ID重新取得新句柄才恢复统计。
 * 修复：reset就地清零计数，保留条目身份；reset后新旧句柄指向同一计数器。
 */
@Fast
public class TestPerfCounterResetKeepsTableHandles {

	@Test
	public void resetKeepsTableCounterHandleIdentity() {
		Assertions.assertTrue(ZezeCounter.instance instanceof PerfCounter, "需要默认PerfCounter实现");
		var perf = (PerfCounter)ZezeCounter.instance;
		long tableId = 0x7e5e7e5eL; // 无实际表映射，名称退化为id字符串
		var metric = ZezeCounter.TableMetric.CACHE_GET;
		String tableName = String.valueOf(tableId);

		var handle = perf.tableCounter(tableId, metric);
		handle.inc(1);
		assertEquals(1L, perf.collectAndReset().tableResults()
				.getOrDefault(tableName, java.util.Map.of()).getOrDefault(metric.key, 0L),
				"reset前句柄样本正常收集");

		perf.resetCounter();

		// reset后：旧句柄与新取得的句柄必须仍指向同一计数器（样本都收集）
		handle.inc(2);
		var handleAgain = perf.tableCounter(tableId, metric);
		handleAgain.inc(3);
		assertEquals(5L, perf.collectAndReset().tableResults()
						.getOrDefault(tableName, java.util.Map.of()).getOrDefault(metric.key, 0L),
				"reset不得使旧句柄脱离收集（清map后旧句柄加的2丢失、只剩新句柄的3）");
	}
}
