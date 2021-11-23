package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConcurrentLruLike<K, V> {
	private static final Logger logger = LogManager.getLogger(ConcurrentLruLike.class);

	private final ConcurrentHashMap<K, ConcurrentLruItem<K, V>> DataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, ConcurrentLruItem<K, V>>> LruQueue
		= new ConcurrentLinkedQueue<> ();
	private volatile ConcurrentHashMap<K, ConcurrentLruItem<K, V>> LruHot;

	private int Capacity;
	public final int getCapacity() {
		return Capacity;
	}
	public final void setCapacity(int value) {
		Capacity = value;
	}
	private int InitialCapacity;
	public final int getInitialCapacity() {
		return InitialCapacity;
	}
	public final void setInitialCapacity(int cap) {
		InitialCapacity = cap;
	}
	private int ConcurrencyLevel;
	public final int getConcurrencyLevel() {
		return ConcurrencyLevel;
	}
	public final void setConcurrencyLevel(int c) {
		ConcurrencyLevel = c;
	}

	private long MaxLruInitialCapaicty = 100000;
	public final long getMaxLruInitialCapaicty() {
		return MaxLruInitialCapaicty;
	}
	public final void setMaxLruInitialCapaicty(long value) {
		MaxLruInitialCapaicty = value;
	}

	private long NewLruHotPeriod = 1000;
	public final long getNewLruHotPeriod() {
		return NewLruHotPeriod;
	}
	public final void setNewLruHotPeriod(long value) {
		NewLruHotPeriod = value;
	}
	private long CleanPeriod = 1000;
	public final long getCleanPeriod() {
		return CleanPeriod;
	}
	public final void setCleanPeriod(long value) {
		CleanPeriod = value;
	}

	private int CleanPeriodWhenExceedCapacity = 0;
	public final int getCleanPeriodWhenExceedCapacity() {
		return CleanPeriodWhenExceedCapacity;
	}
	public final void setCleanPeriodWhenExceedCapacity(int value) {
		CleanPeriodWhenExceedCapacity = value;
	}
	private boolean ContinueWhenTryRemoveCallbackFail = true;
	public final boolean getContinueWhenTryRemoveCallbackFail() {
		return ContinueWhenTryRemoveCallbackFail;
	}
	public final void setContinueWhenTryRemoveCallbackFail(boolean value) {
		ContinueWhenTryRemoveCallbackFail = value;
	}

	private TryRemoveHandle<K, V> TryRemoveCallback = null;
	public final TryRemoveHandle<K, V> getTryRemoveCallback() {
		return TryRemoveCallback;
	}
	public final void setTryRemoveCallback(TryRemoveHandle<K, V> value) {
		TryRemoveCallback = value;
	}

	@FunctionalInterface
	public interface TryRemoveHandle<K, V> {
		boolean tryRemove(K key, V value);
	}

	public ConcurrentLruLike(int capacity, TryRemoveHandle<K, V> tryRemove, long newLruHotPeriod, long cleanPeriod, int initialCapacity) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, initialCapacity, 1024);
	}

	public ConcurrentLruLike(int capacity, TryRemoveHandle<K, V> tryRemove, long newLruHotPeriod, long cleanPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31, 1024);
	}

	public ConcurrentLruLike(int capacity, TryRemoveHandle<K, V> tryRemove, long newLruHotPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, 2000, 31, 1024);
	}

	public ConcurrentLruLike(int capacity, TryRemoveHandle<K, V> tryRemove) {
		this(capacity, tryRemove, 200, 2000, 31, 1024);
	}

	public ConcurrentLruLike(int capacity) {
		this(capacity, null, 200, 2000, 31, 1024);
	}

	public ConcurrentLruLike(int capacity,
							 TryRemoveHandle<K, V> tryRemove,
							 long newLruHotPeriod,
							 long cleanPeriod,
							 int initialCapacity,
							 int concurrencyLevel) {
		setCapacity(capacity);
		InitialCapacity = initialCapacity;
		ConcurrencyLevel = concurrencyLevel;
		setTryRemoveCallback(tryRemove);
		setNewLruHotPeriod(newLruHotPeriod);
		setCleanPeriod(cleanPeriod);

		DataMap = new ConcurrentHashMap<>(initialCapacity, 0.75f, concurrencyLevel);
		NewLruHot();

		Task.schedule((task) -> {
			// 访问很少的时候不创建新的热点。这个选项没什么意思。
			if (LruHot.size() > GetLruInitialCapaicty() / 2) {
				NewLruHot();
			}
		}, getNewLruHotPeriod(), getNewLruHotPeriod());
		Task.schedule((thisTask) -> CleanNow(thisTask), getCleanPeriod(), -1);
	}

	public final V GetOrAdd(K k, Factory<V> factory) {
		final var isNew = new OutObject<Boolean>();
		isNew.Value = false;
		var lruItem = DataMap.computeIfAbsent(k, (k2) -> {
				V value = factory.create();
				isNew.Value = true;
				var curLruHot = LruHot;
				var lruItemNew = new ConcurrentLruItem<>(value, curLruHot);
				curLruHot.put(k, lruItemNew); // MUST replace
				return lruItemNew;
		});

		if (false == isNew.Value) {
			AdjustLru(k, lruItem);
		}
		return lruItem.Value;
	}

	private void AdjustLru(K key, ConcurrentLruItem<K, V> lruItem) {
		if (lruItem.LruNode != LruHot) {
			// compare key and value
			lruItem.LruNode.remove(key, lruItem);
			var curLruHot = LruHot;
			if (curLruHot.putIfAbsent(key, lruItem) == null) { // maybe fail
				lruItem.LruNode = curLruHot;
			}
		}
	}

	/**
	 @param key key
	 @return
	*/

	public final V get(K key) {
		return get(key, true);
	}

	public final V get(K key, boolean adjustLru) {
		var lruItem = DataMap.get(key);
		if (null != lruItem) {
			if (adjustLru) {
				AdjustLru(key, lruItem);
			}
			return lruItem.Value;
		}
		return null;
	}

	private long GetLruInitialCapaicty() {
		int lruInitialCapacity = (int)(InitialCapacity * 0.2);
		return Math.min(lruInitialCapacity, getMaxLruInitialCapaicty());
	}

	private void NewLruHot() {
		var newLru = new ConcurrentHashMap<K, ConcurrentLruItem<K, V>>(
				(int)GetLruInitialCapaicty(), 0.75f, ConcurrencyLevel);;
		LruHot = newLru;
		LruQueue.add(LruHot);
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final V remove(K key) {
		var lruItemRemoved = DataMap.remove(key);
		if (null != lruItemRemoved) {
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// 1. GetOrAdd 需要 replace 更新
			// 2. 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			lruItemRemoved.LruNode.remove(key, lruItemRemoved);
			return lruItemRemoved.Value;
		}
		return null;
	}

	public final void CleanNow(Task taskNotUsed) {
		// 这个任务的执行时间可能很长，
		// 不直接使用 Scheduler 的定时任务，
		// 每次执行完重新调度。

		if (getCapacity() <= 0) {
			Task.schedule((thisTask) -> CleanNow(thisTask), getCleanPeriod(), -1);
			return; // 容量不限
		}

		while (DataMap.size() > getCapacity()) { // 超出容量，循环尝试
			var node = LruQueue.peek();
			if (null == node || node == LruHot) { // 热点。不回收。
				break;
			}

			for (var e : node.entrySet()) {
				if (null != getTryRemoveCallback()) {
					if (TryRemoveCallback.tryRemove(e.getKey(), e.getValue().Value)) {
						continue;
					}
					if (getContinueWhenTryRemoveCallbackFail()) {
						continue;
					}
					break;
				}
				remove(e.getKey());
			}

			if (node.size() == 0) {
				LruQueue.poll();
			}
			else {
				logger.warn("remain record when clean oldest lrunode.");
			}

			int sleepms = CleanPeriodWhenExceedCapacity > 1000
					? CleanPeriodWhenExceedCapacity : 1000;
			try {
				Thread.sleep(sleepms);
			} catch (InterruptedException skip) {
				logger.error(skip);
			}
			Task.schedule(this::CleanNow, getCleanPeriod(), -1);
		}
	}
}
