package UnitTest.Zeze.Util;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.CharHashMap;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashMap;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-10回归：LongMap文档承诺"Same as java.util.Map"，但putIfAbsent/computeIfAbsent
 * 只判"键存在"不判"值是否null"——put(k,null)后putIfAbsent(k,v)静默不写（返回null却
 * 未生效，"写了读不到"），computeIfAbsent漏调mappingFunction直接返回null。修复：
 * 值为null的既有条目按Map契约视为absent（含零键分支）；IntHashMap/CharHashMap孪生
 * 同步。断言与java.util.HashMap同序列行为逐项对齐。
 */
@Fast
public class TestFnd810PrimitiveMapNullContract {

	@Test
	public void testLongHashMapPutIfAbsentNullEntry() {
		var m = new LongHashMap<String>();
		Assertions.assertNull(m.put(5, null)); // 合法：null值条目
		Assertions.assertTrue(m.containsKey(5));

		// 修复前红：返回null但v未写入，get仍为null
		Assertions.assertNull(m.putIfAbsent(5, "v"));
		Assertions.assertEquals("v", m.get(5));
		Assertions.assertEquals(1, m.size());

		// 既有非null值：putIfAbsent不动旧值
		Assertions.assertEquals("v", m.putIfAbsent(5, "w"));
		Assertions.assertEquals("v", m.get(5));

		// 零键分支
		Assertions.assertNull(m.put(0, null));
		Assertions.assertNull(m.putIfAbsent(0, "z"));
		Assertions.assertEquals("z", m.get(0));
		Assertions.assertEquals(2, m.size());
	}

	@Test
	public void testLongHashMapComputeIfAbsentNullEntry() {
		var m = new LongHashMap<String>();
		m.put(5, null);
		var calls = new AtomicInteger();

		// 修复前红：值为null的既有条目直接返回null，mappingFunction漏调
		Assertions.assertEquals("new5", m.computeIfAbsent(5, k -> {
			calls.incrementAndGet();
			return "new" + k;
		}));
		Assertions.assertEquals(1, calls.get());
		Assertions.assertEquals("new5", m.get(5));

		// 既有非null值：不调函数
		Assertions.assertEquals("new5", m.computeIfAbsent(5, k -> {
			calls.incrementAndGet();
			return "recomputed" + k;
		}));
		Assertions.assertEquals(1, calls.get());

		// 零键分支 + 函数返回null不写入、条目保留
		m.put(0, null);
		Assertions.assertEquals("z0", m.computeIfAbsent(0, k -> "z" + k));
		Assertions.assertEquals("z0", m.get(0));
		Assertions.assertNull(m.computeIfAbsent(7, k -> null));
		Assertions.assertFalse(m.containsKey(7));

		m.put(9, null);
		Assertions.assertNull(m.computeIfAbsent(9, k -> null)); // 返回null不覆写
		Assertions.assertTrue(m.containsKey(9)); // 原null条目保留（同java.util.Map）
		Assertions.assertEquals(3, m.size()); // 5,0,9
	}

	@Test
	public void testIntHashMapTwins() {
		var m = new IntHashMap<String>();
		m.put(5, null);
		Assertions.assertNull(m.putIfAbsent(5, "v"));
		Assertions.assertEquals("v", m.get(5));
		var calls = new AtomicInteger();
		m.put(6, null); // 独立键：保持"存在但值为null"状态再测computeIfAbsent
		Assertions.assertEquals("new6", m.computeIfAbsent(6, k -> {
			calls.incrementAndGet();
			return "new" + k;
		}));
		Assertions.assertEquals(1, calls.get());

		m.put(0, null);
		Assertions.assertNull(m.putIfAbsent(0, "z"));
		Assertions.assertEquals("z", m.get(0));
	}

	@Test
	public void testCharHashMapTwins() {
		var m = new CharHashMap<String>();
		m.put('a', null);
		Assertions.assertNull(m.putIfAbsent('a', "v"));
		Assertions.assertEquals("v", m.get('a'));
		var calls = new AtomicInteger();
		m.put('b', null);
		Assertions.assertEquals("new", m.computeIfAbsent('b', k -> {
			calls.incrementAndGet();
			return "new";
		}));
		Assertions.assertEquals(1, calls.get());

		m.put((char)0, null);
		Assertions.assertNull(m.putIfAbsent((char)0, "z"));
		Assertions.assertEquals("z", m.get((char)0));
	}
}
