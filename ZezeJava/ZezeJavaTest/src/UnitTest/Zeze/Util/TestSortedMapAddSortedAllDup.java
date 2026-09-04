package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.SortedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * addSortedAll 批内重复键的链头跟踪（FND2-U2-5）：
 * 同键≥3个条目且 hash 递减时，addEntry 返回 r&gt;0 使 e 挂为链新头，
 * 原实现只 newElements.set 替换了槽位、lastE 仍指向旧头，后续同键条目挂到
 * 已出列的旧头上，链中段整条静默丢失（size 计数照加、与实际条目数不符）。
 * 修复后 r&gt;0 分支补 lastE = e。
 * <p>
 * hash 按 index 构造为递减(100/50/10)：批内第2、3个同键条目都成为新头，
 * 默认 hashFunc 下 r&gt;0 不可达，故必须用自定义 HashFunc 触发。
 */
@Fast
public class TestSortedMapAddSortedAllDup {

	/** index 0/1/2 → hash 100/50/10，保证后加入的同键条目 hash 更小（r&gt;0，成为链新头）。 */
	private static SortedMap.HashFunc<String, String> descendingHash() {
		return (k, v, i) -> i == 0 ? 100 : i == 1 ? 50 : 10;
	}

	/** elements 为空时的追加循环（第二处 r&gt;0 分支）。 */
	@Test
	public void testDupKeysAppendLoop() {
		var m = new SortedMap<String, String>(descendingHash());
		m.addAll(new String[]{"k", "k", "k"}, "v");
		Assertions.assertEquals(3, m.size());
		Assertions.assertEquals(1, m.keySize());
		// 链上 3 个条目全部可达（修复前中段条目丢失，toString 只有 2 个）
		Assertions.assertEquals(3, countChain(m.getAt(0).toString()));
		// 三个不同 hash 的条目都能按 (value,hash) 移除（修复前旧头链上的条目 remove 返回 null）
		for (int i = 0; i < 3; i++) {
			int idx = i;
			Assertions.assertNotNull(m.remove("k", "v", idx), () -> "index=" + idx);
		}
		Assertions.assertEquals(0, m.size());
		Assertions.assertEquals(0, m.keySize());
	}

	/** 已有更大键时的归并循环（第一处 r&gt;0 分支）。 */
	@Test
	public void testDupKeysMergeLoop() {
		var m = new SortedMap<String, String>(descendingHash());
		m.add("z", "v", 0);
		m.addAll(new String[]{"k", "k", "k"}, "v");
		Assertions.assertEquals(4, m.size());
		Assertions.assertEquals(2, m.keySize());
		Assertions.assertEquals("k", m.getAt(0).getKey());
		Assertions.assertEquals("z", m.getAt(1).getKey());
		Assertions.assertEquals(3, countChain(m.getAt(0).toString()));
		for (int i = 0; i < 3; i++) {
			int idx = i;
			Assertions.assertNotNull(m.remove("k", "v", idx), () -> "index=" + idx);
		}
		Assertions.assertNotNull(m.remove("z", "v", 0));
		Assertions.assertEquals(0, m.size());
		Assertions.assertEquals(0, m.keySize());
	}

	/** Entry.toString 打印整条链 "(k:v:hash)-(k:v:hash)-..."，数链节点个数。 */
	private static int countChain(String chain) {
		return chain.split("\\)-\\(").length;
	}
}
