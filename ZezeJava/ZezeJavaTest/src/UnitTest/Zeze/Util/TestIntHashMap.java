package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.HashSet;
import Zeze.Util.IntHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestIntHashMap {
	// 复制 IntHashMap 的私有hash公式（fibonacci hashing）。
	// 前提：cap=8 -> tableSize=16, mask=15, shift=Long.numberOfLeadingZeros(15)=60。内部参数变化时这里的前置断言会失败，需同步调整。
	private static int slot(int key) {
		return (int)((key * 0x9E3779B97F4A7C15L) >>> 60) & 15;
	}

	@Test
	public void testForeachUpdateDeleteKeepsVisitingChain() {
		// 找3个hash相同(探测链同槽起点)的key：k1,k2,k3，插入后占据相邻槽位 s,s+1,s+2。
		int k1 = 0, k2 = 0, k3 = 0;
		outer:
		for (int a = 1; a <= 3000; a++) {
			for (int b = a + 1; b <= 3000; b++) {
				if (slot(a) == slot(b)) {
					for (int c = b + 1; c <= 3000; c++) {
						if (slot(a) == slot(c)) {
							k1 = a;
							k2 = b;
							k3 = c;
							break outer;
						}
					}
				}
			}
		}
		Assertions.assertNotEquals(0, k1);
		final var fk1 = k1;

		var map = new IntHashMap<String>(8);
		map.put(k1, "v1");
		map.put(k2, "v2");
		map.put(k3, "v3");
		// 前置断言：确实构成同槽探测链 k1@i, k2@i+1, k3@i+2
		var kt = map.getKeyTable();
		int i = slot(k1);
		Assertions.assertEquals(k1, kt[i]);
		Assertions.assertEquals(k2, kt[(i + 1) & 15]);
		Assertions.assertEquals(k3, kt[(i + 2) & 15]);

		// foreachUpdate语义：对每个条目应用func恰好一次；返回null删除该条目，其余保留。
		// 修复前：删除k1的backward-shift把k2、k3搬进已遍历区域，func跳过它们。
		var seen = new HashSet<Integer>();
		map.foreachUpdate((k, v) -> {
			seen.add(k);
			return k == fk1 ? null : v;
		});

		Assertions.assertEquals(new HashSet<>(java.util.Set.of(k1, k2, k3)), seen, "func必须访问链上所有条目");
		Assertions.assertNull(map.get(k1), "返回null的条目被删除");
		Assertions.assertEquals("v2", map.get(k2));
		Assertions.assertEquals("v3", map.get(k3));
		Assertions.assertEquals(2, map.size());
	}

	@Test
	public void testForeachUpdateDeleteMany() {
		// 批量删除回归：覆盖removedKeys缓冲的倍增扩容路径，并验证混合"删除+更新+保留"后状态一致。
		// 100个条目装入128槽（负载~78%，探测链充分），threshold=102不会触发rehash。
		var map = new IntHashMap<String>(64);
		for (int k = 1; k <= 100; k++)
			map.put(k, "v" + k);

		var seen = new HashSet<Integer>();
		map.foreachUpdate((k, v) -> {
			seen.add(k);
			if (k % 3 == 0)
				return null; // 删除
			if (k % 5 == 0)
				return v + "!"; // 更新
			return v; // 保留
		});

		Assertions.assertEquals(100, seen.size(), "func必须访问全部条目");
		int expectedSize = 0;
		for (int k = 1; k <= 100; k++) {
			if (k % 3 == 0) {
				Assertions.assertNull(map.get(k), "被删除: " + k);
			} else {
				expectedSize++;
				if (k % 5 == 0)
					Assertions.assertEquals("v" + k + "!", map.get(k), "被更新: " + k);
				else
					Assertions.assertEquals("v" + k, map.get(k));
			}
		}
		Assertions.assertEquals(expectedSize, map.size());
	}
}
