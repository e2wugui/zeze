package Zeze.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestFastRWLockInterruption {
	@Test
	public void readLockPreservesInterruption() throws Exception {
		var lock = new FastRWLock();
		checkInterruption(lock, lock::readLock, lock::readUnlock);
	}

	@Test
	public void writeLockPreservesInterruption() throws Exception {
		var lock = new FastRWLock();
		checkInterruption(lock, lock::writeLock, lock::writeUnlock);
	}

	@Test
	public void waitLockPreservesInterruption() throws Exception {
		var lock = new FastRWLock();
		checkInterruption(lock, lock::waitLock, () -> { });
	}

	private static void checkInterruption(FastRWLock lock, Runnable acquire, Runnable release) throws Exception {
		lock.writeLock();
		var interruptedAfter = new AtomicBoolean();
		var worker = Thread.ofPlatform().daemon().unstarted(() -> {
			Thread.currentThread().interrupt();
			acquire.run();
			try {
				interruptedAfter.set(Thread.currentThread().isInterrupted());
			} finally {
				release.run();
			}
		});
		worker.start();
		try {
			var started = System.nanoTime();
			boolean waitingObserved = false;
			while (System.nanoTime() - started < TimeUnit.SECONDS.toNanos(5)) {
				if (worker.getState() == Thread.State.TIMED_WAITING) {
					waitingObserved = true;
					break;
				}
				Thread.sleep(1);
			}
			assertTrue(waitingObserved, "等待者必须已进入锁的等待路径");
		} finally {
			lock.writeUnlock();
			worker.join(5000);
		}
		assertFalse(worker.isAlive());
		assertTrue(interruptedAfter.get(), "不可中断的锁等待返回后必须保留原始中断");
	}
}
