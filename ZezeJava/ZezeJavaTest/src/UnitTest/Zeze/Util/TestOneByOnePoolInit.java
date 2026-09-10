package UnitTest.Zeze.Util;

import java.util.List;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskOneByOneByKey2;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * FND3-14 回归：one-by-one 引擎（锁版经 TaskOneByOneQueue、无锁版 ByKey2）走全局线程池时，
 * 池未初始化/已停机（三静态池字段为 null，等价于"Application 构造前提交"与"停机窗口"）
 * 的提交必须在【入队前】抛带初始化指引的 IllegalStateException——修复前任务先入队、
 * 派发时 NPE：锁版队列非空且再无派发点（后续 submit 全走 size!=1 分支）、无锁版 submitted
 * 恒 true，桶永久卡死，shutdown 的 waitComplete 在 cond 上永等。
 * 本类会真实关闭全局池，@Isolated 独占运行，AfterEach 重建（与 TestTaskShutdown 相同）。
 */
@Fast
@Isolated
public class TestOneByOnePoolInit {

	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务不会恢复。
		Task.tryInitThreadPool();
	}

	@Test
	public void testLockedEngineFailsFastBeforeEnqueue() throws Exception {
		shutdownIgnoringTerminationTimeout();
		var engine = new TaskOneByOneByKey();
		var ex = Assertions.assertThrows(IllegalStateException.class,
				() -> TaskSpec.ofAction(() -> { }).name("t").executeOneByOne(1, engine));
		Assertions.assertTrue(ex.getMessage().contains("not initialized"),
				"unexpected message: " + ex.getMessage());
		// 任务未入队：卡死态不存在，shutdown 的 waitComplete 立即通过（不挂死）。
		engine.shutdown(true);
	}

	@Test
	public void testLockFreeEngineFailsFastBeforeEnqueue() throws Exception {
		shutdownIgnoringTerminationTimeout();
		var engine = new TaskOneByOneByKey2();
		Assertions.assertThrows(IllegalStateException.class,
				() -> engine.executeBatch(List.of(1), k -> { }, () -> { }, null));
	}

	// 同 TestTaskShutdown：短等待关池（先置 null 再等待），终止超时忽略——
	// 用例只依赖"三静态池字段已 null"，不依赖遗留任务全部结束。
	private static void shutdownIgnoringTerminationTimeout() throws InterruptedException {
		try {
			Task.shutdown(200);
		} catch (java.util.concurrent.TimeoutException expected) {
			// 全套件环境下其他测试类遗留的周期任务令终止等待超时，忽略。
		}
	}
}
