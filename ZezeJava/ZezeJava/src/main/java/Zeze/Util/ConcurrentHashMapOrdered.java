package Zeze.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 使用ConcurrentHashMap和ConcurrentLinkedQueue包装实现的简单Map实现，
 * 【它会记住映射项第一次加入的顺序，并一直保持不变。】
 * 【需要定时遍历，以从Queue中回收被删除的项。】
 *
 * @param <K> key
 * @param <V> value
 */
public class ConcurrentHashMapOrdered<K, V> implements Iterable<V> {
	private final static Object deleted = new Object();

	private final @NotNull ConcurrentHashMap<K, V> map;
	private final @NotNull ConcurrentLinkedQueue<K> queue = new ConcurrentLinkedQueue<>();
	private final @NotNull AtomicInteger size = new AtomicInteger();

	public ConcurrentHashMapOrdered() {
		map = new ConcurrentHashMap<>();
	}

	public ConcurrentHashMapOrdered(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
	}

	public int size() {
		return size.get();
	}

	public boolean isEmpty() {
		return size.get() == 0;
	}

	public boolean containsKey(@NotNull K key) {
		return get(key) != null;
	}

	public boolean containsValue(@NotNull V value) {
		return map.containsValue(value);
	}

	public void clear() {
		queue.clear();
		map.clear();
		size.set(0);
	}

	public class OrderedIterator implements Iterator<V> {
		private final @NotNull Iterator<K> queueIt = queue.iterator();
		private K key;
		private V value;

		public K key() {
			return key;
		}

		@Override
		public boolean hasNext() {
			if (value != null)
				return true;
			for (; ; ) {
				var has = queueIt.hasNext();
				if (!has)
					return false;
				key = queueIt.next();
				value = map.get(key);
				while (value == deleted) {
					if (map.remove(key, value)) {
						value = null;
						break;
					}
					value = map.get(key);
				}
				if (value != null)
					return true;
				queueIt.remove();
			}
		}

		@Override
		public @NotNull V next() {
			if (!hasNext())
				throw new NoSuchElementException();

			V next = value;
			value = null;
			return next;
		}
	}

	@Override
	public @NotNull OrderedIterator iterator() {
		return new OrderedIterator();
	}

	public void foreach(@NotNull BiConsumer<K, V> consumer) {
		for (var it = queue.iterator(); it.hasNext(); ) {
			K k = it.next();
			V v = map.get(k);
			while (v == deleted) {
				if (map.remove(k, v)) {
					v = null;
					break;
				}
				v = map.get(k);
			}
			if (v == null)
				it.remove();
			else
				consumer.accept(k, v);
		}
	}

	public @Nullable V put(@NotNull K key, @NotNull V value) {
		V old = map.put(key, value);
		if (old == null) {
			queue.add(key); // 第一次加入。只保持第一次的顺序，重复put不加入queue。
			size.incrementAndGet();
		}
		if (old == deleted) {
			size.incrementAndGet();
			return null;
		}
		return old;
	}

	public @Nullable V putIfAbsent(@NotNull K key, @NotNull V value) {
		var oldValue = new OutObject<V>();
		map.compute(key, (k, v) -> {
			if (v == null) {
				queue.add(key);
				size.incrementAndGet();
				return value;
			}
			if (v == deleted) {
				size.incrementAndGet();
				return value;
			}
			oldValue.value = v;
			return v;
		});
		return oldValue.value;
	}

	public @Nullable V get(@NotNull K key) {
		V v = map.get(key);
		return v == deleted ? null : v;
	}

	public V getOrDefault(@NotNull K key, V defaultValue) {
		V v = get(key);
		return v != null ? v : defaultValue;
	}

	public @Nullable V remove(@NotNull K key) {
		@SuppressWarnings("unchecked")
		V old = map.replace(key, (V)deleted);
		if (old == null || old == deleted)
			return null;
		size.decrementAndGet();
		return old;
	}

	@SuppressWarnings("unchecked")
	public boolean remove(@NotNull K key, @NotNull V value) {
		if (map.replace(key, value, (V)deleted)) {
			size.decrementAndGet();
			return true;
		}
		return false;
	}

	public @Nullable V replace(@NotNull K key, @NotNull V value) {
		var oldValue = new OutObject<V>();
		map.computeIfPresent(key, (__, v) -> {
			if (v == deleted)
				return v;
			oldValue.value = v;
			return value;
		});
		return oldValue.value;
	}

	public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
		return map.replace(key, oldValue, newValue);
	}

	@Override
	public @NotNull String toString() {
		return map.toString();
	}
}
