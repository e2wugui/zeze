package Zeze.Util;

/** 防重放（仅递增版）：仅记录已见最大 serialId，小于等于即判重放。 */
public class ReplayAttackMax extends FastLock implements ReplayAttack {
	// 哨兵初值 -1，确保合法首包 serialId=0 放行且负数被拒
	private long max = -1;

	@Override
	public boolean replay(long serialId) {
		if (serialId > max) {
			max = serialId;
			return false;
		}
		return true;
	}
}
