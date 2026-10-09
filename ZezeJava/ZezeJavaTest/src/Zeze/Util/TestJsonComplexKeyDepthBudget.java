package Zeze.Util;

import java.util.IdentityHashMap;
import java.util.Map;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 复杂Map键的序列化必须消耗调用方的深度预算：旧实现对每个复杂键
 * new JsonWriter()（零深度、丢flags），键图含循环（如IdentityHashMap
 * 以自身为键）时绕过FLAG_THROW_ON_DEPTH_LIMIT直接StackOverflowError，
 * 超深复杂键也绕过预算。子writer继承深度flags并共享剩余预算。
 */
@Fast
public class TestJsonComplexKeyDepthBudget {

	@Test
	public void cyclicComplexKeyIsBoundedByDepthLimit() {
		Map<Object, Integer> cyclicKey = new IdentityHashMap<>();
		cyclicKey.put(cyclicKey, 1); // 键含自身：序列化键图无限递归
		assertThrows(IllegalStateException.class,
				() -> new JsonWriter().setDepthLimit(2).setFlags(JsonWriter.FLAG_THROW_ON_DEPTH_LIMIT).write(cyclicKey),
				"循环键必须被深度预算拦截（修复前StackOverflowError）");
	}

	@Test
	public void nonCyclicComplexKeyStillEncodes() {
		// 复杂键（非keyReader注册类型）仍正常编码为字符串键
		var key = new Json.Pos(42);
		Map<Json.Pos, Integer> m = new IdentityHashMap<>();
		m.put(key, 1);
		var s = new JsonWriter().write(m).toString();
		assertTrue(s.contains("42"), "复杂键仍编码: " + s);
		assertTrue(s.contains("1"));
	}
}
