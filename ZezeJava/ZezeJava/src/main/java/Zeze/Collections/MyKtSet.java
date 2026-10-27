package Zeze.Collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import org.pcollections.PSet;

import Zeze.Transaction.Collections.PersistentHashSet;

/**
 * pcollections {@link PSet} 的自移植 PersistentHashSet（CHAMP）内核实现（myktt 档）。
 * 内核为仓库内从 kotlinx.collections.immutable 0.5.2 私有移植的
 * {@link PersistentHashSet}，适配层与 {@link KtSet} 同构（同一契约两副桥接，
 * 语义总述见 {@link MyKtMap}）。仅经 {@link Persistents#set()} 构建使用。
 *
 * <p>契约与 {@link KtSet} 逐位一致（回归测试同一套钉住）：
 * <ul>
 *   <li>no-op 引用恒等：add 已存在元素/remove 不存在元素/批量全 no-op 返回同一实例
 *       （PSet1.add/remove 等写路径 {@code !=} 判定依赖）；</li>
 *   <li>equals/hashCode/contains 走 JDK Set 契约（AbstractSet 白拿，contains 直达内核）；</li>
 *   <li>迭代顺序与 pcollections 同为无序。</li>
 * </ul>
 */
public final class MyKtSet<E> extends AbstractSet<E> implements PSet<E> {
	private final PersistentHashSet<E> kt;

	private MyKtSet(PersistentHashSet<E> kt) {
		this.kt = kt;
	}

	public static <E> MyKtSet<E> empty() {
		return new MyKtSet<>(PersistentHashSet.empty());
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
		var n = kt.adding(e);
		return n == kt ? this : new MyKtSet<>(n);
	}

	@Override
	public PSet<E> minus(Object e) {
		var n = kt.removing(e);
		return n == kt ? this : new MyKtSet<>(n);
	}

	@Override
	public PSet<E> plusAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.adding(e);
			return n == kt ? this : new MyKtSet<>(n);
		}
		var b = kt.builder();
		boolean changed = b.addAll(c);
		return changed ? new MyKtSet<>(b.build()) : this;
	}

	@Override
	public PSet<E> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.removing(e);
			return n == kt ? this : new MyKtSet<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : c)
			changed |= b.remove(e);
		return changed ? new MyKtSet<>(b.build()) : this;
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
