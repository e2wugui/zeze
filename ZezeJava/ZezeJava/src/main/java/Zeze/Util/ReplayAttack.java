package Zeze.Util;

// 防重放检查接口：按 serialId 判定是否重复/非法（自带锁供检查复合操作使用）
public interface ReplayAttack {
	/**
	 * @param serialId 传入新得到的serialId, 此ID应该≥0
	 * @return 是否判断传入的serialId是否非法
	 */
	boolean replay(long serialId);
	void lock();
	void unlock();
}
