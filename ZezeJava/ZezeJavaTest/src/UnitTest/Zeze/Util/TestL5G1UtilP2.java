package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import Zeze.Util.CharHashMap;
import Zeze.Util.FewModifySortedMap;
import Zeze.Util.IntHashMap;
import Zeze.Util.IntList;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.Str;
import Zeze.Util.StringFuzzySearch;

/**
 * L5/G1 组（U1×15 + U2×6 P2 + 携带5条）中可快速红绿验证的修复。
 * 注意：本文件中的断言对应 review-2026-09/l5/ 下的 patch 应用后的行为；
 * patch 未应用时，除标注“回归”外的用例预期失败（红）。
 * 刻意排除“修复前会死循环/挂死”的用例（U1-13 容量倍增、U2-10 构造器），
 * 避免主会在未应用 patch 时跑测试卡死。
 */
@Fast
public final class TestL5G1UtilP2 {

	// ---------- FND-U1-19：无类型上下文 null 不再被解析成 NaN ----------

	@SuppressWarnings("unchecked")
	@Test
	public void testUntypedNull() throws Exception {
		var m = (Map<String, Object>)JsonReader.local().buf("{\"a\":null}").parse();
		assertNull(m.get("a"), "untyped null 应解析成 null 而不是 NaN");

		var list = (List<Object>)JsonReader.local().buf("[null,1]").parse();
		assertEquals(2, list.size());
		assertNull(list.get(0));
		assertEquals(1, list.get(1));
	}

	@Test
	public void testUntypedNaNStillWorks() throws Exception { // 回归：lenient 的 nan/NaN 词法不受影响
		var list = (List<?>)JsonReader.local().buf("[NaN,nan]").parse();
		assertEquals(Double.NaN, list.get(0));
		assertEquals(Double.NaN, list.get(1));
	}

	// ---------- FND-U1-20：TYPE_OBJECT 字段重复解析先清空容器 ----------

	public static class BeanObjectField {
		public Object o;
	}

	@Test
	public void testObjectFieldReparseNoAppend() {
		var b = Json.parse("{\"o\":[1,2]}", BeanObjectField.class);
		assertNotNull(b.o);
		assertEquals(2, ((List<?>)b.o).size());
		// 同一对象再次解析：元素不应翻倍
		b = Json.parse("{\"o\":[1,2]}", b);
		assertEquals(2, ((List<?>)b.o).size());
		// null 值应把字段置 null（而不是 NaN 或残留）
		b = Json.parse("{\"o\":null}", b);
		assertNull(b.o);
	}

	// ---------- CARRY-2：无引号 key 的转义解码（parseKeyHashNoQuot） ----------

	public static class BeanUnquotKey {
		public int abc;
	}

	@Test
	public void testUnquotEscapedKey() {
		var b = Json.parse("{\\u0061bc:1}", BeanUnquotKey.class);
		assertEquals(1, b.abc, "\\u0061bc 应解码后匹配字段 abc");
	}

	// ---------- CARRY-4：parseNumber 的 Infinity/NaN 分支 return 前设置 pos ----------

	@Test
	public void testParseNumberPosAfterWord() {
		var jr = new JsonReader("[Infinity,2]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		jr.skipNext(); // 前进到 '[' 之后的 'I'（pos 停在词首）
		var v = jr.parseNumber();
		assertEquals(Double.POSITIVE_INFINITY, v);
		assertEquals(9, jr.pos(), "pos 应指向词后的 ','（下标9），而不是停留在 'I'");
	}

	// ---------- FND-U1-7：IntHashMap/CharHashMap compute/foreachUpdate 的 null 删除语义 ----------

	@Test
	public void testIntHashMapComputeNullRemoves() {
		var map = new IntHashMap<String>();
		map.put(1, null); // 本 map 允许 null 值
		assertEquals(1, map.size());
		assertNull(map.compute(1, (k, v) -> null), "既有 null 值条目经 compute(k,x->null) 应被删除");
		assertEquals(0, map.size(), "条目应被删除（Map.compute 契约）");

		assertNull(map.compute(2, (k, v) -> null), " absent key + remapping null 不插入");
		assertEquals(0, map.size());

		map.put(3, null);
		map.foreachUpdate((k, v) -> null);
		assertEquals(0, map.size(), "foreachUpdate 对 null 值既有条目返回 null 应删除");
	}

	@Test
	public void testCharHashMapComputeNullRemoves() {
		var map = new CharHashMap<String>();
		map.put('a', null);
		assertEquals(1, map.size());
		assertNull(map.compute('a', (k, v) -> null));
		assertEquals(0, map.size());

		map.put('b', null);
		map.foreachUpdate((k, v) -> null);
		assertEquals(0, map.size());
	}

	@Test
	public void testIntHashMapComputeRetainSemantics() { // 回归：非 null 路径语义不变
		var map = new IntHashMap<String>();
		map.put(1, "a");
		assertEquals("a", map.compute(1, (k, v) -> v)); // 保留
		assertEquals("b", map.compute(1, (k, v) -> "b")); // 更新
		assertEquals("c", map.compute(2, (k, v) -> "c")); // 插入
		assertEquals(2, map.size());
	}

	// ---------- FND-U1-15：IntList.compareTo 不得用减法 ----------

	@Test
	public void testIntListCompareToOverflow() {
		var a = new IntList(new int[] {2_000_000_000});
		var b = new IntList(new int[] {-2_000_000_000});
		assertTrue(a.compareTo(b) > 0, "2e9 > -2e9，减法溢出会得到反号结果");
		assertTrue(b.compareTo(a) < 0);
	}

	// ---------- FND-U2-5：Vector2IntList.indexOfVector 两参签名 ----------
	// 注：本条修复改动公开方法签名（三参→两参），测试若引用新签名会在 patch 未应用时阻断整个测试模块
	// 编译，故不在此设用例；patch 自明（镜像 Vector2List），全仓（含测试）无调用者。

	// ---------- FND-U2-7：PropertiesHelper.parse 容忍连续空白 ----------

	@Test
	public void testPropertiesHelperSpaces() {
		var p1 = PropertiesHelper.parse("-a  1 -b 2"); // 连续空格
		assertEquals("1", p1.getProperty("-a"));
		assertEquals("2", p1.getProperty("-b"));
		var p2 = PropertiesHelper.parse("  -c 3  "); // 行首/行尾空白
		assertEquals("3", p2.getProperty("-c"));
		assertThrows(IllegalArgumentException.class, () -> PropertiesHelper.parse("x -a 1"), "回归：无 -key 的 value 仍报错");
	}

	// ---------- FND-U2-8：Str.format 的 Date 分支不再生成裸 %t ----------

	@Test
	public void testStrFormatDate() {
		var d = new Date(0);
		var s = Str.format("time={t}", Map.of("t", d));
		assertEquals("time=" + d.toString(), s, "Date 走 %s（toString），修复前 100% 抛 UnknownFormatConversionException");
	}

	// ---------- FND-U2-9：StringFuzzySearch.search 空串不除零 ----------

	@Test
	public void testStringFuzzySearchEmpty() {
		var s = new StringFuzzySearch();
		s.add("abcdefgh");
		assertEquals(0, s.search("", new String[4]), "空搜索串返回 0 个结果，修复前 ArithmeticException: / by zero");
		assertTrue(s.search("abcdefg", new String[4]) >= 0); // 回归：正常搜索不异常
	}

	// ---------- FND-U1-18：BloomFilter.getTotalBits 用 long 遍历 long 容量 ----------

	@Test
	public void testBloomFilterGetTotalBitsLongCapacity() {
		// 稀疏 BitArray：容量 > 2^31 位，仅记录置位下标；负下标像真实数组实现一样抛 AIOOBE
		// （修复前 int 循环变量回绕为负 → 抛 AIOOBE；修复后 long 循环正常计数）。
		var bits = new java.util.HashSet<Long>();
		Zeze.Util.BloomFilter.BitArray ba = new Zeze.Util.BloomFilter.BitArray() {
			@Override
			public long getCapacity() {
				return (1L << 32) + 8; // 非 2 幂，走 slow-path
			}

			@Override
			public void setBit(long index) {
				if (index < 0)
					throw new ArrayIndexOutOfBoundsException((int)index);
				bits.add(index);
			}

			@Override
			public boolean getBit(long index) {
				if (index < 0)
					throw new ArrayIndexOutOfBoundsException((int)index);
				return bits.contains(index);
			}
		};
		var bf = new Zeze.Util.BloomFilter(ba, 3);
		bf.addKey(1);
		bf.addKey(2);
		assertEquals(bits.size(), bf.getTotalBits(), "置位计数应与 BitArray 记录一致，容量 >2^31 不得越界（修复前 AIOOBE）");
	}

	// ---------- FND-U1-12：FewModifySortedMap.poll* 从真实数据取出并失效快照 ----------

	@Test
	public void testFewModifySortedMapPoll() {
		var map = new FewModifySortedMap<Integer, String>();
		map.put(1, "a");
		map.put(2, "b");
		map.put(3, "c");
		var head = map.firstEntry(); // 生成读快照
		assertNotNull(head);

		var e = map.pollFirstEntry();
		assertEquals(Integer.valueOf(1), e.getKey());
		assertEquals(2, map.size(), "poll 后 size 应减 1（修复前 size 不变且快照被挖洞）");
		assertNull(map.get(1), "poll 掉的 key 不应再 get 到");
		assertEquals("b", map.firstEntry().getValue());

		var e2 = map.pollLastEntry();
		assertEquals(Integer.valueOf(3), e2.getKey());
		assertEquals(1, map.size());
		assertEquals("b", map.get(2));
	}

	// ---------- FND-U1-14：IntList/FloatList.decode 拒绝负长度 ----------

	@Test
	public void testIntListDecodeNegativeCount() {
		var list = new IntList();
		assertThrows(IllegalArgumentException.class,
				() -> list.decode(Zeze.Serialize.ByteBuffer.Allocate(8), -1),
				"负长度应立即报错（修复前 count 被毒化为 -1，此后 add 恒 AIOOBE）");
		assertEquals(0, list.size());
	}
}
