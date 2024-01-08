package Zeze.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;

// 利用AbstractQueuedSynchronizer实现简单高效的不可重入锁,性能比ReentrantLock略高一点,开启压缩指针时包括对象头共32字节
public class FastLock extends AbstractQueuedSynchronizer implements Lock {
	@Override
	public boolean tryLock() {
		return compareAndSetState(0, 1);
	}

	@Override
	public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		return tryLock() || tryAcquireNanos(1, unit.toNanos(time));
	}

	@Override
	public void lock() {
		acquire(1);
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		if (!tryLock())
			acquireInterruptibly(1);
	}

	@Override
	public void unlock() {
		release(1);
	}

	@Override
	public @NotNull ConditionObject newCondition() {
		return new ConditionObject();
	}

	@Override
	protected boolean tryAcquire(int acquires) {
		return compareAndSetState(0, acquires);
	}

	@Override
	protected boolean tryRelease(int releases) {
		setState(0);
		return true;
	}

	@Override
	protected boolean isHeldExclusively() {
		return true; // 假定 await/signal 一定在锁内
	}
}
