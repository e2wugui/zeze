package UnitTest.Zeze.Util;

import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-12 回归（2026-09-14复审三轮终态）：KV的key/value均参与hashCode/equals，
 * 任一字段可变都破坏HashMap/HashSet键不变式——收口为不可变二元组：两字段final、
 * 无setKey/setValue；空契约按仓内惯例以jetbrains注解声明（key @NotNull、
 * value @Nullable——FND4-16的null value形态保留），不做运行时拒绝（用户裁定，
 * 对齐仓内@NotNull参数普遍无显式检查的口径）。原setKey调用方（Service×2、
 * Dbh2AgentManager）已改累积后create；setValue生产调用方为0（精确扫描）。
 */
@Fast
public class TestKvNullKey {

	@Test
	public void testImmutablePair() {
		Assertions.assertThrows(NoSuchMethodException.class,
				() -> KV.class.getMethod("setKey", Object.class),
				"setKey必须移除：可变key破坏HashMap键不变式（FND5-12复审）");
		Assertions.assertThrows(NoSuchMethodException.class,
				() -> KV.class.getMethod("setValue", Object.class),
				"setValue必须移除：value同样参与hashCode，可变value同样破坏键不变式（FND5-12复审二轮）");
		var kv = KV.create("k", "v1");
		Assertions.assertEquals("k", kv.getKey());
		Assertions.assertEquals("v1", kv.getValue());
	}

	@Test
	public void testNullContractIsAnnotationOnly() {
		// 空契约为注解声明（jetbrains @NotNull为CLASS保留，反射不可见——不做反射断言）：
		// 构造点不做运行时拒绝（用户终态裁定，对齐仓内@NotNull参数普遍无显式检查口径）。
		Assertions.assertDoesNotThrow(() -> {
			var kv = KV.create("k", null);
			Assertions.assertNull(kv.getValue());
		}, "构造点不得做运行时拒绝（注解契约终态）");
	}

	@Test
	public void testNullValueContract() {
		var kv = KV.create("k", null);
		Assertions.assertNull(kv.getValue());
		// value null 的 hashCode/equals 契约（FND4-16 行为不变）。
		Assertions.assertEquals(KV.create("k", null).hashCode(), kv.hashCode());
		Assertions.assertEquals(KV.create("k", null), kv);
		Assertions.assertNotEquals(KV.create("k", "v"), kv);
	}
}
