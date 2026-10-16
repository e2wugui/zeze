package Zeze.Collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentSet;

import org.pcollections.PSet;

/**
 * pcollections {@link PSet} 的 kotlinx PersistentHashSet（CHAMP）内核实现。
 * 仅经 {@link Persistents#set()} 构建使用，替换动机是内存（pcollections MapPSet 基于
 * HashTreePMap 的 IntTree 二叉树，每元素摊 60-80B+，CHAMP 内核省数倍）。
 *
 * <p>契约对齐 pcollections（回归测试钉住）：
 * <ul>
 *   <li>no-op 引用恒等：add 已存在元素/remove 不存在元素/批量全 no-op 返回同一实例
 *       （PSet1.add/remove 等写路径 {@code !=} 判定依赖；历史注记：pcollections MapPSet 的
 *       plusAll/minusAll 恒新建包装曾迫使调用点绕行——本实现批量路径亦保恒等，绕行不再是
 *       必要但保持现状不追改）；</li>
 *   <li>equals/hashCode/contains 走 JDK Set 契约（AbstractSet 白拿，contains 直达内核）；</li>
 *   <li>迭代顺序与 pcollections 同为无序。</li>
 * </ul>
 */
public final class KtSet<E> extends AbstractSet<E> implements PSet<E> {
	private final PersistentSet<E> kt;

	private KtSet(PersistentSet<E> kt) {
		this.kt = kt;
	}

	public static <E> KtSet<E> empty() {
		return new KtSet<>(ExtensionsKt.<E>persistentHashSetOf());
	}

	// ===== 读 =====

	@Override
	public int size() {
		return kt.size();
	}

	@Override
	public boolean isEmpty() {
		return kt.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return kt.contains(o);
	}

	/** 直接走内核迭代器（AbstractSet 默认经 iterator 链，少一层转发）。 */
	@Override
	public Iterator<E> iterator() {
		return kt.iterator();
	}

	// ===== 持久化写 =====

	@Override
	public PSet<E> plus(E e) {
		var n = kt.add(e);
		return n == kt ? this : new KtSet<>(n);
	}

	@Override
	public PSet<E> minus(Object e) {
		var n = kt.remove((E)e);
		return n == kt ? this : new KtSet<>(n);
	}

	@Override
	public PSet<E> plusAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.add(e);
			return n == kt ? this : new KtSet<>(n);
		}
		var b = kt.builder();
		boolean changed = b.addAll(c);
		return changed ? new KtSet<>(b.build()) : this;
	}

	@Override
	public PSet<E> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.remove((E)e);
			return n == kt ? this : new KtSet<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : c)
			changed |= b.remove(e);
		return changed ? new KtSet<>(b.build()) : this;
	}

	// ===== 可变写：不可变实现恒 UOE =====

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}
}
