package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Util.FastLock;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-06回归：FastLock.isHeldExclusively恒true且tryRelease无持有校验——未持锁线程
 * cond.await()静默走AQS释放路径把真正持有者的锁释放掉（互斥破坏），signal/unlock
 * 同样可被非持有者调用且静默通过。修复：isHeldExclusively改owner判断（await/signal
 * 入口即抛IMSE），tryRelease非持有者抛IMSE（对齐ReentrantLock，先校验后变更状态）。
 */
@Fast
public class TestFnd806FastLockOwnership {

	/** 非持有者unlock必须失败可见且不释放持有者的锁。 */
	@Test
	public void testNonHolderUnlockThrowsAndKeepsLock() throws Exception {
		var lock = new FastLock();
		lock.lock();
		Assertions.assertTrue(lock.isHeldByCurrentThread());

		var thrown = new AtomicReference<Throwable>();
		var other = new Thread(() -> {
			try {
				lock.unlock();
			} catch (Throwable e) {
				thrown.compareAndSet(null, e);
			}
		}, "fnd806-unlocker");
		other.setDaemon(true);
		other.start();
		other.join(5000);
		// 修复前红：静默放锁（无异常），此后他人可获取、原持有者互斥被破坏
		Assertions.assertInstanceOf(IllegalMonitorStateException.class, thrown.get());
		Assertions.assertTrue(lock.isHeldByCurrentThread(), "非持有者unlock不得释放持有者的锁");
		Assertions.assertFalse(other.isAlive());

		// 持有者unlock照常
		lock.unlock();
		Assertions.assertFalse(lock.isHeldByCurrentThread());
		Assertions.assertTrue(lock.tryLock());
		lock.unlock();
	}

	/** 非持有者await/signal必须入口即抛IMSE，不触碰他人锁状态。 */
	@Test
	public void testNonHolderConditionThrows() throws Exception {
		var lock = new FastLock();
		var cond = lock.newCondition();
		lock.lock();

		var awaitThrown = new AtomicReference<Throwable>();
		var awaiter = new Thread(() -> {
			try {
				cond.await();
			} catch (Throwable e) {
				awaitThrown.compareAndSet(null, e);
			}
		}, "fnd806-awaiter");
		awaiter.setDaemon(true);
		awaiter.start();
		awaiter.join(5000);
		// 修复前红：await静默走释放路径——持有者（本线程）的锁被释放
		Assertions.assertInstanceOf(IllegalMonitorStateException.class, awaitThrown.get());
		Assertions.assertTrue(lock.isHeldByCurrentThread(), "非持有者await不得释放持有者的锁");

		var signalThrown = new AtomicReference<Throwable>();
		var signaler = new Thread(() -> {
			try {
				cond.signal();
			} catch (Throwable e) {
				signalThrown.compareAndSet(null, e);
			}
		}, "fnd806-signaler");
		signaler.setDaemon(true);
		signaler.start();
		signaler.join(5000);
		Assertions.assertInstanceOf(IllegalMonitorStateException.class, signalThrown.get());
		Assertions.assertTrue(lock.isHeldByCurrentThread());

		lock.unlock();
		Assertions.assertTrue(lock.tryLock(), "释放后必须可重新获取");
		lock.unlock();
	}

	/** 持有者await/signal照常工作：等待者持锁await原子释放，signal唤醒后重新获取。 */
	@Test
	public void testHolderAwaitSignalStillWorks() throws Exception {
		var lock = new FastLock();
		var cond = lock.newCondition();
		var waiterReady = new CountDownLatch(1); // 等待者已持锁即将await
		var waiterDone = new CountDownLatch(1);

		var waiter = new Thread(() -> {
			lock.lock();
			try {
				waiterReady.countDown();
				cond.await(); // 持锁等待：进入await才释放锁，主线程lock()必在其后
				waiterDone.countDown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				lock.unlock();
			}
		}, "fnd806-waiter");
		waiter.setDaemon(true);
		waiter.start();
		Assertions.assertTrue(waiterReady.await(5, TimeUnit.SECONDS));

		// waiter持锁countDown后进入await才放锁——这里lock()返回即证明await已入队并释放
		lock.lock();
		try {
			Assertions.assertTrue(lock.isHeldByCurrentThread());
			cond.signal();
		} finally {
			lock.unlock();
		}
		Assertions.assertTrue(waiterDone.await(5, TimeUnit.SECONDS), "signal必须唤醒持锁等待者");
		waiter.join(5000);
		Assertions.assertFalse(lock.isHeldByCurrentThread());
	}
}
