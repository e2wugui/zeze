package Zeze.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * 基于 ConcurrentHashMap 与 ConcurrentLinkedQueue 的有序并发 Map：
 * 保持键首次插入顺序，遍历时懒惰回收已删除的队列节点；拒绝 null key/value。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class ConcurrentHashMapOrdered<K, V> implements Iterable<V> {

	/** queue/map/size 组合快照；clear 通过原子替换整个 State 避免并发交错下的计数漂移。 */
	private static final class State<K, V> {
		final ConcurrentHashMap<K, Entry<K, V>> map;
		final ConcurrentLinkedQueue<Entry<K, V>> queue = new ConcurrentLinkedQueue<>();
		final AtomicInteger size = new AtomicInteger();

		State(int initialCapacity) {
			map = new ConcurrentHashMap<>(initialCapacity);
		}
	}

	/** 队列与 map 共享的条目节点；以 dead 标记区分已删除与尚未发布，避免遍历清理误摘新条目。 */
	private static final class Entry<K, V> {
		final K key;
		volatile V value;
		volatile boolean dead;

		Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	private final AtomicReference<State<K, V>> state;

	public ConcurrentHashMapOrdered() {
		state = new AtomicReference<>(new State<>(16));
	}

	public ConcurrentHashMapOrdered(int initialCapacity) {
		state = new AtomicReference<>(new State<>(initialCapacity));
	}

	public int size() {
		return state.get().size.get();
	}

	public boolean isEmpty() {
		return state.get().size.get() == 0;
	}

	public boolean containsKey(K key) {
		return get(key) != null;
	}

	public boolean containsValue(V value) {
		for (var entry : state.get().map.values())
			if (!entry.dead && Objects.equals(entry.value, value))
				return true;
		return false;
	}

	public void clear() {
		state.set(new State<>(16)); // 单次volatile写：读者要么看到全新整体，要么全旧
	}

	public class OrderedIterator implements Iterator<V> {
		private final State<K, V> snapshot = state.get(); // 迭代期间固定在一个整体快照上
		private final Iterator<Entry<K, V>> queueIt = snapshot.queue.iterator();
		private @Nullable K key;
		private @Nullable V value;

		public @Nullable K key() {
			return key;
		}

		@Override
		public boolean hasNext() {
			if (value != null)
				return true;
			for (; ; ) {
				if (!queueIt.hasNext())
					return false;
				var entry = queueIt.next();
				if (entry.dead) {
					queueIt.remove(); // 回收已删除项
					continue;
				}
				key = entry.key;
				value = entry.value;
				return true;
			}
		}

		@Override
		public V next() {
			if (!hasNext())
				throw new NoSuchElementException();

			V next = Objects.requireNonNull(value);
			value = null;
			return next;
		}
	}

	@Override
	public OrderedIterator iterator() {
		return new OrderedIterator();
	}

	public void foreach(BiConsumer<K, V> consumer) {
		var s = state.get();
		for (var it = s.queue.iterator(); it.hasNext(); ) {
			var entry = it.next();
			if (entry.dead) {
				it.remove(); // 回收已删除项
				continue;
			}
			consumer.accept(entry.key, entry.value);
		}
	}

	public @Nullable V put(K key, V value) {
		Objects.requireNonNull(value);
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.compute(key, (k, existing) -> {
			if (existing != null) {
				oldValue.value = existing.value;
				existing.value = value; // 原地改写：队列节点与首次顺序保持不变
				return existing;
			}
			var entry = new Entry<>(key, value); // 入队与发布必须是同一实例
			s.queue.add(entry); // 第一次加入。只保持第一次的顺序，重复put不加入queue。
			s.size.incrementAndGet();
			return entry;
		});
		return oldValue.value;
	}

	public @Nullable V putIfAbsent(K key, V value) {
		Objects.requireNonNull(value);
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.compute(key, (k, existing) -> {
			if (existing != null) {
				oldValue.value = existing.value;
				return existing; // 已存在：保持现值（与并发remove的交错由bin锁串行化）
			}
			var entry = new Entry<>(key, value);
			s.queue.add(entry);
			s.size.incrementAndGet();
			return entry;
		});
		return oldValue.value;
	}

	public @Nullable V get(K key) {
		var entry = state.get().map.get(key);
		return entry != null && !entry.dead ? entry.value : null;
	}

	public V getOrDefault(K key, V defaultValue) {
		var v = get(key);
		return v != null ? v : defaultValue;
	}

	public @Nullable V remove(K key) {
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.computeIfPresent(key, (k, entry) -> {
			if (entry.dead)
				return null; // 与并发remove串行化后的死亡条目：不重复计数
			oldValue.value = entry.value;
			entry.dead = true;
			s.size.decrementAndGet();
			return null; // 从map移除；队列节点留待遍历回收
		});
		return oldValue.value;
	}

	public boolean remove(K key, V value) {
		var s = state.get();
		var removed = new boolean[1];
		s.map.computeIfPresent(key, (k, entry) -> {
			if (entry.dead)
				return null; // 已死亡条目不重复计数
			if (!Objects.equals(entry.value, value))
				return entry;
			entry.dead = true;
			s.size.decrementAndGet();
			removed[0] = true;
			return null;
		});
		return removed[0];
	}

	public @Nullable V replace(K key, V value) {
		Objects.requireNonNull(value);
		var s = state.get();
		var oldValue = new OutObject<V>();
		s.map.computeIfPresent(key, (k, entry) -> {
			if (entry.dead)
				return null;
			oldValue.value = entry.value;
			entry.value = value;
			return entry;
		});
		return oldValue.value;
	}

	public boolean replace(K key, V oldValue, V newValue) {
		Objects.requireNonNull(newValue);
		var s = state.get();
		var replaced = new boolean[1];
		s.map.computeIfPresent(key, (k, entry) -> {
			if (entry.dead)
				return null;
			if (!Objects.equals(entry.value, oldValue))
				return entry;
			entry.value = newValue;
			replaced[0] = true;
			return entry;
		});
		return replaced[0];
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("{");
		var first = true;
		for (var entry : state.get().map.values()) {
			if (entry.dead)
				continue;
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(entry.key).append('=').append(entry.value);
		}
		return sb.append('}').toString();
	}
}
