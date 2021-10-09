package Zeze.Transaction;

import Zeze.Services.*;
import Zeze.*;
import java.util.*;

// MESI？

/** 
 ConcurrentLruLike
 普通Lru一般把最新访问的放在列表一端，这直接导致并发上不去。
 基本思路是按块（用ConcurrentDictionary）保存最近访问。
 定时添加新块。
 访问需要访问1 ~3次ConcurrentDictionary。
 
 通用类的写法需要在V外面包装一层。这里直接使用Record来达到这个目的。
 这样，这个类就不通用了。通用类需要包装，多创建一个对象，还需要包装接口。
 
 
 <typeparam name="K"></typeparam>
 <typeparam name="V"></typeparam>
*/
//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public class TableCache<K, V> where V : Bean, new()
public class TableCache<K, V extends Bean> {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private Util.HugeConcurrentDictionary<K, Record<K, V>> DataMap;
	public final Util.HugeConcurrentDictionary<K, Record<K, V>> getDataMap() {
		return DataMap;
	}

	private ConcurrentQueue<Util.HugeConcurrentDictionary<K, Record<K, V>>> LruQueue = new ConcurrentQueue<Util.HugeConcurrentDictionary<K, Record<K, V>>> ();
	private ConcurrentQueue<Util.HugeConcurrentDictionary<K, Record<K, V>>> getLruQueue() {
		return LruQueue;
	}

	private Util.HugeConcurrentDictionary<K, Record<K, V>> LruHot;
	private Util.HugeConcurrentDictionary<K, Record<K, V>> getLruHot() {
		return LruHot;
	}
	private void setLruHot(Util.HugeConcurrentDictionary<K, Record<K, V>> value) {
		LruHot = value;
	}

	private Table<K, V> Table;
	public final Table<K, V> getTable() {
		return Table;
	}

	public TableCache(Application app, Table<K, V> table) {
		this.Table = table;
		DataMap = new Util.HugeConcurrentDictionary<K, Record<K, V>>(GetCacheBuckets(), GetCacheConcurrencyLevel(), GetCacheInitialCapaicty());
		NewLruHot();
		Util.Scheduler.getInstance().Schedule((task) -> {
				// 访问很少的时候不创建新的热点。这个选项没什么意思。
				if (getLruHot().getCount() > table.TableConf.getCacheNewAccessHotThreshold()) {
					NewLruHot();
				}
		}, table.TableConf.getCacheNewLruHotPeriod(), table.TableConf.getCacheNewLruHotPeriod());
		Util.Scheduler.getInstance().Schedule(::CleanNow, getTable().TableConf.getCacheCleanPeriod(), -1);
	}

	private int GetCacheBuckets() {
		return getTable().TableConf.getCacheBuckets() < 16 ? 16 : getTable().TableConf.getCacheBuckets();
	}

	private int GetCacheConcurrencyLevel() {
		// 这样写，当配置修改，可以使用的时候马上生效。
		return getTable().TableConf.getCacheConcurrencyLevel() > Environment.ProcessorCount ? getTable().TableConf.getCacheConcurrencyLevel() : Environment.ProcessorCount;
	}

	private long GetCacheInitialCapaicty() {
		// 31 from c# document
		// 这样写，当配置修改，可以使用的时候马上生效。
		return getTable().TableConf.getCacheInitialCapaicty() < 31 ? 31 : getTable().TableConf.getCacheInitialCapaicty();
	}

	private long GetLruInitialCapaicty() {
		long c = (long)(GetCacheInitialCapaicty() * 0.2);
		return c < getTable().TableConf.getCacheMaxLruInitialCapaicty() ? c : getTable().TableConf.getCacheMaxLruInitialCapaicty();
	}

	private void NewLruHot() {
		setLruHot(new Util.HugeConcurrentDictionary<K, Record<K, V>>(GetCacheBuckets(), GetCacheConcurrencyLevel(), GetLruInitialCapaicty()));
		getLruQueue().Enqueue(getLruHot());
	}

	public final Record<K, V> GetOrAdd(K key, tangible.Func1Param<K, Record<K, V>> valueFactory) {
		boolean isNew = false;
		Record<K, V> result = getDataMap().GetOrAdd(key, (k) -> {
					var r = valueFactory.invoke(k);
					getLruHot().set(k, r); // replace: add or update see this.Remove
					r.LruNode = getLruHot();
					isNew = true;
					return r;
		});

		if (false == isNew && result.getLruNode() != getLruHot()) {
			result.getLruNode().TryRemove(KeyValuePair.Create(key, result));
			if (getLruHot().TryAdd(key, result)) {
				result.setLruNode(getLruHot());
			}
			// else maybe fail in concurrent access.
			// 并发访问导致重复的TryAdd，这里先这样写吧。可能会快点。
			// LruHot[key] = result;
			// result.LruNode = LruHot;
		}
		return result;
	}

	/** 
	 内部特殊使用，不调整 Lru。
	 
	 @param key
	 @return 
	*/
	public final Record<K, V> Get(K key) {
		V r;
		tangible.OutObject<Record<K, V>> tempOut_r = new tangible.OutObject<Record<K, V>>();
		if (getDataMap().TryGetValue(key, tempOut_r)) {
		r = tempOut_r.outArgValue;
			return r;
		}
	else {
		r = tempOut_r.outArgValue;
	}
		return null;
	}

	// 不再提供删除，由 Cleaner 集中清理。
	// under lockey.writelock
	/*
	internal void Remove(K key)
	{
	    map.Remove(key, out var _);
	}
	*/

	public final void CleanNow(Zeze.Util.SchedulerTask ThisTask) {
		// 这个任务的执行时间可能很长，
		// 不直接使用 Scheduler 的定时任务，
		// 每次执行完重新调度。

		if (getTable().TableConf.getCacheCapacity() <= 0) {
			Util.Scheduler.getInstance().Schedule(::CleanNow, getTable().TableConf.getCacheCleanPeriod(), -1);
			return; // 容量不限
		}

		while (getDataMap().getCount() > getTable().TableConf.getCacheCapacity()) { // 超出容量，循环尝试
			T node;
			tangible.OutObject<Util.HugeConcurrentDictionary<K, Record<K, V>>> tempOut_node = new tangible.OutObject<Util.HugeConcurrentDictionary<K, Record<K, V>>>();
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
				if (false == TryRemoveRecord(e)) {
					// 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
					getTable().getZeze().CheckpointRun();
				}
			}
			if (node.Count == 0) {
				T _;
				tangible.OutObject<Util.HugeConcurrentDictionary<K, Record<K, V>>> tempOut__ = new tangible.OutObject<Util.HugeConcurrentDictionary<K, Record<K, V>>>();
				getLruQueue().TryDequeue(tempOut__);
			_ = tempOut__.outArgValue;
			}
			else {
				logger.Warn(String.format("remain record when clean oldest lrunode."));
			}

			if (getTable().TableConf.getCacheCleanPeriodWhenExceedCapacity() > 0) {
				Thread.sleep(getTable().TableConf.getCacheCleanPeriodWhenExceedCapacity());
			}
		}
		Util.Scheduler.getInstance().Schedule(::CleanNow, getTable().TableConf.getCacheCleanPeriod(), -1);
	}

	// under lockey.writelock
	private boolean Remove(Map.Entry<K, Record<K, V>> p) {
		V e;
		tangible.OutObject<Record<K, V>> tempOut_e = new tangible.OutObject<Record<K, V>>();
		if (getDataMap().TryRemove(p.getKey(), tempOut_e)) {
		e = tempOut_e.outArgValue;
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// see GetOrAdd
			p.getValue().State = GlobalCacheManager.StateRemoved;
			// 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			e.LruNode.TryRemove(p);
			return true;
		}
	else {
		e = tempOut_e.outArgValue;
	}
		return false;
	}

	private boolean TryRemoveRecord(Map.Entry<K, Record<K, V>> p) {
		TableKey tkey = new TableKey(this.getTable().getId(), p.getKey());
		Lockey lockey = Locks.getInstance().Get(tkey);
		if (false == lockey.TryEnterWriteLock(0)) {
			return false;
		}
		try {
			var storage = getTable().getTStorage();
			if (null == storage) {
				/* 不支持内存表cache同步。
				if (p.Value.Acquire(GlobalCacheManager.StateInvalid) != GlobalCacheManager.StateInvalid)
				    return false;
				*/
				return Remove(p);
			}

			// 这个变量的修改操作在不同 CheckpointMode 下并发模式不同。
			// case CheckpointMode.Immediately
			// 永远不会为false。记录Commit的时候就Flush到数据库。
			// case CheckpointMode.Period
			// 修改的时候需要记录锁（lockey）。
			// 这里只是读取，就不加锁了。
			// case CheckpointMode.Table 修改的时候需要RelativeRecordSet锁。
			// （修改为true的时也在记录锁（lockey）下）。
			// 这里只是读取，就不加锁了。

			if (p.getValue().Dirty) {
				return false;
			}

			if (p.getValue().State != GlobalCacheManager.StateInvalid) {
				if (p.getValue().Acquire(GlobalCacheManager.StateInvalid) != GlobalCacheManager.StateInvalid) {
					return false;
				}
			}
			return Remove(p);
		}
		finally {
			lockey.ExitWriteLock();
		}
	}
}