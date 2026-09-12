package UnitTest.Zeze.Util;

import Zeze.Util.FloatList;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-17：compareTo 用减法判序——Inf-Inf=NaN、含NaN差恒NaN，c!=0 恒真恒返回1：
 * 相等元素比较为"大于"、违反反对称性。改用 Float.compare（JDK 定义的全序，
 * NaN 有确定位次），对齐 IntList（已修减法溢出）/LongList（FND3-15）形态。
 */
@Fast
public class TestFloatListCompareTo {

	private static FloatList of(float... xs) {
		var l = new FloatList();
		for (var x : xs)
			l.add(x);
		return l;
	}

	@Test
	public void testNonFiniteTotalOrder() {
		var inf = of(Float.POSITIVE_INFINITY);
		Assertions.assertEquals(0, inf.compareTo(of(Float.POSITIVE_INFINITY)),
				"Inf vs Inf 相等（原实现 Inf-Inf=NaN 恒返回1）");

		var nan = of(Float.NaN);
		Assertions.assertEquals(0, nan.compareTo(of(Float.NaN)), "NaN vs NaN 相等");

		// 反对称性：sgn(compare(x,y)) == -sgn(compare(y,x))
		var one = of(1f);
		Assertions.assertTrue(inf.compareTo(one) > 0 && one.compareTo(inf) < 0);
		Assertions.assertTrue(nan.compareTo(one) > 0 && one.compareTo(nan) < 0,
				"Float.compare语义：NaN大于一切有限值");

		// 有限值回归
		Assertions.assertEquals(0, of(1.5f, 2.5f).compareTo(of(1.5f, 2.5f)));
		Assertions.assertTrue(of(1.5f, 2.4f).compareTo(of(1.5f, 2.5f)) < 0);
		Assertions.assertTrue(of(1.5f).compareTo(of(1.5f, 0f)) < 0, "前缀小于更长者");
	}
}
