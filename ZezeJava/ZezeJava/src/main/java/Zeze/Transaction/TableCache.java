package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Util.Factory;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateInvalid;
import static Zeze.Services.GlobalCacheManagerConst.StateRemoved;

/**
 * 表记录分块近似 LRU 缓存：用定时轮换的 ConcurrentHashMap 热点块替代全局链表以提高并发，
 * 直接持有 Record1 避免额外包装层，并负责超容量清理与过期块收缩。
 */
public class TableCache<K extends Comparable<K>, V extends Bean> {
	private static final Zeze.Util.ZezeCounter.LongObserver evictAcquireObserver =
			Zeze.Util.ZezeCounter.instance.getRunTimeObserver("TableCache.evictAcquire");

	private static final Logger logger = LogManager.getLogger(TableCache.class);
	private static final int MAX_NODE_COUNT = 8640; // LRU 节点数上限，超出触发 shrink
	private static final int SHRINK_NODE_COUNT = 8000; // shrink 目标节点数
	private static final int CLEAN_TIME_LIMIT_PERIODS = 3; // 单次 cleanNow 时界（CacheCleanPeriod 的倍数）

	private final TableX<K, V> table;
	private final ConcurrentHashMap<K, Record1<K, V>> dataMap;
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

	TableCache(Application ignoredApp, TableX<K, V> table) {
		this.table = table;
		dataMap = new ConcurrentHashMap<>(getCacheInitialCapacity());
		newLruHot();
		var newLruHotPeriod = table.getTableConf().getCacheNewLruHotPeriod();
		timerNewHot = TaskSpec.ofAction(this::onTimerNewHot).schedulePeriodNow(newLruHotPeriod, newLruHotPeriod);
		var cleanPeriod = this.table.getTableConf().getCacheCleanPeriod();
		try {
			timerClean = TaskSpec.ofAction(this::cleanNow).schedulePeriodNow(cleanPeriod, cleanPeriod);
		} catch (Throwable e) {
			// 第二个定时器注册失败时取消已注册的 timerNewHot，防止缓存对象泄漏。
			timerNewHot.cancel(false);
			throw e;
		}
	}

	final ConcurrentHashMap<K, Record1<K, V>> getDataMap() {
		return dataMap;
	}

	private int getCacheInitialCapacity() {
		// 31 from c# document
		// 这样写，当配置修改，可以使用的时候马上生效。
		return Math.max(table.getTableConf().getCacheInitialCapacity(), 31);
	}

	public long walkKey(TableWalkKey<K> callback) throws Exception {
		long cw = 0;
		for (var k : dataMap.keySet()) {
			++cw;
			if (!callback.handle(k))
				return callback.endWalk(cw);
		}
		return callback.endWalk(cw);
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

	/** timerNewHot 周期体：热点块轮换 + lruQueue 收缩。 */
	void onTimerNewHot() {
		if (lruHot.size() > table.getTableConf().getCacheNewAccessHotThreshold())
			newLruHot();
		// 不阻塞定时线程：cleanNow 持锁时跳过，下一拍重试。
		if (cleanNowLock.tryLock()) {
			try {
				tryPollLruQueue();
			} finally {
				cleanNowLock.unlock();
			}
		}
	}

	public final Record1<K, V> getOrAdd(K key, Factory<Record1<K, V>> valueFactory) {
		var lruHot = this.lruHot;
		var result = dataMap.get(key);
		if (result == null) { // slow-path
			result = dataMap.computeIfAbsent(key, k -> {
				var r = Objects.requireNonNull(valueFactory.create());
				lruHot.put(k, r); // 覆盖可能残留的旧登记，见 remove()
				r.setLruNode(lruHot);
				return r;
			});
		}

		// 并发换块时按捕获到的 lruHot 调整，允许轻微统计偏差。
		if (result.getLruNode() != lruHot)
			adjustLru(key, result, lruHot);
		return result;
	}

	// 仅在 dataMap 仍映射到 result 时登记进热点块，防止已删除记录滞留导致节点永不为空。
	private void adjustLru(K key, Record1<K, V> result,
						   ConcurrentHashMap<K, Record1<K, V>> curLruHot) {
		var oldNode = result.getAndSetLruNodeNull();
		if (oldNode == null)
			return;
		// 必须按 (key, result) 摘除，防止误删并发新建的同键记录。
		oldNode.remove(key, result);
		while (dataMap.get(key) == result) {
			var prev = curLruHot.putIfAbsent(key, result);
			if (prev == null) {
				result.setLruNode(curLruHot);
				if (dataMap.get(key) != result)
					curLruHot.remove(key, result); // 登记期间被并发删除则回滚
				return;
			}
			// 槽位被占时：若占坑者仍是当前活映射则让位，否则摘除过期占位后重试。
			if (dataMap.get(key) == prev)
				return;
			curLruHot.remove(key, prev);
		}
	}

	/**
	 * 内部特殊使用，不调整 Lru。
	 */
	final @Nullable Record1<K, V> get(K key) {
		return dataMap.get(key);
	}

	@SuppressWarnings("ConstantConditions") // poll 在队列被清空时确实允许返回 null。
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
				// concurrent see adjustLru
				var r = e.getValue();
				if (!r.compareAndSetLruNodeNull(poll))
					continue; // 并发访问导致这个记录已经被迁移走。
				var key = e.getKey();
				// 仅迁移仍在 dataMap 中的活记录，避免死记录滞留 head。
				while (dataMap.get(key) == r) {
					var prev = head.putIfAbsent(key, r);
					if (prev == null) {
						r.setLruNode(head);
						if (dataMap.get(key) == r)
							recordCount++;
						else
							head.remove(key, r); // 登记期间被并发删除则回滚
						break;
					}
					if (dataMap.get(key) == prev)
						break;
					head.remove(key, prev);
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
			var capacity = table.getTableConf().getRealCacheCapacity();
			if (capacity >= 0) {
				var timeBegin = System.nanoTime();
				// 单次清理设时界，避免最老块持续无法驱逐时占满调度池线程，剩余留待下轮。
				var cleanPeriod = table.getTableConf().getCacheCleanPeriod();
				long timeLimitMs = Math.max(cleanPeriod, 1) * (long)CLEAN_TIME_LIMIT_PERIODS;
				long deadline = System.currentTimeMillis() + timeLimitMs;
				int recordCount = 0, nodeCount = 0;
				while (dataMap.size() > capacity && table.getZeze().isStart()) { // 超出容量，循环尝试
					if (System.currentTimeMillis() >= deadline) {
						logger.info("({}){}: cleanNow reach time limit {} ms, cleaned {} records, {} nodes, remain {}/{}, wait next schedule",
								table.getZeze().getConfig().getServerId(), table.getName(),
								timeLimitMs, recordCount, nodeCount, dataMap.size(), capacity);
						break;
					}

					var node = lruQueue.peek();
					if (node == null || node == lruHot) // 热点。不回收。
						break;

					for (var e : node.entrySet()) {
						if (dataMap.get(e.getKey()) != e.getValue()) {
							// 过期残留直接摘除，避免节点永不为空。
							node.remove(e.getKey(), e.getValue());
							continue;
						}
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

	// 调用方须持有 r.fairLock（驱逐路径还持 lockey 写锁与 rrs 锁）；并发读见 StateRemoved 会重走 getOrAdd。
	void remove(K k, Record1<K, V> r, boolean removeLocalRocks) {
		// 镜像清理须先于摘除 dataMap，防止新 load 写入镜像后被旧记录清理误删；过期记录不删镜像。
		if (removeLocalRocks && dataMap.get(k) == r) {
			try {
				table.rocksCacheRemove(k);
			} catch (Throwable e) { // logger.error
				// 清理失败仍作废缓存记录，新记录会经 load 刷新镜像。
				logger.error("TableCache.remove localRocks cleanup failed (harmless, reload enforces):", e);
			}
		}
		if (dataMap.remove(k, r)) {
			r.setState(StateRemoved);
			// 按 (k, r) 摘除，防止误删并发新建的同键记录。
			var oldNode = r.getLruNode();
			if (oldNode != null)
				oldNode.remove(k, r);
		} else {
			r.setState(StateRemoved);
			// r 已非当前映射时仍须按 (k, r) 摘除，防止死记录滞留 LRU 块。
			var oldNode = r.getLruNode();
			if (oldNode != null)
				oldNode.remove(k, r);
		}
	}

	private boolean tryRemoveRecordUnderLock(Map.Entry<K, Record1<K, V>> p) {
		if (table.isMemory()) {
			// 内存表仅回收非脏驻留（数据在 localRocks），不减逻辑条目计数 sizeCounter。
			if (p.getValue().getDirty())
				return false;
			remove(p.getKey(), p.getValue(), false);
			return true;
		}

		var record = p.getValue();
		if (record.getDirty())
			return false;

		if (record.isFreshAcquire())
			return false;

		if (record.getState() != StateInvalid) {
			record.setState(StateInvalid); // 先改本地状态，避免 VerifyGlobalRecordState 误报
			try {
				// sync/raft 版 GlobalServer 需同步等待以保证 lockey 顺序，此处记录同步 acquire 耗时。
				var evictBegin = Zeze.Util.ZezeCounter.ENABLE ? System.nanoTime() : 0;
				record.acquire(StateInvalid, false, false);
				if (evictBegin != 0)
					evictAcquireObserver.observe(System.nanoTime() - evictBegin);
			} catch (Throwable e) { // logger.error
				logger.debug("Acquire({}:{}) exception:", record.getTable().getName(), record.getObjectKey(), e);
				// 服务端可能已置 StateInvalid，本地保持 Invalid 保证安全。
			}
		}

		remove(p.getKey(), p.getValue(), true);
		return true;
	}

	private boolean tryRemoveRecord(Map.Entry<K, Record1<K, V>> p) {
		// lockey 第一优先，和事务并发。
		final TableKey tkey = new TableKey(table.getId(), p.getKey());
		final Locks locks = table.getZeze().getLocks();
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
						return false; // 刚刚被合并或者删除（flushed）的记录认为是活跃的，不删除。

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
