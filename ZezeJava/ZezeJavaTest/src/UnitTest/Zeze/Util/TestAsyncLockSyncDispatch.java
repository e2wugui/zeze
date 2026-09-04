package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Util.AsyncLock;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * tryNextSync 改迭代（FND2-U0-4）：
 * 同步模式(opt-in系统属性 AsyncLock.tryNextSync)原先经 回调finally→leave()→tryNextSync
 * 逐个嵌套递归执行等待队列，每个排队回调净增约3个栈帧，深队列 StackOverflowError，
 * 且 SOE 从 leave() 的 finally 抛出会吞掉原回调异常、state 滞留1导致锁永久卡死。
 * 修复后回调在派发循环内迭代执行（每回调固定栈帧），回调内部的 leave() 由
 * "派发中"重入标记直接返回、外层循环继续 poll。
 * <p>
 * tryNextSync 开关为 volatile 非常量字段（static final 是编译期常量、无法运行时切换），
 * 测试直接翻转并在 finally 恢复；两种派发模式共享同一状态机，中途切换语义安全。
 */
@Fast
public class TestAsyncLockSyncDispatch {

	/** 深等待队列在受限栈线程上派发：修复前每回调嵌套3帧必然 SOE。 */
	@Test
	public void testDeepQueueNoStackOverflow() throws Exception {
		var old = AsyncLock.tryNextSync;
		AsyncLock.tryNextSync = true;
		try {
			var lock = new AsyncLock();
			int n = 20_000;
			var executed = new AtomicInteger();
			var seq = new int[n];
			var started = new CountDownLatch(1);
			var release = new CountDownLatch(1);
			// 256KB 栈：旧递归实现 2万回调×3帧远超栈深；迭代实现只需常数帧。
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
		} finally {
			AsyncLock.tryNextSync = old;
		}
	}

	/** 回调内部显式 leave()：不嵌套派发、不双跑，外层循环继续处理后续回调，最终正确释放。 */
	@Test
	public void testReentrantLeaveInsideCallback() throws Exception {
		var old = AsyncLock.tryNextSync;
		AsyncLock.tryNextSync = true;
		try {
			var lock = new AsyncLock();
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
			lock.enter(lock::leave); // 派发循环执行的首个回调：内部显式释放
			lock.enter(count::incrementAndGet);
			lock.enter(count::incrementAndGet);
			release.countDown();
			holder.join(10_000);
			Assertions.assertFalse(holder.isAlive());
			Assertions.assertEquals(2, count.get()); // 各恰好一次
			Assertions.assertFalse(lock.isLocked());
		} finally {
			AsyncLock.tryNextSync = old;
		}
	}
}
