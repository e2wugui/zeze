package Zeze.Util;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LongConcurrentLruLike<V> {
	private static final Logger logger = LogManager.getLogger(LongConcurrentLruLike.class);

	static final class LruItem<V> {
		final V Value;
		volatile LongConcurrentHashMap<LruItem<V>> LruNode;

		LruItem(V value, LongConcurrentHashMap<LruItem<V>> lruNode) {
			Value = value;
			LruNode = lruNode;
		}
	}

	public interface LongObjPredicate<V> {
		boolean test(long key, V value);
	}

	private final LongConcurrentHashMap<LruItem<V>> DataMap;
	private final ConcurrentLinkedQueue<LongConcurrentHashMap<LruItem<V>>> LruQueue = new ConcurrentLinkedQueue<>();
	private volatile LongConcurrentHashMap<LruItem<V>> LruHot;
	private int Capacity;
	private int LruInitialCapacity;
	private int CleanPeriod;
	private LongObjPredicate<V> TryRemoveCallback;
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

	public final LongObjPredicate<V> getTryRemoveCallback() {
		return TryRemoveCallback;
	}

	public final void setTryRemoveCallback(LongObjPredicate<V> value) {
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

	public LongConcurrentLruLike(int capacity) {
		this(capacity, null, 200, 2000, 31);
	}

	public LongConcurrentLruLike(int capacity, LongObjPredicate<V> tryRemove) {
		this(capacity, tryRemove, 200, 2000, 31);
	}

	public LongConcurrentLruLike(int capacity, LongObjPredicate<V> tryRemove, int newLruHotPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, 2000, 31);
	}

	public LongConcurrentLruLike(int capacity, LongObjPredicate<V> tryRemove, int newLruHotPeriod, int cleanPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31);
	}

	public LongConcurrentLruLike(int capacity, LongObjPredicate<V> tryRemove, int newLruHotPeriod, int cleanPeriod,
								 int initialCapacity) {
		DataMap = new LongConcurrentHashMap<>(initialCapacity);
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

	private void newLruHot() {
		var newLru = new LongConcurrentHashMap<LruItem<V>>(LruInitialCapacity);
		LruHot = newLru;
		LruQueue.add(newLru);
	}

	private void adjustLru(long key, LruItem<V> lruItem, LongConcurrentHashMap<LruItem<V>> curLruHot) {
		var itemLruNode = lruItem.LruNode;
		if (itemLruNode != curLruHot) {
			itemLruNode.remove(key, lruItem); // compare key and value
			if (curLruHot.putIfAbsent(key, lruItem) == null) // maybe fail
				lruItem.LruNode = curLruHot; // 这里可能会有潜在的并发问题,不过影响不大
		}
	}

	public final V GetOrAdd(long key, Factory<V> factory) {
		var curLruHot = LruHot;
		var lruItem = DataMap.get(key);
		if (lruItem == null) {
			V value = factory.create();
			lruItem = new LruItem<>(value, curLruHot);
			var oldLruItem = DataMap.putIfAbsent(key, lruItem);
			if (oldLruItem == null) {
				curLruHot.put(key, lruItem); // MUST replace
				return value;
			}
			lruItem = oldLruItem;
		}
		adjustLru(key, lruItem, curLruHot);
		return lruItem.Value;
	}

	public final V get(long key) {
		return get(key, true);
	}

	public final V get(long key, boolean adjustLru) {
		var lruItem = DataMap.get(key);
		if (lruItem == null)
			return null;
		if (adjustLru)
			adjustLru(key, lruItem, LruHot);
		return lruItem.Value;
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final V remove(long key) {
		var lruItemRemoved = DataMap.remove(key);
		if (lruItemRemoved == null)
			return null;
		// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
		// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
		// 1. GetOrAdd 需要 replace 更新
		// 2. 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
		lruItemRemoved.LruNode.remove(key, lruItemRemoved);
		return lruItemRemoved.Value;
	}

	public final void CleanNow() {
		try {
			int capacity = Capacity;
			if (capacity <= 0) // 容量不限
				return;
			while (DataMap.size() > capacity) { // 超出容量，循环尝试
				var node = LruQueue.peek();
				if (node == LruHot || node == null) // 热点不回收
					break;

				var tryRemoveCallback = TryRemoveCallback;
				if (tryRemoveCallback != null) {
					for (var it = node.entryIterator(); it.moveToNext(); ) {
						if (!tryRemoveCallback.test(it.key(), it.value().Value)
								&& !ContinueWhenTryRemoveCallbackFail)
							break;
					}
				} else {
					for (var it = node.keyIterator(); it.hasNext(); )
						remove(it.next());
				}

				if (node.isEmpty())
					LruQueue.poll();
				else
					logger.warn("remain record when clean oldest lruNode.");

				try {
					//noinspection BusyWait
					Thread.sleep(CleanPeriodWhenExceedCapacity);
				} catch (InterruptedException e) {
					logger.error("CleanNow Interrupted", e);
				}
			}
		} finally {
			Task.schedule(CleanPeriod, this::CleanNow);
		}
	}
}
