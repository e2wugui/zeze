package Zeze.Util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 适合读极多写极少的场合,也就是需要等锁的场合极少,让读锁的开销最小化,一旦需要等锁则使用sleep忙等,只支持读锁与读锁的重入
 */
public class FastRWLock extends AtomicLong {
	private static final long LOCK_MASK = 0x7fff_ffff_ffff_ffffL;
	private static final long WRITE_WAIT_FLAG = 0x8000_0000_0000_0000L;
	private static final long WRITE_LOCK_FLAG = 0xc000_0000_0000_0000L;

	private static void wait1() {
		try {
			Thread.sleep(1); // 忙等,主要用于竞争不多也不着急的情况
		} catch (InterruptedException ignored) {
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
		for (; ; ) {
			final long c = get();
			if (c < 0)
				wait1();
			else if (compareAndSet(c, c + 1))
				return;
			else
				Thread.onSpinWait();
		}
	}

	public void readUnlock() {
		getAndDecrement();
	}

	/**
	 * 等到没有读写锁的时刻返回
	 */
	public void waitLock() {
		for (; ; ) {
			final long s = get();
			if (s == 0)
				return;
			if (s == WRITE_WAIT_FLAG) {
				if (compareAndSet(s, 0))
					return;
			} else if (s < 0 || compareAndSet(s, s | WRITE_WAIT_FLAG)) { // 如果只有读标记,那么加写等待标记,阻止读锁
				wait1();
				continue;
			}
			Thread.onSpinWait();
		}
	}

	public void writeLock() {
		for (; ; ) {
			final long s = get();
			if ((s & LOCK_MASK) == 0) { // 如果没有读标记和写独占
				if (compareAndSet(s, WRITE_LOCK_FLAG))
					return; // 加写独占标记,阻止其它读写操作
			} else if (s < 0 || compareAndSet(s, s | WRITE_WAIT_FLAG)) { // 如果只有读标记,那么加写等待标记,阻止读锁
				wait1();
				continue;
			}
			Thread.onSpinWait();
		}
	}

	public void writeUnlock() {
		set(0);
	}
}
