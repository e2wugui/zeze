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

public class ConcurrentLruLike<K, V> {
	private static final Logger logger = LogManager.getLogger(ConcurrentLruLike.class);
	private static final int MAX_NODE_COUNT = 8640;

	static final class LruItem<K, V> {
		private static final VarHandle LRU_NODE_HANDLE;

		static {
			try {
				LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(LruItem.class, "LruNode", ConcurrentHashMap.class);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		final V Value;
		volatile ConcurrentHashMap<K, LruItem<K, V>> LruNode;

		LruItem(V value, ConcurrentHashMap<K, LruItem<K, V>> lruNode) {
			Value = value;
			LruNode = lruNode;
		}

		@SuppressWarnings("unchecked")
		ConcurrentHashMap<K, LruItem<K, V>> getAndSetLruNodeNull() {
			return (ConcurrentHashMap<K, LruItem<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
		}

		boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, LruItem<K, V>> c) {
			return LRU_NODE_HANDLE.compareAndSet(this, c, null);
		}
	}

	private final String name;
	private final ConcurrentHashMap<K, LruItem<K, V>> DataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, LruItem<K, V>>> LruQueue = new ConcurrentLinkedQueue<>();
	private volatile ConcurrentHashMap<K, LruItem<K, V>> LruHot;
	private int Capacity;
	private int LruInitialCapacity;
	private int CleanPeriod;
	private BiPredicate<K, V> TryRemoveCallback;
	private int CleanPeriodWhenExceedCapacity = 1000;
	private boolean ContinueWhenTryRemoveCallbackFail = true;

	public final int getCapacity() {
		return Capacity;
	}

	public final void setCapacity(int value) {
		Capacity = value;
	}

	public final int getLruInitialCapacity() {
		return LruInitialCapacity;
	}

	public final void setLruInitialCapacity(int value) {
		LruInitialCapacity = value;
	}

	public final int getCleanPeriod() {
		return CleanPeriod;
	}

	public final void setCleanPeriod(int value) {
		CleanPeriod = value;
	}

	public final BiPredicate<K, V> getTryRemoveCallback() {
		return TryRemoveCallback;
	}

	public final void setTryRemoveCallback(BiPredicate<K, V> value) {
		TryRemoveCallback = value;
	}

	public final int getCleanPeriodWhenExceedCapacity() {
		return CleanPeriodWhenExceedCapacity;
	}

	public final void setCleanPeriodWhenExceedCapacity(int value) {
		CleanPeriodWhenExceedCapacity = value;
	}

	public final boolean getContinueWhenTryRemoveCallbackFail() {
		return ContinueWhenTryRemoveCallbackFail;
	}

	public final void setContinueWhenTryRemoveCallbackFail(boolean value) {
		ContinueWhenTryRemoveCallbackFail = value;
	}

	public ConcurrentLruLike(String name, int capacity) {
		this(name, capacity, null, 200, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, BiPredicate<K, V> tryRemove) {
		this(name, capacity, tryRemove, 200, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, BiPredicate<K, V> tryRemove, int newLruHotPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, BiPredicate<K, V> tryRemove, int newLruHotPeriod,
							 int cleanPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31);
	}

	public ConcurrentLruLike(String name, int capacity, BiPredicate<K, V> tryRemove, int newLruHotPeriod,
							 int cleanPeriod, int initialCapacity) {
		this.name = name;
		DataMap = new ConcurrentHashMap<>(initialCapacity);
		Capacity = capacity;
		LruInitialCapacity = Math.min(initialCapacity / 5, 100000);
		CleanPeriod = cleanPeriod;
		TryRemoveCallback = tryRemove;
		newLruHot();

		Task.schedule(newLruHotPeriod, newLruHotPeriod, () -> {
			if (LruHot.size() > LruInitialCapacity / 2) // 访问很少的时候不创建新的热点
				newLruHot();
		});
		// 下面这个任务的执行时间可能很长，不直接使用带period的schedule的定时任务，每次执行完重新调度。
		Task.schedule(CleanPeriod, this::CleanNow);
	}

	public long WalkKey(TableWalkKey<K> callback) {
		long cw = 0;
		for (var e : DataMap.entrySet()) {
			if (!callback.handle(e.getKey()))
				return cw;
			++cw;
		}
		return cw;
	}

	private void newLruHot() {
		var newLru = new ConcurrentHashMap<K, LruItem<K, V>>(LruInitialCapacity);
		LruHot = newLru;
		LruQueue.add(newLru);
	}

	private void adjustLru(K key, LruItem<K, V> lruItem, ConcurrentHashMap<K, LruItem<K, V>> curLruHot) {
		var oldNode = lruItem.getAndSetLruNodeNull();
		if (oldNode != null) {
			oldNode.remove(key);
			if (curLruHot.putIfAbsent(key, lruItem) == null)
				lruItem.LruNode = curLruHot;
		}
	}

	public final V GetOrAdd(K key, Factory<V> factory) {
		var lruHot = LruHot;
		var lruItem = DataMap.get(key);
		if (lruItem == null) { // slow-path
			lruItem = DataMap.computeIfAbsent(key, k -> {
				var item = new LruItem<>(factory.create(), lruHot);
				lruHot.put(key, item); // MUST replace
				return item;
			});
		}
		if (lruItem.LruNode != lruHot)
			adjustLru(key, lruItem, lruHot);
		return lruItem.Value;
	}

	public final V get(K key) {
		return get(key, true);
	}

	public final V get(K key, boolean adjustLru) {
		var lruItem = DataMap.get(key);
		if (lruItem == null)
			return null;
		if (adjustLru) {
			var lruHot = LruHot;
			if (lruItem.LruNode != lruHot)
				adjustLru(key, lruItem, lruHot);
		}
		return lruItem.Value;
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final V remove(K key) {
		var lruItemRemoved = DataMap.remove(key);
		if (lruItemRemoved == null)
			return null;
		// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
		// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
		// 1. GetOrAdd 需要 replace 更新
		// 2. 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
		var node = lruItemRemoved.LruNode;
		if (node != null)
			node.remove(key, lruItemRemoved);
		return lruItemRemoved.Value;
	}

	private void TryPollLruQueue() {
		var cap = LruQueue.size() - MAX_NODE_COUNT;
		if (cap <= 0)
			return;

		var timeBegin = System.nanoTime();
		int recordCount = 0, nodeCount = 0;
		var polls = new ArrayList<ConcurrentHashMap<K, LruItem<K, V>>>(cap);
		while (LruQueue.size() > MAX_NODE_COUNT) {
			// 大概，删除超过一天的节点。
			var node = LruQueue.poll();
			if (null == node)
				break;
			polls.add(node);
			nodeCount++;
		}

		// 把被删除掉的node里面的记录迁移到当前最老(head)的node里面。
		var head = LruQueue.peek();
		if (null == head)
			throw new RuntimeException("Impossible!");
		for (var poll : polls) {
			for (var e : poll.entrySet()) {
				// concurrent see adjustLru
				var r = e.getValue();
				if (r.compareAndSetLruNodeNull(poll) && head.putIfAbsent(e.getKey(), r) == null) { // 并发访问导致这个记录已经被迁移走。
					r.LruNode = head;
					recordCount++;
				}
			}
		}
		logger.info("{}: shrank {} nodes, moved {} records, {} ms, result: {}/{}", name,
				nodeCount, recordCount, (System.nanoTime() - timeBegin) / 1_000_000, LruQueue.size(), MAX_NODE_COUNT);
	}

	private void CleanNow() {
		try {
			int capacity = Capacity;
			if (capacity > 0) {
				var timeBegin = System.nanoTime();
				int recordCount = 0, nodeCount = 0;
				while (DataMap.size() > capacity) { // 超出容量，循环尝试
					var node = LruQueue.peek();
					if (node == LruHot || node == null) // 热点不回收
						break;

					var tryRemoveCallback = TryRemoveCallback;
					if (tryRemoveCallback != null) {
						for (var e : node.entrySet()) {
							if (!tryRemoveCallback.test(e.getKey(), e.getValue().Value)
									&& !ContinueWhenTryRemoveCallbackFail)
								break;
							recordCount++;
						}
					} else {
						recordCount += node.size();
						for (var k : node.keySet())
							remove(k);
					}

					if (node.isEmpty()) {
						LruQueue.poll();
						nodeCount++;
					} else {
						logger.warn("remain record when clean oldest lruNode.");
						try {
							//noinspection BusyWait
							Thread.sleep(CleanPeriodWhenExceedCapacity);
						} catch (InterruptedException e) {
							logger.error("CleanNow Interrupted", e);
						}
					}
				}
				if (recordCount > 0 || nodeCount > 0) {
					logger.info("{}: cleaned {} records, {} nodes, {} ms, result: {}/{}", name, recordCount, nodeCount,
							(System.nanoTime() - timeBegin) / 1_000_000, DataMap.size(), capacity);
				}
			}
			TryPollLruQueue();
		} finally {
			Task.schedule(CleanPeriod, this::CleanNow);
		}
	}
}
