package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import Zeze.Util.Json;

/**
 * JsonWriter 的深度限制必须覆盖紧凑模式（toCompactString）下的纯集合/数组/Map 嵌套链：
 * 修复前三个非 pretty 分支递归不递增 tabs，深度检查形同虚设，自引用结构直接
 * StackOverflowError（见 FND2-U1-3）。现在超限安全截断为 "!OVERDEPTH!"。
 */
@Fast
public final class TestJsonWriterCompactDepth {
	@Test
	public void testSelfRefMapCompact() {
		// 自引用 Map（业务侧普通编程错误）：安全截断而非 SOE
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("self", m);
		String s = Json.toCompactString(m);
		assertTrue(s.contains("!OVERDEPTH!"), s);
	}

	@Test
	public void testSelfRefListCompact() {
		List<Object> l = new ArrayList<>();
		l.add(l);
		String s = Json.toCompactString(l);
		assertTrue(s.contains("!OVERDEPTH!"), s);
	}

	@Test
	public void testSelfRefArrayCompact() {
		Object[] arr = new Object[1];
		arr[0] = arr;
		String s = Json.toCompactString(arr);
		assertTrue(s.contains("!OVERDEPTH!"), s);
	}

	@Test
	public void testMutualRefMapCompact() {
		// 互相引用 Map A->B->A：截断后仍能闭环收尾（输出是有限串）
		Map<String, Object> a = new LinkedHashMap<>();
		Map<String, Object> b = new LinkedHashMap<>();
		a.put("b", b);
		b.put("a", a);
		String s = Json.toCompactString(a);
		assertTrue(s.contains("!OVERDEPTH!"), s);
	}

	@Test
	public void testDeepNestListCompact() {
		// 非自引用的深嵌套（1000 层，远超默认深度 16）同型：截断而非 SOE
		List<Object> root = new ArrayList<>();
		List<Object> cur = root;
		for (int i = 0; i < 1000; i++) {
			var next = new ArrayList<Object>();
			cur.add(next);
			cur = next;
		}
		cur.add(1);
		String s = Json.toCompactString(root);
		assertTrue(s.contains("!OVERDEPTH!"), s);
	}

	@Test
	public void testNormalOutputUnchanged() {
		// 回归：正常结构的紧凑输出逐字节不变（tabs++/-- 平衡，紧凑模式无输出副作用）
		var m = new LinkedHashMap<String, Object>();
		m.put("a", List.of(1, 2));
		m.put("b", Map.of("c", List.of("x")));
		assertEquals("{\"a\":[1,2],\"b\":{\"c\":[\"x\"]}}", Json.toCompactString(m));
		assertEquals("[[1],[2],{}]", Json.toCompactString(List.of(List.of(1), List.of(2), Map.of())));
	}

	public static void main(String[] args) {
		var t = new TestJsonWriterCompactDepth();
		t.testSelfRefMapCompact();
		t.testSelfRefListCompact();
		t.testSelfRefArrayCompact();
		t.testMutualRefMapCompact();
		t.testDeepNestListCompact();
		t.testNormalOutputUnchanged();
		System.out.println(t.getClass().getSimpleName() + ": 6 tests OK!");
	}
}
