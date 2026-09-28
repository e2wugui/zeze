package Zeze.Util;

import java.util.TreeMap;

import Zeze.Util.FewModifySortedMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND16 util-01 红绿钉板：FewModifySortedMap 的全部视图方法（subMap/keySet/entrySet/
 * descendingMap 等）不得返回可写的共享快照——写视图会静默腐蚀共享 read（幻影条目：
 * 读侧可见、权威 write 不知情、下次写重建后蒸发）且构成无同步并发读写（CME/中间态）。
 * 修复：prepareRead 统一 unmodifiable 包装（镜像 FewModifyMap 家族），误写抛
 * UnsupportedOperationException 而非静默破坏。
 */
@Fast
public class TestFnd16Util01FewModifySortedMapViewGuard {

	@Test
	public void testViewsRejectWrites() {
		var m = new FewModifySortedMap<String, Integer>(new TreeMap<>());
		m.put("a", 1);
		m.put("b", 2);
		m.put("c", 3);

		// 各视图的写入口必须 fail-fast（UOE），不得静默腐蚀共享快照。
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.subMap("a", "c").put("phantom", 9), "subMap视图put必须拒绝");
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.keySet().remove("a"), "keySet视图remove必须拒绝");
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.entrySet().iterator().remove(), "entrySet迭代器remove必须拒绝");
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.values().remove(1), "values视图remove必须拒绝");
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.descendingMap().put("phantom", 9), "descendingMap视图put必须拒绝");
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> m.snapshot().put("phantom", 9), "snapshot视图put必须拒绝（原有防护）");

		// 权威侧未被腐蚀：幻影条目不存在、后续写重建快照后读侧一致。
		Assertions.assertNull(m.get("phantom"), "视图写被拒后不得产生幻影条目");
		m.put("d", 4); // 触发快照重建
		Assertions.assertNull(m.get("phantom"), "重建后不得出现幻影条目");
		Assertions.assertEquals(4, m.size());
	}

	@Test
	public void testNormalReadPathsUnaffected() {
		var m = new FewModifySortedMap<String, Integer>(new TreeMap<>());
		for (var i = 0; i < 10; i++)
			m.put("k" + i, i);

		// 只读消费面（仓内 HotManager/HttpServer 形态）不受包装影响。
		Assertions.assertEquals(10, m.size());
		Assertions.assertEquals(5, m.subMap("k2", true, "k6", true).size());
		Assertions.assertEquals(10, m.keySet().size());
		Assertions.assertEquals(10, m.values().size());
		Assertions.assertEquals(10, m.entrySet().size());
		Assertions.assertEquals(10, m.descendingMap().size());
		Assertions.assertEquals(10, m.navigableKeySet().size());
		Assertions.assertEquals(0, m.headMap("k0").size());
		Assertions.assertEquals(0, m.tailMap("k9", false).size());
		Assertions.assertEquals("k0", m.firstKey());
		Assertions.assertEquals("k9", m.lastKey());
		var iterated = 0;
		for (var ignored : m.subMap("k0", "k9").keySet())
			iterated++;
		Assertions.assertEquals(9, iterated);
	}
}
