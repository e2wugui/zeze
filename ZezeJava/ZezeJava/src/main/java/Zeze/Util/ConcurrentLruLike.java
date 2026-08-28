package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import Zeze.Transaction.TableWalkKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConcurrentLruLike<K, V> {
	private static final @NotNull Logger logger = LogManager.getLogger(ConcurrentLruLike.class);
	private static final int MAX_NODE_COUNT = 8640; // 最大的LRU节点数量,超过时会触发shrink
	private static final int SHRINK_NODE_COUNT = 8000; // shrink的目标节点数量

	static final class LruItem<K, V> {
		private static final @NotNull VarHandle LRU_NODE_HANDLE;

		static {
			try {
				LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(LruItem.class, "lruNode", ConcurrentHashMap.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		final @NotNull V value;
		volatile @NotNull ConcurrentHashMap<K, LruItem<K, V>> lruNode;

		LruItem(@NotNull V value, @NotNull ConcurrentHashMap<K, LruItem<K, V>> lruNode) {
			this.value = value;
			this.lruNode = lruNode;
		}

		@SuppressWarnings("unchecked")
		ConcurrentHashMap<K, LruItem<K, V>> getAndSetLruNodeNull() {
			return (ConcurrentHashMap<K, LruItem<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
		}

		boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, LruItem<K, V>> c) {
			return LRU_NODE_HANDLE.compareAndSet(this, c, null);
		}
	}

	private final @NotNull String name;
	private final @NotNull ConcurrentHashMap<K, LruItem<K, V>> dataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, LruItem<K, V>>> lruQueue = new ConcurrentLinkedQueue<>();
	private volatile @NotNull ConcurrentHashMap<K, LruItem<K, V>> lruHot;
	private int capacity;
	private int lruInitialCapacity;
	private int cleanPeriod;
	private @Nullable BiPredicate<K, V> tryRemoveCallback;
	private boolean continueWhenTryRemoveCallbackFail = true;

	public final int getCapacity() {
		return capacity;
	}

	public final void setCapacity(int value) {
		capacity = value;
	}

	public final int getLruInitialCapacity() {
		return lruInitialCapacity;
	}

	public final void setLruInitialCapacity(int value) {
		lruInitialCapacity = value;
	}

	public final int getCleanPeriod() {
		return cleanPeriod;
	}

	public final void setCleanPeriod(int value) {
		cleanPeriod = value;
	}

	public final @Nullable BiPredicate<K, V> getTryRemoveCallback() {
		return tryRemoveCallback;
	}

	public final void setTryRemoveCallback(@Nullable BiPredicate<K, V> value) {
		tryRemoveCallback = value;
	}

	public final boolean getContinueWhenTryRemoveCallbackFail() {
		return continueWhenTryRemoveCallbackFail;
	}

	public final void setContinueWhenTryRemoveCallbackFail(boolean value) {
		continueWhenTryRemoveCallbackFail = value;
	}

	public ConcurrentLruLike(@NotNull String name, int capacity) {
		this(name, capacity, null, 200, 2000, 31);
	}

	public ConcurrentLruLike(@NotNull String name, int capacity, @Nullable BiPredicate<K, V> tryRemove) {
		this(name, capacity, tryRemove, 200, 2000, 31);
	}

	public ConcurrentLruLike(@NotNull String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, 2000, 31);
	}

	public ConcurrentLruLike(@NotNull String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod, int cleanPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31);
	}

	public ConcurrentLruLike(@NotNull String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod, int cleanPeriod, int initialCapacity) {
		this.name = name;
		dataMap = new ConcurrentHashMap<>(initialCapacity);
		this.capacity = capacity;
		lruInitialCapacity = Math.min(initialCapacity / 5, 100000);
		this.cleanPeriod = cleanPeriod;
		tryRemoveCallback = tryRemove;
		newLruHot();

		TaskSpec.ofAction(() -> {
			if (lruHot.size() > lruInitialCapacity / 2) // 访问很少的时候不创建新的热点
				newLruHot();
		}).schedule(newLruHotPeriod, newLruHotPeriod);
		// 下面这个清理任务的执行时间可能很长；schedule(delay, period) 是固定延迟调度(scheduleWithFixedDelay)，
		// 本次执行完才开始计时下一次，执行时间长只会推迟后续执行，不会并发重入或堆积。
		TaskSpec.ofAction(this::cleanNow).schedule(this.cleanPeriod, this.cleanPeriod);
	}

	public long walkKey(@NotNull TableWalkKey<K> callback) throws Exception {
		long cw = 0;
		for (var k : dataMap.keySet()) {
			if (!callback.handle(k))
				return callback.endWalk(cw);
			++cw;
		}
		return callback.endWalk(cw);
	}

	private void newLruHot() {
		var newLru = new ConcurrentHashMap<K, LruItem<K, V>>(lruInitialCapacity);
		lruHot = newLru;
		lruQueue.add(newLru);
	}

	private void adjustLru(@NotNull K key, @NotNull LruItem<K, V> lruItem,
						   @NotNull ConcurrentHashMap<K, LruItem<K, V>> curLruHot) {
		var oldNode = lruItem.getAndSetLruNodeNull();
		if (oldNode != null) {
			oldNode.remove(key);
			if (curLruHot.putIfAbsent(key, lruItem) == null)
				lruItem.lruNode = curLruHot;
		}
	}

	public final @NotNull V getOrAdd(@NotNull K key, @NotNull Factory<V> factory) {
		var lruHot = this.lruHot;
		var lruItem = dataMap.get(key);
		if (lruItem == null) { // slow-path
			lruItem = dataMap.computeIfAbsent(key, k -> {
				var item = new LruItem<>(factory.create(), lruHot);
				lruHot.put(k, item); // MUST replace
				return item;
			});
		}
		if (lruItem.lruNode != lruHot)
			adjustLru(key, lruItem, lruHot);
		return lruItem.value;
	}

	public final @Nullable V get(@NotNull K key) {
		return get(key, true);
	}

	public final @Nullable V get(@NotNull K key, boolean adjustLru) {
		var lruItem = dataMap.get(key);
		if (lruItem == null)
			return null;
		if (adjustLru) {
			var lruHot = this.lruHot;
			if (lruItem.lruNode != lruHot)
				adjustLru(key, lruItem, lruHot);
		}
		return lruItem.value;
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final @Nullable V remove(@NotNull K key) {
		var lruItemRemoved = dataMap.remove(key);
		if (lruItemRemoved == null)
			return null;
		// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
		// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
		// 1. GetOrAdd 需要 replace 更新
		// 2. 必须使用 Pair，有可能 LruNode 里面已经有新建的记录了。
		removeLruRecord(key, lruItemRemoved);
		return lruItemRemoved.value;
	}

	// 仅当key当前映射的值与expectedValue是同一个对象(引用相等)时才删除，返回是否删除成功。
	// 用于并发场景避免误删同key的新映射(如TaskOneByOneByKeyLru淘汰残留旧队列时，同名key可能已建有活跃的新队列)。
	public final boolean remove(@NotNull K key, @NotNull V expectedValue) {
		var item = dataMap.get(key);
		if (item == null || item.value != expectedValue)
			return false;
		if (!dataMap.remove(key, item)) // 原子删除，失败说明映射已被并发改变
			return false;
		removeLruRecord(key, item);
		return true;
	}

	private void removeLruRecord(@NotNull K key, @NotNull LruItem<K, V> item) {
		var node = item.lruNode;
		//noinspection ConstantValue
		if (node != null)
			node.remove(key, item);
	}

	private void tryPollLruQueue() {
		if (lruQueue.size() <= MAX_NODE_COUNT)
			return;

		var timeBegin = System.nanoTime();
		int recordCount = 0, nodeCount = 0;
		var polls = new ArrayList<ConcurrentHashMap<K, LruItem<K, V>>>(lruQueue.size() - SHRINK_NODE_COUNT);
		while (lruQueue.size() > SHRINK_NODE_COUNT) {
			// 大概，删除超过一天的节点。
			var node = lruQueue.poll();
			if (node == null)
				break;
			polls.add(node);
			nodeCount++;
		}

		// 把被删除掉的node里面的记录迁移到当前最老(head)的node里面。
		var head = lruQueue.peek();
		assert head != null;
		for (var poll : polls) {
			for (var e : poll.entrySet()) {
				// concurrent see adjustLru
				var r = e.getValue();
				if (r.compareAndSetLruNodeNull(poll) && head.putIfAbsent(e.getKey(), r) == null) { // 并发访问导致这个记录已经被迁移走。
					r.lruNode = head;
					recordCount++;
				}
			}
		}
		logger.info("{}: shrank {} nodes, moved {} records, {} ms, result: {}/{}", name,
				nodeCount, recordCount, (System.nanoTime() - timeBegin) / 1_000_000, lruQueue.size(), MAX_NODE_COUNT);
	}

	private void cleanNow() {
		int capacity = this.capacity;
		if (capacity > 0) {
			var timeBegin = System.nanoTime();
			int recordCount = 0, nodeCount = 0;
			// 从最老到最新逐个node尝试驱逐。不对最老node忙等：回调失败（如队列忙）时继续尝试下一个node，
			// 遍历完仍超容量的等下一次周期调度重试（scheduleWithFixedDelay本身就是重试机制）。
			for (var node : lruQueue) {
				if (dataMap.size() <= capacity)
					break;
				if (node == lruHot) // 热点不回收
					break;

				var tryRemoveCallback = this.tryRemoveCallback;
				if (tryRemoveCallback != null) {
					var strictFail = false;
					for (var e : node.entrySet()) {
						var removed = tryRemoveCallback.test(e.getKey(), e.getValue().value);
						if (!removed && !continueWhenTryRemoveCallbackFail) {
							strictFail = true;
							break;
						}
						if (removed)
							recordCount++; // 只统计成功删除的，回调失败(队列忙)时不计数。
					}
					if (strictFail)
						break; // 严格模式：任何回调失败即终止本轮清理。
				} else {
					recordCount += node.size();
					for (var k : node.keySet())
						remove(k);
				}

				if (node.isEmpty()) {
					lruQueue.remove(node);
					nodeCount++;
				} else {
					logger.warn("remain record when clean lruNode.");
				}
			}
			if (recordCount > 0 || nodeCount > 0) {
				logger.info("{}: cleaned {} records, {} nodes, {} ms, result: {}/{}", name, recordCount, nodeCount,
						(System.nanoTime() - timeBegin) / 1_000_000, dataMap.size(), capacity);
			}
		}
		tryPollLruQueue();
	}
}
