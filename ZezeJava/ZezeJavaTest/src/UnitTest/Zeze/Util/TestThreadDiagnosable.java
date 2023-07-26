package UnitTest.Zeze.Util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.ThreadDiagnosable;
import org.junit.Assert;
import org.junit.Test;

public class TestThreadDiagnosable {
	@Test
	public void test() throws InterruptedException, ExecutionException, TimeoutException {
		ThreadDiagnosable.startDiagnose(10);
		var r = new TaskCompletionSource<Boolean>();
		var executor = Executors.newSingleThreadExecutor(ThreadDiagnosable.newFactory("testExecutor"));
		executor.execute(() -> {
			try (var ignored = Task.createTimeout(500)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("Interrupted!");
					r.setResult(true);
				}
			}
		});

		Assert.assertTrue(r.get());
		executor.shutdown();
		//ThreadDiagnosable.stopDiagnose(); // 诊断是全局的，没有处理好多次启动重启，为了不影响其他测试，不做停止。
	}
}
