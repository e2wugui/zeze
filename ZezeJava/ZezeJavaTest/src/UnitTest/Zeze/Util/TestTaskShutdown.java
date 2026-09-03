package UnitTest.Zeze.Util;

import Zeze.Transaction.DispatchMode;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Task.shutdown/shutdownNow 的幂等与空安全（对应 review FND-U0-6）：
 * 池未初始化或已停机（三静态池字段为 null）时重复停机不得抛 NPE；
 * 停机后的提交/调度路径抛明确 IllegalStateException 而非裸 NPE。
 *
 * Task 的三个池是静态字段，同 JVM 的其他测试可能已初始化。
 * 本类会真实关闭全局池，必须 @Isolated 独占运行（gradle test 为类级并发），
 * 并在 AfterEach 用 tryInitThreadPool 重建（幂等：已初始化时保留现有池），避免污染后续测试类。
 */
@Fast
@Isolated
public class TestTaskShutdown {

	@BeforeEach
	public void before() {
		// 统一起始状态：池已初始化，使第一个 shutdown 走正常停机路径
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务（如 GlobalTimer tick）不会恢复。
		Task.tryInitThreadPool();
	}

	@Test
	public void testShutdownIdempotentWhenPoolsNull() throws Exception {
		// 第一次：正常停机，三池关闭并置 null
		Task.shutdown(10_000);
		// 此后三池为 null（等价于"从未初始化"和"已停机"两种状态），重复停机必须幂等（原来此处 NPE）
		Task.shutdown(10_000);
		Task.shutdownNow(10_000);
		Task.shutdownNow(10_000);
	}

	@Test
	public void testSubmitAfterShutdownThrowsIllegalState() throws Exception {
		Task.shutdown(10_000); // 前置：进入停机后状态（三池 null）
		// 提交/执行/调度各路径必须抛明确 IllegalStateException（原来 null.submit NPE）
		assertThrowsIllegalState("default submit", () -> TaskSpec.ofAction(() -> {
		}).name("submitDefault").submitNow());
		assertThrowsIllegalState("critical submit",
				() -> TaskSpec.ofAction(() -> {
				}).name("submitCritical").dispatchMode(DispatchMode.Critical).submitNow());
		assertThrowsIllegalState("default execute", () -> TaskSpec.ofAction(() -> {
		}).name("executeDefault").runNow());
		assertThrowsIllegalState("schedule", () -> TaskSpec.ofAction(() -> {
		}).name("schedule").scheduleNow(1));
		assertThrowsIllegalState("schedulePeriod", () -> TaskSpec.ofAction(() -> {
		}).name("schedulePeriod").schedulePeriodNow(1, 1));
	}

	private static void assertThrowsIllegalState(String what, Runnable submit) {
		var ex = Assertions.assertThrows(IllegalStateException.class, submit::run);
		Assertions.assertTrue(ex.getMessage().contains("not initialized"),
				what + ": unexpected message: " + ex.getMessage());
	}
}
