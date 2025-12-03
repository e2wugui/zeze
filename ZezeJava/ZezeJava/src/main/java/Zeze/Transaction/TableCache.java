package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Util.Factory;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateInvalid;
import static Zeze.Services.GlobalCacheManagerConst.StateRemoved;

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
	private static final @NotNull Logger logger = LogManager.getLogger(TableCache.class);
	private static final int MAX_NODE_COUNT = 8640; // 最大的LRU节点数量,超过时会触发shrink
	private static final int SHRINK_NODE_COUNT = 8000; // shrink的目标节点数量

	private final @NotNull TableX<K, V> table;
	private final @NotNull ConcurrentHashMap<K, Record1<K, V>> dataMap;
	private final ConcurrentLinkedQueue<ConcurrentHashMap<K, Record1<K, V>>> lruQueue = new ConcurrentLinkedQueue<>();
	private volatile ConcurrentHashMap<K, Record1<K, V>> lruHot;
	private @Nullable Future<?> timerNewHot;
	private @Nullable Future<?> timerClean;
	private final LongAdder sizeCounter = new LongAdder();
	private final ReentrantLock cleanNowLock = new ReentrantLock();

	LongAdder getSizeCounter() {
		return sizeCounter;
	}

	public long size() {
		return table.isMemory() ? sizeCounter.sum() : dataMap.size();
	}

	TableCache(Application ignoredApp, @NotNull TableX<K, V> table) {
		this.table = table;
		dataMap = new ConcurrentHashMap<>(getCacheInitialCapacity());
		newLruHot();
		var newLruHotPeriod = table.getTableConf().getCacheNewLruHotPeriod();
		timerNewHot = Task.scheduleUnsafe(newLruHotPeriod, newLruHotPeriod, () -> {
			// 访问很少的时候不创建新的热点。这个选项没什么意思。
			if (lruHot.size() > table.getTableConf().getCacheNewAccessHotThreshold())
				newLruHot();
		});
		var cleanPeriod = this.table.getTableConf().getCacheCleanPeriod();
		timerClean = Task.scheduleUnsafe(cleanPeriod, cleanPeriod, this::cleanNow);
	}

	final @NotNull ConcurrentHashMap<K, Record1<K, V>> getDataMap() {
		return dataMap;
	}

	private int getCacheInitialCapacity() {
		// 31 from c# document
		// 这样写，当配置修改，可以使用的时候马上生效。
		return Math.max(table.getTableConf().getCacheInitialCapacity(), 31);
	}

	public long walkKey(@NotNull TableWalkKey<K> callback) throws Exception {
		long cw = 0;
		for (var k : dataMap.keySet()) {
			if (!callback.handle(k))
				return cw;
			++cw;
		}
		return cw;
	}

	private int getLruInitialCapacity() {
		int c = getCacheInitialCapacity();
		return Math.min(c, table.getTableConf().getCacheMaxLruInitialCapacity());
	}

	private void newLruHot() {
		var newLru = new ConcurrentHashMap<K, Record1<K, V>>(getLruInitialCapacity());
		lruHot = newLru;
		lruQueue.add(newLru);
	}

	public final @NotNull Record1<K, V> getOrAdd(@NotNull K key, @NotNull Factory<Record1<K, V>> valueFactory) {
		var lruHot = this.lruHot;
		var result = dataMap.get(key);
		if (result == null) { // slow-path
			result = dataMap.computeIfAbsent(key, k -> {
				var r = valueFactory.create();
				lruHot.put(k, r); // replace: add or update see this.Remove
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
	final @Nullable Record1<K, V> get(K key) {
		return dataMap.get(key);
	}

	private void tryPollLruQueue() {
		if (lruQueue.size() <= MAX_NODE_COUNT)
			return;

		var timeBegin = System.nanoTime();
		int recordCount = 0, nodeCount = 0;
		var polls = new ArrayList<ConcurrentHashMap<K, Record1<K, V>>>(lruQueue.size() - SHRINK_NODE_COUNT);
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
				// concurrent see GetOrAdd
				var r = e.getValue();
				if (r.compareAndSetLruNodeNull(poll) && head.putIfAbsent(e.getKey(), r) == null) { // 并发访问导致这个记录已经被迁移走。
					r.setLruNode(head);
					recordCount++;
				}
			}
		}
		logger.info("({}){}: shrank {} nodes, moved {} records, {} ms, result: {}/{}",
				table.getZeze().getConfig().getServerId(), table.getName(), nodeCount, recordCount,
				(System.nanoTime() - timeBegin) / 1_000_000, lruQueue.size(), MAX_NODE_COUNT);
	}

	public void cleanNow() throws Exception {
		cleanNowLock.lock();
		try {
			// 这个任务的执行时间可能很长，
			// 不直接使用 Scheduler 的定时任务，
			// 每次执行完重新调度。
			var capacity = table.getTableConf().getRealCacheCapacity();
			if (capacity >= 0) {
				var timeBegin = System.nanoTime();
				int recordCount = 0, nodeCount = 0;
				while (dataMap.size() > capacity && table.getZeze().isStart()) { // 超出容量，循环尝试
					var node = lruQueue.peek();
					if (node == null || node == lruHot) // 热点。不回收。
						break;

					for (var e : node.entrySet()) {
						if (tryRemoveRecord(e))
							recordCount++;
					}
					if (node.isEmpty()) {
						lruQueue.poll();
						nodeCount++;
					} else {
						logger.info("remain {} records when clean oldest lruNode", node.size());
						// 出现回收不了，一般是批量修改数据，此时启动一次Checkpoint。
						var checkpoint = table.getZeze().getCheckpoint();
						if (checkpoint != null)
							checkpoint.runOnce();
						//noinspection BusyWait
						Thread.sleep(table.getTableConf().getCacheCleanPeriodWhenExceedCapacity());
					}
				}
				if (recordCount > 0 || nodeCount > 0) {
					logger.info("({}){}: cleaned {} records, {} nodes, {} ms, result: {}/{}",
							table.getZeze().getConfig().getServerId(), table.getName(), recordCount, nodeCount,
							(System.nanoTime() - timeBegin) / 1_000_000, dataMap.size(), capacity);
				}
			}
			tryPollLruQueue();
		} finally {
			cleanNowLock.unlock();
		}
	}

	void close() {
		if (timerClean != null) {
			try {
				timerClean.cancel(true);
			} catch (Throwable e) { // logger.error
				logger.error("timerClean.cancel exception:", e);
			}
			timerClean = null;
		}
		if (timerNewHot != null) {
			try {
				timerNewHot.cancel(true);
			} catch (Throwable e) { // logger.error
				logger.error("timerNewHot.cancel exception:", e);
			}
			timerNewHot = null;
		}
	}

	// under lockey.writeLock and record.fairLock
	void remove(@NotNull K k, @NotNull Record1<K, V> r, boolean removeLocalRocks) {
		if (dataMap.remove(k, r)) {
			// 这里有个时间窗口：先删除DataMap再去掉Lru引用，
			// 当对Key再次GetOrAdd时，LruNode里面可能已经存在旧的record。
			// see GetOrAdd
			r.setState(StateRemoved);
			// 必须使用 Pair，有可能 LurNode 里面已经有新建的记录了。
			var oldNode = r.getLruNode();
			if (oldNode != null)
				oldNode.remove(k, r);
			if (removeLocalRocks)
				table.rocksCacheRemove(k);
		} else
			r.setState(StateRemoved); // 也确保已删除状态
	}

	private boolean tryRemoveRecordUnderLock(@NotNull Map.Entry<K, Record1<K, V>> p) {
		if (table.isMemory()) {
			// 不支持内存表cache同步。
			// 内存表删除cache，只需要判断是否dirty。
			// 内存表不执行clean，代码不会执行到这里，这里是以后需要执行clean时才会到达的。
			if (p.getValue().getDirty())
				return false;
			remove(p.getKey(), p.getValue(), false);
			return true;
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

		if (record.getState() != StateInvalid) {
			record.setState(StateInvalid); // 先本地改成Invalid,避免TableX.VerifyGlobalRecordState验证失败
			try {
				// noWait=true时只能连接async版GlobalServer, 而连sync和raft版会出现VerifyGlobalRecordState验证失败
				// 可能原因是lockey特别容易乱序, sync和raft版需要用队列保证顺序
				record.acquire(StateInvalid, false, false);
			} catch (Throwable e) { // logger.error
				// 降低这个日志级别，因为fastError或者其他原因，会导致大量的日志。而此时这里的错误是可以忽略的。
				logger.debug("Acquire({}:{}) exception:", record.getTable().getName(), record.getObjectKey(), e);
				// 此时GlobalServer可能已经改成StateInvalid了, 无论如何还是当成已经Invalid保证安全
			}
		}

		remove(p.getKey(), p.getValue(), true);
		return true;
	}

	private boolean tryRemoveRecord(@NotNull Map.Entry<K, Record1<K, V>> p) {
		// lockey 第一优先，和事务并发。
		final TableKey tkey = new TableKey(table.getId(), p.getKey());
		final Locks locks = table.getZeze().getLocks();
//		if (locks == null) // 可能是已经执行Application.Stop导致的
//			return tryRemoveRecordUnderLock(p); // 临时修正
		final Lockey lockey = locks.get(tkey);
		if (!lockey.tryEnterWriteLock(0))
			return false;
		try {
			// record.lock 和事务并发。
			if (!p.getValue().tryEnterFairLockWhenIdle())
				return false;
			try {
				// rrs.lock
				var rrs = p.getValue().getRelativeRecordSet();
				if (!rrs.tryLockWhenIdle())
					return false;
				try {
					if (rrs.getMergeTo() != null)
						return false; // // 刚刚被合并或者删除（flushed）的记录认为是活跃的，不删除。

					if (rrs.getRecordSet() != null && rrs.getRecordSet().size() > 1)
						return false; // 只包含自己的时候才可以删除，多个记录关联起来时不删除。

					return tryRemoveRecordUnderLock(p);
				} finally {
					rrs.unlock();
				}
			} finally {
				p.getValue().exitFairLock();
			}
		} finally {
			lockey.exitWriteLock();
		}
	}
}
