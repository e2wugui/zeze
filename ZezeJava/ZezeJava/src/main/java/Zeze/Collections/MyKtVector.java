package Zeze.Collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;

import org.pcollections.PVector;

import Zeze.Transaction.Collections.PersistentList;

/**
 * 基于自移植 {@link PersistentList}（32 扇出 trie）的 {@link PVector} 实现，经 {@link Persistents#vector()} 构建。
 * 契约与 {@link KtVector} 一致：无变化删除返回同一实例；不支持 {@link #subList(int, int)}。
 */
public final class MyKtVector<E> extends AbstractList<E> implements PVector<E> {
	private final PersistentList<E> kt;

	private MyKtVector(PersistentList<E> kt) {
		this.kt = kt;
	}

	public static <E> MyKtVector<E> empty() {
		return new MyKtVector<>(PersistentList.empty());
	}

	// ===== 读 =====

	@Override
	public E get(int index) {
		return kt.get(index);
	}

	@Override
	public int size() {
		return kt.size();
	}

	@Override
	public boolean isEmpty() {
		return kt.isEmpty();
	}

	/** 直接走内核迭代器（AbstractList 默认迭代经 get(i)，多付一层 O(log32 n) 寻址）。 */
	@Override
	public Iterator<E> iterator() {
		return kt.iterator();
	}

	// ===== 持久化写 =====

	@Override
	public PVector<E> plus(E e) {
		return new MyKtVector<>(kt.adding(e));
	}

	@Override
	public PVector<E> plus(int index, E e) {
		return new MyKtVector<>(kt.addingAt(index, e));
	}

	@Override
	public PVector<E> with(int index, E e) {
		return new MyKtVector<>(kt.replacingAt(index, e));
	}

	@Override
	public PVector<E> minus(int index) {
		return new MyKtVector<>(kt.removingAt(index));
	}

	@Override
	public PVector<E> minus(Object e) {
		var n = kt.removing(e);
		return n == kt ? this : new MyKtVector<>(n);
	}

	@Override
	public PVector<E> plusAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.adding(e);
			return n == kt ? this : new MyKtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(c);
		return new MyKtVector<>(b.build());
	}

	@Override
	public PVector<E> plusAll(int index, Collection<? extends E> c) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException("index=" + index + ", size=" + size());
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.addingAt(index++, e);
			return n == kt ? this : new MyKtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(index, c);
		return new MyKtVector<>(b.build());
	}

	@Override
	public PVector<E> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.removing(e);
			return n == kt ? this : new MyKtVector<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		// 按 pcollections 契约逐个删除首个匹配项（removeAll 会删掉全部重复元素）。
		for (var e : c)
			changed |= b.remove(e);
		return changed ? new MyKtVector<>(b.build()) : this;
	}

	@Override
	public PVector<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("subList");
	}

	// ===== 可变写：不可变实现恒 UOE =====

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}
}
