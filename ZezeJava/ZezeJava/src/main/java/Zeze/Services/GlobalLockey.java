package Zeze.Services;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;
import Zeze.Raft.RocksRaft.PessimismLock;

public final class GlobalLockey implements Comparable<GlobalLockey>, PessimismLock {
	private final GlobalTableKey GlobalTableKey;
	private Lock lock;
	private Condition cond;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 */
	public GlobalLockey(GlobalTableKey key) {
		GlobalTableKey = key;
	}

	public GlobalTableKey getGlobalTableKey() {
		return GlobalTableKey;
	}

	@Override
	public void lock() {
		Enter();
	}

	@Override
	public void unlock() {
		Exit();
	}

	public void Enter() {
		lock.lock();
	}

	public void Wait() throws InterruptedException {
		cond.await();
	}

	public void Pulse() {
		cond.signal();
	}

	public void PulseAll() {
		cond.signalAll();
	}

	public void Exit() {
		lock.unlock();
	}

	GlobalLockey Alloc() {
		lock = new ReentrantLock();
		cond = lock.newCondition();
		return this;
	}

	@Override
	public int compareTo(GlobalLockey other) {
		if (other == null)
			return 1; // null always small

		return GlobalTableKey.compareTo(other.GlobalTableKey);
	}

	@Override
	public int hashCode() {
		return GlobalTableKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof GlobalLockey)
			return GlobalTableKey.equals(((GlobalLockey)obj).GlobalTableKey);

		return false;
	}
}
