package Zeze.Transaction;

import Zeze.*;

public final class Lockey implements java.lang.Comparable<Lockey> {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
	private TableKey TableKey;
	public TableKey getTableKey() {
		return TableKey;
	}
	private System.Threading.ReaderWriterLockSlim rwLock;

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
		rwLock = new System.Threading.ReaderWriterLockSlim(System.Threading.LockRecursionPolicy.SupportsRecursion);
		return this;
	}

	public void EnterReadLock() {
		//logger.Debug("EnterReadLock {0}", TableKey);
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
		if (false == rwLock.IsReadLockHeld) { // 第一次才计数
			TableStatistics.getInstance().GetOrAdd(getTableKey().getTableId()).getReadLockTimes().IncrementAndGet();
		}
//#endif
		rwLock.EnterReadLock();
	}

	public void ExitReadLock() {
		//logger.Debug("ExitReadLock {0}", TableKey);
		rwLock.ExitReadLock();
	}

	public void EnterWriteLock() {
		//logger.Debug("EnterWriteLock {0}", TableKey);
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
		if (false == rwLock.IsWriteLockHeld) { // 第一次才计数
			TableStatistics.getInstance().GetOrAdd(getTableKey().getTableId()).getWriteLockTimes().IncrementAndGet();
		}
//#endif
		rwLock.EnterWriteLock();
	}

	public void ExitWriteLock() {
		//logger.Debug("ExitWriteLock {0}", TableKey);
		rwLock.ExitWriteLock();
	}

	public boolean TryEnterReadLock(int millisecondsTimeout) {
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
		if (false == rwLock.IsReadLockHeld) { // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
			TableStatistics.getInstance().GetOrAdd(getTableKey().getTableId()).getTryReadLockTimes().IncrementAndGet();
		}
//#endif
		return rwLock.TryEnterReadLock(millisecondsTimeout);
	}

	public boolean TryEnterWriteLock(int millisecondsTimeout) {
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if ENABLE_STATISTICS
		if (false == rwLock.IsWriteLockHeld) { // 第一次才计数，即时失败了也计数，根据观察情况再决定采用那种方案。
			TableStatistics.getInstance().GetOrAdd(getTableKey().getTableId()).getTryWriteLockTimes().IncrementAndGet();
		}
//#endif
		return rwLock.TryEnterWriteLock(millisecondsTimeout);
	}

	public boolean isWriteLockHeld() {
		return rwLock.IsWriteLockHeld;
	}

	/** 
	 根据参数进入读或写锁。
	 进入写锁时如果已经获得读锁，会先释放，使用时注意竞争条件。
	 EnterUpgradeableReadLock 看起来不好用，慢慢研究。
	 
	 @param isWrite
	*/
	public void EnterLock(boolean isWrite) {
		if (isWrite) {
			if (rwLock.IsReadLockHeld) {
				throw new AbortException("Invalid Lock State.");
			}

			//logger.Debug("EnterLock::EnterWriteLock {0}", TableKey);
			EnterWriteLock();
		}
		else {
			//logger.Debug("EnterLock::EnterReadLock {0}", TableKey);
			EnterReadLock();
		}
	}

	public void ExitLock() {
		if (rwLock.IsReadLockHeld) {
			//logger.Debug("ExitLock::ExitReadLock {0}", TableKey);
			rwLock.ExitReadLock();
		}
		else if (rwLock.IsWriteLockHeld) {
			//logger.Debug("ExitLock::ExitWriteLock {0}", TableKey);
			rwLock.ExitWriteLock();
		}
		else {
			throw new RuntimeException("no lock held.");
		}

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