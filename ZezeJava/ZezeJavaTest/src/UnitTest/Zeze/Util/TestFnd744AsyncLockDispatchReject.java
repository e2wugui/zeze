package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.Util.AsyncLock;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND7-44 回归：AsyncLock.tryNextAsync 裸用 Task.getThreadPool().execute——池未初始化/
 * 已停机（shutdownPools 先置 null）时 NPE：回调已出队、state==1 且唯一复位点在
 * tryNextAsync 尾部（未到达），后续 enter 的两次 CAS 均失败，该锁永久楔死；
 * NPE 还会从 leave() 逃出 runWithLeave 的 finally，被 executor 吞掉无诊断。
 * 修复：改用 Task.poolOrThrow（明确 ISE）+ 派发失败回滚（回调重新入队、复位 state、
 * 原类型重抛）。
 * 本类会真实关闭全局池，@Isolated 独占运行，AfterEach 重建（与 TestTaskShutdown 相同）。
 */
@Fast
@Isolated
public class TestFnd744AsyncLockDispatchReject {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务不会恢复。
		Task.tryInitThreadPool();
	}

	@Test
	public void testDispatchFailRollsBackAndRecovers() throws Exception {
		shutdownIgnoringTerminationTimeout();

		var lock = new AsyncLock(); // 异步派发模式
		var holderInside = new CountDownLatch(1);
		var cb1AwaitCb2 = new CountDownLatch(1);
		var cb2Ran = new CountDownLatch(1);
		var cb3Ran = new CountDownLatch(1);

		// cb1 快路径占住派发权后先示意再等待；确认 cb1 已在临界区内才启动入队线程，
		// 排除 enqueuer 先赢 CAS 直跑 cb2 的交错（那会让失败发生在 enqueuer 线程上）。
		// cb2 趁窗口入队（只入队不派发）；cb1 返回触发的 leave→tryNextAsync 在停机池上派发失败。
		var thrown = new AtomicReference<Throwable>();
		var holder = new Thread(() -> {
			try {
				lock.enter(() -> {
					holderInside.countDown();
					cb1AwaitCb2.await();
				});
			} catch (Throwable e) {
				thrown.compareAndSet(null, e);
			}
		}, "fnd744-holder");
		holder.setDaemon(true);
		holder.start();
		Assertions.assertTrue(holderInside.await(5, TimeUnit.SECONDS), "holder必须先占住派发权");

		var enqueuer = new Thread(() -> lock.enter(cb2Ran::countDown), "fnd744-enqueuer");
		enqueuer.setDaemon(true);
		enqueuer.start();
		enqueuer.join(5000); // cb2 已入队（此时派发权在 holder 手里，enter 只入队）
		cb1AwaitCb2.countDown();
		holder.join(5000);
		Assertions.assertFalse(holder.isAlive(), "holder必须在5秒内结束");

		var ex = thrown.get();
		Assertions.assertNotNull(ex, "池停机时leave的派发必须明确失败（原为裸NPE静默楔死）");
		Assertions.assertInstanceOf(IllegalStateException.class, ex,
				"必须抛带初始化指引的IllegalStateException而非裸NPE（FND7-44）");

		// 回滚生效：state 复位（修复前恒1，后续enter的CAS永败），cb2 保留在队列。
		Assertions.assertFalse(lock.isLocked(), "派发失败回滚后state不得楔死");

		// 池恢复后新 enter 必须重新驱动：回滚保留的 cb2 与新提交的 cb3 都执行。
		Task.tryInitThreadPool();
		lock.enter(cb3Ran::countDown);
		Assertions.assertTrue(cb2Ran.await(5, TimeUnit.SECONDS), "回滚保留的cb2必须照常执行");
		Assertions.assertTrue(cb3Ran.await(5, TimeUnit.SECONDS), "恢复后新enter的cb3必须照常执行");
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
