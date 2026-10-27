package Zeze.Collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;

import org.pcollections.PVector;

import Zeze.Transaction.Collections.PersistentList;

/**
 * pcollections {@link PVector} 的自移植 PersistentList（32 扇出 trie）内核实现（myktt 档）。
 * 内核为仓库内从 kotlinx.collections.immutable 0.5.2 私有移植的 {@link PersistentList}，
 * 适配层与 {@link KtVector} 同构（同一契约两副桥接，语义总述见 {@link MyKtMap}）。
 * 仅经 {@link Persistents#vector()} 构建使用。
 *
 * <p>契约与 {@link KtVector} 逐位一致（回归测试同一套钉住）：
 * <ul>
 *   <li>no-op 引用恒等：{@code minus(不存在元素)}/{@code minusAll(全不存在)} 返回同一实例
 *       （Zeze 写路径 {@code !=} 判定依赖）；追加/按下标 op 恒产生变更，天然新实例；</li>
 *   <li>{@code plus(int)}/{@code with}/{@code minus(int)} 越界由内核按 JDK 时机抛 IOOBE，
 *       持久化语义下不存在"部分修改"；</li>
 *   <li>equals/hashCode/迭代走 JDK List 契约（AbstractList 白拿）。</li>
 * </ul>
 *
 * <p>{@link #subList(int, int)} 不支持（Zeze 事务 PList 层本就拒绝 subList，无消费方）。
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
		// 逐个 remove 而非 removeAll：pcollections 契约是"每个元素删首个匹配"（重复元素
		// 按 c 中出现次数逐个消），removeAll 是含即全删，二者对重复元素结果不同。
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
