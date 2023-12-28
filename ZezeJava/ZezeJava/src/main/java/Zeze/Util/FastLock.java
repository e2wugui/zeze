package Zeze.Util;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// 利用AbstractQueuedSynchronizer实现简单高效的不可重入锁,性能比ReentrantLock略高一点,开启压缩指针时包括对象头共32字节
public class FastLock extends AbstractQueuedSynchronizer {
	public boolean tryLock() {
		return compareAndSetState(0, 1);
	}

	public void lock() {
		acquire(1);
	}

	public void unlock() {
		release(1);
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
}
