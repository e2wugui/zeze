package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;

/**
 * 指数溢出（|exp| 超过内部 maxExp）分支不得吞掉后续元素/分隔符（见 FND-U1-3）：
 * 修复前 do-while 吃掉剩余指数数字后停在分隔符上，控制回到外层 while 再 ++p，
 * 把分隔符后的元素继续当指数数字吞掉（[1e-9999999999,2] -> [0]）或跳过分隔符错位。
 */
@Fast
public final class TestJsonExpOverflow {
	static class B {
		int i;
		int j;
		long l;
		long m;
		double d;
		double e;
	}

	@Test
	public void testIntNextFieldKept() throws ReflectiveOperationException {
		// 修复前：j 被 pos 漂移吞掉，结果 j=0
		B b = Json.parse("{\"i\":1e-9999999999,\"j\":2}", B.class);
		assertNotNull(b);
		assertEquals(0, b.i);
		assertEquals(2, b.j);
	}

	@Test
	public void testLongNextFieldKept() throws ReflectiveOperationException {
		B b = Json.parse("{\"l\":1e-9999999999,\"m\":7}", B.class);
		assertNotNull(b);
		assertEquals(0L, b.l);
		assertEquals(7L, b.m);
	}

	@Test
	public void testDoubleNextFieldKept() throws ReflectiveOperationException {
		B b = Json.parse("{\"d\":1e-9999999999,\"e\":2.5}", B.class);
		assertNotNull(b);
		assertEquals(0.0, b.d);
		assertEquals(2.5, b.e);
	}

	@Test
	public void testUntypedListElementsKept() throws ReflectiveOperationException {
		ArrayList<?> a = (ArrayList<?>)JsonReader.local().buf("[1e-9999999999,2]").parse();
		assertNotNull(a);
		assertEquals(2, a.size());
		assertEquals(0.0, a.get(0)); // 无类型列表的数字元素是 Double
		assertEquals(2, a.get(1));

		// 修复前：pos 恰好多跳 1 字节停在开引号上，"x" 被吞
		a = (ArrayList<?>)JsonReader.local().buf("[1e-9999999999,\"x\"]").parse();
		assertNotNull(a);
		assertEquals(2, a.size());
		assertEquals(0.0, a.get(0));
		assertEquals("x", a.get(1));
	}

	@Test
	public void testPositiveExpSaturation() throws ReflectiveOperationException {
		// 正指数溢出饱和：值语义 + 后续元素不受影响
		ArrayList<?> a = (ArrayList<?>)JsonReader.local().buf("[1e9999999999,2]").parse();
		assertNotNull(a);
		assertEquals(2, a.size());
		assertEquals(Double.POSITIVE_INFINITY, a.get(0));
		assertEquals(2, a.get(1));

		B b = Json.parse("{\"i\":1e9999999999,\"j\":2}", B.class);
		assertNotNull(b);
		assertEquals(Integer.MAX_VALUE, b.i);
		assertEquals(2, b.j);
	}

	@Test
	public void testNormalNumbersUnaffected() throws ReflectiveOperationException {
		B b = Json.parse("{\"i\":123,\"l\":9007199254740993,\"d\":1.5e-3,\"e\":2.0e10,\"j\":-5,\"m\":6}", B.class);
		assertNotNull(b);
		assertEquals(123, b.i);
		assertEquals(9007199254740993L, b.l);
		assertEquals(1.5e-3, b.d);
		assertEquals(2.0e10, b.e);
		assertEquals(-5, b.j);
		assertEquals(6L, b.m);
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonExpOverflow();
		t.testIntNextFieldKept();
		t.testLongNextFieldKept();
		t.testDoubleNextFieldKept();
		t.testUntypedListElementsKept();
		t.testPositiveExpSaturation();
		t.testNormalNumbersUnaffected();
		System.out.println(t.getClass().getSimpleName() + ": 6 tests OK!");
	}
}
