package UnitTest.Zeze.Util;

import Zeze.Util.IdentityHashSet;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-15：Collection.toArray(T[]) 契约要求容量大于元素数时紧随末尾的元素为 null
 * （供调用方判定结束点）。原实现填充后直接返回，大数组传入时槽位残留旧引用。
 */
@Fast
public class TestIdentityHashSetToArray {

	@Test
	public void testToArrayTailNull() {
		var set = new IdentityHashSet<String>();
		set.add("a");
		set.add("b");
		Assertions.assertEquals(2, set.size());

		// 容量大于元素数：末尾紧邻槽位必须置 null（原实现残留旧引用）
		var stale = new String[5];
		stale[2] = "stale";
		stale[3] = "stale";
		String[] a = set.toArray(stale);
		Assertions.assertSame(stale, a);
		Assertions.assertNull(a[2], "size后紧邻槽位必须为null（JDK契约）");
		Assertions.assertEquals("stale", a[3], "仅size位置null，其后不动");

		// 容量正好：无尾部槽位，不需要null
		String[] exact = set.toArray(new String[2]);
		Assertions.assertEquals(2, exact.length);

		// 容量不足：按契约扩容到size
		String[] grown = set.toArray(new String[0]);
		Assertions.assertEquals(2, grown.length);
	}
}
