package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.AsyncLock;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 同步派发模式（构造参数 new AsyncLock(true)，取代原系统属性全局开关）：
 * 拿到派发权的线程在 dispatchLoop 内同线程顺序内联执行整个队列，循环独占释放；
 * 回调内部的 leave() 只清 ownerThread/current、不派发。
 * （旧递归版经 回调finally→leave()→tryNextSync 逐个嵌套递归执行等待队列，
 * 每个排队回调净增约3个栈帧，深队列 StackOverflowError，且 SOE 从 leave() 的
 * finally 抛出会吞掉原回调异常、state 滞留1导致锁永久卡死——FND2-U0-4。）
 * <p>
 * 模式为实例级不可变配置：同一实例运行中不存在模式切换，比旧全局 volatile 开关语义更强。
 */
@Fast
public class TestAsyncLockSyncDispatch {

	/** 深等待队列在受限栈线程上派发：递归版每回调嵌套3帧必然 SOE，循环版只需常数帧。 */
	@Test
	public void testDeepQueueNoStackOverflow() throws Exception {
		var lock = new AsyncLock(true);
		int n = 20_000;
		var executed = new AtomicInteger();
		var seq = new int[n];
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		// 256KB 栈：旧递归实现 2万回调×3帧远超栈深；循环实现只需常数帧。
		var holder = new Thread(null, () -> lock.enter(() -> {
			started.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				// ignore
			}
		}), "TestAsyncLockSyncDispatch-holder", 256 * 1024);
		holder.setDaemon(true);
		holder.start();
		started.await();
		for (int i = 0; i < n; i++) {
			int idx = i;
			lock.enter(() -> { // state==1，全部只入队
				seq[executed.getAndIncrement()] = idx;
			});
		}
		Assertions.assertTrue(lock.isLocked());
		release.countDown();
		holder.join(10_000);
		Assertions.assertFalse(holder.isAlive(), "dispatch thread should finish without StackOverflowError");
		Assertions.assertEquals(n, executed.get());
		for (int i = 0; i < n; i++) {
			int idx = i;
			Assertions.assertEquals(idx, seq[idx], () -> "FIFO order broken at " + idx);
		}
		Assertions.assertFalse(lock.isLocked());
	}

	/** 回调内部显式 leave()：不派发不双跑，外层循环继续处理后续回调，最终正确释放。 */
	@Test
	public void testReentrantLeaveInsideCallback() throws Exception {
		var lock = new AsyncLock(true);
		var count = new AtomicInteger();
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var holder = new Thread(null, () -> lock.enter(() -> {
			started.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				// ignore
			}
		}), "TestAsyncLockSyncDispatch-holder2", 256 * 1024);
		holder.setDaemon(true);
		holder.start();
		started.await();
		lock.enter(lock::leave); // 派发循环执行的排队回调：内部显式释放
		lock.enter(count::incrementAndGet);
		lock.enter(count::incrementAndGet);
		release.countDown();
		holder.join(10_000);
		Assertions.assertFalse(holder.isAlive());
		Assertions.assertEquals(2, count.get()); // 各恰好一次
		Assertions.assertFalse(lock.isLocked());
	}
}
