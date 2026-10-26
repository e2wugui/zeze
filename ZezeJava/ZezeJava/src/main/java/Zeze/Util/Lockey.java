package Zeze.Util;


/**
 * Locks 按 hashCode/equals 去重并选择 segment：键值相同的查询必须命中同一个注册实例，
 * 否则相同键每次都得到一把新锁，互斥完全失效。因此实现类必须以键值为语义覆写
 * hashCode()/equals()，并保持与 compareTo 一致；不能落回 Object 的身份语义。
 *
 * @param <Subclass> 实现类自身
 */
public interface Lockey<Subclass> extends Comparable<Subclass> {
	/**
	 * 创建或初始化实际锁对象，可返回this；结果必须保持相同键值语义。
	 * 注册表保存并返回这个结果，分配失败时不登记探测键。
	 */
	Subclass alloc();
}
