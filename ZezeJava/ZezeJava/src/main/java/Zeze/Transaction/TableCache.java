package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import Zeze.Application;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// MESI？

/**
 * 普通Lru一般把最新访问的放在列表一端，这直接导致并发上不去。
 * 基本思路是按块（用ConcurrentHashMap）保存最近访问。
 * 定时添加新块。
 * 访问需要访问1~3次ConcurrentHashMap。
 * <p>
 * 通用类的写法需要在V外面包装一层。这里直接使用Record来达到这个目的。
 * 这样，这个类就不通用了。通用类需要包装，多创建一个对象，还需要包装接口。
 */
public class TableCache<K extends Comparable<K>, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(TableCache.class);
	private static final int MAX_NODE_COUNT = 8640; // 最大的LRU节点数量,超过时会触发shrink
	private static final int SHRINK_NODE_COUNT = 8000; // shrink的目标节点数量

	private final TableX<K, V> Table;
	private final ConcurrentHashMap<K, Record1<K, V>> DataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, Record1<K, V>>> LruQueue = new ConcurrentLinkedQueue<>();
	private volatile ConcurrentHashMap<K, Record1<K, V>> LruHot;
	private Future<?> TimerNewHot;
	private Future<?> TimerClean;

	public TableCache(Application ignoredApp, TableX<K, V> table) {
		Table = table;
		DataMap = new ConcurrentHashMap<>(GetCacheInitialCapacity());
		NewLruHot();
		var newLruHotPeriod = table.getTableConf().getCacheNewLruHotPeriod();
		TimerNewHot = Task.schedule(newLruHotPeriod, newLruHotPeriod, () -> {
			// 访问很少的时候不创建新的热点。这个选项没什么意思。
			if (LruHot.size() > table.getTableConf().getCacheNewAccessHotThreshold())
				NewLruHot();
		});
		var cleanPeriod = Table.getTableConf().getCacheCleanPeriod();
		TimerClean = Task.schedule(cleanPeriod, cleanPeriod, this::CleanNow);
	}

	public final ConcurrentHashMap<K, Record1<K, V>> getDataMap() {
		return DataMap;
	}

	private int GetCacheInitialCapacity() {
		// 31 from c# document
		// 这样写，当配置修改，可以使用的时候马上生效。
		return Math.max(Table.getTableConf().getCacheInitialCapacity(), 31);
	}

	public long WalkKey(TableWalkKey<K> callback) {
		long cw = 0;
		for (var k : DataMap.keySet()) {
			if (!callback.handle(k))
				return cw;
			++cw;
		}
		return cw;
	}

	private int GetLruInitialCapacity() {
		int c = (int)(GetCacheInitialCapacity() * 0.2);
		return Math.min(c, Table.getTableConf().getCacheMaxLruInitialCapacity());
	}

	private void NewLruHot() {
		var newLru = new ConcurrentHashMap<K, Record1<K, V>>(GetLruInitialCapacity());
		LruHot = newLru;
		LruQueue.add(newLru);
	}

	public final Record1<K, V> GetOrAdd(K key, Zeze.Util.Factory<Record1<K, V>> valueFactory) {
		var lruHot = LruHot;
		var result = DataMap.get(key);
		if (result == null) { // slow-path
			result = DataMap.computeIfAbsent(key, k -> {
				var r = valueFactory.create();
				lruHot.put(key, r); // replace: add or update see this.Remove
				r.setLruNode(lruHot);
				return r;
			});
		}

		// 旧纪录 && 优化热点执行调整
		// 下面在发生LruHot变动+并发GetOrAdd时，哪个后执行，就调整到哪个node，不严格调整到真正的LruHot。
		if (result.getLruNode() != lruHot) {
			var oldNode = result.getAndSetLruNodeNull();
			if (oldNode != null) {
				oldNode.remove(key);
				if (lruHot.putIfAbsent(key, result) == null)
					result.setLruNode(lruHot);
			}
		}
		return result;
	}

	/**
	 * 内部特殊使用，不调整 Lru。
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

	private void TryPollLruQueue() {
		if (LruQueue.size() <= MAX_NODE_COUNT)
			return;

		var timeBegin = System.nanoTime();
		int recordCount = 0, nodeCount = 0;
		var polls = new ArrayList<ConcurrentHashMap<K, Record1<K, V>>>(LruQueue.size() - SHRINK_NODE_COUNT);
		while (LruQueue.size() > SHRINK_NODE_COUNT) {
			// 大概，删除超过一天的节点。
			var node = LruQueue.poll();
			if (null == node)
				break;
			polls.add(node);
			nodeCount++;
		}

		// 把被删除掉的node里面的记录迁移到当前最老(head)的node里面。
		var head = LruQueue.peek();
		assert head != null;
		for (var poll : polls) {
			for (var e : poll.entrySet()) {
				// concurrent see GetOrAdd
				var r = e.getValue();
				if (r.compareAndSetLruNodeNull(poll) && head.putIfAbsent(e.getKey(), r) == null) { // 并发访问导致这个记录已经被迁移走。
					r.setLruNode(head);
					recordCount++;
				}
			}
		}
		logger.info("({}){}: shrank {} nodes, moved {} records, {} ms, result: {}/{}",
				Table.getZeze().getConfig().getServerId(), Table.getName(), nodeCount, recordCount,
				(System.nanoTime() - timeBegin) / 1_000_000, LruQueue.size(), MAX_NODE_COUNT);
	}

	private void CleanNow() throws Throwable {
		// 这个任务的执行时间可能很长，
		// 不直接使用 Scheduler 的定时任务，
		// 每次执行完重新调度。
		var capacity = Table.getTableConf().getRealCacheCapacity();
		if (capacity > 0) {
			var timeBegin = System.nanoTime();
			int recordCount = 0, nodeCount = 0;
			while (DataMap.size() > capacity && Table.getZeze().isStart()) { // 超出容量，循环尝试
				var node = LruQueue.peek();
				if (null == node || node == LruHot) { // 热点。不回收。
					break;
				}

				for (var e : node.entrySet()) {
					if (TryRemoveRecord(e))
						recordCount++;
				}
				if (node.isEmpty()) {
					LruQueue.poll();
					nodeCount++;
				} else {
					logger.warn("remain record when clean oldest lruNode.");
					// 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
					Table.getZeze().getCheckpoint().RunOnce();
					//noinspection BusyWait
					Thread.sleep(Table.getTableConf().getCacheCleanPeriodWhenExceedCapacity());
				}
			}
			if (recordCount > 0 || nodeCount > 0) {
				logger.info("({}){}: cleaned {} records, {} nodes, {} ms, result: {}/{}",
						Table.getZeze().getConfig().getServerId(), Table.getName(), recordCount, nodeCount,
						(System.nanoTime() - timeBegin) / 1_000_000, DataMap.size(), capacity);
			}
		}
		TryPollLruQueue();
	}

	public void close() {
		if (null != TimerClean)
			TimerClean.cancel(true);
		TimerClean = null;
		if (null != TimerNewHot)
			TimerNewHot.cancel(true);
		TimerNewHot = null;
	}

	// under lockey.writeLock and record.fairLock
	private boolean Remove(Map.Entry<K, Record1<K, V>> p) {
		if (DataMap.remove(p.getKey(), p.getValue())) {
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// see GetOrAdd
			p.getValue().setState(GlobalCacheManagerServer.StateRemoved);
			// 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			var oldNode = p.getValue().getLruNode();
			if (oldNode != null)
				oldNode.remove(p.getKey(), p.getValue());
			Table.RocksCacheRemove(p.getKey());
			return true;
		}
		return true; // 没有删除成功，仍然返回true。
	}

	private boolean TryRemoveRecordUnderLock(Map.Entry<K, Record1<K, V>> p) {
		if (Table.GetStorage() == null) {
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

		var record = p.getValue();
		if (record.getDirty())
			return false;

		if (record.isFreshAcquire())
			return false;

		if (record.getState() != GlobalCacheManagerServer.StateInvalid) {
			try {
				var r = record.Acquire(GlobalCacheManagerServer.StateInvalid, false);
				if (r.ResultCode != 0 || r.ResultState != GlobalCacheManagerServer.StateInvalid)
					return false;
			} catch (Throwable e) {
				logger.error("Acquire({}:{}) exception:", record.getTable().getName(), record.getObjectKey(), e);
				// 此时GlobalServer可能已经改成StateInvalid了, 无论如何还是当成已经Invalid保证安全
			}
		}
		return Remove(p);
	}

	private boolean TryRemoveRecord(Map.Entry<K, Record1<K, V>> p) {
		// lockey 第一优先，和事务并发。
		final TableKey tkey = new TableKey(Table.getId(), p.getKey());
		final Locks locks = Table.getZeze().getLocks();
		if (locks == null) // 可能是已经执行Application.Stop导致的
			return TryRemoveRecordUnderLock(p); // 临时修正
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

					return TryRemoveRecordUnderLock(p);
				} finally {
					rrs.UnLock();
				}
			} finally {
				p.getValue().ExitFairLock();
			}
		} finally {
			lockey.ExitWriteLock();
		}
	}
}
