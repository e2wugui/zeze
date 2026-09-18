package UnitTest.Zeze.Util;

import Zeze.Util.Ranges;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-09回归：Ranges.include(Ranges)原为"任一子区间被包含即真"（exists聚合），
 * 与同族assertInclude(Ranges)的全包含语义（forall）对同一输入给出相反答案
 * （this=[1,10]，rs={[1,2],[100,200]}时include真、assertInclude抛）——按名字
 * 直觉用做"整组已授权"判断即放行越界区间。修复：include(Ranges)改forall全包含，
 * assertInclude(Ranges)随之写成if(!include(rs))throw与单区间模式对偶；
 * Gen/Zeze/Util/Ranges.cs孪生同步（C#版无法在Java测试覆盖，仅代码同步）。
 */
@Fast
public class TestFnd809RangesIncludeAll {

	@Test
	public void testIncludeRangesIsSubsetSemantics() {
		var base = new Ranges("1-100,200-300");
		// 全部子区间被包含：真
		Assertions.assertTrue(base.include(new Ranges("1-50,201-250")));
		Assertions.assertTrue(base.include(new Ranges("5")));
		Assertions.assertTrue(base.include(new Ranges("1-100,200-300"))); // 恰好等于自身
		// 任一子区间越界即假（修复前红：{[1,2]}命中即返回true放行[100,200]）
		Assertions.assertFalse(base.include(new Ranges("1-2,100-200")),
				"任一子区间不被包含必须整体为假（全包含语义）");
		Assertions.assertFalse(base.include(new Ranges("150")));
		Assertions.assertFalse(base.include(new Ranges("50-150"))); // 部分重叠不算包含
	}

	@Test
	public void testAssertIncludeAgreesWithInclude() {
		var base = new Ranges("1-100,200-300");
		// assert族与include族对同一输入答案一致（修复前两者相反）
		Assertions.assertDoesNotThrow(() -> base.assertInclude(new Ranges("1-50,201-250")));
		Assertions.assertThrows(AssertionError.class, () -> base.assertInclude(new Ranges("1-2,100-200")));
		Assertions.assertThrows(AssertionError.class, () -> base.assertInclude(new Ranges("150")));
	}

	@Test
	public void testSingleRangeAndIntUnchanged() {
		var base = new Ranges("1-100,200-300");
		Assertions.assertTrue(base.include(new Ranges.Range(5, 6)));
		Assertions.assertFalse(base.include(new Ranges.Range(100, 201))); // 跨区间间隙
		Assertions.assertDoesNotThrow(() -> base.assertInclude(5));
		Assertions.assertThrows(AssertionError.class, () -> base.assertInclude(150));
	}
}
