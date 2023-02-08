package Zeze.Util;

public interface ReplayAttack {
	/**
	 * @param serialId 传入新得到的serialId, 此ID应该>=0
	 * @return 是否判断传入的serialId是否非法
	 */
	public boolean replay(long serialId);
}
