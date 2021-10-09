package Zeze.Util;

import Zeze.*;

public class HugeConcurrentLruLike<K, V> {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private static class LruItem {
		private V Value;
		public final V getValue() {
			return Value;
		}
		private HugeConcurrentDictionary<K, LruItem> LruNode;
		public final HugeConcurrentDictionary<K, LruItem> getLruNode() {
			return LruNode;
		}
		public final void setLruNode(HugeConcurrentDictionary<K, LruItem> value) {
			LruNode = value;
		}

		public LruItem(V value, HugeConcurrentDictionary<K, LruItem> lruNode) {
			Value = value;
			setLruNode(lruNode);
		}
	}

	private HugeConcurrentDictionary<K, LruItem> DataMap;
	private HugeConcurrentDictionary<K, LruItem> getDataMap() {
		return DataMap;
	}
	private ConcurrentQueue<Zeze.Util.HugeConcurrentDictionary<K, LruItem>> LruQueue = new ConcurrentQueue<Zeze.Util.HugeConcurrentDictionary<K, LruItem>> ();
	private ConcurrentQueue<Zeze.Util.HugeConcurrentDictionary<K, LruItem>> getLruQueue() {
		return LruQueue;
	}
	private HugeConcurrentDictionary<K, LruItem> LruHot;
	private HugeConcurrentDictionary<K, LruItem> getLruHot() {
		return LruHot;
	}
	private void setLruHot(HugeConcurrentDictionary<K, LruItem> value) {
		LruHot = value;
	}

	private long Capacity;
	public final long getCapacity() {
		return Capacity;
	}
	public final void setCapacity(long value) {
		Capacity = value;
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
	private tangible.Func2Param<K, V, Boolean> TryRemoveCallback = null;
	public final tangible.Func2Param<K, V, Boolean> getTryRemoveCallback() {
		return TryRemoveCallback;
	}
	public final void setTryRemoveCallback(tangible.Func2Param<K, V, Boolean> value) {
		TryRemoveCallback = value;
	}


	public HugeConcurrentLruLike(long capacity, Func<K, V, Boolean> tryRemove, long newLruHotPeriod, long cleanPeriod, long initialCapacity, int buckets) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, initialCapacity, buckets, 1024);
	}

	public HugeConcurrentLruLike(long capacity, Func<K, V, Boolean> tryRemove, long newLruHotPeriod, long cleanPeriod, long initialCapacity) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, initialCapacity, 16, 1024);
	}

	public HugeConcurrentLruLike(long capacity, Func<K, V, Boolean> tryRemove, long newLruHotPeriod, long cleanPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, cleanPeriod, 31, 16, 1024);
	}

	public HugeConcurrentLruLike(long capacity, Func<K, V, Boolean> tryRemove, long newLruHotPeriod) {
		this(capacity, tryRemove, newLruHotPeriod, 2000, 31, 16, 1024);
	}

	public HugeConcurrentLruLike(long capacity, Func<K, V, Boolean> tryRemove) {
		this(capacity, tryRemove, 200, 2000, 31, 16, 1024);
	}

	public HugeConcurrentLruLike(long capacity) {
		this(capacity, null, 200, 2000, 31, 16, 1024);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public HugeConcurrentLruLike(long capacity, Func<K, V, bool> tryRemove = null, long newLruHotPeriod = 200, long cleanPeriod = 2000, long initialCapacity = 31, int buckets = 16, int concurrencyLevel = 1024)
	public HugeConcurrentLruLike(long capacity, tangible.Func2Param<K, V, Boolean> tryRemove, long newLruHotPeriod, long cleanPeriod, long initialCapacity, int buckets, int concurrencyLevel) {
		setCapacity(capacity);
		setTryRemoveCallback(::tryRemove);
		setNewLruHotPeriod(newLruHotPeriod);
		setCleanPeriod(cleanPeriod);

		DataMap = new HugeConcurrentDictionary<K, LruItem>(buckets, concurrencyLevel, initialCapacity);
		NewLruHot();

		Scheduler.getInstance().Schedule((task) -> {
				// 访问很少的时候不创建新的热点。这个选项没什么意思。
				if (getLruHot().getCount() > GetLruInitialCapaicty() / 2) {
					NewLruHot();
				}
		}, getNewLruHotPeriod(), getNewLruHotPeriod());
		Util.Scheduler.getInstance().Schedule(::CleanNow, getCleanPeriod(), -1);
	}

	public final V GetOrAdd(K k, tangible.Func1Param<K, V> factory) {
		boolean isNew = false;
		var lruItem = getDataMap().GetOrAdd(k, (k) -> {
				V value = factory.invoke(k);
				isNew = true;
				var lruItem = new LruItem(value, getLruHot());
				getLruHot().set(k, lruItem); // MUST replace
				return lruItem;
		});

		if (false == isNew) {
			AdjustLru(k, lruItem);
		}
		return lruItem.Value;
	}

	private void AdjustLru(K key, LruItem lruItem) {
		if (lruItem.getLruNode() != getLruHot()) {
			// compare key and value
			lruItem.getLruNode().TryRemove(KeyValuePair.Create(key, lruItem));
			if (getLruHot().TryAdd(key, lruItem)) { // maybe fail
				lruItem.setLruNode(getLruHot());
			}
		}
	}

	/** 
	 
	 
	 @param key
	 @param value
	 @param adjustLru 是否调整lru 
	 @return 
	*/

	public final boolean TryGetValue(K key, tangible.OutObject<V> value) {
		return TryGetValue(key, value, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public bool TryGetValue(K key, out V value, bool adjustLru = true)
	public final boolean TryGetValue(K key, tangible.OutObject<V> value, boolean adjustLru) {
		V lruItem;
		tangible.OutObject<LruItem> tempOut_lruItem = new tangible.OutObject<LruItem>();
		if (getDataMap().TryGetValue(key, tempOut_lruItem)) {
		lruItem = tempOut_lruItem.outArgValue;
			if (adjustLru) {
				AdjustLru(key, lruItem);
			}
			value.outArgValue = lruItem.Value;
			return true;
		}
	else {
		lruItem = tempOut_lruItem.outArgValue;
	}
		value.outArgValue = null;
		return false;
	}

	private long GetLruInitialCapaicty() {
		long lruInitialCapacity = (long)(getDataMap().getInitialCapacity() * 0.2);
		return lruInitialCapacity < getMaxLruInitialCapaicty() ? lruInitialCapacity : getMaxLruInitialCapaicty();
	}

	private void NewLruHot() {
		setLruHot(new Zeze.Util.HugeConcurrentDictionary<K, LruItem>(getDataMap().getBucketCount(), getDataMap().getConcurrencyLevel(), GetLruInitialCapaicty()));
		getLruQueue().Enqueue(getLruHot());
	}

	// 自定义TryRemoveCallback时，需要调用这个方法真正删除。
	public final boolean TryRemove(K key, tangible.OutObject<V> value) {
		V e;
		tangible.OutObject<LruItem> tempOut_e = new tangible.OutObject<LruItem>();
		if (getDataMap().TryRemove(key, tempOut_e)) {
		e = tempOut_e.outArgValue;
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// 1. GetOrAdd 需要 replace 更新
			// 2. 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			e.LruNode.TryRemove(KeyValuePair.Create(key, e));
			value.outArgValue = e.Value;
			return true;
		}
	else {
		e = tempOut_e.outArgValue;
	}
		value.outArgValue = null;
		return false;
	}

	public final void CleanNow(SchedulerTask taskNotUsed) {
		// 这个任务的执行时间可能很长，
		// 不直接使用 Scheduler 的定时任务，
		// 每次执行完重新调度。

		if (getCapacity() <= 0) {
			Scheduler.getInstance().Schedule(::CleanNow, getCleanPeriod(), -1);
			return; // 容量不限
		}

		while (getDataMap().getCount() > getCapacity()) { // 超出容量，循环尝试
			T node;
			tangible.OutObject<Zeze.Util.HugeConcurrentDictionary<K, LruItem>> tempOut_node = new tangible.OutObject<Zeze.Util.HugeConcurrentDictionary<K, LruItem>>();
			if (false == getLruQueue().TryPeek(tempOut_node)) {
			node = tempOut_node.outArgValue;
				break;
			}
		else {
			node = tempOut_node.outArgValue;
		}

			if (node == getLruHot()) { // 热点。不回收。
				break;
			}

			for (var e : node) {
				if (null != getTryRemoveCallback()) {
					if (TryRemoveCallback(e.Key, e.Value.Value)) {
						continue;
					}
					if (getContinueWhenTryRemoveCallbackFail()) {
						continue;
					}
					break;
				}
				V _;
				tangible.OutObject<V> tempOut__ = new tangible.OutObject<V>();
				TryRemove(e.Key, tempOut__);
			_ = tempOut__.outArgValue;
			}
			if (node.Count == 0) {
				T _;
				tangible.OutObject<Zeze.Util.HugeConcurrentDictionary<K, LruItem>> tempOut__2 = new tangible.OutObject<Zeze.Util.HugeConcurrentDictionary<K, LruItem>>();
				getLruQueue().TryDequeue(tempOut__2);
			_ = tempOut__2.outArgValue;
			}
			else {
				logger.Warn(String.format("remain record when clean oldest lrunode."));
			}

			if (getCleanPeriodWhenExceedCapacity() > 0) {
				Thread.sleep(getCleanPeriodWhenExceedCapacity());
			}
		}
		Util.Scheduler.getInstance().Schedule(::CleanNow, getCleanPeriod(), -1);
	}
}