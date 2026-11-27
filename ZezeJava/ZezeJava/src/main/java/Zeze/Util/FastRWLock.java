package Zeze.Util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 面向读极多写极少场景的轻量读写锁（竞争时 sleep 忙等），不记录持有者线程、无重入计数。
 * 写等待期间嵌套 {@link #readLock()} 会与等待写者死锁，嵌套读场景应改用 {@link #tryReadLock()}。
 * 等待不可中断，返回时保留中断标志。
 */
public class FastRWLock extends AtomicLong {
	private static final long WRITE_WAIT_FLAG = 0x8000_0000_0000_0000L;
	private static final long WRITE_LOCK_FLAG = 0xc000_0000_0000_0000L;

	private static boolean wait1() {
		try {
			Thread.sleep(1); // 忙等,主要用于竞争不多也不着急的情况
			return false;
		} catch (InterruptedException ignored) {
			return true;
		}
	}

	public boolean tryReadLock() {
		for (; ; ) {
			final long s = get();
			if (s < 0)
				return false;
			if (compareAndSet(s, s + 1))
				return true;
			Thread.onSpinWait();
		}
	}

	public void readLock() {
		boolean interrupted = false;
		try {
			for (; ; ) {
				final long c = get();
				if (c < 0)
					interrupted |= wait1();
				else if (compareAndSet(c, c + 1))
					return;
				else
					Thread.onSpinWait();
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	public void readUnlock() {
		getAndDecrement();
	}

	/**
	 * 等到没有读写锁的时刻返回
	 */
	public void waitLock() {
		boolean interrupted = false;
		try {
			for (; ; ) {
				final long s = get();
				if (s == 0)
					return;
				if (s == WRITE_WAIT_FLAG) {
					if (compareAndSet(s, 0))
						return;
				} else if (s < 0 || compareAndSet(s, s | WRITE_WAIT_FLAG)) { // 如果只有读标记,那么加写等待标记,阻止读锁
					interrupted |= wait1();
					continue;
				}
				Thread.onSpinWait();
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	public void writeLock() {
		boolean interrupted = false;
		try {
			for (; ; ) {
				final long s = get();
				if (s == 0 || s == WRITE_WAIT_FLAG) { // 仅无读者且无写独占时升级（不能掩掉写独占位，否则写-写不互斥）
					if (compareAndSet(s, WRITE_LOCK_FLAG))
						return; // 加写独占标记,阻止其它读写操作
				} else if (s < 0 || compareAndSet(s, s | WRITE_WAIT_FLAG)) { // 如果只有读标记,那么加写等待标记,阻止读锁
					interrupted |= wait1();
					continue;
				}
				Thread.onSpinWait();
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	public void writeUnlock() {
		set(0);
	}
}
