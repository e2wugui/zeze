package UnitTest.Zeze.Util;

import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U3-F4：NO_QUOTE_KEY 模式下键的强制加引号守卫。
 * <p>
 * 原守卫只查 ':'，而写侧 ESCAPE 表不转义空格/逗号/花括号/斜杠，读侧 parseStringNoQuot
 * 在 ≤0x20 或 ':' 截断、parseKeyHashNoQuot 在 '/' 提前返回——裸写这些字符的键产出
 * 不可解析 JSON 且往返静默损坏。修复：空串或含任一终止/歧义字符（≤0x20、':'、','、
 * '}'、']'、'"'、'\''、'\\'、'/'）强制加引号；bean 字段名路径（fieldNameFilter 可产出
 * 任意串）同样收口。
 */
@Fast
public final class TestJsonNoQuoteKeyForceQuote {

	static class Bean {
		public int n = 7;
	}

	private static Map<String, Object> specialKeysMap() {
		var map = new LinkedHashMap<String, Object>();
		map.put("normal", 1);
		map.put("", 2);
		map.put("a b", 3);
		map.put("a:b", 4);
		map.put("a,b", 5);
		map.put("a}b", 6);
		map.put("a]b", 7);
		map.put("a\"b", 8);
		map.put("a'b", 9);
		map.put("a\\b", 10);
		map.put("a/b", 11);
		return map;
	}

	@Test
	public void testMapKeyForceQuoteCompact() throws ReflectiveOperationException {
		var map = specialKeysMap();
		var w = new JsonWriter().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY);
		w.write(map);
		var json = new String(w.toBytes(), StandardCharsets.UTF_8);
		// 合法标识符键仍免引号
		assertTrue(json.contains("normal:1"), json);
		// 空串与含歧义字符的键必须带引号
		assertTrue(json.contains("\"\":2"), json);
		assertTrue(json.contains("\"a b\":3"), json);
		assertTrue(json.contains("\"a:b\":4"), json);
		assertTrue(json.contains("\"a,b\":5"), json);
		assertTrue(json.contains("\"a}b\":6"), json);
		assertTrue(json.contains("\"a]b\":7"), json);
		assertTrue(json.contains("\"a\\\"b\":8"), json);
		assertTrue(json.contains("\"a'b\":9"), json);
		assertTrue(json.contains("\"a\\\\b\":10"), json);
		assertTrue(json.contains("\"a/b\":11"), json);
		// 往返同一性
		var back = new JsonReader().buf(json).parseMap(new HashMap<>());
		assertEquals(map, back);
	}

	@Test
	public void testMapKeyForceQuotePretty() throws ReflectiveOperationException {
		var map = specialKeysMap();
		var w = new JsonWriter().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY).setPrettyFormat(true);
		w.write(map);
		var json = new String(w.toBytes(), StandardCharsets.UTF_8);
		assertTrue(json.contains("\"a b\":"), json);
		assertTrue(json.contains("\"a/b\":"), json);
		assertTrue(json.contains("normal:"), json);
		var back = new JsonReader().buf(json).parseMap(new HashMap<>());
		assertEquals(map, back);
	}

	/**
	 * bean 字段名路径：fieldNameFilter（public BiFunction）可产出任意串，606/611 处
	 * 原先连 ':' 守卫都没有、直接裸写。
	 */
	@Test
	public void testBeanFieldNameForceQuote() throws ReflectiveOperationException {
		var json = new Json();
		json.fieldNameFilter = (klass, field) -> "a b".equals(field.getName()) ? null : "a b";
		var w = new JsonWriter().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY);
		w.write(json, new Bean());
		assertEquals("{\"a b\":7}", new String(w.toBytes(), StandardCharsets.UTF_8));
		// 含 '/' 的键同样必须加引号（parseKeyHashNoQuot 在 '/' 提前返回，裸写必坏）
		var json2 = new Json();
		json2.fieldNameFilter = (klass, field) -> "a/b".equals(field.getName()) ? null : "a/b";
		var w2 = new JsonWriter().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY);
		w2.write(json2, new Bean());
		assertEquals("{\"a/b\":7}", new String(w2.toBytes(), StandardCharsets.UTF_8));
		// 往返：带引号的特殊键仍能命中字段
		Bean back = new JsonReader().buf("{\"a b\":7}").parse(json, Bean.class);
		assertEquals(7, back.n);
		Bean back2 = new JsonReader().buf("{\"a/b\":7}").parse(json2, Bean.class);
		assertEquals(7, back2.n);
	}
}
