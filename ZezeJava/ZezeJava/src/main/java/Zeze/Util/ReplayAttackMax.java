package Zeze.Util;

public class ReplayAttackMax implements ReplayAttack {
	private long max;
	@Override
	public boolean replay(long serialId) {
		if (serialId > max) {
			max = serialId;
			return false;
		}
		return true;
	}
}
