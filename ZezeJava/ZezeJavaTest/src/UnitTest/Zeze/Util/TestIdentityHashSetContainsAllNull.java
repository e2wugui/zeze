package UnitTest.Zeze.Util;

import Zeze.Util.IdentityHashSet;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U2-F3：IdentityHashSet.containsAll(Collection) 对 null 元素错误地返回"包含"。
 * <p>
 * 本集合契约上永不含 null（add 参数即 @NotNull），按 JDK Set.containsAll 语义
 * null 元素应返回 false——原 e != null 短路把 null 误判为已包含。
 */
@Fast
public class TestIdentityHashSetContainsAllNull {

	@Test
	public void testNullElementMeansNotContained() {
		var a = new Object();
		var b = new Object();
		var set = new IdentityHashSet<Object>();
		set.add(a);
		set.add(b);

		assertTrue(set.containsAll(Arrays.asList(a, b)));
		assertFalse(set.containsAll(Arrays.asList(a, null)), "null 元素按 JDK 语义为不包含");
		assertFalse(set.containsAll(Arrays.asList((Object)null)));
		assertTrue(set.containsAll(Collections.emptyList()));

		// 基本契约不回归
		assertTrue(set.contains(a));
		assertFalse(set.contains(new Object()));
	}

	@Test
	public void testIdentityHashSetOverloadUnchanged() {
		var a = new Object();
		var other = new IdentityHashSet<Object>();
		other.add(a);
		var set = new IdentityHashSet<Object>();
		set.add(a);
		assertTrue(set.containsAll(other));
	}
}
