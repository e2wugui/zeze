package Zeze.Collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentList;

import org.pcollections.PVector;

/**
 * 基于 kotlinx PersistentList（32 扇出 trie）的 {@link PVector} 实现，经 {@link Persistents#vector()} 构建。
 * 写路径保证无变化删除返回同一实例（支持写日志 {@code !=} 变更检测）；不支持 {@link #subList(int, int)}。
 */
public final class KtVector<E> extends AbstractList<E> implements PVector<E> {
	private final PersistentList<E> kt;

	private KtVector(PersistentList<E> kt) {
		this.kt = kt;
	}

	public static <E> KtVector<E> empty() {
		return new KtVector<>(ExtensionsKt.<E>persistentListOf());
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
		return new KtVector<>(kt.adding(e));
	}

	@Override
	public PVector<E> plus(int index, E e) {
		return new KtVector<>(kt.addingAt(index, e));
	}

	@Override
	public PVector<E> with(int index, E e) {
		return new KtVector<>(kt.replacingAt(index, e));
	}

	@Override
	public PVector<E> minus(int index) {
		return new KtVector<>(kt.removingAt(index));
	}

	@Override
	public PVector<E> minus(Object e) {
		var n = kt.removing((E)e);
		return n == kt ? this : new KtVector<>(n);
	}

	@Override
	public PVector<E> plusAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.adding(e);
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(c);
		return new KtVector<>(b.build());
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
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(index, c);
		return new KtVector<>(b.build());
	}

	@Override
	public PVector<E> minusAll(Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.removing((E)e);
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : c)
			changed |= b.remove(e);
		return changed ? new KtVector<>(b.build()) : this;
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
