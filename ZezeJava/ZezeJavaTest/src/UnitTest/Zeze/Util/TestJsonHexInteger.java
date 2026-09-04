package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.Json;

/**
 * typed 整数字段（int/long 及包装类型）必须支持类注释声明的 JSON5 十六进制（0x）词法
 * （untyped 路径 parseNumber 早已支持，见 FND2-U1-2）；修复前 {"i":0x10} 静默解析为 0，
 * "x10" 被当垃圾扫过。Infinity/NaN 词法对整型字段属非法输入，这里选择与 parseDouble
 * 对齐：消费整个词（pos 停在词尾，后继字段不受影响），值按 (int)/(long)double 强转的
 * 饱和语义取值（Infinity -> MAX/MIN，NaN -> 0），修复前同样静默取 0 且词法被当垃圾吞掉。
 */
@Fast
public final class TestJsonHexInteger {
	static class B {
		int i;
		long l;
		Integer ii;
		Long ll;
		int x;
	}

	@Test
	public void testIntHex() throws ReflectiveOperationException {
		B b = Json.parse("{\"i\":0x10}", B.class);
		assertNotNull(b);
		assertEquals(16, b.i);

		b = Json.parse("{\"i\":0X1F}", B.class); // 大写 X / 大写 hex
		assertEquals(31, b.i);

		b = Json.parse("{\"i\":-0x10}", B.class);
		assertEquals(-16, b.i);

		b = Json.parse("{\"ii\":0xff}", B.class); // 包装类型走同一 TYPE_INT 路径
		assertNotNull(b.ii);
		assertEquals(255, b.ii);
	}

	@Test
	public void testLongHex() throws ReflectiveOperationException {
		B b = Json.parse("{\"l\":0x10}", B.class);
		assertEquals(16L, b.l);

		b = Json.parse("{\"l\":0x7FFFFFFFFFFFFFFF}", B.class);
		assertEquals(Long.MAX_VALUE, b.l);

		b = Json.parse("{\"ll\":0x10}", B.class);
		assertNotNull(b.ll);
		assertEquals(16L, b.ll);
	}

	@Test
	public void testHexNextFieldKept() throws ReflectiveOperationException {
		// hex 词解析完 pos 必须停在词尾，后继字段不受影响（修复前停在 'x'，垃圾扫过殃及后续）
		B b = Json.parse("{\"i\":0x10,\"x\":5}", B.class);
		assertEquals(16, b.i);
		assertEquals(5, b.x);
	}

	@Test
	public void testIntNonFiniteClamp() throws ReflectiveOperationException {
		B b = Json.parse("{\"i\":Infinity,\"x\":1}", B.class);
		assertEquals(Integer.MAX_VALUE, b.i);
		assertEquals(1, b.x);

		b = Json.parse("{\"i\":-Infinity}", B.class);
		assertEquals(Integer.MIN_VALUE, b.i);

		b = Json.parse("{\"i\":NaN}", B.class);
		assertEquals(0, b.i);
	}

	@Test
	public void testLongNonFiniteClamp() throws ReflectiveOperationException {
		B b = Json.parse("{\"l\":Infinity,\"x\":2}", B.class);
		assertEquals(Long.MAX_VALUE, b.l);
		assertEquals(2, b.x);

		b = Json.parse("{\"l\":-Infinity}", B.class);
		assertEquals(Long.MIN_VALUE, b.l);

		b = Json.parse("{\"l\":NaN}", B.class);
		assertEquals(0L, b.l);
	}

	@Test
	public void testDecimalRegression() throws ReflectiveOperationException {
		// 回归：十进制/负数/包装类型语义不变
		B b = Json.parse("{\"i\":-42,\"l\":12345678901,\"ii\":7,\"ll\":99}", B.class);
		assertEquals(-42, b.i);
		assertEquals(12345678901L, b.l);
		assertNotNull(b.ii);
		assertEquals(7, b.ii);
		assertNotNull(b.ll);
		assertEquals(99L, b.ll);
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonHexInteger();
		t.testIntHex();
		t.testLongHex();
		t.testHexNextFieldKept();
		t.testIntNonFiniteClamp();
		t.testLongNonFiniteClamp();
		t.testDecimalRegression();
		System.out.println(t.getClass().getSimpleName() + ": 6 tests OK!");
	}
}
