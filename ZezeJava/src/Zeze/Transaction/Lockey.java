package Zeze.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Lockey implements java.lang.Comparable<Lockey> {
	
	private TableKey TableKey;
	public TableKey getTableKey() {
		return TableKey;
	}

	private ReentrantReadWriteLock rwLock;

	/** 
	 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 不要自己构造这个对象。开放出去仅仅为了测试。
	 
	 @param key
	*/
	public Lockey(TableKey key) {
		TableKey = key;
	}

	/** 
	 创建真正的锁对象。
	 
	 @return 
	*/
	public Lockey Alloc() {
		//rwLock = new System.Threading.ReaderWriterLockSlim();
		rwLock = new ReentrantReadWriteLock();
		return this;
	}

	public void EnterReadLock() {
		//logger.Debug("EnterReadLock {0}", TableKey);

		var r = rwLock.readLock();

		//if (false == rwLock.IsReadLockHeld) { // 第一次才计数. java 没有这个，那么每次访问都统计。
			TableStatistics.getInstance().GetOrAdd(getTableKey().getName()).getReadLockTimes().incrementAndGet();
		//}

		r.lock();
	}

	public void ExitReadLock() {
		//logger.Debug("ExitReadLock {0}", TableKey);
		rwLock.readLock().unlock();
	}

	public void EnterWriteLock() {
		//logger.Debug("EnterWriteLock {0}", TableKey);

		if (false == rwLock.isWriteLocked()) { // 第一次才计数
			TableStatistics.getInstance().GetOrAdd(getTableKey().getName()).getWriteLockTimes().incrementAndGet();
		}

		var w = rwLock.writeLock();
		w.lock();
	}

	public void ExitWriteLock() {
		//logger.Debug("ExitWriteLock {0}", TableKey);
		
		rwLock.writeLock().unlock();
	}

	public boolean TryEnterReadLock(int millisecondsTimeout) {
		//if (false == rwLock.IsReadLockHeld) { // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
			TableStatistics.getInstance().GetOrAdd(getTableKey().getName()).getTryReadLockTimes().incrementAndGet();
		//}

		try {
			return rwLock.readLock().tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean TryEnterWriteLock(int millisecondsTimeout) {

		if (false == rwLock.isWriteLocked()) { // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
			TableStatistics.getInstance().GetOrAdd(getTableKey().getName()).getTryWriteLockTimes().incrementAndGet();
		}

		try {
			return rwLock.writeLock().tryLock(millisecondsTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isWriteLockHeld() {
		return rwLock.isWriteLocked();
	}

	/** 
	 根据参数进入读或写锁。
	 进入写锁时如果已经获得读锁，会先释放，使用时注意竞争条件。
	 EnterUpgradeableReadLock 看起来不好用，慢慢研究。
	 
	 @param isWrite
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

			//logger.Debug("EnterLock::EnterWriteLock {0}", TableKey);
			EnterWriteLock();
		}
		else {
			//logger.Debug("EnterLock::EnterReadLock {0}", TableKey);
			EnterReadLock();
		}
	}

	public void ExitLock() {
		if (rwLock.isWriteLocked()) {
			//logger.Debug("ExitLock::ExitWriteLock {0}", TableKey);
			rwLock.writeLock().unlock();
		}
		else // if (rwLock.IsReadLockHeld)
		{
			//logger.Debug("ExitLock::ExitReadLock {0}", TableKey);
			rwLock.readLock().unlock();
		}
		/*
		else {
			throw new RuntimeException("no lock held.");
		}
		*/
		// java 没有判断是否拥有读锁，不严格检查了状态了。
	}

	public int compareTo(Lockey other) {
		if (other == null) {
			return 1; // null always small
		}

		return getTableKey().compareTo(other.getTableKey());
	}

	@Override
	public int hashCode() {
		return getTableKey().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		boolean tempVar = obj instanceof Lockey;
		Lockey another = tempVar ? (Lockey)obj : null;
		if (tempVar) {
			return getTableKey().equals(another.getTableKey());
		}

		return false;
	}
}