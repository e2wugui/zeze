package Zeze.Util;

/** 防重放检查接口：按 serialId 判定是否重复或非法，自带锁保护复合检查。 */
public interface ReplayAttack {
	/**
	 * @param serialId 传入新得到的serialId, 此ID应该≥0
	 * @return 是否判断传入的serialId是否非法
	 */
	boolean replay(long serialId);
	void lock();
	void unlock();
}
