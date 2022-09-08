package Zeze.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Util.Macro;

public final class Lockey implements Comparable<Lockey> {
	private final TableKey TableKey;
	private ReentrantReadWriteLock rwLock;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 *
	 * @param key table key
	 */
	public Lockey(TableKey key) {
		TableKey = key;
	}

	public TableKey getTableKey() {
		return TableKey;
	}

	/**
	 * 创建真正的锁对象。
	 */
	Lockey Alloc() {
		rwLock = new ReentrantReadWriteLock();
		return this;
	}

	public void EnterReadLock() {
		// if (!rwLock.IsReadLockHeld) // 第一次才计数. java 没有这个，那么每次访问都统计。
		if (Macro.EnableStatistics) {
			TableStatistics.getInstance().GetOrAdd(TableKey.getId()).getReadLockTimes().increment();
		}
		// logger.debug("EnterReadLock {}", TableKey);
		rwLock.readLock().lock();
	}

	public void ExitReadLock() {
		// logger.debug("ExitReadLock {}", TableKey);
		rwLock.readLock().unlock();
	}

	public void EnterWriteLock() {
		if (Macro.EnableStatistics) {
			if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数
				TableStatistics.getInstance().GetOrAdd(TableKey.getId()).getWriteLockTimes().increment();
		}
		// logger.debug("EnterWriteLock {}", TableKey);
		rwLock.writeLock().lock();
	}

	public void ExitWriteLock() {
		// logger.debug("ExitWriteLock {}", TableKey);
		rwLock.writeLock().unlock();
	}

	public boolean TryEnterReadLock(int millisecondsTimeout) {
		// if (!rwLock.IsReadLockHeld) // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
		if (Macro.EnableStatistics) {
			TableStatistics.getInstance().GetOrAdd(TableKey.getId()).getTryReadLockTimes().increment();
		}
		try {
			return rwLock.readLock().tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean TryEnterWriteLock(int millisecondsTimeout) {
		if (Macro.EnableStatistics) {
			if (!rwLock.isWriteLockedByCurrentThread()) // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
				TableStatistics.getInstance().GetOrAdd(TableKey.getId()).getTryWriteLockTimes().increment();
		}
		try {
			return rwLock.writeLock().tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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
	public void EnterLock(boolean isWrite) {
		if (isWrite) {
			/*
			// 需要试试：拥有 readLock 时，再次去锁 writeLock 会死锁，但java没有提供手段检测。
			// zeze需要保证不会发生这种情况。
			if (rwLock.IsReadLockHeld) {
				throw new AbortException("Invalid Lock State.");
			}
			*/
			// logger.debug("EnterLock::EnterWriteLock {}", TableKey);
			EnterWriteLock();
		} else {
			// logger.debug("EnterLock::EnterReadLock {}", TableKey);
			EnterReadLock();
		}
	}

	public void ExitLock() {
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
		return TableKey.compareTo(other.TableKey);
	}

	@Override
	public int hashCode() {
		return TableKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		return obj instanceof Lockey && TableKey.equals(((Lockey)obj).TableKey);
	}
}
