package Zeze.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

	/**
	 * queue/map/size 三组件的整体快照（FND4-14）：clear 以原子替换 state 完成，三组件间永不出中间态。
	 * 原实现 clear 三步分离（queue.clear/map.clear/size.set(0)），与并发 put 交错时 increment 被
	 * set(0) 永久覆盖——map 有条目而 size()==0/isEmpty()，后续 remove 再减成负数。整体替换后，
	 * clear 竞态期间迟到 put 的增量落在废弃 state 上（对齐 CHM 自身 clear 的弱一致语义：并发更新
	 * 下 clear 与 put 的胜负本就未定义），新 state 的计数与内容恒一致。
	 */
	private static final class State<K, V> {
		final @NotNull ConcurrentHashMap<K, V> map;
		final @NotNull ConcurrentLinkedQueue<K> queue = new ConcurrentLinkedQueue<>();
		final @NotNull AtomicInteger size = new AtomicInteger();

		State(int initialCapacity) {
			map = new ConcurrentHashMap<>(initialCapacity);
		}
	}

	private final @NotNull AtomicReference<State<K, V>> state;

	public ConcurrentHashMapOrdered() {
		state = new AtomicReference<>(new State<K, V>(16));
	}

	public ConcurrentHashMapOrdered(int initialCapacity) {
		state = new AtomicReference<>(new State<K, V>(initialCapacity));
	}

	public int size() {
		return state.get().size.get();
	}

	public boolean isEmpty() {
		return state.get().size.get() == 0;
	}

	public boolean containsKey(@NotNull K key) {
		return get(key) != null;
	}

	public boolean containsValue(@NotNull V value) {
		return state.get().map.containsValue(value);
	}

	public void clear() {
		state.set(new State<>(16)); // 单次volatile写：读者要么看到全新整体，要么全旧
	}

	public class OrderedIterator implements Iterator<V> {
		private final @NotNull State<K, V> snapshot = state.get(); // 迭代期间固定在一个整体快照上
		private final @NotNull Iterator<K> queueIt = snapshot.queue.iterator();
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
				value = snapshot.map.get(key);
				while (value == deleted) {
					if (snapshot.map.remove(key, value)) {
						value = null;
						break;
					}
					value = snapshot.map.get(key);
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
		var s = state.get();
		for (var it = s.queue.iterator(); it.hasNext(); ) {
			K k = it.next();
			V v = s.map.get(k);
			while (v == deleted) {
				if (s.map.remove(k, v)) {
					v = null;
					break;
				}
				v = s.map.get(k);
			}
			if (v == null)
				it.remove();
			else
				consumer.accept(k, v);
		}
	}

	public @Nullable V put(@NotNull K key, @NotNull V value) {
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.compute(key, (k, v) -> {
			if (v == null) {
				s.queue.add(key); // 第一次加入。只保持第一次的顺序，重复put不加入queue。
				s.size.incrementAndGet();
				return value;
			}
			if (v == deleted) {
				s.size.incrementAndGet();
				return value;
			}
			oldValue.value = v;
			return value;
		});
		return oldValue.value;
	}

	public @Nullable V putIfAbsent(@NotNull K key, @NotNull V value) {
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.compute(key, (k, v) -> {
			if (v == null) {
				s.queue.add(key);
				s.size.incrementAndGet();
				return value;
			}
			if (v == deleted) {
				s.size.incrementAndGet();
				return value;
			}
			oldValue.value = v;
			return v;
		});
		return oldValue.value;
	}

	public @Nullable V get(@NotNull K key) {
		V v = state.get().map.get(key);
		return v == deleted ? null : v;
	}

	public V getOrDefault(@NotNull K key, V defaultValue) {
		var v = get(key);
		return v != null ? v : defaultValue;
	}

	public @Nullable V remove(@NotNull K key) {
		var s = state.get();
		@SuppressWarnings("unchecked")
		V old = s.map.replace(key, (V)deleted);
		if (old == null || old == deleted)
			return null;
		s.size.decrementAndGet();
		return old;
	}

	public boolean remove(@NotNull K key, @NotNull V value) {
		var s = state.get();
		if (s.map.replace(key, value, (V)deleted)) {
			s.size.decrementAndGet();
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public @Nullable V replace(@NotNull K key, @NotNull V value) {
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.computeIfPresent(key, (__, v) -> {
			if (v == deleted)
				return v;
			oldValue.value = v;
			return value;
		});
		return oldValue.value;
	}

	public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
		return state.get().map.replace(key, oldValue, newValue);
	}

	@Override
	public @NotNull String toString() {
		return state.get().map.toString();
	}
}
