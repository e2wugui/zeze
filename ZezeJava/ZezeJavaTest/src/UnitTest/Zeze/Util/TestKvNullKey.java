package UnitTest.Zeze.Util;

import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-12 回归（2026-09-14复审三轮终态）：KV的key/value均参与hashCode/equals，
 * 任一字段可变都破坏HashMap/HashSet键不变式——收口为不可变二元组：两字段final、
 * 无setKey/setValue。空契约（FND6-42终态）：key在构造点requireNonNull拒绝（null key
 * 的NPE从hashCode处延迟爆发提前到构造点，异常消息"key"，可诊断）；value仅注解声明
 * 允许null（@Nullable，FND4-16的null value形态保留），不做运行时拒绝。
 * 原setKey调用方（Service×2、Dbh2AgentManager）已改累积后create；
 * setValue生产调用方为0（精确扫描）。
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
	public void testNullKeyRejectedNullValueAllowed() {
		// key契约（FND6-42终态）：构造点requireNonNull拒绝null key，异常消息为"key"。
		var e = Assertions.assertThrows(NullPointerException.class, () -> KV.create(null, "v"),
				"null key必须在构造点拒绝（FND6-42）");
		Assertions.assertEquals("key", e.getMessage(), "NPE消息必须指明\"key\"（可诊断契约）");
		// value契约不变：仅注解声明允许null（jetbrains @NotNull为CLASS保留，反射不可见——不做反射断言），
		// 构造点不做运行时拒绝。
		Assertions.assertDoesNotThrow(() -> {
			var kv = KV.create("k", null);
			Assertions.assertNull(kv.getValue());
		}, "null value允许（@Nullable注解契约），不得运行时拒绝");
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
