package UnitTest.Zeze.Util;

import java.util.concurrent.CountDownLatch;

import Zeze.Util.Task;
import Zeze.Util.ThreadDiagnosable;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND7-43 回归：ThreadDiagnosable 的 critical 豁免检查运行在 DiagnoseThread 上，
 * 原先读 Critical.tlCritical.get() 取的是诊断线程自己的 ThreadLocal 副本（恒 null），
 * 而 enterCritical(true) 只设置工作线程的副本——豁免从未生效：
 * 虚拟线程 critical 池（优先级恒 NORM）上的超时任务照样被打断。
 * 修复：Timeout 构造时快照创建线程的 critical 标志，诊断线程据此跳过。
 * <p>
 * R2稳定性加固：@Isolated 独占运行——startDiagnose/stopDiagnose 经全局 currentSerial
 * 互杀诊断线程，与 TestThreadDiagnosable 并行时（套件固定8并发）对方的 stopDiagnose
 * 会杀死本类的诊断线程：对照组"必须被打断"假红（单跑绿）。
 */
@Fast
@Isolated
public class TestFnd743CriticalExempt {
	private boolean savedDisableInterrupt;

	@BeforeEach
	public void before() {
		savedDisableInterrupt = ThreadDiagnosable.disableInterrupt;
		ThreadDiagnosable.disableInterrupt = false;
	}

	@AfterEach
	public void after() {
		ThreadDiagnosable.stopDiagnose(); // 让本测试启动的诊断线程退出
		ThreadDiagnosable.disableInterrupt = savedDisableInterrupt;
	}

	/** 对照组：未标记critical的超时任务必须被打断——证明诊断线程确实在跑，
	 * 防止豁免用例因诊断未启动而假绿。 */
	@Test
	public void testNonCriticalStillInterrupted() throws Exception {
		ThreadDiagnosable.startDiagnose(10);
		var interrupted = runWorker(false);
		Assertions.assertTrue(interrupted, "非critical的超时任务必须被打断（对照组）");
	}

	/** 主用例：enterCritical(true) 临界区内 createTimeout 的工作线程（优先级NORM，
	 * 无优先级兜底保护）超时不得被打断——修复前豁免读错线程的ThreadLocal，恒被打断。 */
	@Test
	public void testCriticalExemptNotInterrupted() throws Exception {
		ThreadDiagnosable.startDiagnose(10);
		var interrupted = runWorker(true);
		Assertions.assertFalse(interrupted, "critical豁免必须生效（FND7-43）");
	}

	/** NORM优先级工作线程内分段睡眠共3s，超时200ms；critical 控制是否 enterCritical(true)。
	 * R2稳定性加固：原先单次sleep(1000)，满载时诊断线程（10ms周期）可能整个1s窗口内
	 * 未获调度——对照组"必须被打断"假红（单跑绿）。分段睡眠把暴露窗口拉宽到~2.8s，
	 * 两个用例窗口一致（对照组同时证明诊断线程确实在跑），join相应放宽。 */
	private static boolean runWorker(boolean critical) throws Exception {
		var interrupted = new CountDownLatch(1);
		var worker = new Thread(null, () -> {
			try {
				if (critical) {
					try (var ignoredCritical = Task.enterCritical(true);
						 var ignoredTimeout = Task.createTimeout(200)) {
						for (int i = 0; i < 6; ++i)
							Thread.sleep(500);
					}
				} else {
					try (var ignoredTimeout = Task.createTimeout(200)) {
						for (int i = 0; i < 6; ++i)
							Thread.sleep(500);
					}
				}
			} catch (InterruptedException e) {
				interrupted.countDown();
			}
		}, "fnd743-worker-" + critical);
		worker.setDaemon(true);
		Assertions.assertEquals(Thread.NORM_PRIORITY, worker.getPriority(),
				"用例前提：NORM优先级线程无优先级兜底，只能靠critical豁免保护");
		worker.start();
		worker.join(10_000);
		Assertions.assertFalse(worker.isAlive(), "worker必须在10秒内结束");
		return interrupted.getCount() == 0;
	}
}
