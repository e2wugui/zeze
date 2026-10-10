package Zeze.Util;

import java.util.HashSet;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 有界随机闭区间跨度不得按有符号同位宽计算：跨度可达2^32/2^64个元素，
 * int/long正数表示不了。
 * <p>
 * nextInt(min,max)旧实现用int算max-min+1，跨度>=2^31时溢出为负：
 * [0,INT_MAX]产生区间外负数，全宽[INT_MIN,INT_MAX]跨度为0恒返回MIN；
 * nextLong(min,max)同构——[0,LONG_MAX]跨度2^63成负数走进32位乘法，
 * 全宽跨度0恒MIN。修复：跨度一律long/unsigned运算，全宽直接用原始位，
 * 宽跨度无符号取模；旧有效范围（int跨度<=2^31-1、long正跨度）的算术
 * 与随机消耗逐位保持，种子序列不变（这是"稳定"随机存在的前提）。
 */
@Fast
public class TestStableRandomRangeOverflow {
	private static final int SAMPLES = 10_000;

	/** 小跨度序列兼容：同种子下与旧算术（int跨度、32位乘法）逐位一致。 */
	@Test
	public void testSmallRangeSequenceUnchanged() {
		var a = new StableRandom(42);
		var b = new StableRandom(42);
		for (var i = 0; i < 1000; i++) {
			var raw = b.nextBits(32) & 0xffff_ffffL; // v1的nextBits(32)==next()原始32位
			var expected = (int)((raw * 32L >> 32) + 5); // 旧公式：闭区间[5,36]
			Assertions.assertEquals(expected, a.nextInt(5, 36), "第" + i + "个值");
		}
		var a2 = new StableRandom2(1, 2, 3, 4, 5);
		var b2 = new StableRandom2(1, 2, 3, 4, 5);
		for (var i = 0; i < 1000; i++) {
			var raw = b2.next() & 0xffff_ffffL; // v2的next()即nextBits(32)原始32位
			var expected = (int)((raw * 32L >> 32) + 5);
			Assertions.assertEquals(expected, a2.nextInt(5, 36), "第" + i + "个值");
		}
	}

	/** 宽int区间全部落在界内；全宽不退化为恒定值；等值/反向端点正常。 */
	@Test
	public void testWideIntRangesStayInRange() {
		var rng = new StableRandom(1);
		var rng2 = new StableRandom2(1, 2, 3, 4, 5);
		for (var i = 0; i < SAMPLES; i++) {
			var v = rng.nextInt(0, Integer.MAX_VALUE); // 跨度2^31（修复前：溢出产生负数）
			Assertions.assertTrue(v >= 0 && v <= Integer.MAX_VALUE, "v1 [0,MAX]越界: " + v);
			var w = rng.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE); // 全宽（修复前恒MIN）
			Assertions.assertTrue(w >= Integer.MIN_VALUE && w <= Integer.MAX_VALUE, "v1 全宽越界: " + w);
			Assertions.assertEquals(7, rng.nextInt(7, 7), "等值端点");
			var x = rng.nextInt(Integer.MAX_VALUE, 0); // 反向端点：内部swap
			Assertions.assertTrue(x >= 0 && x <= Integer.MAX_VALUE, "v1 反向越界: " + x);
			var v2 = rng2.nextInt(0, Integer.MAX_VALUE);
			Assertions.assertTrue(v2 >= 0 && v2 <= Integer.MAX_VALUE, "v2 [0,MAX]越界: " + v2);
			var w2 = rng2.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
			Assertions.assertTrue(w2 >= Integer.MIN_VALUE && w2 <= Integer.MAX_VALUE, "v2 全宽越界: " + w2);
			var q = rng.nextInt(-5, 5); // 跨0
			Assertions.assertTrue(q >= -5 && q <= 5, "v1 跨0越界: " + q);
		}
		var seen = new HashSet<Integer>();
		var r = new StableRandom(7);
		for (var i = 0; i < 100; i++)
			seen.add(r.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
		Assertions.assertTrue(seen.size() > 10, "全宽不得退化为恒定值（修复前恒MIN），实际去重数=" + seen.size());
	}

	/** 宽long区间全部落在界内；全宽不退化为恒定值。 */
	@Test
	public void testWideLongRangesStayInRange() {
		var rng = new StableRandom(1);
		var rng2 = new StableRandom2(1, 2, 3, 4, 5);
		for (var i = 0; i < SAMPLES; i++) {
			var v = rng.nextLong(0, Long.MAX_VALUE); // 无符号跨度2^63（修复前：负数走进32位乘法）
			Assertions.assertTrue(v >= 0 && v <= Long.MAX_VALUE, "v1 [0,LMAX]越界: " + v);
			var w = rng.nextLong(Long.MIN_VALUE, -1L); // 负半轴跨度2^63
			Assertions.assertTrue(w >= Long.MIN_VALUE && w <= -1L, "v1 负半轴越界: " + w);
			var u = rng.nextLong(Long.MIN_VALUE, Long.MAX_VALUE); // 全宽（修复前恒MIN）
			Assertions.assertTrue(u >= Long.MIN_VALUE && u <= Long.MAX_VALUE, "v1 全宽越界: " + u);
			var q = rng.nextLong(-5, 5);
			Assertions.assertTrue(q >= -5 && q <= 5, "v1 跨0越界: " + q);
			var v2 = rng2.nextLong(0, Long.MAX_VALUE);
			Assertions.assertTrue(v2 >= 0 && v2 <= Long.MAX_VALUE, "v2 [0,LMAX]越界: " + v2);
			var u2 = rng2.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
			Assertions.assertTrue(u2 >= Long.MIN_VALUE && u2 <= Long.MAX_VALUE, "v2 全宽越界: " + u2);
		}
		var seen = new HashSet<Long>();
		var r = new StableRandom(7);
		for (var i = 0; i < 100; i++)
			seen.add(r.nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
		Assertions.assertTrue(seen.size() > 10, "全宽不得退化为恒定值（修复前恒MIN），实际去重数=" + seen.size());
	}
}
