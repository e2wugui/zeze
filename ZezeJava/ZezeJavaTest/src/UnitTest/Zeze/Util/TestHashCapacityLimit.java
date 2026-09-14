package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import Zeze.Util.CharHashMap;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntHashSet;

/**
 * FND5-08：IntHashMap/IntHashSet/CharHashMap/IdentityHashSet 的 resize
 * 无扩容上限守卫，表长 1<<30 再左移回绕为负，new int[负数] 抛
 * NegativeArraySizeException（FND4-19 只给 LongHashMap/LongHashSet 加了守卫）。
 * 守卫分支无法用真实数据触达（keyTable 即需 4GB），故反射直调私有 resize
 * 传入回绕值 Integer.MIN_VALUE（1<<30<<1），断言显式契约 IllegalStateException。
 * 修复前本用例红（NegativeArraySizeException）；testNormalGrowthUnchanged
 * 守兼容红线：常规倍增路径行为不变。
 */
@Fast
public final class TestHashCapacityLimit {

	private static void assertResizeLimit(Object map, String name) throws Exception {
		var resize = map.getClass().getDeclaredMethod("resize", int.class);
		resize.setAccessible(true);
		try {
			resize.invoke(map, Integer.MIN_VALUE);
			fail(name + ".resize(Integer.MIN_VALUE) 未抛异常");
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			assertInstanceOf(IllegalStateException.class, cause,
					name + " 应显式抛 IllegalStateException，实际: " + cause);
			assertTrue(cause.getMessage().contains(name),
					name + " 异常消息应含类名: " + cause.getMessage());
			assertTrue(cause.getMessage().contains("capacity limit"),
					name + " 异常消息应含 capacity limit: " + cause.getMessage());
		}
	}

	@Test
	public void testResizeOverflowGuard() throws Exception {
		assertResizeLimit(new IntHashMap<>(), "IntHashMap");
		assertResizeLimit(new IntHashSet(), "IntHashSet");
		assertResizeLimit(new CharHashMap<String>(), "CharHashMap");
		assertResizeLimit(new IdentityHashSet<Object>(), "IdentityHashSet");
	}

	@Test
	public void testNormalGrowthUnchanged() {
		var map = new IntHashMap<String>();
		for (int i = 1; i <= 1000; i++)
			map.put(i * 31, "v" + i);
		assertEquals(1000, map.size());
		for (int i = 1; i <= 1000; i++)
			assertEquals("v" + i, map.get(i * 31));

		var set = new IntHashSet();
		for (int i = 1; i <= 1000; i++)
			set.add(i * 31);
		assertEquals(1000, set.size());

		var cMap = new CharHashMap<String>();
		for (int i = 1; i <= 1000; i++)
			cMap.put((char)i, "c" + i);
		assertEquals(1000, cMap.size());
		assertEquals("c500", cMap.get((char)500));

		var iSet = new IdentityHashSet<String>();
		for (int i = 1; i <= 1000; i++)
			iSet.add(new String("same"));
		assertEquals(1000, iSet.size());
	}
}
