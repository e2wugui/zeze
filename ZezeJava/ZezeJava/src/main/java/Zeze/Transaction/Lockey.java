package Zeze.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import org.jspecify.annotations.Nullable;

/**
 * 记录锁键：包装 TableKey 并持有其读写锁；相同 TableKey 必须通过 Locks 获取同一个 Lockey 实例，
 * 事务间按 TableKey 全序加锁以防死锁。
 */
public final class Lockey implements Zeze.Util.Lockey<Lockey> {
	private final TableKey tableKey;
	private @Nullable ReentrantReadWriteLock rwLock;
	private ZezeCounter.@Nullable LongCounter readLockCounter;
	private ZezeCounter.@Nullable LongCounter writeLockCounter;
	private ZezeCounter.@Nullable LongCounter tryReadLockCounter;
	private ZezeCounter.@Nullable LongCounter tryWriteLockCounter;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 *
	 * @param key table key
	 */
	public Lockey(TableKey key) {
		tableKey = key;
	}

	public TableKey getTableKey() {
		return tableKey;
	}

	/**
	 * 创建真正的锁对象。
	 */
	@Override
	public Lockey alloc() {
		// 非公平构造是取舍而非疏忽（对照Record.fairLock的公平锁）：记录锁读多写少、
		// 持锁区间短，公平锁的排队唤醒开销在吞吐上不划算。代价是极端连续读下写者
		// 排队较久（非公平锁固有）；
		rwLock = new ReentrantReadWriteLock();
		readLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.READ_LOCK);
		writeLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.WRITE_LOCK);
		tryReadLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.TRY_READ_LOCK);
		tryWriteLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.TRY_WRITE_LOCK);
		return this;
	}

	public void enterReadLock() {
		//noinspection DataFlowIssue
		readLockCounter.increment();
		//noinspection DataFlowIssue
		var readLock = rwLock.readLock();
		if (readLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitReadLock", tableKey)) {
			readLock.lock();
		}
	}

	public void exitReadLock() {
		//noinspection DataFlowIssue
		rwLock.readLock().unlock();
	}

	public void enterWriteLock() {
		//noinspection DataFlowIssue
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数
			//noinspection DataFlowIssue
			writeLockCounter.increment();
		var writeLock = rwLock.writeLock();
		if (writeLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitWriteLock", tableKey)) {
			writeLock.lock();
		}
	}

	public void exitWriteLock() {
		//noinspection DataFlowIssue
		rwLock.writeLock().unlock();
	}

	public boolean tryEnterReadLock(int millisecondsTimeout) {
		//noinspection DataFlowIssue
		tryReadLockCounter.increment();
		try {
			//noinspection DataFlowIssue
			var readLock = rwLock.readLock();
			if (millisecondsTimeout > 0)
				return readLock.tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
			return readLock.tryLock();
		} catch (InterruptedException e) {
			throw Task.forceThrow(e);
		}
	}

	public boolean tryEnterWriteLock(int millisecondsTimeout) {
		//noinspection DataFlowIssue
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数，失败了也计数。
			//noinspection DataFlowIssue
			tryWriteLockCounter.increment();
		try {
			var writeLock = rwLock.writeLock();
			if (millisecondsTimeout > 0)
				return writeLock.tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
			return writeLock.tryLock();
		} catch (InterruptedException e) {
			throw Task.forceThrow(e);
		}
	}

	public boolean isWriteLockHeld() {
		//noinspection DataFlowIssue
		return rwLock.isWriteLockedByCurrentThread();
	}

	/**
	 * 根据参数进入读或写锁。
	 * 不会自动释放已有读锁。读转写前，调用方必须先释放读锁，
	 * 并按全局锁序重新获取所需锁；Transaction会先释放相应后缀。
	 *
	 * @param isWrite Write Lock Need.
	 */
	public void enterLock(boolean isWrite) {
		if (isWrite) {
			// 持有读锁时不能直接等待写锁；调用方负责先释放读锁。
			enterWriteLock();
		} else {
			enterReadLock();
		}
	}

	/**
	 * 释放当前持有的单一形态锁（与enterLock配对）。
	 * 契约：仅支持"读或写"单一持有形态。锁升级路径（先enterReadLock再enterWriteLock、
	 * 读写同持）调用本方法只解写锁，读锁滞留——升级持有须自行enterReadLock/
	 * exitReadLock配对释放，不得依赖本方法收尾。
	 */
	public void exitLock() {
		//noinspection DataFlowIssue
		if (rwLock.isWriteLockedByCurrentThread()) {
			rwLock.writeLock().unlock();
		} else {
			rwLock.readLock().unlock();
		}
		// 重入次数由调用方配对管理，此处按单一持有形态释放一次。
	}

	@Override
	public int compareTo(@Nullable Lockey other) {
		if (other == null)
			return 1; // null always small
		return tableKey.compareTo(other.tableKey);
	}

	@Override
	public int hashCode() {
		return tableKey.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		return obj instanceof Lockey && tableKey.equals(((Lockey)obj).tableKey);
	}
}
