package Zeze.Services;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.PessimismLock;

public final class GlobalLockey implements Comparable<GlobalLockey>, PessimismLock {
	private final Binary globalKey;
	private Lock lock;
	private Condition cond;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 */
	public GlobalLockey(Binary key) {
		globalKey = key;
	}

	public Binary getGlobalKey() {
		return globalKey;
	}

	@Override
	public void lock() {
		enter();
	}

	public boolean tryLock() {
		return lock.tryLock();
	}

	@Override
	public void unlock() {
		exit();
	}

	public void enter() {
		lock.lock();
	}

	public void await() throws InterruptedException {
		cond.await();
	}

	public void pulse() {
		cond.signal();
	}

	public void pulseAll() {
		cond.signalAll();
	}

	public void exit() {
		lock.unlock();
	}

	GlobalLockey alloc() {
		lock = new ReentrantLock();
		cond = lock.newCondition();
		return this;
	}

	@Override
	public int compareTo(GlobalLockey other) {
		if (other == null)
			return 1; // null always small

		return globalKey.compareTo(other.globalKey);
	}

	@Override
	public int hashCode() {
		return globalKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof GlobalLockey)
			return globalKey.equals(((GlobalLockey)obj).globalKey);

		return false;
	}
}
