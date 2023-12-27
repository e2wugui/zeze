package Zeze.Util;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// 只提供几个方法,效果跟ReentrantLock一致,但去掉了对象包装,相当于ReentrantLock.NonfairSync,开启压缩指针时包括对象头共32字节
public class FastLock extends AbstractQueuedSynchronizer {
	public boolean tryLock() {
		var current = Thread.currentThread();
		int c = getState();
		if (c == 0) {
			if (compareAndSetState(0, 1)) {
				setExclusiveOwnerThread(current);
				return true;
			}
		} else if (getExclusiveOwnerThread() == current) {
			if (++c < 0)
				throw new Error("Maximum lock count exceeded");
			setState(c);
			return true;
		}
		return false;
	}

	public void lock() {
		var current = Thread.currentThread();
		if (compareAndSetState(0, 1))
			setExclusiveOwnerThread(current);
		else if (getExclusiveOwnerThread() == current) {
			int c = getState() + 1;
			if (c < 0)
				throw new Error("Maximum lock count exceeded");
			setState(c);
		} else
			acquire(1);
	}

	public void unlock() {
		release(1);
	}

	@Override
	protected boolean isHeldExclusively() {
		return getExclusiveOwnerThread() == Thread.currentThread();
	}

	@Override
	protected boolean tryAcquire(int acquires) {
		if (getState() == 0 && compareAndSetState(0, acquires)) {
			setExclusiveOwnerThread(Thread.currentThread());
			return true;
		}
		return false;
	}

	@Override
	protected boolean tryRelease(int releases) {
		int c = getState() - releases;
		if (getExclusiveOwnerThread() != Thread.currentThread())
			throw new IllegalMonitorStateException();
		var free = c == 0;
		if (free)
			setExclusiveOwnerThread(null);
		setState(c);
		return free;
	}
}
