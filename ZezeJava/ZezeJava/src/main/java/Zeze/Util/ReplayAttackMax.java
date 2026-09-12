package Zeze.Util;

public class ReplayAttackMax extends FastLock implements ReplayAttack {
	// 哨兵-1（对齐GrowRange2）：契约"serialId应该≥0"，max初始0会把首个serialId=0判成重放
	// （0>0不成立）——0起编的协议首包被丢。max=-1时replay(0)正常放行；负数天然被拒。
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
