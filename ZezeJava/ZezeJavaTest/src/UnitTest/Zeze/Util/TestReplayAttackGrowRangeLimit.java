package UnitTest.Zeze.Util;

import harness.Fast;
import Zeze.Util.ReplayAttackGrowRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 构造上限收紧（FND2-U2-1）：
 * 窗口 N=capacity*8 在 replay() 内全程按 int 计算，capacity&gt;2^27 时 N 溢出
 * （2^31→MIN_VALUE、2^32→0），前向分支取模失效/除零，回退分支 (int)grow 截断回绕
 * 还可能放行过期 serialId。旧的 1&lt;&lt;30 上限只防了 capacity 倍增死循环。
 * 修复后 limit&gt;2^27 构造即抛 IAE（此时 N=2^30 是最后一个 int 安全值）。
 */
@Fast
public class TestReplayAttackGrowRangeLimit {

	@Test
	public void testLimitUpperBound() {
		// 越界各档全部构造期拒绝（不实际分配 range）
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ReplayAttackGrowRange((1 << 27) + 1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ReplayAttackGrowRange(1 << 28));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ReplayAttackGrowRange(1 << 30));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ReplayAttackGrowRange(Integer.MAX_VALUE));
	}

	/**
	 * 大窗口(1MB=2^20 窗口位)仍正常工作：前向/重复/过期语义不变。
	 * 边界值 1&lt;&lt;27 本身合法(N=2^30)但需分配 128MB，不在测试中构造。
	 */
	@Test
	public void testLargeLegalWindow() {
		var r = new ReplayAttackGrowRange(1 << 20);
		Assertions.assertFalse(r.replay(1));
		Assertions.assertTrue(r.replay(1)); // 重复，拒绝
		Assertions.assertFalse(r.replay(2));
		// 跳过大半窗口：沿途清位、正常落位放行
		Assertions.assertFalse(r.replay(1L << 21));
		// serialId=1 回退落位恰是仍未清理的位1(首次replay(1)置位)，判重拒绝
		Assertions.assertTrue(r.replay(1));
	}
}
