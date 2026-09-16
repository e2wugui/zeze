package UnitTest.Zeze.Util;

import harness.Fast;
import java.util.concurrent.ExecutionException;
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
	public void test() throws InterruptedException, ExecutionException {
		Task.tryInitThreadPool();
		ThreadDiagnosable.disableInterrupt = false;
		ThreadDiagnosable.startDiagnose(10);
		var r = new TaskCompletionSource<Boolean>();
		TaskSpec.ofAction(() -> {
			try (var ignored = Task.createTimeout(500)) {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
				r.setResult(true);
			}
		}).name("TestThreadDiagnosable").run();
		Assertions.assertTrue(r.get());
	}
}
