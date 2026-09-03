package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.Json;

/**
 * 带转义的 JSON key（unicode 转义"反斜杠u+4位hex"等，Python json.dumps 默认 ensure_ascii=True 的输出形态）
 * 必须按解码后的字段名匹配，不能静默丢字段（见 FND-U1-2）。
 */
@Fast
public final class TestJsonEscapedKey {
	static class Bean {
		int abc;
		int Abc;
		int 姓名;
	}

	@Test
	public void testUnicodeEscapeAscii() throws ReflectiveOperationException {
		// unicode 转义的 'a'（1 字节路径）；修复前按原始 "u0061" 六字节计算 hash，字段静默丢失
		Bean b = Json.parse("{\"\\u0061bc\":123}", Bean.class);
		assertNotNull(b);
		assertEquals(123, b.abc);
	}

	@Test
	public void testUnicodeEscapeHexCase() throws ReflectiveOperationException {
		// unicode 转义的 'A'（大写 hex 解析）
		Bean b = Json.parse("{\"\\u0041bc\":7,\"\\u0061bc\":8}", Bean.class);
		assertNotNull(b);
		assertEquals(7, b.Abc);
		assertEquals(8, b.abc);
	}

	@Test
	public void testUnicodeEscapeNonAscii() throws ReflectiveOperationException {
		// "姓名" 的 unicode 转义形式（3 字节 UTF-8 路径，Python json.dumps 默认输出形态）
		Bean b = Json.parse("{\"\\u59D3\\u540D\":456}", Bean.class);
		assertNotNull(b);
		assertEquals(456, b.姓名);
		// 对照：未转义的非 ASCII key 原本就正常
		b = Json.parse("{\"姓名\":789}", Bean.class);
		assertNotNull(b);
		assertEquals(789, b.姓名);
	}

	@Test
	public void testMixedEscapedAndPlainKeys() throws ReflectiveOperationException {
		Bean b = Json.parse("{\"姓名\":1,\"\\u0061bc\":2,\"Abc\":3}", Bean.class);
		assertNotNull(b);
		assertEquals(1, b.姓名);
		assertEquals(2, b.abc);
		assertEquals(3, b.Abc);
	}

	@Test
	public void testRoundTrip() throws ReflectiveOperationException {
		Bean b = new Bean();
		b.abc = 1;
		b.Abc = 2;
		b.姓名 = 3;
		// 写侧输出未转义 UTF-8，读侧（含转义 hash 路径重写后）必须仍能正确还原
		Bean r = Json.parse(Json.toCompactString(b), Bean.class);
		assertNotNull(r);
		assertEquals(1, r.abc);
		assertEquals(2, r.Abc);
		assertEquals(3, r.姓名);
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonEscapedKey();
		t.testUnicodeEscapeAscii();
		t.testUnicodeEscapeHexCase();
		t.testUnicodeEscapeNonAscii();
		t.testMixedEscapedAndPlainKeys();
		t.testRoundTrip();
		System.out.println(t.getClass().getSimpleName() + ": 5 tests OK!");
	}
}
