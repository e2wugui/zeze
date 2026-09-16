package Zeze.Services;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Util.Task;
import harness.Fast;

/**
 * 复审R2回归：周期任务句柄（TimerFuture）的 cancel 在持业务锁时调用与在飞任务体构成 ABBA。
 * Task.schedulePeriodCore 在任务体执行期间持有 TimerFuture 锁（body.call 在 future.lock 内），
 * TimerFuture.cancel 需先取该锁——cancel 天然 join 在飞的一轮任务体。若调用方（如 LoginQueue.stop）
 * 持 allocateLock 期间 cancel，而在飞的分配 tick 体（drainQueue）正持 TimerFuture 锁等 allocateLock，
 * 即互喂死锁：cancel 等 future 锁、tick 体等 allocateLock，双方永久挂起。
 * 修复后：stop 在锁内只捕获句柄并置 null，cancel 在锁外调用（锁序恒为 future.lock→allocateLock，
 * 调用方不再持有第二把锁）。
 * 用可停顿的 drainQueue 覆写把 tick 体精确停在未来锁内、业务锁外的位置，时序全由测试控制。
 * 自包含（Config 无 acceptor/connector，start/stop 不绑端口），标 @Fast。
 */
@Fast
public class TestFnd7R2TimerCancelUnderLockDeadlock {
	/** 把 tick 体停在"已持TimerFuture锁、未取allocateLock"的确定位置。 */
	private static final class StallingLoginQueue extends LoginQueue {
		final CountDownLatch bodyEntered = new CountDownLatch(1);
		final CountDownLatch releaseBody = new CountDownLatch(1);

		StallingLoginQueue() {
			super(new Config(), 2, false);
		}

		@Override
		void drainQueue() throws Exception {
			bodyEntered.countDown();
			//noinspection ResultOfMethodCallIgnored
			releaseBody.await(); // 停在此处：schedulePeriodCore 持 future.lock 跑到本方法内
			super.drainQueue();
		}
	}

	@Test
	@Timeout(60)
	public void testStopMustNotCancelPeriodTimerUnderBusinessLock() throws Exception {
		Task.tryInitThreadPool();
		var lq = new StallingLoginQueue();
		ExecutorService stopExecutor = null;
		try {
			lq.start();
			Assertions.assertTrue(lq.bodyEntered.await(10, TimeUnit.SECONDS),
					"1秒分配tick必须在10s内进入任务体");

			// 此刻tick体持TimerFuture锁停在releaseBody上（未取allocateLock）。
			// 后台启动stop：缺陷形态下stop持allocateLock进入cancel等future.lock；
			// 修复形态下stop在锁外cancel（同样等future.lock，但不持任何tick体需要的锁）。
			stopExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "UnitTest.FND7R2.stop"));
			var stopFuture = stopExecutor.submit(() -> {
				lq.stop();
				return null;
			});
			// 给stop到达cancel留足时间（缺陷/修复两形态下都在此处阻塞等future.lock）
			Thread.sleep(1_000);

			// 放行tick体：修复形态——体取allocateLock（空闲）完成，future.lock释放，cancel返回，stop完成；
			// 缺陷形态——体等allocateLock（stop持有）、cancel等体，死锁不因放行而解除。
			lq.releaseBody.countDown();
			stopFuture.get(10, TimeUnit.SECONDS); // 缺陷形态TimeoutException → 红
		} finally {
			lq.releaseBody.countDown();
			if (stopExecutor != null)
				stopExecutor.shutdownNow();
			// 停机兜底放守护线程且不join：缺陷形态下stop线程已死锁持allocateLock，本线程
			// （测试线程）再调stop会在同一把锁上永久挂起——@Timeout救不了非中断的lock()，
			// 测试线程必须无条件返回（红由上方stopFuture.get的超时断言给出）。
			var cleanup = new Thread(() -> {
				try {
					lq.stop();
				} catch (Exception ignored) {
				}
			}, "UnitTest.FND7R2.cleanup");
			cleanup.setDaemon(true);
			cleanup.start();
		}
	}
}
