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

@Fast
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
