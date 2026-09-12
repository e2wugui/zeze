package UnitTest.Zeze.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Util.FewModifySortedMap;
import harness.Fast;

/**
 * FND4-11：以Comparator构造的FewModifySortedMap，其只读快照
 * 曾以new TreeMap<>()（自然序）重建，比较器被丢弃——序敏感读与comparator()错乱。
 * 修复：快照与clone继承write的比较器。
 */
@Fast
public class TestFewModifySortedMapComparator {

	@Test
	public void testSnapshotKeepsComparator() {
		var map = new FewModifySortedMap<String, String>(Comparator.reverseOrder());
		map.put("a", "1");
		map.put("b", "2");
		map.put("c", "3");

		// 读快照建立（首次读触发prepareRead）
		Assertions.assertEquals("c", map.firstKey(), "逆序比较器下first应为c");
		Assertions.assertEquals("a", map.lastKey(), "逆序比较器下last应为a");
		Assertions.assertNotNull(map.comparator(), "快照必须保留比较器");

		// 迭代顺序必须逆序
		var keys = new ArrayList<String>();
		map.forEach((k, v) -> keys.add(k));
		Assertions.assertEquals(List.of("c", "b", "a"), keys, "迭代顺序须按比较器");

		// headMap语义
		Assertions.assertEquals(List.of("c", "b"), new ArrayList<>(map.headMap("a").keySet()));
	}

	@Test
	public void testCloneKeepsComparator() throws Exception {
		var map = new FewModifySortedMap<String, String>(Comparator.reverseOrder());
		map.put("a", "1");
		map.put("b", "2");

		var cloned = map.clone();
		Assertions.assertNotNull(cloned.comparator(), "clone必须保留比较器");
		Assertions.assertEquals("b", cloned.firstKey(), "clone的序语义须与原一致");
		Assertions.assertEquals("a", cloned.lastKey());

		// 写侧快照发布后（clone触发过prepareRead）再写再读，序保持
		map.put("d", "4");
		Assertions.assertEquals("d", map.firstKey());
	}

	@Test
	public void testNaturalOrderUnchanged() throws CloneNotSupportedException {
		// 自然序（无比较器）行为回归：既有4处构造点全为自然序
		var map = new FewModifySortedMap<Integer, String>();
		map.put(3, "c");
		map.put(1, "a");
		map.put(2, "b");
		Assertions.assertEquals(1, map.firstKey());
		Assertions.assertEquals(3, map.lastKey());
		Assertions.assertNull(map.comparator());
		var keys = new ArrayList<Integer>();
		map.forEach((k, v) -> keys.add(k));
		Assertions.assertEquals(List.of(1, 2, 3), keys);

		var cloned = map.clone();
		Assertions.assertEquals(1, cloned.firstKey());
	}
}
