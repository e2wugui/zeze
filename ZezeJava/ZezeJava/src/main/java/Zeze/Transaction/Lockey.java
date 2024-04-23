package Zeze.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;

public final class Lockey implements Zeze.Util.Lockey<Lockey> {
	private final TableKey tableKey;
	private ReentrantReadWriteLock rwLock;

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
		rwLock = new ReentrantReadWriteLock();
		return this;
	}

	public void enterReadLock() {
		// if (!rwLock.IsReadLockHeld) // 第一次才计数. java 没有这个，那么每次访问都统计。
		PerfCounter.instance.getOrAddTableInfo(tableKey.getId()).readLock.increment();
		// logger.debug("EnterReadLock {}", TableKey);
		var readLock = rwLock.readLock();
		if (readLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitReadLock", tableKey)) {
			readLock.lock();
		}
	}

	public void exitReadLock() {
		// logger.debug("ExitReadLock {}", TableKey);
		rwLock.readLock().unlock();
	}

	public void enterWriteLock() {
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数
			PerfCounter.instance.getOrAddTableInfo(tableKey.getId()).writeLock.increment();
		// logger.debug("EnterWriteLock {}", TableKey);
		var writeLock = rwLock.writeLock();
		if (writeLock.tryLock())
			return;
		try (var ignored = Profiler.begin("LockeyWaitWriteLock", tableKey)) {
			writeLock.lock();
		}
	}

	public void exitWriteLock() {
		// logger.debug("ExitWriteLock {}", TableKey);
		rwLock.writeLock().unlock();
	}

	public boolean tryEnterReadLock(int millisecondsTimeout) {
		// if (!rwLock.IsReadLockHeld) // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
		PerfCounter.instance.getOrAddTableInfo(tableKey.getId()).tryReadLock.increment();
		try {
			var readLock = rwLock.readLock();
			if (millisecondsTimeout > 0)
				return readLock.tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
			return readLock.tryLock();
		} catch (InterruptedException e) {
			Task.forceThrow(e);
			return false; // never run here
		}
	}

	public boolean tryEnterWriteLock(int millisecondsTimeout) {
		if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
			PerfCounter.instance.getOrAddTableInfo(tableKey.getId()).tryWriteLock.increment();
		try {
			var writeLock = rwLock.writeLock();
			if (millisecondsTimeout > 0)
				return writeLock.tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
			return writeLock.tryLock();
		} catch (InterruptedException e) {
			Task.forceThrow(e);
			return false; // never run here
		}
	}

	public boolean isWriteLockHeld() {
		return rwLock.isWriteLockedByCurrentThread();
	}

	/**
	 * 根据参数进入读或写锁。
	 * 进入写锁时如果已经获得读锁，会先释放，使用时注意竞争条件。
	 * EnterUpgradeableReadLock 看起来不好用，慢慢研究。
	 *
	 * @param isWrite Write Lock Need.
	 */
	public void enterLock(boolean isWrite) {
		if (isWrite) {
			/*
			// 需要试试：拥有 readLock 时，再次去锁 writeLock 会死锁，但java没有提供手段检测。
			// zeze需要保证不会发生这种情况。
			if (rwLock.IsReadLockHeld) {
				throw new AbortException("Invalid Lock State.");
			}
			*/
			// logger.debug("EnterLock::EnterWriteLock {}", TableKey);
			enterWriteLock();
		} else {
			// logger.debug("EnterLock::EnterReadLock {}", TableKey);
			enterReadLock();
		}
	}

	public void exitLock() {
		if (rwLock.isWriteLockedByCurrentThread()) {
			// logger.debug("ExitLock::ExitWriteLock {}", TableKey);
			rwLock.writeLock().unlock();
		} else { // if (rwLock.IsReadLockHeld)
			// logger.debug("ExitLock::ExitReadLock {}", TableKey);
			rwLock.readLock().unlock();
		}
		// else throw new IllegalStateException("no lock held.");
		// java 没有判断是否拥有读锁，不严格检查了状态了。
	}

	@Override
	public int compareTo(Lockey other) {
		if (other == null)
			return 1; // null always small
		return tableKey.compareTo(other.tableKey);
	}

	@Override
	public int hashCode() {
		return tableKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		return obj instanceof Lockey && tableKey.equals(((Lockey)obj).tableKey);
	}
}
