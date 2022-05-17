package Zeze.Services;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.PessimismLock;

public final class GlobalLockey implements Comparable<GlobalLockey>, PessimismLock {
	private final Binary GlobalKey;
	private Lock lock;
	private Condition cond;

	/**
	 * 相同值的 TableKey 要得到同一个 Lock 引用，必须使用 Locks 查询。
	 * 不要自己构造这个对象。开放出去仅仅为了测试。
	 */
	public GlobalLockey(Binary key) {
		GlobalKey = key;
	}

	public Binary getGlobalKey() {
		return GlobalKey;
	}

	@Override
	public void lock() {
		Enter();
	}

	public boolean tryLock() {
		return lock.tryLock();
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

		return GlobalKey.compareTo(other.GlobalKey);
	}

	@Override
	public int hashCode() {
		return GlobalKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof GlobalLockey)
			return GlobalKey.equals(((GlobalLockey)obj).GlobalKey);

		return false;
	}
}
