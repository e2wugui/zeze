package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.Json;

/**
 * 非有限 double（Infinity/NaN）必须能被框架自身写出的文本还原（见 FND-U1-4）：
 * JsonWriter 对非有限 double 输出 Infinity/-Infinity/NaN（类头声明支持 JSON5 词法），
 * 修复前 typed 的 double/float 字段读回全部变成 0.0。
 */
@Fast
public final class TestJsonNonFinite {
	static class B {
		double d;
		float f;
		Double dd;
		Float ff;
		int i;
	}

	@Test
	public void testParseInfinity() throws ReflectiveOperationException {
		B b = Json.parse("{\"d\":Infinity}", B.class);
		assertNotNull(b);
		assertEquals(Double.POSITIVE_INFINITY, b.d);

		b = Json.parse("{\"d\":-Infinity}", B.class);
		assertNotNull(b);
		assertEquals(Double.NEGATIVE_INFINITY, b.d);

		b = Json.parse("{\"d\":+Infinity}", B.class);
		assertNotNull(b);
		assertEquals(Double.POSITIVE_INFINITY, b.d);
	}

	@Test
	public void testParseNaN() throws ReflectiveOperationException {
		B b = Json.parse("{\"d\":NaN}", B.class);
		assertNotNull(b);
		assertTrue(Double.isNaN(b.d));
	}

	@Test
	public void testFloatAndWrapFields() throws ReflectiveOperationException {
		B b = Json.parse("{\"f\":Infinity,\"ff\":-Infinity,\"dd\":NaN}", B.class);
		assertNotNull(b);
		assertEquals(Float.POSITIVE_INFINITY, b.f);
		assertNotNull(b.ff);
		assertEquals(Float.NEGATIVE_INFINITY, (float)b.ff);
		assertNotNull(b.dd);
		assertTrue(b.dd.isNaN());
	}

	@Test
	public void testNextFieldKept() throws ReflectiveOperationException {
		// 非有限词法后 pos 必须停在词尾，后继字段不受影响
		B b = Json.parse("{\"d\":NaN,\"i\":5,\"f\":Infinity}", B.class);
		assertNotNull(b);
		assertTrue(Double.isNaN(b.d));
		assertEquals(5, b.i);
		assertEquals(Float.POSITIVE_INFINITY, b.f);
	}

	@Test
	public void testRoundTrip() throws ReflectiveOperationException {
		B b = new B();
		b.d = 1.0 / 0; // Infinity
		String json = Json.toCompactString(b);
		assertEquals("{\"d\":Infinity,\"f\":0.0,\"i\":0}", json);
		B r = Json.parse(json, B.class);
		assertNotNull(r);
		assertEquals(Double.POSITIVE_INFINITY, r.d);

		b = new B();
		b.d = -1.0 / 0;
		json = Json.toCompactString(b);
		assertEquals("{\"d\":-Infinity,\"f\":0.0,\"i\":0}", json);
		r = Json.parse(json, B.class);
		assertNotNull(r);
		assertEquals(Double.NEGATIVE_INFINITY, r.d);

		b = new B();
		b.d = 0.0 / 0; // NaN
		json = Json.toCompactString(b);
		assertEquals("{\"d\":NaN,\"f\":0.0,\"i\":0}", json);
		r = Json.parse(json, B.class);
		assertNotNull(r);
		assertTrue(Double.isNaN(r.d));
	}

	@Test
	public void testFiniteUnaffected() throws ReflectiveOperationException {
		B b = Json.parse("{\"d\":-1.5e10,\"i\":7}", B.class);
		assertNotNull(b);
		assertEquals(-1.5e10, b.d);
		assertEquals(7, b.i);
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonNonFinite();
		t.testParseInfinity();
		t.testParseNaN();
		t.testFloatAndWrapFields();
		t.testNextFieldKept();
		t.testRoundTrip();
		t.testFiniteUnaffected();
		System.out.println(t.getClass().getSimpleName() + ": 6 tests OK!");
	}
}
