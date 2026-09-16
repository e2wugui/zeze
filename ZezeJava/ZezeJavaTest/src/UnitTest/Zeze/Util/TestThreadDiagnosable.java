package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import Zeze.Util.ThreadDiagnosable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * R2稳定性加固：@Isolated 独占运行——startDiagnose/stopDiagnose 经全局 currentSerial
 * 互杀诊断线程，与其他同类（如 TestFnd743CriticalExempt）并行时（套件固定8并发）
 * 双方的 stopDiagnose/startDiagnose 会互相杀死对方刚启动的诊断线程，被打断断言假红。
 * <p>
 * R3稳定性加固（对齐 02aab9c3c 判例，仅姊妹测试当时拉宽了窗口）：R2-S 满载实测本类
 * 300s 假红——单次sleep(1000)窗口满载时可能整窗未被诊断线程打断，且 r.get() 无界，
 * 一次打断未达即挂死到套件超时（300s假红的放大器）。worker改6段sleep(500)拉宽暴露
 * 窗口（~2.8s），等待改 r.get(10s) 有界：即使仍未被打断也在10s内以明确的
 * TimeoutException 失败，而非300s挂死。断言语义不变。
 */
@Fast
@Isolated
public class TestThreadDiagnosable {
	private boolean savedDisableInterrupt;

	@BeforeEach
	public void before() {
		// 保存全局开关原值，after 恢复，避免泄漏到并行车道的其他测试类
		savedDisableInterrupt = ThreadDiagnosable.disableInterrupt;
	}

	@AfterEach
	public void after() {
		ThreadDiagnosable.stopDiagnose(); // 让本测试启动的诊断线程退出
		ThreadDiagnosable.disableInterrupt = savedDisableInterrupt;
	}

	@Test
	public void test() throws InterruptedException, TimeoutException {
		Task.tryInitThreadPool();
		ThreadDiagnosable.disableInterrupt = false;
		ThreadDiagnosable.startDiagnose(10);
		var r = new TaskCompletionSource<Boolean>();
		TaskSpec.ofAction(() -> {
			try (var ignored = Task.createTimeout(500)) {
				for (int i = 0; i < 6; ++i) // 分段睡眠：满载时诊断线程（10ms周期）可能整窗未获调度
					Thread.sleep(500);
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
				r.setResult(true);
			}
		}).name("TestThreadDiagnosable").run();
		Assertions.assertTrue(r.get(10, TimeUnit.SECONDS), "worker必须在10秒内被诊断线程打断（未打断=超时失败，不再挂死）");
	}
}
