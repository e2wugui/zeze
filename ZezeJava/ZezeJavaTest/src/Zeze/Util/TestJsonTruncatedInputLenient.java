package Zeze.Util;

import org.jspecify.annotations.Nullable;

import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * JsonReader 截断/尾部注释输入的 EOF 越界回归（20261020-09）：
 * skipComment 首读、end、skipNext、skipVar（循环头/','快跳/字符串扫描/嵌套扫描）此前
 * 均无 EOF 守卫——"[1 /"、"/"、"[1"、"{"、"[ " 等畸形输入直接 AIOOBE 穿透
 * Json.parse（只捕 ReflectiveOperationException）到达 DbWeb 等 HTTP 入口成 500+栈，
 * 绕过既有 400 "parse value failed!" 分类通道（与在档 /api/search 空 body 事故同形）。
 * 修复口径对齐 next() 的 NUL 哨兵哲学：注释到 EOF 按耗尽、容器截断按闭合定界符宽容收尾。
 */
@Fast
public final class TestJsonTruncatedInputLenient {

	static class Bean {
		int value;
		@Nullable String name;
	}

	/** 全语料不抛（此前各自命中一个无守卫读点）。 */
	@Test
	public void testTruncatedInputsNoThrow() {
		for (var s : new String[] {
				"[1 /", "/", "//c", "/*c", "[1", "{", "[ ", "[1,", "[1, \"ab", "[{", "[1, {2",
				"[\"ab", "{\"a\"", "{\"a\\", // 带引号串/键截断与转义截断
				"[1] /" /* 尾随注释（end路径，仅手工end触达，顶层parse不读尾随） */}) {
			assertDoesNotThrow(() -> Json.parse(s, Bean.class), "输入[" + s + "]不得抛AIOOBE");
			assertDoesNotThrow(() -> Json.parse(s, Object.class), "输入[" + s + "]泛型解析同");
		}
	}

	/** 宽容收尾的值语义：截断容器按闭合处理、纯注释/耗尽回退默认对象。 */
	@Test
	public void testTruncatedLenientValues() {
		assertEquals(java.util.List.of(1), Json.parse("[1 /", Object.class), "值后截断注释→宽容闭合");
		assertEquals(java.util.List.of(1), Json.parse("[1", Object.class), "截断于值后→宽容闭合");
		var bean = Json.parse("/", Bean.class);
		assertNotNull(bean, "纯注释耗尽→默认对象（对齐空输入哨兵契约）");
		assertEquals(0, bean.value);
	}

	/**
	 * 字符串族扫描点的截断/残缺转义EOF守卫（skipQuot九扫描点修复后仍缺守卫的漏网点）：
	 * unicode转义窗口（decodeStringSlow的parseHex4固定读4）、无引号值快/慢双路径、
	 * skipQuot裸扫、parseByteString整方法、parseBooleanKey开引号裸读。
	 */
	@Test
	public void testTruncatedStringScannersNoThrow() {
		for (var s : new String[] {
				"{\"k\":\"a\\u", "{\"k\":\"a\\u1", "{\"k\":\"a\\u12", // 值内unicode转义截断
				"{\"k\":\"a\\u12\\", // 残缺unicode转义后紧接反斜杠到EOF
				"{\"k\":\"a\\u12\"}", // 闭引号紧随残缺转义（非截断输入同命中）
				"{\"k\":abc", "{\"k\":a中", // 无引号值快/慢路径EOF
				"{\"a\\u12"}) { // 键内unicode转义截断（keyEquals重扫描）
			assertDoesNotThrow(() -> Json.parse(s, Bean.class), "输入[" + s + "]不得抛AIOOBE");
			assertDoesNotThrow(() -> Json.parse(s, Object.class), "输入[" + s + "]泛型解析同");
		}
		assertEquals("a\\u12", assertDoesNotThrow(() -> new JsonReader().buf("\"a\\u12\\").parseString()));
		assertEquals("a\\u12", assertDoesNotThrow(() -> new JsonReader().buf("a\\u12\\").parseStringNoQuot()));
		// 叶解析器直打：parseByteString快路径截断/末字节'\'/残缺unicode转义（含代理对窥视边界）
		for (var s : new String[] {"\"abc", "\"a\\", "\"a\\u", "\"a\\u1", "\"a\\u12",
				"\"a\\uD83D", "\"a\\uD83D\\u" /* 代理对第二转义截断 */}) {
			var jr = new JsonReader().buf(s);
			assertDoesNotThrow(jr::parseByteString, "输入[" + s + "]不得抛AIOOBE");
		}
		// skipQuot截断（typed key收尾路径）与parseBooleanKey截断于开引号
		assertDoesNotThrow(() -> JsonReader.parseIntegerKey(new JsonReader().buf("\"12"), '"'));
		assertDoesNotThrow(() -> JsonReader.parseBooleanKey(new JsonReader().buf("\""), '"'));
	}

	/** 宽容收尾不改变良构输入的解码语义（正样本控制）。 */
	@Test
	public void testStringScannersPositiveControl() {
		assertEquals("a中", Json.parse("{\"name\":\"a\\u4e2d\"}", Bean.class).name, "完整unicode转义照常解码");
		assertEquals("a\\u12", Json.parse("{\"name\":\"a\\u12\"}", Bean.class).name,
				"残缺unicode转义按字面量收尾（'a'+反斜杠+u+1+2）");
		assertEquals("abc", Json.parse("{\"name\":abc", Bean.class).name,
				"无引号值EOF按终结收尾（typed路径；泛型容器对无引号串值按约定返回null）");
		assertArrayEquals(new byte[] {(byte)0x61, (byte)0xe4, (byte)0xb8, (byte)0xad},
				new JsonReader().buf("\"a\\u4e2d\"").parseByteString(), "parseByteString完整转义→UTF-8三字节");
		assertArrayEquals(new byte[] {(byte)0xf0, (byte)0x9f, (byte)0x98, (byte)0x80},
				new JsonReader().buf("\"\\uD83D\\uDE00\"").parseByteString(), "parseByteString代理对→UTF-8四字节");
	}
}
