package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.ReplayAttackGrowRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-U2-2：replay 的 (int)grow 截断。
 * <p>
 * grow 仅做上界检查（&gt; Integer.MAX_VALUE 拒绝），(int)grow 截断低 32 位：
 * serialId 远低于 max（大负 grow，远超 8192 位窗口，本应判"过期拒绝"）时截断结果可为正数，
 * 走前向分支放行，且 max 倒退 ~4×10^9，滤波器进入持续错误状态（防重放被绕过）。
 * 修复后：前向/过期判断均基于 long grow，(int) 转换只发生在已证明无损的区间。
 */
@Fast
public class TestReplayAttackGrowRangeTruncate {

	private static final long MAX = 5L << 30; // 5368709120 > 2^32

	private static void jumpToBeyondUint32Range(ReplayAttackGrowRange r) {
		for (int i = 1; i <= 5; i++)
			Assertions.assertFalse(r.replay((long)i << 30), "legal forward jump #" + i);
	}

	/**
	 * finding 的攻击序列：grow = −(2^32−5) 截断为 +5，走前向分支放行且 max 倒退。
	 */
	@Test
	public void testTruncatedNegativeGrowRejected() {
		var r = new ReplayAttackGrowRange();
		jumpToBeyondUint32Range(r);
		long crafted = MAX - (1L << 32) + 5; // 1073741829
		Assertions.assertTrue(r.replay(crafted), "beyond-window past id must be rejected"); // 修复前为 false（放行）
		// 滤波器状态未损坏：正常的下一个 id 放行、重复拒绝。
		// 修复前 max 已倒退到 crafted，这里的 grow 超过 Integer.MAX_VALUE 被拒绝，断言同样失败。
		Assertions.assertFalse(r.replay(MAX + 1));
		Assertions.assertTrue(r.replay(MAX + 1));
	}

	/**
	 * 截断为较大正数的变体：grow = −(2^32 − 2^30) 截断为 +2^30，同样必须拒绝。
	 */
	@Test
	public void testTruncatedToLargePositiveRejected() {
		var r = new ReplayAttackGrowRange();
		jumpToBeyondUint32Range(r);
		Assertions.assertTrue(r.replay(MAX - (1L << 32) + (1L << 30)));
		Assertions.assertFalse(r.replay(MAX + 2));
		Assertions.assertTrue(r.replay(MAX + 2));
	}

	/**
	 * 回归：窗口内的正常后向（旧但未过期）与恰好过期边界不受影响。
	 */
	@Test
	public void testBackwardWindowUnchanged() {
		var r = new ReplayAttackGrowRange();
		Assertions.assertFalse(r.replay(9000));
		Assertions.assertFalse(r.replay(8100)); // 窗口内最老：放行
		Assertions.assertFalse(r.replay(8090));
		Assertions.assertTrue(r.replay(8090)); // 重复
		Assertions.assertTrue(r.replay(9000 - 8192)); // 恰好过期：拒绝
		Assertions.assertFalse(r.replay(9000 - 8192 + 1)); // 过期+1：放行
	}
}
