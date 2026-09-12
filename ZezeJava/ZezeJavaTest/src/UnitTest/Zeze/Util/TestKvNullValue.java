package UnitTest.Zeze.Util;

import java.util.HashMap;
import java.util.HashSet;

import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-16：create(key, null)/setValue(null) 是公开 API 允许的形态，但 hashCode/equals
 * 直接解引用 value——含 null value 的 KV 放进 HashMap/HashSet 即 NPE。null 安全化对齐
 * KVList 既有判例（Objects.equals）。
 */
@Fast
public class TestKvNullValue {

	@Test
	public void testNullValueHashEquals() {
		var kv = KV.create("k", (String)null);

		// 原实现：key.hashCode() ^ value.hashCode() → NPE
		Assertions.assertDoesNotThrow(kv::hashCode);

		var kvSame = KV.create("k", (String)null);
		var kvNonNull = KV.create("k", "v");
		Assertions.assertEquals(kv, kvSame);
		Assertions.assertNotEquals(kv, kvNonNull);
		Assertions.assertEquals(kv.hashCode(), kvSame.hashCode());

		// null value 作 HashMap/HashSet 键不再 NPE，查找语义正确
		var map = new HashMap<KV<String, String>, Integer>();
		map.put(kv, 1);
		Assertions.assertEquals(1, map.get(kvSame));
		Assertions.assertNull(map.get(kvNonNull));

		var set = new HashSet<KV<String, String>>();
		set.add(kv);
		Assertions.assertTrue(set.contains(kvSame));
		Assertions.assertFalse(set.contains(kvNonNull));
	}
}
