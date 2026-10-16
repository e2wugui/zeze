package Zeze.Collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.PersistentList;
import org.jetbrains.annotations.NotNull;
import org.pcollections.PVector;

/**
 * pcollections {@link PVector} 的 kotlinx PersistentList（32 扇出 trie）内核实现。
 * 仅经 {@link Persistents#vector()} 构建使用，替换动机是内存（pcollections TreePVector 的
 * IntTree 二叉树每元素摊 60-80B+，trie 内核省数倍）。
 *
 * <p>契约对齐 pcollections（回归测试钉住）：
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
public final class KtVector<E> extends AbstractList<E> implements PVector<E> {
	private final @NotNull PersistentList<E> kt;

	private KtVector(@NotNull PersistentList<E> kt) {
		this.kt = kt;
	}

	public static <E> @NotNull KtVector<E> empty() {
		return new KtVector<>(ExtensionsKt.<E>persistentListOf());
	}

	// ===== 读 =====

	@Override
	public @NotNull E get(int index) {
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
	public @NotNull Iterator<E> iterator() {
		return kt.iterator();
	}

	// ===== 持久化写 =====

	@Override
	public @NotNull PVector<E> plus(@NotNull E e) {
		return new KtVector<>(kt.add(e));
	}

	@Override
	public @NotNull PVector<E> plus(int index, @NotNull E e) {
		return new KtVector<>(kt.add(index, e));
	}

	@Override
	public @NotNull PVector<E> with(int index, @NotNull E e) {
		return new KtVector<>(kt.set(index, e));
	}

	@Override
	public @NotNull PVector<E> minus(int index) {
		return new KtVector<>(kt.removeAt(index));
	}

	@Override
	public @NotNull PVector<E> minus(@NotNull Object e) {
		var n = kt.remove((E)e);
		return n == kt ? this : new KtVector<>(n);
	}

	@Override
	public @NotNull PVector<E> plusAll(@NotNull Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.add(e);
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(c);
		return new KtVector<>(b.build());
	}

	@Override
	public @NotNull PVector<E> plusAll(int index, @NotNull Collection<? extends E> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.add(index++, e);
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		b.addAll(index, c);
		return new KtVector<>(b.build());
	}

	@Override
	public @NotNull PVector<E> minusAll(@NotNull Collection<?> c) {
		if (c.isEmpty())
			return this;
		if (!Persistents.BULK) {
			var n = kt;
			for (var e : c)
				n = n.remove((E)e);
			return n == kt ? this : new KtVector<>(n);
		}
		var b = kt.builder();
		boolean changed = false;
		for (var e : c)
			changed |= b.remove(e);
		return changed ? new KtVector<>(b.build()) : this;
	}

	@Override
	public @NotNull PVector<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("subList");
	}

	// ===== 可变写：不可变实现恒 UOE =====

	@Override
	public @NotNull E set(int index, @NotNull E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, @NotNull E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}
}
