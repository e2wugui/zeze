package UnitTest.Zeze.Util;

import Zeze.Util.ReplayAttack;
import Zeze.Util.ReplayAttackGrowRange;
import Zeze.Util.ReplayAttackGrowRange2;
import Zeze.Util.ReplayAttackMax;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-20：接口契约"serialId 应该≥0"，三实现对 serialId=0 判定互相矛盾——
 * GrowRange（<=0拒绝）与 Max（max初始0，replay(0)走0>0不成立判重放）拒绝首包0；
 * GrowRange2（<0拒绝，max初始-1）放行。统一到接口契约：0 合法。
 */
@Fast
public class TestReplayAttackZero {

	private static void checkZeroFirstPacket(ReplayAttack ra, String impl) {
		Assertions.assertFalse(ra.replay(0), impl + "：首个serialId=0必须放行（契约≥0合法）");
		Assertions.assertTrue(ra.replay(0), impl + "：重复的serialId=0必须判重放");
		Assertions.assertFalse(ra.replay(5), impl + "：递增serialId必须放行");
		Assertions.assertTrue(ra.replay(-1), impl + "：负数serialId必须拒绝");
	}

	@Test
	public void testZeroConsistentAcrossImplementations() {
		checkZeroFirstPacket(new ReplayAttackMax(), "ReplayAttackMax");
		checkZeroFirstPacket(new ReplayAttackGrowRange(1024), "ReplayAttackGrowRange");
		checkZeroFirstPacket(new ReplayAttackGrowRange2(), "ReplayAttackGrowRange2");
	}
}
