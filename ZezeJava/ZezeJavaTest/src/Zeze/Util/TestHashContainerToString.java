package Zeze.Util;

import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHashContainerToString {

	@Test
	public void testCharHashMapToStringFindsHighBucketEntriesAndTruncatesByCount() {
		var map = new CharHashMap<Integer>(32);
		assertTrue(map.getKeyTable().length >= 32);

		// 找到一个散列在下标 >= 20 桶槽中的 char 键
		char highKey = 0;
		for (char c = 'a'; c <= 'z'; c++) {
			map.clear();
			map.put(c, 42);
			char[] kt = map.getKeyTable();
			for (int i = 20; i < kt.length; i++) {
				if (kt[i] == c) {
					highKey = c;
					break;
				}
			}
			if (highKey != 0)
				break;
		}
		assertNotEquals(0, highKey, "应找到散列在 >=20 槽位的测试键");
		assertEquals("{" + highKey + "=42}", map.toString());

		// 恰好 20 个条目：全部输出且不带截断省略号
		map.clear();
		for (int i = 0; i < 20; i++)
			map.put((char)('A' + i), i);
		String exact20 = map.toString();
		assertFalse(exact20.contains(",..."), exact20);
		assertFalse(exact20.startsWith("{,"), exact20);
		assertEquals(20, exact20.substring(1, exact20.length() - 1).split(",").length);

		// 超过 20 个条目：输出前 20 条并以 ",...}" 结尾
		map.put('Z', 20);
		String over20 = map.toString();
		assertTrue(over20.endsWith(",...}"), over20);
		assertFalse(over20.startsWith("{,"), over20);
		assertEquals(21, over20.substring(1, over20.length() - 1).split(",").length);
	}

	@Test
	public void testIdentityHashSetToStringFindsHighBucketEntriesAndTruncatesByCount() {
		var set = new IdentityHashSet<String>(32);
		assertTrue(((IdentityHashSet<?>)set).getKeyTable().length >= 32);

		// 找到一个散列在下标 >= 20 桶槽中的对象
		@Nullable String highObj = null;
		for (int attempt = 0; attempt < 256 && highObj == null; attempt++) {
			String candidate = new String("item-" + attempt);
			set.clear();
			set.add(candidate);
			@Nullable Object[] kt = ((IdentityHashSet<?>)set).getKeyTable();
			for (int i = 20; i < kt.length; i++) {
				if (kt[i] == candidate) {
					highObj = candidate;
					break;
				}
			}
		}
		assertNotNull(highObj, "应找到散列在 >=20 槽位的测试对象");
		assertEquals("{" + highObj + "}", set.toString());

		// 恰好 20 个元素：全部输出且不带截断省略号
		set.clear();
		for (int i = 0; i < 20; i++)
			set.add(new String("e" + i));
		String exact20 = set.toString();
		assertFalse(exact20.contains(",..."), exact20);
		assertFalse(exact20.startsWith("{,"), exact20);
		assertEquals(20, exact20.substring(1, exact20.length() - 1).split(",").length);

		// 超过 20 个元素：输出前 20 个并以 ",...}" 结尾
		set.add(new String("e20"));
		String over20 = set.toString();
		assertTrue(over20.endsWith(",...}"), over20);
		assertFalse(over20.startsWith("{,"), over20);
		assertEquals(21, over20.substring(1, over20.length() - 1).split(",").length);
	}
}
