package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 利用AbstractQueuedSynchronizer实现简单高效的不可重入锁,性能比ReentrantLock略高一点,开启压缩指针时包括对象头共32字节
public class FastLock extends AbstractQueuedSynchronizer implements Lock {
	// FND7-70：锁等待登记表（等待线程→等待的锁）。JDK21没有公开API枚举虚拟线程
	// （ThreadGroup.enumerate与Thread.getAllStackTraces均不含VT），findDeadlockedThreads
	// 也不检测VT的AQS死锁；竞争失败进入慢路径（已是park级开销）的线程在此登记，
	// DeadlockBreaker据此构建等待图。快路径CAS成功零额外开销。
	static final @NotNull ConcurrentHashMap<Thread, FastLock> waitingThreads = new ConcurrentHashMap<>();

	@Override
	public boolean tryLock() {
		if (compareAndSetState(0, 1)) {
			setExclusiveOwnerThread(Thread.currentThread()); // FND7-70：owner供死锁检测
			return true;
		}
		return false;
	}

	@Override
	public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
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
	public @NotNull ConditionObject newCondition() {
		return new ConditionObject();
	}

	/** 当前持有者（AQS继承字段，非重入锁下语义精确）；死锁检测构建等待图用。 */
	public @Nullable Thread getOwner() {
		return getExclusiveOwnerThread();
	}

	@Override
	protected boolean tryAcquire(int acquires) {
		if (compareAndSetState(0, acquires)) {
			setExclusiveOwnerThread(Thread.currentThread()); // FND7-70：owner供死锁检测
			return true;
		}
		return false;
	}

	@Override
	protected boolean tryRelease(int releases) {
		if (getExclusiveOwnerThread() != Thread.currentThread())
			throw new IllegalMonitorStateException(); // 非持有者放锁：失败可见，不静默释放他人持有的锁
		setExclusiveOwnerThread(null); // 先清owner再放锁，避免“锁已空闲但owner还在”的观测窗口
		setState(0);
		return true;
	}

	@Override
	protected boolean isHeldExclusively() {
		// owner判断（FND8-06）：恒true会令未持锁线程的cond.await()/signal()静默走AQS
		// 释放路径，把真正持有者的锁释放掉；改为owner判断后await/signal入口即抛IMSE。
		return getExclusiveOwnerThread() == Thread.currentThread();
	}

	public boolean isHeldByCurrentThread() {
		return getExclusiveOwnerThread() == Thread.currentThread();
	}
}
