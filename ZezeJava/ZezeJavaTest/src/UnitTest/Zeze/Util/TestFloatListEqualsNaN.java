package UnitTest.Zeze.Util;

import Zeze.Util.FloatList;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-04：FloatList.equals 用原生 != 比较，双方均 NaN 时判不等（NaN != NaN 为 true），
 * 与自身 compareTo（Float.compare==0 判等）、JDK Float.equals（floatToIntBits）相反
 * ——equals/compareTo 矛盾，contains/indexOf/去重行为分裂。
 * 修复：equals 改 Float.compare != 0，与 compareTo 对齐。
 * 注：+0.0/-0.0 混合位形随对齐变为不等（Float.compare 口径），hashCode 为算术累积、
 * ±0/NaN 等值列表哈希仍一致，equals/hashCode 契约保持。
 */
@Fast
public class TestFloatListEqualsNaN {

	@Test
	public void testNaNListsEqualAndConsistentWithCompareTo() {
		var a = new FloatList();
		a.add(Float.NaN);
		a.add(1.5f);
		var b = new FloatList();
		b.add(Float.NaN);
		b.add(1.5f);

		Assertions.assertEquals(0, a.compareTo(b), "compareTo按Float.compare判等（NaN有确定位次）");
		Assertions.assertTrue(a.equals(b), "equals必须与compareTo一致（修复前NaN!=NaN判不等）");
		Assertions.assertTrue(a.equals((Object)b), "Object重载同样须一致");
		Assertions.assertEquals(a.hashCode(), b.hashCode(), "等值列表哈希必须一致");
	}

	@Test
	public void testRegularValuesUnchanged() {
		var a = new FloatList();
		a.add(1.0f);
		a.add(-2.5f);
		a.add(Float.POSITIVE_INFINITY);
		var b = new FloatList();
		b.add(1.0f);
		b.add(-2.5f);
		b.add(Float.POSITIVE_INFINITY);
		Assertions.assertTrue(a.equals(b));
		Assertions.assertEquals(a.hashCode(), b.hashCode());

		b.set(1, -2.6f);
		Assertions.assertFalse(a.equals(b));
		Assertions.assertFalse(a.equals((Object)b));

		var c = new FloatList();
		c.add(1.0f);
		Assertions.assertFalse(a.equals(c), "长度不等判不等");

		Assertions.assertFalse(a.equals((Object)null));
		Assertions.assertFalse(a.equals((FloatList)null));
	}
}
