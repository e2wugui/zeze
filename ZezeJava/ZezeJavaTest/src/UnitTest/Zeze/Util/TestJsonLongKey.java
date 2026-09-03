package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.Json;
import Zeze.Util.JsonWriter;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;

/**
 * 块边界回归：LongHashMap/LongConcurrentHashMap 的 key writer 在 4096 字节块边界上对
 * 19~20 位 long key 的 ensure 预留不足（原 12~15，实际最多需要 24），触发 AIOOBE。
 * 每条定长记录使 pos 以固定步长扫过整个块周期，覆盖全部 [ensure值,实际需要值) 危险窗口，
 * 保证确定性命中（见 FND-U1-1 分析）。
 */
@Fast
public final class TestJsonLongKey {
	static class BeanL {
		final LongHashMap<String> m = new LongHashMap<>();
	}

	static class BeanC {
		final LongConcurrentHashMap<String> m = new LongConcurrentHashMap<>();
	}

	// 19 位十进制的 key（1000000000000000000..1000000000000002099），值均为单字符，保证每条记录等长
	private static final int COUNT = 2100;
	private static final long KEY_BASE = 1_000_000_000_000_000_000L;

	private static void fill(LongHashMap<String> m) {
		for (int i = 0; i < COUNT; i++)
			m.put(KEY_BASE + i, "x");
	}

	private static void fill(LongConcurrentHashMap<String> m) {
		for (int i = 0; i < COUNT; i++)
			m.put(KEY_BASE + i, "x");
	}

	private static void check(String json, int expectSize) throws ReflectiveOperationException {
		BeanL l = Json.parse(json, BeanL.class);
		assertNotNull(l);
		assertEquals(expectSize, l.m.size());
		assertEquals("x", l.m.get(KEY_BASE));
		assertEquals("x", l.m.get(KEY_BASE + COUNT - 1));
	}

	@Test
	public void testLongHashMapCompact() throws ReflectiveOperationException {
		var b = new BeanL();
		fill(b.m);
		// 修复前：约 26 字节/条扫过 4096 字节块时，某条 key 的 pos 落入 [14,23) 窗口 → AIOOBE
		check(Json.toCompactString(b), COUNT);
	}

	@Test
	public void testLongHashMapNoQuote() throws ReflectiveOperationException {
		var b = new BeanL();
		fill(b.m);
		String json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY).write(b).toString();
		check(json, COUNT);
	}

	@Test
	public void testLongHashMapPretty() throws ReflectiveOperationException {
		var b = new BeanL();
		fill(b.m);
		String json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_PRETTY_FORMAT).write(b).toString();
		check(json, COUNT);
		// pretty+noQuote：危险窗口为 [13,22)
		json = JsonWriter.local().clear()
				.setFlags(JsonWriter.FLAG_PRETTY_FORMAT | JsonWriter.FLAG_NO_QUOTE_KEY).write(b).toString();
		check(json, COUNT);
	}

	@Test
	public void testLongConcurrentHashMap() throws ReflectiveOperationException {
		var b = new BeanC();
		fill(b.m);
		BeanC c = Json.parse(Json.toCompactString(b), BeanC.class);
		assertNotNull(c);
		assertEquals(COUNT, c.m.size());
		assertEquals("x", c.m.get(KEY_BASE));

		String json = JsonWriter.local().clear()
				.setFlags(JsonWriter.FLAG_PRETTY_FORMAT | JsonWriter.FLAG_NO_QUOTE_KEY).write(b).toString();
		c = Json.parse(json, BeanC.class);
		assertNotNull(c);
		assertEquals(COUNT, c.m.size());
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonLongKey();
		t.testLongHashMapCompact();
		t.testLongHashMapNoQuote();
		t.testLongHashMapPretty();
		t.testLongConcurrentHashMap();
		System.out.println(t.getClass().getSimpleName() + ": 4 tests OK!");
	}
}
