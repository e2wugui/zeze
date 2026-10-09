package Zeze.Util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
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

	/**
	 * queue/map/size 三组件的整体快照：clear 以原子替换 state 完成，三组件间永不出中间态。
	 * clear 三步分离（queue.clear/map.clear/size.set(0)）与并发 put 交错时 increment 会被
	 * set(0) 永久覆盖——map 有条目而 size()==0/isEmpty()，后续 remove 再减成负数。整体替换后，
	 * clear 竞态期间迟到 put 的增量落在废弃 state 上（对齐 CHM 自身 clear 的弱一致语义：并发更新
	 * 下 clear 与 put 的胜负本就未定义），新 state 的计数与内容恒一致。
	 */
	private static final class State<K, V> {
		final @NotNull ConcurrentHashMap<K, Entry<K, V>> map;
		final @NotNull ConcurrentLinkedQueue<Entry<K, V>> queue = new ConcurrentLinkedQueue<>();
		final @NotNull AtomicInteger size = new AtomicInteger();

		State(int initialCapacity) {
			map = new ConcurrentHashMap<>(initialCapacity);
		}
	}

	/**
	 * 队列节点持有带身份的条目（key+value+dead），与map的发布解耦：
	 * put在compute内先入队后发布到map，清理者据map.get(key)==null无法区分
	 * "未发布"与"已删除"（裸key共用null哨兵时会把发布窗口内的key当已删除永久摘出队列，
	 * map有条目而遍历永久漏项）。以条目自身状态判定：只有dead条目（remove路径在map的
	 * bin锁内置位）可被摘出队列；未发布的条目保持在线，遍历弱一致地提前可见或留待下轮。
	 * 同key删除重插产生新代条目与新的队列节点，旧节点按自身dead身份摘除，不会误杀新一代。
	 */
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

	private final @NotNull AtomicReference<State<K, V>> state;

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

	public boolean containsKey(@NotNull K key) {
		return get(key) != null;
	}

	public boolean containsValue(@NotNull V value) {
		for (var entry : state.get().map.values())
			if (!entry.dead && Objects.equals(entry.value, value))
				return true;
		return false;
	}

	public void clear() {
		state.set(new State<>(16)); // 单次volatile写：读者要么看到全新整体，要么全旧
	}

	public class OrderedIterator implements Iterator<V> {
		private final @NotNull State<K, V> snapshot = state.get(); // 迭代期间固定在一个整体快照上
		private final @NotNull Iterator<Entry<K, V>> queueIt = snapshot.queue.iterator();
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
			var entry = it.next();
			if (entry.dead) {
				it.remove(); // 回收已删除项
				continue;
			}
			consumer.accept(entry.key, entry.value);
		}
	}

	public @Nullable V put(@NotNull K key, @NotNull V value) {
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

	public @Nullable V putIfAbsent(@NotNull K key, @NotNull V value) {
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

	public @Nullable V get(@NotNull K key) {
		var entry = state.get().map.get(key);
		return entry != null && !entry.dead ? entry.value : null;
	}

	public V getOrDefault(@NotNull K key, V defaultValue) {
		var v = get(key);
		return v != null ? v : defaultValue;
	}

	public @Nullable V remove(@NotNull K key) {
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

	public boolean remove(@NotNull K key, @NotNull V value) {
		var s = state.get();
		var removed = new boolean[1];
		s.map.computeIfPresent(key, (k, entry) -> {
			if (entry.dead)
				return null; // 防御：死亡条目不该残留map，见Entry注释
			if (!Objects.equals(entry.value, value))
				return entry;
			entry.dead = true;
			s.size.decrementAndGet();
			removed[0] = true;
			return null;
		});
		return removed[0];
	}

	public @Nullable V replace(@NotNull K key, @NotNull V value) {
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

	public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
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
	public @NotNull String toString() {
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
