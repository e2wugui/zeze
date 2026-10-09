package Zeze.Transaction.Collections;

import java.util.ConcurrentModificationException;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PMap/PSet/PSortedMap 迭代器 remove 无身份校验：next() 返回的条目在迭代期间
 * 被外部结构性修改（同键移除/重put、元素移除）后，remove 按键/按元素删除会
 * 删错条目或静默 no-op——错位信号被吞。PList 迭代器已有身份 fail-fast
 * （ConcurrentModificationException），三个容器族对齐该语义。
 */
@Fast
public class TestMapSetIteratorRemoveFailFast {

	@Test
	public void testPMapStaleEntryRemoveThrows() {
		var pm = new PMap1<>(Long.class, String.class);
		pm.put(1L, "v1");
		pm.put(2L, "v2");

		var it = pm.iterator();
		var entry1 = it.next();
		Assertions.assertEquals(1L, entry1.getKey());
		pm.remove(1L); // 迭代期间外部移除：next()持有的条目已陈旧

		Assertions.assertThrows(ConcurrentModificationException.class, it::remove,
				"陈旧条目的remove必须fail-fast（旧行为：按键删除静默no-op）");
		// 校验失败的remove不得改动容器
		Assertions.assertEquals(1, pm.size());
	}

	@Test
	public void testPMapReplacedValueRemoveThrows() {
		var pm = new PMap1<>(Long.class, String.class);
		pm.put(1L, "v1");
		var it = pm.iterator();
		it.next();
		pm.put(1L, new String("v1")); // 同键重put成内容相等但身份不同的实例

		Assertions.assertThrows(ConcurrentModificationException.class, it::remove,
				"同键被替换成不同实例后，remove按身份校验必须fail-fast");
	}

	@Test
	public void testPMapLiveEntryRemoveStillWorks() {
		var pm = new PMap1<>(Long.class, String.class);
		pm.put(1L, "v1");
		pm.put(2L, "v2");
		var it = pm.iterator();
		it.next(); // entry k=1，期间无修改
		Assertions.assertDoesNotThrow(it::remove);
		Assertions.assertEquals(1, pm.size());
		Assertions.assertFalse(pm.containsKey(1L));
	}

	@Test
	public void testPSetStaleElementRemoveThrows() {
		var ps = new PSet1<>(String.class);
		ps.add("v1");
		ps.add("v2");

		var it = ps.iterator();
		var first = it.next();
		Assertions.assertEquals("v1", first);
		ps.remove(first); // 迭代期间外部移除next()返回的元素

		Assertions.assertThrows(ConcurrentModificationException.class, it::remove,
				"已移除元素的remove必须fail-fast（旧行为：静默no-op）");
		Assertions.assertEquals(1, ps.size());
	}

	@Test
	public void testPSortedMapStaleEntryRemoveThrows() {
		var pm = new PSortedMap1<>(Long.class, String.class);
		pm.put(1L, "v1");
		pm.put(2L, "v2");

		var it = pm.entrySet().iterator();
		var entry1 = it.next();
		Assertions.assertEquals(1L, entry1.getKey());
		pm.remove(1L);

		Assertions.assertThrows(ConcurrentModificationException.class, it::remove);
		Assertions.assertEquals(1, pm.size());
	}

	@Test
	public void testPSortedMapLiveEntryRemoveStillWorks() {
		var pm = new PSortedMap1<>(Long.class, String.class);
		pm.put(1L, "v1");
		pm.put(2L, "v2");
		var it = pm.entrySet().iterator();
		it.next();
		Assertions.assertDoesNotThrow(it::remove);
		Assertions.assertEquals(1, pm.size());
	}
}
