package Zeze.Util;

import org.jetbrains.annotations.NotNull;

/**
 * Locks 按 hashCode/equals 去重并选择 segment：键值相同的查询必须命中同一个注册实例，
 * 否则相同键每次都得到一把新锁，互斥完全失效。因此实现类必须以键值为语义覆写
 * hashCode()/equals()，并保持与 compareTo 一致；不能落回 Object 的身份语义。
 *
 * @param <Subclass> 实现类自身
 */
public interface Lockey<Subclass> extends Comparable<Subclass> {
	@NotNull Subclass alloc();
}
