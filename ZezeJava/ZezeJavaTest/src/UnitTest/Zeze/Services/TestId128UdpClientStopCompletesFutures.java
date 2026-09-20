package UnitTest.Zeze.Services;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import Zeze.Services.ServiceManager.Id128UdpClient;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SM1-F3 回归：stop()原来只置running=false/close/interrupt/join，超时检查器只在worker的
 * processTick里跑——worker死后在途FutureNode永无完成者，等待分配的线程（finalCommit链）
 * 无超时永久park。
 * 修复：stop()在worker.join()后遍历currentFuture/tailFuture/pendingRpc，对全部未完成
 * FutureNode setException("client stopped")并清空三表。
 */
@Fast
public class TestId128UdpClientStopCompletesFutures {
	@Test
	public void testStopCompletesInFlightFutures() throws Exception {
		var nextSessionId = new AtomicLong();
		// 指向无服务监听的端口且不启动worker（无超时检查器）：发出的rpc永无响应，
		// 修复前future只能永久park。
		var client = new Id128UdpClient(null, "127.0.0.1", 1, nextSessionId::incrementAndGet);
		var future = client.allocateFuture("global1", 128);
		Assertions.assertFalse(future.isDone());
		Assertions.assertFalse(future.isCompletedExceptionally());

		client.stop();

		Assertions.assertTrue(future.isDone(), "stop必须在worker死后完成全部在途FutureNode");
		var ex = Assertions.assertThrows(CompletionException.class, () -> future.get(1, TimeUnit.SECONDS));
		Assertions.assertTrue(ex.getCause() instanceof IllegalStateException);
		Assertions.assertTrue(String.valueOf(ex.getCause().getMessage()).contains("client stopped"),
				"unexpected cause: " + ex.getCause());
	}
}
