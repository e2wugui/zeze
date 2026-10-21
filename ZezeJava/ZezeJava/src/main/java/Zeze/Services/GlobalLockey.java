package Zeze.Services;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.PessimismLock;

/**
 * 全局锁键：包装GlobalTableKey的Binary，实现PessimismLock；经GlobalLocks分配实际的
 * ReentrantLock与Condition，相同key全局唯一实例。
 */
public final class GlobalLockey implements Comparable<GlobalLockey>, PessimismLock {
	private final Binary globalKey;
	private @Nullable Lock lock;
	private @Nullable Condition cond;

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
		//noinspection DataFlowIssue
		return lock.tryLock();
	}

	@Override
	public void unlock() {
		exit();
	}

	public void enter() {
		//noinspection DataFlowIssue
		lock.lock();
	}

	public void await() throws InterruptedException {
		//noinspection DataFlowIssue
		cond.await();
	}

	public void pulse() {
		//noinspection DataFlowIssue
		cond.signal();
	}

	public void pulseAll() {
		//noinspection DataFlowIssue
		cond.signalAll();
	}

	public void exit() {
		//noinspection DataFlowIssue
		lock.unlock();
	}

	GlobalLockey alloc() {
		lock = new ReentrantLock();
		cond = lock.newCondition();
		return this;
	}

	@Override
	public int compareTo(@Nullable GlobalLockey other) {
		if (other == null)
			return 1; // null always small

		return globalKey.compareTo(other.globalKey);
	}

	@Override
	public int hashCode() {
		return globalKey.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof GlobalLockey)
			return globalKey.equals(((GlobalLockey)obj).globalKey);

		return false;
	}
}
