package Zeze.Util;

import java.util.concurrent.CountDownLatch;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 任务看门狗（TimeoutDiagnosable，经 TimeoutManager 秒桶轮子派发）的行为契约：
 * 1. 非critical超时任务在超时+秒桶量化窗口（timeout ~ timeout+1s，另加派发开销）内被打断；
 * 2. 任务结束（close）清除残留中断标志，不毒化池线程上的下一个任务；
 * 3. critical 任务不注册看门狗（create 返回 null），超时不打断。
 * <p>
 * 取代旧 ThreadDiagnosable 扫描线程机制及 TestThreadDiagnosable/TestCriticalExempt。
 * 注意：create 在 Reflect.inDebugMode 下返回 null（调试器豁免），本类断言假设非调试运行（gradle 车道）。
 */
@Fast
public class TestTimeoutDiagnosable {

	/** NORM优先级线程内分段睡眠共~3s，超时200ms；watchdog 在工作线程内创建（thread 捕获语义）。 */
	private static boolean runWorker(boolean critical) throws Exception {
		var interrupted = new CountDownLatch(1);
		var worker = new Thread(null, () -> {
			try (var ignored = TimeoutDiagnosable.create(200, critical)) {
				for (int i = 0; i < 6; ++i)
					Thread.sleep(500);
			} catch (InterruptedException e) {
				interrupted.countDown();
			}
		}, "watchdog-worker-" + critical);
		worker.setDaemon(true);
		Assertions.assertEquals(Thread.NORM_PRIORITY, worker.getPriority(), "用例前提：NORM优先级无兜底");
		worker.start();
		worker.join(10_000);
		Assertions.assertFalse(worker.isAlive(), "worker必须在10秒内结束");
		return interrupted.getCount() == 0;
	}

	/** 对照组兼主用例：非critical超时任务必须被打断（量化窗口 200~1200ms + 派发）。 */
	@Test
	public void testNonCriticalInterrupted() throws Exception {
		Task.tryInitThreadPool();
		Assertions.assertTrue(runWorker(false), "非critical的超时任务必须被打断");
	}

	/** critical 不注册看门狗（创建即 null），超时不得打断。 */
	@Test
	public void testCriticalExemptNotInterrupted() throws Exception {
		Task.tryInitThreadPool();
		Assertions.assertNull(TimeoutDiagnosable.create(200, true), "critical必须不注册看门狗");
		Assertions.assertFalse(runWorker(true), "critical豁免必须生效");
	}

	/** 中断落在纯计算（无可中断操作）上时，close 必须清除标志——否则毒化池线程的下一个任务。 */
	@Test
	public void testCloseClearsInterruptFlag() throws Exception {
		Task.tryInitThreadPool();
		var worker = new Thread(null, () -> {
			try (var ignored = TimeoutDiagnosable.create(200)) {
				// 纯自旋：中断只能落在标志位上，try 退出时 close 负责清除
				//noinspection StatementWithEmptyBody
				while (!Thread.currentThread().isInterrupted()) {
				}
			}
		}, "flag-clear-worker");
		worker.setDaemon(true);
		worker.start();
		worker.join(10_000);
		Assertions.assertFalse(worker.isAlive(), "worker必须在10秒内结束");
		Assertions.assertFalse(worker.isInterrupted(), "close 后不得残留中断标志（毒化池线程）");
	}
}
