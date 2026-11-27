package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import org.jspecify.annotations.Nullable;

/** 基于 AQS 的轻量级不可重入独占锁（开启压缩指针时含对象头 32 字节）。 */
public class FastLock extends AbstractQueuedSynchronizer implements Lock {
	// 慢路径等待登记表（等待线程 → 锁），供 DeadlockBreaker 检测含虚拟线程的死锁环；快路径无开销。
	static final ConcurrentHashMap<Thread, FastLock> waitingThreads = new ConcurrentHashMap<>();

	@Override
	public boolean tryLock() {
		if (compareAndSetState(0, 1)) {
			setExclusiveOwnerThread(Thread.currentThread()); // owner供死锁检测
			return true;
		}
		return false;
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		if (tryLock())
			return true;
		waitingThreads.put(Thread.currentThread(), this);
		try {
			return tryAcquireNanos(1, unit.toNanos(time));
		} finally {
			waitingThreads.remove(Thread.currentThread());
		}
	}

	@Override
	public void lock() {
		if (tryLock())
			return;
		waitingThreads.put(Thread.currentThread(), this);
		try {
			acquire(1);
		} finally {
			waitingThreads.remove(Thread.currentThread());
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		if (tryLock())
			return;
		waitingThreads.put(Thread.currentThread(), this);
		try {
			acquireInterruptibly(1);
		} finally {
			waitingThreads.remove(Thread.currentThread());
		}
	}

	@Override
	public void unlock() {
		release(1);
	}

	@Override
	public ConditionObject newCondition() {
		return new ConditionObject();
	}

	/** 当前持有者（AQS继承字段，非重入锁下语义精确）；死锁检测构建等待图用。 */
	public @Nullable Thread getOwner() {
		return getExclusiveOwnerThread();
	}

	@Override
	protected boolean tryAcquire(int acquires) {
		if (compareAndSetState(0, acquires)) {
			setExclusiveOwnerThread(Thread.currentThread()); // owner供死锁检测
			return true;
		}
		return false;
	}

	@Override
	protected boolean tryRelease(int releases) {
		if (getExclusiveOwnerThread() != Thread.currentThread())
			throw new IllegalMonitorStateException();
		setExclusiveOwnerThread(null); // 先清 owner 再放锁，避免锁已空闲但 owner 仍在的观测窗口
		setState(0);
		return true;
	}

	@Override
	protected boolean isHeldExclusively() {
		// 必须校验持有者，确保未持锁调用 Condition.await/signal 立即抛 IMSE
		return getExclusiveOwnerThread() == Thread.currentThread();
	}

	public boolean isHeldByCurrentThread() {
		return getExclusiveOwnerThread() == Thread.currentThread();
	}
}
