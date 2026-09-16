package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import Zeze.Util.DeadlockBreaker;
import Zeze.Util.FastLock;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;

/**
 * FND7-70 回归：DeadlockBreaker 对虚拟线程的 FastLock(AQS) 死锁不可见——JDK21 的
 * findDeadlockedThreads 不检测 VT 的 AQS 死锁，ThreadGroup.enumerate 也不含 VT，
 * 而 Task 默认线程池即虚拟线程，检测器对主要保护对象失效。
 * 修复：FastLock 慢路径登记 等待线程→锁（waitingThreads）并维护 owner，
 * DeadlockBreaker 构建 等待者→持有者 等待图找环，报告并打断。
 * 红锚点：修复前 findDeadlockedThreads 对本用例返回 null（JDK21 实测），
 * findLockWaitDeadlockCycles 不存在，死锁不可检测不可打破。
 */
@Fast
public class TestFnd770DeadlockBreaker {

	/**
	 * 两个虚拟线程用 FastLock 构造真死锁（各持一把、交叉获取第二把）：
	 * 修复后等待环可检测（含两个VT）、detect() 返回 true、FATAL 报告等待环、
	 * interrupt 打断 lockInterruptibly 等待线程使死锁解除。
	 */
	@Test
	public void testVirtualThreadFastLockDeadlockDetectedAndBroken() throws Exception {
		var lockA = new FastLock();
		var lockB = new FastLock();
		var bothHoldFirst = new CountDownLatch(2);
		var interrupted = new AtomicInteger();
		var vt1 = Thread.ofVirtual().name("fnd770-vt1").start(() -> {
			lockA.lock();
			try {
				bothHoldFirst.countDown();
				bothHoldFirst.await();
				lockB.lockInterruptibly(); // 死锁点
				lockB.unlock();
			} catch (InterruptedException e) {
				interrupted.incrementAndGet();
			} finally {
				lockA.unlock();
			}
		});
		var vt2 = Thread.ofVirtual().name("fnd770-vt2").start(() -> {
			lockB.lock();
			try {
				bothHoldFirst.countDown();
				bothHoldFirst.await();
				lockA.lockInterruptibly(); // 死锁点
				lockA.unlock();
			} catch (InterruptedException e) {
				interrupted.incrementAndGet();
			} finally {
				lockB.unlock();
			}
		});
		bothHoldFirst.await();
		// 等两个VT都真正park在对方的锁上（AQS以synchronizer自身为park的blocker）
		waitParkedOn(vt1, lockB);
		waitParkedOn(vt2, lockA);

		// 检测：等待环包含两个VT
		var cycles = DeadlockBreaker.findLockWaitDeadlockCycles();
		var cycle = cycles.stream().filter(c -> c.contains(vt1) && c.contains(vt2)).findFirst().orElse(null);
		assertNotNull(cycle, "必须检测到包含两个虚拟线程的FastLock等待环");
		assertEquals(2, cycle.size(), "两线程环");

		// report + break：detect() 经 FATAL 报告并 interrupt 环成员
		var coreLogger = (Logger)LogManager.getLogger(DeadlockBreaker.class);
		var appender = new RecordingAppender();
		appender.start();
		coreLogger.addAppender(appender);
		boolean detected;
		try {
			detected = invokeDetect();
		} finally {
			coreLogger.removeAppender(appender);
			appender.stop();
		}
		assertTrue(detected, "detect()必须报告VT死锁（修复前返回false）");
		var fatalReported = appender.events.stream()
				.anyMatch(e -> e.getLevel() == Level.FATAL
						&& e.getMessage().getFormattedMessage().contains("FastLock wait cycle"));
		assertTrue(fatalReported, "必须FATAL报告等待环");

		// 打断后 lockInterruptibly 等待线程以 InterruptedException 退出，死锁解除
		vt1.join(5000);
		vt2.join(5000);
		assertFalse(vt1.isAlive(), "死锁应被打断解除");
		assertFalse(vt2.isAlive(), "死锁应被打断解除");
		assertEquals(2, interrupted.get());
	}

	/** 平台线程 synchronized 死锁回归：findDeadlockedThreads 原路径不受修复影响。 */
	@Test
	public void testPlatformSynchronizedDeadlockStillDetected() throws Exception {
		var o1 = new Object();
		var o2 = new Object();
		var bothHoldFirst = new CountDownLatch(2);
		var p1 = Thread.ofPlatform().daemon().name("fnd770-p1").start(() -> {
			synchronized (o1) {
				bothHoldFirst.countDown();
				try {
					bothHoldFirst.await();
				} catch (InterruptedException ignored) {
				}
				synchronized (o2) {
				}
			}
		});
		var p2 = Thread.ofPlatform().daemon().name("fnd770-p2").start(() -> {
			synchronized (o2) {
				bothHoldFirst.countDown();
				try {
					bothHoldFirst.await();
				} catch (InterruptedException ignored) {
				}
				synchronized (o1) {
				}
			}
		});
		bothHoldFirst.await();
		waitState(p1, Thread.State.BLOCKED);
		waitState(p2, Thread.State.BLOCKED);
		assertTrue(invokeDetect(), "平台线程synchronized死锁仍必须被检测");
		// monitor死锁不可被interrupt打破：daemon线程留待JVM退出（无害），也验证VT扫描
		// 路径不误报（本用例无FastLock等待环，detect的true来自平台路径）。
	}

	/** FastLock owner 维护语义：持锁可见、释放清空（死锁检测等待图的基础）。 */
	@Test
	public void testFastLockOwnerTracking() {
		var lock = new FastLock();
		assertNull(lock.getOwner());
		lock.lock();
		try {
			assertEquals(Thread.currentThread(), lock.getOwner());
		} finally {
			lock.unlock();
		}
		assertNull(lock.getOwner());
	}

	/**
	 * 合法锁链（非环偏序）不得误报：A持L1等L2（B持有）、B持L2等L3（C持有）、C持L3不等待。
	 * A同时持有L1并等待L2，覆盖"同一线程持多把锁"——等待图按等待者→持有者建边，
	 * 每线程出边唯一（同一时刻最多等一把），持有多把锁不产生额外边，链尾无出边即无环。
	 * 全部锁均经快路径无竞争获取（waitingThreads无登记），环检测不得因此漏检或误报。
	 */
	@Test
	public void testLegalLockChainNoFalsePositive() throws Exception {
		var lock1 = new FastLock();
		var lock2 = new FastLock();
		var lock3 = new FastLock();
		var aHolds = new CountDownLatch(1);
		var bHolds = new CountDownLatch(1);
		var cHolds = new CountDownLatch(1);
		var releaseC = new CountDownLatch(1);
		var a = Thread.ofPlatform().daemon().name("fnd770-chain-a").start(() -> {
			lock1.lock(); // 快路径无竞争获取，不上登记表
			try {
				aHolds.countDown();
				bHolds.await();
				lock2.lockInterruptibly(); // 等待B持有的L2
				lock2.unlock();
			} catch (InterruptedException ignored) {
			} finally {
				lock1.unlock();
			}
		});
		var b = Thread.ofPlatform().daemon().name("fnd770-chain-b").start(() -> {
			lock2.lock();
			try {
				bHolds.countDown();
				cHolds.await();
				lock3.lockInterruptibly(); // 等待C持有的L3
				lock3.unlock();
			} catch (InterruptedException ignored) {
			} finally {
				lock2.unlock();
			}
		});
		var c = Thread.ofPlatform().daemon().name("fnd770-chain-c").start(() -> {
			lock3.lock();
			try {
				cHolds.countDown();
				releaseC.await(); // C持有L3且不等待任何FastLock：链尾
			} catch (InterruptedException ignored) {
			} finally {
				lock3.unlock();
			}
		});
		try {
			aHolds.await();
			bHolds.await();
			cHolds.await();
			waitParkedOn(a, lock2);
			waitParkedOn(b, lock3);
			// 链成形（A→B→C、C无出边）：不得检出任何包含链成员的"环"
			var cycles = DeadlockBreaker.findLockWaitDeadlockCycles();
			for (var cycle : cycles) {
				assertFalse(cycle.contains(a) || cycle.contains(b) || cycle.contains(c),
						"合法锁链（非环偏序）不得误报为死锁环: " + cycle);
			}
		} finally {
			releaseC.countDown(); // 按序解链：C放L3→B得L3放L2→A得L2
			a.join(5000);
			b.join(5000);
			c.join(5000);
		}
		assertFalse(a.isAlive(), "解链后A必须退出");
		assertFalse(b.isAlive(), "解链后B必须退出");
		assertFalse(c.isAlive(), "解链后C必须退出");
	}

	/**
	 * 纯平台线程FastLock环仍由 findDeadlockedThreads 原路径覆盖（VT等待图路径对其
	 * 跳过以避免重复报告的前提）：两平台线程各快路径持一把锁后交叉获取对方锁。
	 */
	@Test
	public void testPlatformFastLockCycleStillDetected() throws Exception {
		var lockA = new FastLock();
		var lockB = new FastLock();
		var bothHoldFirst = new CountDownLatch(2);
		var p1 = Thread.ofPlatform().daemon().name("fnd770-pf1").start(() -> {
			lockA.lock(); // 快路径获取（owner已在CAS路径维护，供ownable-synchronizer死锁检测）
			try {
				bothHoldFirst.countDown();
				bothHoldFirst.await();
				lockB.lockInterruptibly(); // 死锁点
				lockB.unlock();
			} catch (InterruptedException ignored) {
			} finally {
				lockA.unlock();
			}
		});
		var p2 = Thread.ofPlatform().daemon().name("fnd770-pf2").start(() -> {
			lockB.lock();
			try {
				bothHoldFirst.countDown();
				bothHoldFirst.await();
				lockA.lockInterruptibly(); // 死锁点
				lockA.unlock();
			} catch (InterruptedException ignored) {
			} finally {
				lockB.unlock();
			}
		});
		try {
			bothHoldFirst.await();
			waitParkedOn(p1, lockB);
			waitParkedOn(p2, lockA);
			assertTrue(invokeDetect(), "纯平台FastLock环必须由findDeadlockedThreads路径检测");
		} finally {
			p1.interrupt(); // 无论断言结果，打破环防挂死
			p2.interrupt();
			p1.join(5000);
			p2.join(5000);
		}
		assertFalse(p1.isAlive());
		assertFalse(p2.isAlive());
	}

	/** 最小录制appender：捕获指定logger的事件供断言（log4j-core不带test appender）。 */
	private static final class RecordingAppender extends AbstractAppender {
		private final List<LogEvent> events = new CopyOnWriteArrayList<>();

		private RecordingAppender() {
			//noinspection deprecation
			super("fnd770-recorder", null, null, true);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}

	/** detect()是实例私有方法且不使用Application，反射调用以验证集成路径。 */
	private static boolean invokeDetect() throws Exception {
		var breaker = new DeadlockBreaker(null); // detect()不使用zeze，仅为构造
		var method = DeadlockBreaker.class.getDeclaredMethod("detect");
		method.setAccessible(true);
		return (Boolean)method.invoke(breaker);
	}

	private static void waitParkedOn(Thread t, FastLock lock) throws InterruptedException {
		for (int i = 0; i < 500 && LockSupport.getBlocker(t) != lock; i++)
			Thread.sleep(10);
		assertEquals(lock, LockSupport.getBlocker(t), "线程应park在指定FastLock上");
	}

	private static void waitState(Thread t, Thread.State state) throws InterruptedException {
		for (int i = 0; i < 500 && t.getState() != state; i++)
			Thread.sleep(10);
		assertEquals(state, t.getState(), "线程应到达指定状态");
	}
}
