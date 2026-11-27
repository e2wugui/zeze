package Zeze.Collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentSet;

import org.pcollections.PSet;

/**
 * 基于 kotlinx PersistentHashSet（CHAMP）的 {@link PSet} 实现，经 {@link Persistents#set()} 构建。
 * 写路径保证无变化操作返回同一实例（支持写日志 {@code !=} 变更检测），迭代无序。
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
		var n = kt.adding(e);
		return n == kt ? this : new KtSet<>(n);
	}

	@Override
	public PSet<E> minus(Object e) {
		var n = kt.removing((E)e);
		return n == kt ? this : new KtSet<>(n);
	}

	@Override
	public PSet<E> plusAll(Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.adding(e);
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
				n = n.removing((E)e);
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
