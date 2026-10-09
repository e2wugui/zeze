package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Util.AsyncLock;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * 拒绝兜底必须循环驱动：持锁回调期间大量enter排队、线程池停机时，
 * holder的leave→tryNextAsync逐回调递归内联（runWithLeave→leave→tryNextAsync），
 * 深队列栈溢出，且SOE后state==1、owner已清、无人派发——锁永久毒化，
 * 后续enter只入队不执行。修复：内联驱动借用同步dispatchLoop的所有权协议
 * （驱动线程的leave只清owner不派发），每回调固定栈帧。
 * 本类会真实关闭全局池，@Isolated 独占运行，AfterEach 重建。
 */
@Fast
@Isolated
public class TestAsyncLockRejectFallbackDeepQueue {

	private static final int Depth = 100_000;

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		Task.tryInitThreadPool();
	}

	@Test
	public void deepQueueOnRejectedPoolDrivesWithoutRecursion() throws Exception {
		shutdownIgnoringTerminationTimeout();

		var lock = new AsyncLock(); // 异步派发模式
		var holderInside = new CountDownLatch(1);
		var releaseHolder = new CountDownLatch(1);
		var ranCount = new AtomicInteger();

		var holder = new Thread(() -> {
			lock.enter(() -> {
				holderInside.countDown();
				try {
					releaseHolder.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}, "deepqueue-holder");
		holder.setDaemon(true);
		holder.start();
		Assertions.assertTrue(holderInside.await(5, TimeUnit.SECONDS), "holder必须先占住派发权");

		for (int i = 0; i < Depth; i++)
			lock.enter(ranCount::incrementAndGet); // 只入队（派发权在holder手里，CAS必失败）

		releaseHolder.countDown();
		holder.join(10_000);
		Assertions.assertFalse(holder.isAlive(), "holder必须在10秒内结束（深队列递归会SOE）");

		// 全部回调执行完毕：修复前SOE后派发链断在半途，ranCount停在千余
		var deadline = System.currentTimeMillis() + 10_000;
		while (ranCount.get() < Depth && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(10);
		Assertions.assertEquals(Depth, ranCount.get(), "深队列必须全部内联执行完毕（递归SOE会断链）");
		Assertions.assertFalse(lock.isLocked(), "派发链收尾后state必须释放");

		// 池恢复后新enter照常（锁不得毒化）
		Task.tryInitThreadPool();
		var afterRecover = new CountDownLatch(1);
		lock.enter(afterRecover::countDown);
		Assertions.assertTrue(afterRecover.await(5, TimeUnit.SECONDS), "锁不得因SOE永久毒化");
	}

	// 同 TestTaskShutdown：短等待关池（先置 null 再等待），终止超时忽略——
	// 用例只依赖"默认池字段已 null"，不依赖遗留任务全部结束。
	private static void shutdownIgnoringTerminationTimeout() throws InterruptedException {
		try {
			Task.shutdown(200);
		} catch (java.util.concurrent.TimeoutException expected) {
			// 全套件环境下其他测试类遗留的周期任务令终止等待超时，忽略。
		}
	}
}
