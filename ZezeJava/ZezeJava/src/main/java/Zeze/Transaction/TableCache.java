package Zeze.Transaction;

import Zeze.Services.*;
import Zeze.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Util.Task;

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
public class TableCache<K extends Comparable<K>, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(TableCache.class);

	private final ConcurrentHashMap<K, Record1<K, V>> DataMap;
	public final ConcurrentHashMap<K, Record1<K, V>> getDataMap() {
		return DataMap;
	}

	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, Record1<K, V>>> LruQueue = new ConcurrentLinkedQueue<> ();
	private ConcurrentLinkedQueue<ConcurrentHashMap<K, Record1<K, V>>> getLruQueue() {
		return LruQueue;
	}

	private volatile ConcurrentHashMap<K, Record1<K, V>> LruHot;
	private ConcurrentHashMap<K, Record1<K, V>> getLruHot() {
		return LruHot;
	}

	private final TableX<K, V> Table;
	public final TableX<K, V> getTable() {
		return Table;
	}

	public TableCache(Application ignoredApp, TableX<K, V> table) {
		this.Table = table;
		DataMap = new ConcurrentHashMap<>(GetCacheInitialCapacity(), 0.75f, GetCacheConcurrencyLevel());
		NewLruHot();
		Task.schedule(table.getTableConf().getCacheNewLruHotPeriod(), table.getTableConf().getCacheNewLruHotPeriod(),
				() -> {
				// 访问很少的时候不创建新的热点。这个选项没什么意思。
				if (getLruHot().size() > table.getTableConf().getCacheNewAccessHotThreshold()) {
					NewLruHot();
				}
		});
		Task.schedule(getTable().getTableConf().getCacheCleanPeriod(), this::CleanNow);
	}

	private int GetCacheConcurrencyLevel() {
		// 这样写，当配置修改，可以使用的时候马上生效。
		var processors = Runtime.getRuntime().availableProcessors();
		return Math.max(getTable().getTableConf().getCacheConcurrencyLevel(), processors);
	}

	private int GetCacheInitialCapacity() {
		// 31 from c# document
		// 这样写，当配置修改，可以使用的时候马上生效。
		return Math.max(getTable().getTableConf().getCacheInitialCapacity(), 31);
	}

	private int GetLruInitialCapacity() {
		int c = (int)(GetCacheInitialCapacity() * 0.2);
		return Math.min(c, getTable().getTableConf().getCacheMaxLruInitialCapacity());
	}

	private void NewLruHot() {
		var newLru = new ConcurrentHashMap<K, Record1<K, V>>(
				GetLruInitialCapacity(), 0.75f, GetCacheConcurrencyLevel());
		LruHot = newLru;
		getLruQueue().add(newLru);
	}

	public final Record1<K, V> GetOrAdd(K key, Zeze.Util.Factory<Record1<K, V>> valueFactory) {
		final var isNew = new Zeze.Util.OutObject<Boolean>();
		isNew.Value = false;
		Record1<K, V> result = DataMap.computeIfAbsent(key, (k) -> {
					var r = valueFactory.create();
					var lruHot = getLruHot();
					lruHot.put(k, r); // replace: add or update see this.Remove
					r.setLruNode(lruHot);
					isNew.Value = true;
					return r;
		});

		if (!isNew.Value && result.getLruNode() != getLruHot()) {
			result.getLruNode().remove(key, result);
			var lruHot = getLruHot();
			if (null == lruHot.putIfAbsent(key, result)) {
				result.setLruNode(lruHot);
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

	 @param key key
	 @return  Record1
	*/
	public final Record1<K, V> Get(K key) {
		return DataMap.get(key);
	}

	// 不再提供删除，由 Cleaner 集中清理。
	// under lockey.writeLock
	/*
	internal void Remove(K key)
	{
	    map.Remove(key, out var _);
	}
	*/

	public final void CleanNow() {
		// 这个任务的执行时间可能很长，
		// 不直接使用 Scheduler 的定时任务，
		// 每次执行完重新调度。

		if (getTable().getTableConf().getCacheCapacity() <= 0) {
			Task.schedule(getTable().getTableConf().getCacheCleanPeriod(), this::CleanNow);
			return; // 容量不限
		}

		try {
			while (DataMap.size() > getTable().getTableConf().getCacheCapacity()) { // 超出容量，循环尝试
				var node = getLruQueue().peek();
				if (null == node || node == getLruHot()) { // 热点。不回收。
					break;
				}

				for (var e : node.entrySet()) {
					if (!TryRemoveRecord(e)) {
						// 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
						getTable().getZeze().CheckpointRun();
					}
				}
				if (node.size() == 0) {
					getLruQueue().poll();
				} else {
					logger.warn("remain record when clean oldest lruNode.");
				}

				try {
					//noinspection BusyWait
					Thread.sleep(getTable().getTableConf().getCacheCleanPeriodWhenExceedCapacity());
				} catch (InterruptedException skip) {
					// skip
				}
			}
		} finally {
			Task.schedule(getTable().getTableConf().getCacheCleanPeriod(), this::CleanNow);
		}
	}

	// under lockey.writeLock and record.fairLock
	private boolean Remove(Map.Entry<K, Record1<K, V>> p) {
		if (DataMap.remove(p.getKey(), p.getValue())) {
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// see GetOrAdd
			p.getValue().setState(GlobalCacheManagerServer.StateRemoved);
			// 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			p.getValue().getLruNode().remove(p.getKey(), p.getValue());
			return true;
		}
		return true; // 没有删除成功，仍然返回true。
	}

	private boolean TryRemoveRecordUnderLocks(Map.Entry<K, Record1<K, V>> p) {
		var storage = getTable().TStorage;
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

		if (p.getValue().getDirty()) {
			return false;
		}

		if (p.getValue().getState() != GlobalCacheManagerServer.StateInvalid) {
			var r = p.getValue().Acquire(GlobalCacheManagerServer.StateInvalid);
			if (r.ResultCode != 0 || r.ResultState != GlobalCacheManagerServer.StateInvalid) {
				return false;
			}
		}
		return Remove(p);
	}

	private boolean TryRemoveRecord(Map.Entry<K, Record1<K, V>> p) {
		// lockey 第一优先，和事务并发。
		final TableKey tkey = new TableKey(this.getTable().getId(), p.getKey());
		final Locks locks = Table.getZeze().getLocks();
		if (locks == null) // 可能是已经执行Application.Stop导致的
			return TryRemoveRecordUnderLocks(p); // 临时修正
		final Lockey lockey = locks.Get(tkey);
		if (!lockey.TryEnterWriteLock(0))
			return false;
		try {
			// record.lock 和事务并发。
			if (!p.getValue().TryEnterFairLockWhenIdle())
				return false;
			try {
				// rrs.lock
				var rrs = p.getValue().getRelativeRecordSet();
				if (!rrs.TryLockWhenIdle())
					return false;
				try {
					if (rrs.getMergeTo() != null)
						return false; // // 刚刚被合并或者删除（flushed）的记录认为是活跃的，不删除。

					if (rrs.getRecordSet() != null && rrs.getRecordSet().size() > 1)
						return false; // 只包含自己的时候才可以删除，多个记录关联起来时不删除。

					return TryRemoveRecordUnderLocks(p);
				} finally {
					rrs.UnLock();
				}
			} finally {
				p.getValue().ExitFairLock();
			}
		}
		finally {
			lockey.ExitWriteLock();
		}
	}
}
