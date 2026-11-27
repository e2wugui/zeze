package Zeze.Collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import org.pcollections.PSet;

import Zeze.Transaction.Collections.PersistentHashSet;

/**
 * 基于自移植 {@link PersistentHashSet}（CHAMP）的 {@link PSet} 实现，经 {@link Persistents#set()} 构建。
 * 契约与 {@link KtSet} 一致：无变化操作返回同一实例，迭代无序。
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
