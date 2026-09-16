package UnitTest.Zeze.Util;

import java.util.HashMap;
import Zeze.Util.FewModifyList;
import Zeze.Util.FewModifyMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-39 回归：FewModifyList/FewModifyMap 的快照用 List.copyOf / Map.copyOf——
 * JDK 规定遇 null 元素抛 NPE；写侧 add(null)/put(k, null)（ArrayList/HashMap 接受，
 * List/Map 接口语义合法）后快照永远建不成（read 恒 null），任意读方法每次重复抛 NPE，
 * 读侧永久瘫痪；同族 FewModifySortedMap 的 TreeMap 快照却允许 null，行为分叉。
 * 修复：快照改 ArrayList/HashMap 拷贝 + unmodifiable 包装（保持只读契约），
 * 与 FewModifySortedMap 统一家族契约。
 */
@Fast
public class TestFnd739FewModifyNullSnapshot {

	@Test
	public void testListNullElementReadable() {
		var list = new FewModifyList<String>();
		list.add(null);
		list.add("a");
		list.add(null);

		Assertions.assertEquals(3, list.size(), "含null元素的快照必须能建成（FND7-39）");
		Assertions.assertNull(list.get(0));
		Assertions.assertEquals("a", list.get(1));
		Assertions.assertNull(list.get(2));
		Assertions.assertEquals(1, list.indexOf("a"));
		Assertions.assertTrue(list.contains(null));
		Assertions.assertFalse(list.isEmpty());

		// 快照只读契约保持（原 List.copyOf 即不可变）。
		var snapshot = list.snapshot();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.add("x"));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> list.iterator().remove());
	}

	@Test
	public void testListWriteAfterNullSnapshotRebuilds() {
		var list = new FewModifyList<String>();
		list.add(null);
		Assertions.assertNull(list.get(0)); // 建成快照
		list.remove(null); // 写侧移除 null，快照重建
		Assertions.assertEquals(0, list.size());
		list.add("b");
		Assertions.assertEquals("b", list.get(0));
	}

	@Test
	public void testMapNullValueReadable() {
		var map = new FewModifyMap<String, String>();
		map.put("k1", null);
		map.put("k2", "v2");

		Assertions.assertEquals(2, map.size(), "含null value的快照必须能建成（FND7-39）");
		Assertions.assertNull(map.get("k1"));
		Assertions.assertTrue(map.containsKey("k1"), "null value必须可与 absent 区分");
		Assertions.assertFalse(map.containsKey("absent"));
		Assertions.assertEquals("v2", map.get("k2"));
		Assertions.assertTrue(map.containsValue(null));

		var snapshot = map.snapshot();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.put("k3", "v3"));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().remove());
	}

	@Test
	public void testMapWriteAfterNullSnapshotRebuilds() {
		var map = new FewModifyMap<String, String>();
		map.put("k", null);
		Assertions.assertTrue(map.containsKey("k"));
		map.remove("k");
		Assertions.assertEquals(0, map.size());
		map.put("k2", "v");
		Assertions.assertEquals(new HashMap<String, String>() {
			{
				put("k2", "v");
			}
		}, map.snapshot());
	}

	@Test
	public void testNonNullContentUnchanged() {
		// 兼容红线：不含 null 的容器快照内容不变。
		var list = new FewModifyList<Integer>();
		list.add(1);
		list.add(2);
		Assertions.assertEquals(2, list.size());
		Assertions.assertEquals(1, list.get(0));
		Assertions.assertEquals(2, list.get(1));

		var map = new FewModifyMap<Integer, Integer>();
		map.put(1, 10);
		map.put(2, 20);
		Assertions.assertEquals(10, map.get(1));
		Assertions.assertEquals(20, map.get(2));
	}
}
