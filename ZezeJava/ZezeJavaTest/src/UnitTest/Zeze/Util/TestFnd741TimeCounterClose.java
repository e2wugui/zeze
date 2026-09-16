package UnitTest.Zeze.Util;

import java.util.concurrent.Future;
import Zeze.Util.Task;
import Zeze.Util.TimeCounter;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND7-41 回归：TimeCounter 构造器注册的每秒 discard 周期任务句柄曾直接丢弃，
 * 全类无任何取消途径——ProviderSession 每次连接建立即新建 TimeCounter(5)，
 * 会话更替后旧实例的任务仍每秒永久触发并强引用 this，随会话重建无界泄漏。
 * 修复：保存 TimerFuture，实现 AutoCloseable（close 取消任务），
 * 会话终结点（ProviderDirectService.OnSocketClose / LinkdProvider.onProviderClose）关闭。
 */
@Fast
public class TestFnd741TimeCounterClose {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@Test
	public void testCloseCancelsDiscardTask() throws Exception {
		var tc = new TimeCounter(5);
		var timer = discardTimerOf(tc);
		Assertions.assertNotNull(timer, "启用discard任务时必须保存句柄");
		Assertions.assertFalse(timer.isCancelled(), "close前任务不得被取消");

		tc.close();
		Assertions.assertTrue(timer.isCancelled(), "close必须取消discard周期任务（FND7-41）");

		// close只停周期任务，计数器本身仍可用（手动discard照常）。
		tc.increment(1);
		tc.increment(1);
		Assertions.assertEquals(2, tc.count());
		tc.discard(100);
		Assertions.assertEquals(0, tc.count());
	}

	@Test
	public void testDisabledTaskCloseIsNoop() {
		var tc = new TimeCounter(2, false);
		Assertions.assertNull(discardTimerOf(tc), "未启用discard任务时句柄为null");
		Assertions.assertDoesNotThrow(tc::close);
	}

	private static Future<?> discardTimerOf(TimeCounter tc) {
		try {
			var f = TimeCounter.class.getDeclaredField("discardTimer");
			f.setAccessible(true);
			return (Future<?>)f.get(tc);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
