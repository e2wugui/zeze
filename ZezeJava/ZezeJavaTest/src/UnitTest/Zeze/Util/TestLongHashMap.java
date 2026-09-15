package UnitTest.Zeze.Util;

import Zeze.Util.LongHashMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-03：LongHashMap.compute/foreachUpdate 用引用相等（v != oldV）判定变更，
 * 既有条目的值为 null 且 remapping 返回 null 时 v==oldV==null 被误判为"无变化"，
 * 条目永久保留、size 不减——违反 Map.compute 契约（返回 null 应删除条目）。
 * IntHashMap/CharHashMap 同型已修，本类为漏网姊妹。
 * 修复：对齐存在性判定（条目存在且 remapping 返回 null 即删除）。
 */
@Fast
public class TestLongHashMap {

	@Test
	public void testComputeDeletesNullValueEntry() {
		var map = new LongHashMap<String>(8);
		map.put(1, null); // 条目存在、值为 null（公开 API 形态）
		Assertions.assertTrue(map.containsKey(1));
		Assertions.assertEquals(1, map.size());

		// 修复前：v==oldV==null 引用相等 → 跳过 → 条目保留（违反 Map.compute 契约）。
		Assertions.assertNull(map.compute(1, (k, v) -> null));
		Assertions.assertFalse(map.containsKey(1), "remapping返回null必须删除条目");
		Assertions.assertEquals(0, map.size());

		// zero-key 同型。
		map.put(0, null);
		Assertions.assertTrue(map.containsKey(0));
		Assertions.assertNull(map.compute(0, (k, v) -> null));
		Assertions.assertFalse(map.containsKey(0), "zero-key条目同样须删除");
		Assertions.assertEquals(0, map.size());
	}

	@Test
	public void testComputeContractPathsUnchanged() {
		var map = new LongHashMap<String>(8);
		map.put(1, "a");
		Assertions.assertEquals("a", map.compute(1, (k, v) -> v)); // 返回原值 → 保留
		Assertions.assertEquals(1, map.size());
		Assertions.assertEquals("b", map.compute(1, (k, v) -> "b")); // 更新
		Assertions.assertEquals("b", map.get(1));
		Assertions.assertEquals("c", map.compute(2, (k, v) -> "c")); // 不存在 → 插入
		Assertions.assertEquals("c", map.get(2));
		Assertions.assertEquals(2, map.size());
		Assertions.assertNull(map.compute(1, (k, v) -> null)); // 非 null 值条目删除（原路径）
		Assertions.assertEquals(1, map.size());
		Assertions.assertFalse(map.containsKey(1));
	}

	@Test
	public void testForeachUpdateDeletesNullValueEntry() {
		var map = new LongHashMap<String>(8);
		map.put(1, null); // 普通槽位 null 值条目
		map.put(2, "v2");
		map.put(0, null); // zero-key null 值条目
		Assertions.assertEquals(3, map.size());

		map.foreachUpdate((k, v) -> null); // 全部返回 null → 全部删除
		Assertions.assertEquals(0, map.size(), "foreachUpdate返回null的条目（含null值条目）必须全部删除");
		Assertions.assertFalse(map.containsKey(1));
		Assertions.assertFalse(map.containsKey(2));
		Assertions.assertFalse(map.containsKey(0));
	}
}
