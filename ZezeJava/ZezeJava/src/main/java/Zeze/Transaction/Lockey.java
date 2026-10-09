package Zeze.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 记录锁键：包装 TableKey 并持有其读写锁；相同 TableKey 必须通过 Locks 获取同一个 Lockey 实例，
 * 事务间按 TableKey 全序加锁以防死锁。
 */
public final class Lockey implements Zeze.Util.Lockey<Lockey> {
	private final @NotNull TableKey tableKey;
	private ReentrantReadWriteLock rwLock;
	// 锁路径统计句柄缓存（对齐 ProcedureCounter"调用点缓存复用，热路径零名字解析"惯例）：
	// tableCounter 每次调用要解析 (tableId,metric) → counter 的映射（PerfCounter 为 map 查找），
	// 锁获取是每事务多次的最热路径之一，句柄在 alloc 时一次解析终身复用（同 tableId 的
	// Lockey 实例由 Locks 去重，缓存按 key 摊销）。实现保证句柄稳定（Noop 返回单例）。
	private ZezeCounter.LongCounter readLockCounter;
	private ZezeCounter.LongCounter writeLockCounter;
	private ZezeCounter.LongCounter tryReadLockCounter;
	private ZezeCounter.LongCounter tryWriteLockCounter;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 *
	 * @param key table key
	 */
	public Lockey(@NotNull TableKey key) {
		tableKey = key;
	}

	public @NotNull TableKey getTableKey() {
		return tableKey;
	}

	/**
	 * 创建真正的锁对象。
	 */
	@Override
	public Lockey alloc() {
		// 非公平构造是取舍而非疏忽（对照Record.fairLock的公平锁）：记录锁读多写少、
		// 持锁区间短，公平锁的排队唤醒开销在吞吐上不划算。代价是极端连续读下写者
		// 排队较久（非公平锁固有）；若实测出现写者饥饿需公平化，应以基准数据支持再改。
		rwLock = new ReentrantReadWriteLock();
		readLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.READ_LOCK);
		writeLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.WRITE_LOCK);
		tryReadLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.TRY_READ_LOCK);
		tryWriteLockCounter = ZezeCounter.instance.tableCounter(tableKey.getId(), ZezeCounter.TableMetric.TRY_WRITE_LOCK);
		return this;
	}

	public void enterReadLock() {
		readLockCounter.increment();
		var readLock = rwLock.readLock();
		if (readLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitReadLock", tableKey)) {
			readLock.lock();
		}
	}

	public void exitReadLock() {
		rwLock.readLock().unlock();
	}

	public void enterWriteLock() {
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数
			writeLockCounter.increment();
		var writeLock = rwLock.writeLock();
		if (writeLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitWriteLock", tableKey)) {
			writeLock.lock();
		}
	}

	public void exitWriteLock() {
		rwLock.writeLock().unlock();
	}

	public boolean tryEnterReadLock(int millisecondsTimeout) {
		tryReadLockCounter.increment();
		try {
			var readLock = rwLock.readLock();
			if (millisecondsTimeout > 0)
				return readLock.tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
			return readLock.tryLock();
		} catch (InterruptedException e) {
			throw Task.forceThrow(e);
		}
	}

	public boolean tryEnterWriteLock(int millisecondsTimeout) {
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数，失败了也计数。
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
		return rwLock.isWriteLockedByCurrentThread();
	}

	/**
	 * 根据参数进入读或写锁。
	 * 进入写锁时如果已经获得读锁，会先释放，使用时注意竞争条件。
	 *
	 * @param isWrite Write Lock Need.
	 */
	public void enterLock(boolean isWrite) {
		if (isWrite) {
			// 拥有 readLock 时，再次去锁 writeLock 会死锁，但 java 没有提供手段检测，
			// zeze 需要保证不会发生这种情况。
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
		if (rwLock.isWriteLockedByCurrentThread()) {
			rwLock.writeLock().unlock();
		} else {
			rwLock.readLock().unlock();
		}
		// java 没有提供判断是否拥有读锁的手段，此处不严格检查状态。
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
