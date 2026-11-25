package Zeze.Util;

import static java.util.Objects.requireNonNull;

import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * U3-F2：十进制转 double 的正确舍入。
 * <p>
 * 原快速合成路径（整数/小数 d=d*10+c 累加 + EXP 查表乘幂）在有效数字 ≥16 位时偏差
 * 可达 1 ULP，破坏"写（最短表示）→读"的往返同一性。修复：≥16 位有效数字回退
 * Double.parseDouble（正确舍入），parseInt/parseLong/parseDouble/parseNumber 四处同型。
 */
@Fast
public final class TestJsonDoubleCorrectRounding {
	static class B {
		double d;
	}

	/**
	 * 高精度字面量与 Double.parseDouble（JDK 正确舍入参照）逐位一致。
	 */
	@Test
	public void testAgreesWithDoubleParseDouble() {
		String[] nums = {
				"1.2345678901234567",
				"0.12345678901234567",
				"1.7976931348623157e308",
				"8.988465674311579e307",
				"2.2250738585072011e-308",
				"9007199254740993.0",
				"123456789012345678901234567890.5",
				"99999999999999999999",
				"1.0000000000000002",
				"4.4501477170144023e-308",
				"-1.2345678901234567e-15",
				"+1.2345678901234567",
		};
		for (String s : nums) {
			assertEquals(Double.parseDouble(s), new JsonReader().buf(s).parseDouble(), "parseDouble: " + s);
			assertEquals(Double.parseDouble(s), (Double)new JsonReader().buf(s).parseNumber(), "parseNumber: " + s);
		}
	}

	/**
	 * 核心回归：JsonWriter 最短表示 → Json.parse 读回，bit 级等于原值（固定种子确定性）。
	 */
	@Test
	public void testWriteParseRoundTripBits() throws ReflectiveOperationException {
		Random r = new Random(42);
		B b = new B();
		for (int i = 0; i < 2000; i++) {
			double d = Double.longBitsToDouble(r.nextLong());
			if (!Double.isFinite(d))
				continue;
			b.d = d;
			String json = Json.toCompactString(b);
			B back = Json.parse(json, B.class);
			assertEquals(Double.doubleToLongBits(d), Double.doubleToLongBits(requireNonNull(back).d), json);
		}
	}

	/**
	 * int/long 的饱和语义不受回退影响（指数溢出用带定界符的输入，词尾在缓冲区内的
	 * 行为是另一处未列入工单的边缘，不在本修复范围）。
	 */
	@Test
	public void testIntLongSaturationUnchanged() {
		assertEquals(Integer.MAX_VALUE, new JsonReader().buf("1e999,").parseInt());
		assertEquals(Integer.MIN_VALUE, new JsonReader().buf("-1e999,").parseInt());
		assertEquals(Long.MAX_VALUE, new JsonReader().buf("99999999999999999999").parseLong());
		assertEquals(Long.MIN_VALUE, new JsonReader().buf("-99999999999999999999").parseLong());
		// 走回退路径的 17 位长整数（Double 正确舍入后饱和/取整）
		assertEquals((long)Double.parseDouble("9223372036854775807"), new JsonReader().buf("9223372036854775807").parseLong());
		// 15 位以内（非回退路径）不受影响
		assertEquals(123456789012345L, new JsonReader().buf("123456789012345").parseLong());
	}

	/**
	 * 词法不完整的垃圾容忍输入（尾随 'e'）不走回退、保持原 lenient 行为。
	 */
	@Test
	public void testDanglingExponentStillLenient() {
		assertEquals(1.0, new JsonReader().buf("1e,").parseDouble());
		assertEquals(1.0, new JsonReader().buf("1e").parseDouble());
	}

	@Test
	public void testParseNumberLongMinValueExactLong() throws ReflectiveOperationException {
		assertEquals(Long.valueOf(Long.MIN_VALUE), Json.parse("-9223372036854775808", Object.class));
		assertEquals(Long.valueOf(Long.MIN_VALUE), new JsonReader().buf("-9223372036854775808").parseNumber());
		var jr = new JsonReader().buf("-9223372036854775808,");
		assertEquals(Long.valueOf(Long.MIN_VALUE), jr.parseNumber());
		assertEquals(20, jr.pos());

		// 紧凑序列化往返保持 Long 类型与值
		String encoded = Json.toCompactString(Long.MIN_VALUE);
		assertEquals(Long.valueOf(Long.MIN_VALUE), Json.parse(encoded, Object.class));

		// 超出 Long.MIN_VALUE 或带小数/指数后缀仍返回正确符号的 Double
		Double over = assertInstanceOf(Double.class, Json.parse("-92233720368547758080", Object.class));
		assertEquals(Double.parseDouble("-92233720368547758080"), over);

		Double frac = assertInstanceOf(Double.class, Json.parse("-9223372036854775808.5", Object.class));
		assertEquals(Double.parseDouble("-9223372036854775808.5"), frac);

		Double exp = assertInstanceOf(Double.class, Json.parse("-9223372036854775808e0", Object.class));
		assertEquals(-9223372036854775808.0, exp);

		Double danglingExp = assertInstanceOf(Double.class, new JsonReader().buf("-9223372036854775808e").parseNumber());
		assertEquals(-9223372036854775808.0, danglingExp);

		Double posOver = assertInstanceOf(Double.class, Json.parse("+9223372036854775808", Object.class));
		assertEquals(9223372036854775808.0, posOver);
	}
}
