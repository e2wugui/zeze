package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import Zeze.Transaction.TableWalkKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 分段式并发近似 LRU 缓存：按周期轮转热点表入队，后台定期按容量驱逐并收缩节点队列。
 */
public class ConcurrentLruLike<K, V> {
	private static final Logger logger = LogManager.getLogger(ConcurrentLruLike.class);
	private static final int MAX_NODE_COUNT = 8640; // 最大的LRU节点数量,超过时会触发shrink
	private static final int SHRINK_NODE_COUNT = 8000; // shrink的目标节点数量

	static final class LruItem<K, V> {
		private static final VarHandle LRU_NODE_HANDLE;

		static {
			try {
				LRU_NODE_HANDLE = MethodHandles.lookup().findVarHandle(LruItem.class, "lruNode", ConcurrentHashMap.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		final V value;
		volatile @Nullable ConcurrentHashMap<K, LruItem<K, V>> lruNode;

		LruItem(V value, @Nullable ConcurrentHashMap<K, LruItem<K, V>> lruNode) {
			this.value = value;
			this.lruNode = lruNode;
		}

		@SuppressWarnings("unchecked")
		@Nullable ConcurrentHashMap<K, LruItem<K, V>> getAndSetLruNodeNull() {
			return (ConcurrentHashMap<K, LruItem<K, V>>)LRU_NODE_HANDLE.getAndSet(this, null);
		}

		boolean compareAndSetLruNodeNull(ConcurrentHashMap<K, LruItem<K, V>> c) {
			return LRU_NODE_HANDLE.compareAndSet(this, c, null);
		}
	}

	private final String name;
	private final ConcurrentHashMap<K, LruItem<K, V>> dataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, LruItem<K, V>>> lruQueue = new ConcurrentLinkedQueue<>();
	// 只保护节点登记、轮转和退役；factory、驱逐回调与 dataMap 写操作均在锁外。
	private final Object lruGate = new Object();
	private volatile ConcurrentHashMap<K, LruItem<K, V>> lruHot;
	private final TimerFuture<?> newLruHotTimer;
	private final TimerFuture<?> cleanTimer;
	private volatile int capacity;
	private volatile int lruInitialCapacity;
	private int cleanPeriod;
	private volatile @Nullable BiPredicate<K, V> tryRemoveCallback;
	private volatile boolean continueWhenTryRemoveCallbackFail = true;

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

	public ConcurrentLruLike(String name, int capacity) {
		this(name, capacity, null, 200, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, @Nullable BiPredicate<K, V> tryRemove) {
		this(name, capacity, tryRemove, 200, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, 2000, 31);
	}

	public ConcurrentLruLike(String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod, int cleanPeriod) {
		this(name, capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31);
	}

	public ConcurrentLruLike(String name, int capacity, @Nullable BiPredicate<K, V> tryRemove,
							 int newLruHotPeriod, int cleanPeriod, int initialCapacity) {
		if (newLruHotPeriod <= 0 || cleanPeriod <= 0)
			throw new IllegalArgumentException("LRU periods must be > 0");
		this.name = name;
		dataMap = new ConcurrentHashMap<>(initialCapacity);
		this.capacity = capacity;
		lruInitialCapacity = Math.min(initialCapacity / 5, 100000);
		this.cleanPeriod = cleanPeriod;
		tryRemoveCallback = tryRemove;
		newLruHot();

		// 周期任务随构造即刻注册（不依赖事务提交），并在 close() 中取消
		newLruHotTimer = TaskSpec.ofAction(() -> {
			// 访问稀疏且未超容时不轮转；超容且热点非空时强制轮转以触发驱逐
			int cap = capacity;
			if (lruHot.size() > lruInitialCapacity / 2 || (cap > 0 && dataMap.size() > cap && !lruHot.isEmpty()))
				newLruHot();
		}).schedulePeriodNow(newLruHotPeriod, newLruHotPeriod);
		try {
			cleanTimer = TaskSpec.ofAction(this::cleanNow).schedulePeriodNow(this.cleanPeriod, this.cleanPeriod);
		} catch (Throwable failure) {
			newLruHotTimer.cancel(false);
			throw failure;
		}
	}

	/**
	 * 取消构造时注册的周期任务以释放强引用。调用后实例不再执行热点轮转与过期清理，不应继续使用。
	 */
	public void close() {
		newLruHotTimer.cancel(false);
		cleanTimer.cancel(false);
	}

	public long walkKey(TableWalkKey<K> callback) throws Exception {
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
		synchronized (lruGate) {
			lruQueue.add(newLru);
			lruHot = newLru;
		}
	}

	private void adjustLru(K key, LruItem<K, V> lruItem) {
		synchronized (lruGate) {
			var oldNode = lruItem.getAndSetLruNodeNull();
			if (oldNode != null)
				oldNode.remove(key, lruItem);
			if (dataMap.get(key) != lruItem)
				return;
			// 登记时才取得热点，和轮转/退役互斥；新映射的登记也持此锁。
			var currentHot = lruHot;
			currentHot.put(key, lruItem);
			lruItem.lruNode = currentHot;
			if (dataMap.get(key) != lruItem) {
				currentHot.remove(key, lruItem);
				lruItem.lruNode = null;
			}
		}
	}

	/** 获取或创建缓存项；{@code factory} 须非阻塞且不得返回 null。 */
	public final V getOrAdd(K key, Factory<V> factory) {
		var lruItem = dataMap.get(key);
		if (lruItem == null) { // slow-path
			lruItem = dataMap.computeIfAbsent(key, k -> {
				return new LruItem<>(Objects.requireNonNull(factory.create()), null);
			});
		}
		if (lruItem.lruNode != this.lruHot)
			adjustLru(key, lruItem);
		return lruItem.value;
	}

	public final @Nullable V get(K key) {
		return get(key, true);
	}

	public final @Nullable V get(K key, boolean adjustLru) {
		var lruItem = dataMap.get(key);
		if (lruItem == null)
			return null;
		if (adjustLru) {
			var lruHot = this.lruHot;
			if (lruItem.lruNode != lruHot)
				adjustLru(key, lruItem);
		}
		return lruItem.value;
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final @Nullable V remove(K key) {
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

	/** 仅当当前映射值与 expectedValue 引用相同时删除，避免误删并发重建的新映射。 */
	public final boolean remove(K key, V expectedValue) {
		var item = dataMap.get(key);
		if (item == null || item.value != expectedValue)
			return false;
		if (!dataMap.remove(key, item))
			return false;
		removeLruRecord(key, item);
		return true;
	}

	private void removeLruRecord(K key, LruItem<K, V> item) {
		synchronized (lruGate) {
			var node = item.getAndSetLruNodeNull();
			if (node != null)
				node.remove(key, item);
		}
	}

	@SuppressWarnings("ConstantConditions")
	private void tryPollLruQueue() {
		synchronized (lruGate) {
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
					if (!r.compareAndSetLruNodeNull(poll))
						continue; // 并发访问导致这个记录已经被迁移走。
					var key = e.getKey();
					// 仅迁移仍是 dataMap 活映射的条目，避免滞留死条目
					while (dataMap.get(key) == r) {
						var prev = head.putIfAbsent(key, r);
						if (prev == null) {
							r.lruNode = head;
							if (dataMap.get(key) == r)
								recordCount++;
							else
								head.remove(key, r); // 登记过程中被并发 remove，回滚
							break;
						}
						if (dataMap.get(key) == prev)
							break;
						head.remove(key, prev);
					}
				}
			}
			logger.info("{}: shrank {} nodes, moved {} records, {} ms, result: {}/{}", name,
				nodeCount, recordCount, (System.nanoTime() - timeBegin) / 1_000_000, lruQueue.size(), MAX_NODE_COUNT);
		}
	}

	private void cleanNow() {
		int capacity = this.capacity;
		if (capacity > 0) {
			var timeBegin = System.nanoTime();
			int recordCount = 0, nodeCount = 0;
			int remainNodeCount = 0, remainRecordCount = 0;
			// 从最老节点起逐个驱逐，回调失败时继续尝试后续节点
			for (var node : lruQueue) {
				if (dataMap.size() <= capacity)
					break;
				if (node == lruHot) // 热点不回收
					break;

				var tryRemoveCallback = this.tryRemoveCallback;
				if (tryRemoveCallback != null) {
					var strictFail = false;
					for (var e : node.entrySet()) {
						if (dataMap.get(e.getKey()) != e.getValue()) {
							// 清理已过期的节点残留，不触发回调
							node.remove(e.getKey(), e.getValue());
							continue;
						}
						var removed = tryRemoveCallback.test(e.getKey(), e.getValue().value);
						if (!removed && !continueWhenTryRemoveCallbackFail) {
							strictFail = true;
							break;
						}
						if (removed)
							recordCount++;
					}
					if (strictFail)
						break;
				} else {
					for (var e : node.entrySet()) {
						var k = e.getKey();
						if (dataMap.get(k) != e.getValue()) {
							node.remove(k, e.getValue());
						} else if (dataMap.remove(k, e.getValue())) {
							removeLruRecord(k, e.getValue());
							recordCount++;
						}
					}
				}

				synchronized (lruGate) {
					if (node.isEmpty() && node != lruHot) {
						lruQueue.removeIf(candidate -> candidate == node);
						nodeCount++;
					} else {
						remainNodeCount++;
						remainRecordCount += node.size();
					}
				}
			}
			if (remainNodeCount > 0)
				logger.warn("{}: remain {} records in {} nodes when clean lruNode"
								+ " (in-use protected or callback refused).",
						name, remainRecordCount, remainNodeCount);
			if (recordCount > 0 || nodeCount > 0) {
				logger.info("{}: cleaned {} records, {} nodes, {} ms, result: {}/{}", name, recordCount, nodeCount,
						(System.nanoTime() - timeBegin) / 1_000_000, dataMap.size(), capacity);
			}
		}
		tryPollLruQueue();
	}
}
