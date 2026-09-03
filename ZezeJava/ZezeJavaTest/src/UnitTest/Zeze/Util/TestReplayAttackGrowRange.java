package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.ReplayAttackGrowRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * replay(serialId) 返回 true 表示重放/非法/过期（拒绝），false 表示放行。
 */
@Fast
public class TestReplayAttackGrowRange {

	/**
	 * 前向分支 position += increase 的 int 溢出（FND-U2-1）：
	 * position=2 时再收到 serialId=2147483648（increase=2147483646，grow 未超上限），
	 * 2 + 2147483646 == 2^31，int 回绕为负下标，修复前抛 ArrayIndexOutOfBoundsException。
	 * grow &le; Integer.MAX_VALUE 的前向大跳属于合法增长：应放行并正确取模落位。
	 */
	@Test
	public void testForwardOverflow() {
		var r = new ReplayAttackGrowRange();
		Assertions.assertFalse(r.replay(2));
		Assertions.assertFalse(r.replay(2147483648L)); // 修复前：AIOOBE
		Assertions.assertTrue(r.replay(2147483648L)); // 重复，拒绝
		Assertions.assertFalse(r.replay(2147483649L)); // 下一个 id 正常放行
		Assertions.assertTrue(r.replay(2147483649L)); // 重复，拒绝
	}

	/**
	 * 溢出且取模余数非 0：position=8191（窗口末位）时 increase=Integer.MAX_VALUE，
	 * 8191 + 2147483647 &gt; 2^31；落位应为 (8191 + 2147483647) % 8192 == 8190。
	 * 修复前抛 ArrayIndexOutOfBoundsException。
	 */
	@Test
	public void testForwardOverflowNonZeroRemainder() {
		var r = new ReplayAttackGrowRange();
		Assertions.assertFalse(r.replay(8191));
		Assertions.assertFalse(r.replay(8191L + Integer.MAX_VALUE)); // 修复前：AIOOBE
		Assertions.assertTrue(r.replay(8191L + Integer.MAX_VALUE)); // 重复，拒绝
		Assertions.assertFalse(r.replay(8191L + Integer.MAX_VALUE + 1)); // 落位 8191，放行
		Assertions.assertFalse(r.replay(8191L + Integer.MAX_VALUE - 1)); // 落位 8189 已被清空，放行
	}

	/**
	 * 常规语义回归：非法 id、窗口内重复、跨窗口大跳清空、过期拒绝、跳得太远拒绝。
	 */
	@Test
	public void testNormalSemantics() {
		var r = new ReplayAttackGrowRange();
		Assertions.assertTrue(r.replay(0)); // invalid
		Assertions.assertTrue(r.replay(-1)); // invalid
		Assertions.assertFalse(r.replay(1));
		Assertions.assertFalse(r.replay(2));
		Assertions.assertFalse(r.replay(3));
		Assertions.assertTrue(r.replay(2)); // 窗口内重复
		Assertions.assertFalse(r.replay(10000)); // 超过窗口(8192)的大跳：清空后放行
		Assertions.assertTrue(r.replay(1808)); // 10000-8192：正好过期
		Assertions.assertFalse(r.replay(1809)); // 窗口内最老 id：放行
		Assertions.assertTrue(r.replay(2147493648L)); // 10000+Integer.MAX_VALUE+1：跳得太远，拒绝
	}
}
