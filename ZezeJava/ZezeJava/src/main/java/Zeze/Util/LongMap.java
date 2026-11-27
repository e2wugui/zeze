package Zeze.Util;

import java.util.function.LongFunction;
import org.jspecify.annotations.Nullable;

/** 原始 {@code long} 键映射表接口（语义类似 {@code java.util.Map}，避免装箱与 GC 开销）。 */
public interface LongMap<V extends @Nullable Object> extends Iterable<V> {
	/** 返回映射表中的条目数。 */
	int size();

	/** 返回映射表是否为空。 */
	boolean isEmpty();

	boolean containsKey(long key);

	boolean containsValue(V value);

	/** 返回指定 key 对应的值，不存在时返回 {@code null}。 */
	@Nullable V get(long key);

	V getOrDefault(long key, V defaultValue);

	/** 写入键值映射，返回该 key 原先关联的值（不存在时返回 {@code null}）。 */
	@Nullable V put(long key, V value);

	@Nullable V putIfAbsent(long key, V value);

	@Nullable V replace(long key, V value);

	boolean replace(long key, V oldValue, V newValue);

	@Nullable V computeIfAbsent(long key, LongFunction<? extends @Nullable V> mappingFunction);

	/** 移除指定 key 的映射，返回原先关联的值（不存在时返回 {@code null}）。 */
	@Nullable V remove(long key);

	boolean remove(long key, V value);

	/** 清空所有映射。 */
	void clear();

	LongIterator keyIterator();

	MapIterator<V> entryIterator();

	interface LongIterator {
		boolean hasNext();

		long next();

		void remove();
	}

	interface MapIterator<V extends @Nullable Object> {
		boolean moveToNext();

		long key();

		/** 在 {@link #moveToNext()} 返回 true 后，返回当前条目的值。 */
		V value();

		void remove();
	}

	static <V extends @Nullable Object> boolean equals(@Nullable LongMap<V> a, @Nullable LongMap<V> b) {
		if (a == b)
			return true;
		if (a == null || b == null || a.size() != b.size())
			return false;
		for (MapIterator<V> it = a.entryIterator(); it.moveToNext(); ) {
			long k = it.key();
			V v = it.value();
			if (v != null) {
				if (!v.equals(b.get(k)))
					return false;
			} else {
				if (b.get(k) != null || !b.containsKey(k))
					return false;
			}
		}
		return true;
	}
}
